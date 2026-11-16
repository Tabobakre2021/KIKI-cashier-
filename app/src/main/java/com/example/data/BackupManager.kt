package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WalletTransferEntity
import com.example.data.entity.ZReportEntity
import com.example.domain.AuditActionType
import com.example.domain.ExpenseCategory
import com.example.domain.PackagingUnit
import com.example.domain.PricingMode
import com.example.domain.ProductCategory
import com.example.domain.StockMovementType
import com.example.domain.WalletChannel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class BackupRecordInfo(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val createdAt: Long
)

object BackupManager {

    private const val BACKUP_SCHEMA_VERSION = 2
    private const val BACKUP_MAGIC = "KIKI_ERP_BACKUP_V3"
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // Portable AES-256 key so encrypted backups can be transferred and restored across devices
    private val portableAesKey: SecretKeySpec by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("KIKI-POULTRY-CENTER-ERP-PORTABLE-KEY-2026".toByteArray(Charsets.UTF_8))
        SecretKeySpec(digest, "AES")
    }

    fun getBackupDirectory(context: Context): File {
        return File(context.filesDir, "kiki_backups").apply { mkdirs() }
    }

    fun listLocalBackups(context: Context): List<BackupRecordInfo> {
        val dir = getBackupDirectory(context)
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".kikibackup") }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                BackupRecordInfo(
                    fileName = it.name,
                    absolutePath = it.absolutePath,
                    sizeBytes = it.length(),
                    createdAt = it.lastModified()
                )
            } ?: emptyList()
    }

    suspend fun createLocalBackup(
        context: Context,
        db: KikiDatabase,
        actor: UserEntity?,
        isAutomatic: Boolean = false
    ): File {
        val payloadJson = serializeDatabaseToJson(db)
        val sha256 = sha256Hex(payloadJson)
        val encrypted = encryptString(payloadJson)

        val envelope = JSONObject().apply {
            put("magic", BACKUP_MAGIC)
            put("schemaVersion", BACKUP_SCHEMA_VERSION)
            put("createdAt", System.currentTimeMillis())
            put("sha256", sha256)
            put("encryptedPayload", encrypted)
        }

        val prefix = if (isAutomatic) "AUTO" else "MANUAL"
        val file = File(
            getBackupDirectory(context),
            "KIKI_${prefix}_${fileDateFormat.format(Date())}.kikibackup"
        )
        file.writeText(envelope.toString(), Charsets.UTF_8)

        if (actor != null) {
            db.auditLogDao().insert(
                AuditLogEntity(
                    actionType = AuditActionType.BACKUP_CREATED,
                    entityType = "BACKUP",
                    entityId = file.name,
                    summary = "إنشاء نسخة احتياطية (${if (isAutomatic) "تلقائية" else "يدوية"}): ${file.name}",
                    userId = actor.id,
                    userName = actor.displayName
                )
            )
        }
        return file
    }

    suspend fun exportBackupToUri(context: Context, db: KikiDatabase, uri: Uri, actor: UserEntity) {
        val file = createLocalBackup(context, db, actor, isAutomatic = false)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(file.readBytes())
        } ?: throw IllegalStateException("تعذر الكتابة إلى المسار المحدد")
    }

    suspend fun restoreFromLocalFile(
        db: KikiDatabase,
        file: File,
        actor: UserEntity
    ): Result<String> = restoreFromRawJson(db, file.readText(Charsets.UTF_8), actor, file.name)

    suspend fun restoreFromUri(
        context: Context,
        db: KikiDatabase,
        uri: Uri,
        actor: UserEntity
    ): Result<String> {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
            it.readText()
        } ?: return Result.failure(IllegalStateException("تعذر قراءة ملف النسخة الاحتياطية"))
        return restoreFromRawJson(db, raw, actor, uri.lastPathSegment ?: "backup_file")
    }

    private suspend fun restoreFromRawJson(
        db: KikiDatabase,
        envelopeRaw: String,
        actor: UserEntity,
        sourceName: String
    ): Result<String> = runCatching {
        val envelope = JSONObject(envelopeRaw)
        if (envelope.optString("magic") != BACKUP_MAGIC) {
            throw IllegalArgumentException("ملف النسخة الاحتياطية غير صالح أو غير مدعوم")
        }
        val expectedHash = envelope.getString("sha256")
        val decryptedJson = decryptString(envelope.getString("encryptedPayload"))
        val actualHash = sha256Hex(decryptedJson)
        if (expectedHash != actualHash) {
            throw SecurityException("فشل التحقق من سلامة النسخة الاحتياطية (تلف أو تعديل في الملف)")
        }

        val root = JSONObject(decryptedJson)
        applyJsonToDatabase(db, root, actor, sourceName)
        "تم التحقق من سلامة النسخة واستعادة كافة البيانات بنجاح"
    }

    private suspend fun serializeDatabaseToJson(db: KikiDatabase): String {
        val root = JSONObject()
        root.put("products", JSONArray().apply {
            db.productDao().getAllOnce().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category.name)
                    put("defaultUnit", p.defaultUnit.name)
                    put("costPrice", p.costPrice)
                    put("lastPurchasePrice", p.lastPurchasePrice)
                    put("averageCost", p.averageCost)
                    put("profitMarginPercent", p.profitMarginPercent)
                    put("pricingMode", p.pricingMode.name)
                    put("sellingPrice", p.sellingPrice)
                    put("stockQuantity", p.stockQuantity)
                    put("lowStockThreshold", p.lowStockThreshold)
                    put("unitConversionFactor", p.unitConversionFactor)
                    put("sortOrder", p.sortOrder)
                    put("updatedAt", p.updatedAt)
                })
            }
        })

        val salesWithItems = db.saleDao().getAllWithItemsOnce()
        root.put("sales", JSONArray().apply {
            salesWithItems.forEach { sw ->
                val s = sw.sale
                put(JSONObject().apply {
                    put("id", s.id)
                    put("invoiceNumber", s.invoiceNumber)
                    put("subtotalAmount", s.subtotalAmount)
                    put("discountAmount", s.discountAmount)
                    put("totalAmount", s.totalAmount)
                    put("paidAmount", s.paidAmount)
                    put("remainingDebt", s.remainingDebt)
                    put("totalCost", s.totalCost)
                    put("totalProfit", s.totalProfit)
                    put("paymentSummary", s.paymentSummary)
                    put("receiptNote", s.receiptNote)
                    put("customerId", s.customerId ?: JSONObject.NULL)
                    put("customerName", s.customerName)
                    put("cashierUserId", s.cashierUserId)
                    put("cashierName", s.cashierName)
                    put("isVoided", s.isVoided)
                    put("timestamp", s.timestamp)
                    put("items", JSONArray().apply {
                        sw.items.forEach { it ->
                            put(JSONObject().apply {
                                put("id", it.id)
                                put("saleId", it.saleId)
                                put("productId", it.productId)
                                put("productName", it.productName)
                                put("unit", it.unit.name)
                                put("quantity", it.quantity)
                                put("sellingPriceAtSale", it.sellingPriceAtSale)
                                put("costPriceAtSale", it.costPriceAtSale)
                                put("lineTotal", it.lineTotal)
                                put("lineProfit", it.lineProfit)
                            })
                        }
                    })
                })
            }
        })

        root.put("purchases", JSONArray().apply {
            db.purchaseDao().getAllOnce().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("invoiceNumber", p.invoiceNumber)
                    put("productId", p.productId)
                    put("productName", p.productName)
                    put("supplierId", p.supplierId ?: JSONObject.NULL)
                    put("supplierName", p.supplierName)
                    put("unit", p.unit.name)
                    put("quantity", p.quantity)
                    put("unitCostPrice", p.unitCostPrice)
                    put("discountAmount", p.discountAmount)
                    put("transportCost", p.transportCost)
                    put("totalCost", p.totalCost)
                    put("paidAmount", p.paidAmount)
                    put("remainingDebt", p.remainingDebt)
                    put("paymentSummary", p.paymentSummary)
                    put("receiptNote", p.receiptNote)
                    put("recordedByUserId", p.recordedByUserId)
                    put("recordedByName", p.recordedByName)
                    put("timestamp", p.timestamp)
                })
            }
        })

        root.put("expenses", JSONArray().apply {
            db.expenseDao().getAllOnce().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("expenseNumber", e.expenseNumber)
                    put("title", e.title)
                    put("category", e.category.name)
                    put("description", e.description)
                    put("amount", e.amount)
                    put("paymentSummary", e.paymentSummary)
                    put("receiptNote", e.receiptNote)
                    put("recordedByUserId", e.recordedByUserId)
                    put("recordedByName", e.recordedByName)
                    put("timestamp", e.timestamp)
                })
            }
        })

        root.put("allocations", JSONArray().apply {
            db.paymentAllocationDao().getAllOnce().forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("referenceType", a.referenceType)
                    put("referenceId", a.referenceId)
                    put("channel", a.channel.name)
                    put("amount", a.amount)
                    put("receiptNumber", a.receiptNumber)
                    put("operationNumber", a.operationNumber)
                    put("note", a.note)
                    put("timestamp", a.timestamp)
                })
            }
        })

        root.put("stockMovements", JSONArray().apply {
            db.stockMovementDao().getAllOnce().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("productId", m.productId)
                    put("productName", m.productName)
                    put("movementType", m.movementType.name)
                    put("unit", m.unit.name)
                    put("quantityDelta", m.quantityDelta)
                    put("quantityBefore", m.quantityBefore)
                    put("quantityAfter", m.quantityAfter)
                    put("unitCostAtMovement", m.unitCostAtMovement)
                    put("referenceCode", m.referenceCode)
                    put("reason", m.reason)
                    put("userName", m.userName)
                    put("timestamp", m.timestamp)
                })
            }
        })

        root.put("returns", JSONArray().apply {
            db.returnDao().getAllOnce().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("returnCode", r.returnCode)
                    put("returnType", r.returnType)
                    put("saleId", r.saleId ?: JSONObject.NULL)
                    put("saleInvoiceNumber", r.saleInvoiceNumber)
                    put("productId", r.productId)
                    put("productName", r.productName)
                    put("unit", r.unit.name)
                    put("quantity", r.quantity)
                    put("unitCost", r.unitCost)
                    put("refundAmount", r.refundAmount)
                    put("reversedCostAmount", r.reversedCostAmount)
                    put("walletChannel", r.walletChannel.name)
                    put("restockEligible", r.restockEligible)
                    put("reason", r.reason)
                    put("userName", r.userName)
                    put("timestamp", r.timestamp)
                })
            }
        })

        root.put("suppliers", JSONArray().apply {
            db.partyDao().getSuppliersOnce().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("phone", s.phone)
                    put("notes", s.notes); put("totalPurchased", s.totalPurchased)
                    put("totalPaid", s.totalPaid); put("balanceDue", s.balanceDue); put("createdAt", s.createdAt)
                })
            }
        })

        root.put("customers", JSONArray().apply {
            db.partyDao().getCustomersOnce().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("phone", c.phone)
                    put("notes", c.notes); put("totalCreditSales", c.totalCreditSales)
                    put("totalPaid", c.totalPaid); put("balanceDue", c.balanceDue); put("createdAt", c.createdAt)
                })
            }
        })

        root.put("debtPayments", JSONArray().apply {
            db.partyDao().getDebtPaymentsOnce().forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("partyType", d.partyType); put("partyId", d.partyId)
                    put("partyName", d.partyName); put("amount", d.amount); put("walletChannel", d.walletChannel.name)
                    put("receiptNote", d.receiptNote); put("userName", d.userName); put("timestamp", d.timestamp)
                })
            }
        })

        root.put("walletTransfers", JSONArray().apply {
            db.walletTransferDao().getAllOnce().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("fromChannel", t.fromChannel.name); put("toChannel", t.toChannel.name)
                    put("amount", t.amount); put("referenceNote", t.referenceNote)
                    put("userName", t.userName); put("timestamp", t.timestamp)
                })
            }
        })

        root.put("users", JSONArray().apply {
            db.userDao().getAllOnce().forEach { u ->
                put(JSONObject().apply {
                    put("id", u.id); put("displayName", u.displayName); put("roleTitle", u.roleTitle)
                    put("pinCode", u.pinCode); put("mustChangePin", u.mustChangePin); put("isAdmin", u.isAdmin)
                    put("canRecordSales", u.canRecordSales); put("canRecordPurchases", u.canRecordPurchases)
                    put("canRecordExpenses", u.canRecordExpenses); put("canEditPrices", u.canEditPrices)
                    put("canAddProducts", u.canAddProducts); put("canEditProducts", u.canEditProducts)
                    put("canReverseOrDelete", u.canReverseOrDelete); put("canViewInventory", u.canViewInventory)
                    put("canManageParties", u.canManageParties); put("canViewSalesReports", u.canViewSalesReports)
                    put("canViewZReport", u.canViewZReport); put("canExportData", u.canExportData)
                    put("canManageUsers", u.canManageUsers); put("canReopenZReport", u.canReopenZReport)
                    put("canManageBackup", u.canManageBackup); put("canViewProfits", u.canViewProfits)
                    put("isActive", u.isActive); put("createdAt", u.createdAt)
                })
            }
        })

        root.put("zReports", JSONArray().apply {
            db.zReportDao().getAllOnce().forEach { z ->
                put(JSONObject().apply {
                    put("id", z.id); put("dateLabel", z.dateLabel)
                    put("openingCash", z.openingCash); put("openingBankak", z.openingBankak)
                    put("openingFawry", z.openingFawry); put("openingOcash", z.openingOcash); put("openingSahil", z.openingSahil)
                    put("expectedCash", z.expectedCash); put("actualCash", z.actualCash)
                    put("expectedBankak", z.expectedBankak); put("actualBankak", z.actualBankak)
                    put("expectedFawry", z.expectedFawry); put("actualFawry", z.actualFawry)
                    put("expectedOcash", z.expectedOcash); put("actualOcash", z.actualOcash)
                    put("expectedSahil", z.expectedSahil); put("actualSahil", z.actualSahil)
                    put("totalVariance", z.totalVariance); put("notes", z.notes)
                    put("isClosed", z.isClosed); put("isReopened", z.isReopened); put("reopenReason", z.reopenReason)
                    put("closedByUserId", z.closedByUserId); put("closedByName", z.closedByName); put("timestamp", z.timestamp)
                })
            }
        })

        return root.toString()
    }

    private suspend fun applyJsonToDatabase(
        db: KikiDatabase,
        root: JSONObject,
        actor: UserEntity,
        sourceName: String
    ) {
        db.withTransaction {
            db.clearAllTables()

            val productsArr = root.optJSONArray("products") ?: JSONArray()
            val products = (0 until productsArr.length()).map { idx ->
                val o = productsArr.getJSONObject(idx)
                ProductEntity(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    category = ProductCategory.valueOf(o.getString("category")),
                    defaultUnit = PackagingUnit.valueOf(o.getString("defaultUnit")),
                    costPrice = o.getLong("costPrice"),
                    lastPurchasePrice = o.getLong("lastPurchasePrice"),
                    averageCost = o.getLong("averageCost"),
                    profitMarginPercent = o.getDouble("profitMarginPercent"),
                    pricingMode = PricingMode.valueOf(o.getString("pricingMode")),
                    sellingPrice = o.getLong("sellingPrice"),
                    stockQuantity = o.getDouble("stockQuantity"),
                    lowStockThreshold = o.getDouble("lowStockThreshold"),
                    unitConversionFactor = o.optDouble("unitConversionFactor", 1.0),
                    sortOrder = o.optInt("sortOrder", 0),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }
            db.productDao().insertAll(products)

            val usersArr = root.optJSONArray("users") ?: JSONArray()
            val users = (0 until usersArr.length()).map { idx ->
                val o = usersArr.getJSONObject(idx)
                UserEntity(
                    id = o.getLong("id"),
                    displayName = o.getString("displayName"),
                    roleTitle = o.getString("roleTitle"),
                    pinCode = o.getString("pinCode"),
                    mustChangePin = o.optBoolean("mustChangePin", false),
                    isAdmin = o.optBoolean("isAdmin", false),
                    canRecordSales = o.optBoolean("canRecordSales", true),
                    canRecordPurchases = o.optBoolean("canRecordPurchases", false),
                    canRecordExpenses = o.optBoolean("canRecordExpenses", false),
                    canEditPrices = o.optBoolean("canEditPrices", false),
                    canAddProducts = o.optBoolean("canAddProducts", false),
                    canEditProducts = o.optBoolean("canEditProducts", false),
                    canReverseOrDelete = o.optBoolean("canReverseOrDelete", false),
                    canViewInventory = o.optBoolean("canViewInventory", true),
                    canManageParties = o.optBoolean("canManageParties", false),
                    canViewSalesReports = o.optBoolean("canViewSalesReports", false),
                    canViewZReport = o.optBoolean("canViewZReport", false),
                    canExportData = o.optBoolean("canExportData", false),
                    canManageUsers = o.optBoolean("canManageUsers", false),
                    canReopenZReport = o.optBoolean("canReopenZReport", false),
                    canManageBackup = o.optBoolean("canManageBackup", false),
                    canViewProfits = o.optBoolean("canViewProfits", false),
                    isActive = o.optBoolean("isActive", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            }
            db.userDao().insertAll(users)

            val salesArr = root.optJSONArray("sales") ?: JSONArray()
            for (i in 0 until salesArr.length()) {
                val o = salesArr.getJSONObject(i)
                val sale = SaleEntity(
                    id = o.getLong("id"),
                    invoiceNumber = o.getString("invoiceNumber"),
                    subtotalAmount = o.getLong("subtotalAmount"),
                    discountAmount = o.getLong("discountAmount"),
                    totalAmount = o.getLong("totalAmount"),
                    paidAmount = o.getLong("paidAmount"),
                    remainingDebt = o.getLong("remainingDebt"),
                    totalCost = o.getLong("totalCost"),
                    totalProfit = o.getLong("totalProfit"),
                    paymentSummary = o.getString("paymentSummary"),
                    receiptNote = o.optString("receiptNote", ""),
                    customerId = if (o.isNull("customerId")) null else o.getLong("customerId"),
                    customerName = o.optString("customerName", ""),
                    cashierUserId = o.getLong("cashierUserId"),
                    cashierName = o.getString("cashierName"),
                    isVoided = o.optBoolean("isVoided", false),
                    timestamp = o.getLong("timestamp")
                )
                val itemsArr = o.getJSONArray("items")
                val items = (0 until itemsArr.length()).map { j ->
                    val io = itemsArr.getJSONObject(j)
                    SaleItemEntity(
                        id = io.getLong("id"),
                        saleId = io.getLong("saleId"),
                        productId = io.getLong("productId"),
                        productName = io.getString("productName"),
                        unit = PackagingUnit.valueOf(io.getString("unit")),
                        quantity = io.getDouble("quantity"),
                        sellingPriceAtSale = io.getLong("sellingPriceAtSale"),
                        costPriceAtSale = io.getLong("costPriceAtSale"),
                        lineTotal = io.getLong("lineTotal"),
                        lineProfit = io.getLong("lineProfit")
                    )
                }
                db.saleDao().insertSale(sale)
                db.saleDao().insertSaleItems(items)
            }

            val purchasesArr = root.optJSONArray("purchases") ?: JSONArray()
            val purchases = (0 until purchasesArr.length()).map { i ->
                val o = purchasesArr.getJSONObject(i)
                PurchaseEntity(
                    id = o.getLong("id"),
                    invoiceNumber = o.getString("invoiceNumber"),
                    productId = o.getLong("productId"),
                    productName = o.getString("productName"),
                    supplierId = if (o.isNull("supplierId")) null else o.getLong("supplierId"),
                    supplierName = o.optString("supplierName", ""),
                    unit = PackagingUnit.valueOf(o.getString("unit")),
                    quantity = o.getDouble("quantity"),
                    unitCostPrice = o.getLong("unitCostPrice"),
                    discountAmount = o.getLong("discountAmount"),
                    transportCost = o.getLong("transportCost"),
                    totalCost = o.getLong("totalCost"),
                    paidAmount = o.getLong("paidAmount"),
                    remainingDebt = o.getLong("remainingDebt"),
                    paymentSummary = o.getString("paymentSummary"),
                    receiptNote = o.optString("receiptNote", ""),
                    recordedByUserId = o.getLong("recordedByUserId"),
                    recordedByName = o.getString("recordedByName"),
                    timestamp = o.getLong("timestamp")
                )
            }
            db.purchaseDao().insertAll(purchases)

            val expensesArr = root.optJSONArray("expenses") ?: JSONArray()
            val expenses = (0 until expensesArr.length()).map { i ->
                val o = expensesArr.getJSONObject(i)
                ExpenseEntity(
                    id = o.getLong("id"),
                    expenseNumber = o.getString("expenseNumber"),
                    title = o.getString("title"),
                    category = ExpenseCategory.valueOf(o.getString("category")),
                    description = o.optString("description", ""),
                    amount = o.getLong("amount"),
                    paymentSummary = o.getString("paymentSummary"),
                    receiptNote = o.optString("receiptNote", ""),
                    recordedByUserId = o.getLong("recordedByUserId"),
                    recordedByName = o.getString("recordedByName"),
                    timestamp = o.getLong("timestamp")
                )
            }
            db.expenseDao().insertAll(expenses)

            val allocArr = root.optJSONArray("allocations") ?: JSONArray()
            val allocs = (0 until allocArr.length()).map { i ->
                val o = allocArr.getJSONObject(i)
                PaymentAllocationEntity(
                    id = o.getLong("id"),
                    referenceType = o.getString("referenceType"),
                    referenceId = o.getLong("referenceId"),
                    channel = WalletChannel.valueOf(o.getString("channel")),
                    amount = o.getLong("amount"),
                    receiptNumber = o.optString("receiptNumber", ""),
                    operationNumber = o.optString("operationNumber", ""),
                    note = o.optString("note", ""),
                    timestamp = o.getLong("timestamp")
                )
            }
            db.paymentAllocationDao().insertAll(allocs)

            val movArr = root.optJSONArray("stockMovements") ?: JSONArray()
            val movs = (0 until movArr.length()).map { i ->
                val o = movArr.getJSONObject(i)
                StockMovementEntity(
                    id = o.getLong("id"),
                    productId = o.getLong("productId"),
                    productName = o.getString("productName"),
                    movementType = StockMovementType.valueOf(o.getString("movementType")),
                    unit = PackagingUnit.valueOf(o.getString("unit")),
                    quantityDelta = o.getDouble("quantityDelta"),
                    quantityBefore = o.getDouble("quantityBefore"),
                    quantityAfter = o.getDouble("quantityAfter"),
                    unitCostAtMovement = o.getLong("unitCostAtMovement"),
                    referenceCode = o.optString("referenceCode", ""),
                    reason = o.optString("reason", ""),
                    userName = o.getString("userName"),
                    timestamp = o.getLong("timestamp")
                )
            }
            db.stockMovementDao().insertAll(movs)

            val retArr = root.optJSONArray("returns") ?: JSONArray()
            val rets = (0 until retArr.length()).map { i ->
                val o = retArr.getJSONObject(i)
                ReturnEntity(
                    id = o.getLong("id"),
                    returnCode = o.getString("returnCode"),
                    returnType = o.getString("returnType"),
                    saleId = if (o.isNull("saleId")) null else o.getLong("saleId"),
                    saleInvoiceNumber = o.optString("saleInvoiceNumber", ""),
                    productId = o.getLong("productId"),
                    productName = o.getString("productName"),
                    unit = PackagingUnit.valueOf(o.getString("unit")),
                    quantity = o.getDouble("quantity"),
                    unitCost = o.getLong("unitCost"),
                    refundAmount = o.getLong("refundAmount"),
                    reversedCostAmount = o.getLong("reversedCostAmount"),
                    walletChannel = WalletChannel.valueOf(o.getString("walletChannel")),
                    restockEligible = o.optBoolean("restockEligible", true),
                    reason = o.optString("reason", ""),
                    userName = o.getString("userName"),
                    timestamp = o.getLong("timestamp")
                )
            }
            db.returnDao().insertAll(rets)

            val supArr = root.optJSONArray("suppliers") ?: JSONArray()
            db.partyDao().insertAllSuppliers((0 until supArr.length()).map { i ->
                val o = supArr.getJSONObject(i)
                SupplierEntity(
                    id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone", ""),
                    notes = o.optString("notes", ""), totalPurchased = o.getLong("totalPurchased"),
                    totalPaid = o.getLong("totalPaid"), balanceDue = o.getLong("balanceDue"), createdAt = o.getLong("createdAt")
                )
            })

            val custArr = root.optJSONArray("customers") ?: JSONArray()
            db.partyDao().insertAllCustomers((0 until custArr.length()).map { i ->
                val o = custArr.getJSONObject(i)
                CustomerEntity(
                    id = o.getLong("id"), name = o.getString("name"), phone = o.optString("phone", ""),
                    notes = o.optString("notes", ""), totalCreditSales = o.getLong("totalCreditSales"),
                    totalPaid = o.getLong("totalPaid"), balanceDue = o.getLong("balanceDue"), createdAt = o.getLong("createdAt")
                )
            })

            val dpArr = root.optJSONArray("debtPayments") ?: JSONArray()
            db.partyDao().insertAllDebtPayments((0 until dpArr.length()).map { i ->
                val o = dpArr.getJSONObject(i)
                DebtPaymentEntity(
                    id = o.getLong("id"), partyType = o.getString("partyType"), partyId = o.getLong("partyId"),
                    partyName = o.getString("partyName"), amount = o.getLong("amount"),
                    walletChannel = WalletChannel.valueOf(o.getString("walletChannel")),
                    receiptNote = o.optString("receiptNote", ""), userName = o.getString("userName"), timestamp = o.getLong("timestamp")
                )
            })

            val trArr = root.optJSONArray("walletTransfers") ?: JSONArray()
            db.walletTransferDao().insertAll((0 until trArr.length()).map { i ->
                val o = trArr.getJSONObject(i)
                WalletTransferEntity(
                    id = o.getLong("id"),
                    fromChannel = WalletChannel.valueOf(o.getString("fromChannel")),
                    toChannel = WalletChannel.valueOf(o.getString("toChannel")),
                    amount = o.getLong("amount"),
                    referenceNote = o.optString("referenceNote", ""),
                    userName = o.getString("userName"),
                    timestamp = o.getLong("timestamp")
                )
            })

            val zArr = root.optJSONArray("zReports") ?: JSONArray()
            db.zReportDao().insertAll((0 until zArr.length()).map { i ->
                val o = zArr.getJSONObject(i)
                ZReportEntity(
                    id = o.getLong("id"), dateLabel = o.getString("dateLabel"),
                    openingCash = o.getLong("openingCash"), openingBankak = o.getLong("openingBankak"),
                    openingFawry = o.getLong("openingFawry"), openingOcash = o.getLong("openingOcash"),
                    openingSahil = o.getLong("openingSahil"), expectedCash = o.getLong("expectedCash"),
                    actualCash = o.getLong("actualCash"), expectedBankak = o.getLong("expectedBankak"),
                    actualBankak = o.getLong("actualBankak"), expectedFawry = o.getLong("expectedFawry"),
                    actualFawry = o.getLong("actualFawry"), expectedOcash = o.getLong("expectedOcash"),
                    actualOcash = o.getLong("actualOcash"), expectedSahil = o.getLong("expectedSahil"),
                    actualSahil = o.getLong("actualSahil"), totalVariance = o.getLong("totalVariance"),
                    notes = o.optString("notes", ""), isClosed = o.optBoolean("isClosed", true),
                    isReopened = o.optBoolean("isReopened", false), reopenReason = o.optString("reopenReason", ""),
                    closedByUserId = o.getLong("closedByUserId"), closedByName = o.getString("closedByName"),
                    timestamp = o.getLong("timestamp")
                )
            })

            db.auditLogDao().insert(
                AuditLogEntity(
                    actionType = AuditActionType.BACKUP_RESTORED,
                    entityType = "BACKUP",
                    entityId = sourceName,
                    summary = "استعادة نسخة احتياطية كاملة من: $sourceName",
                    userId = actor.id,
                    userName = actor.displayName
                )
            )
        }
    }

    private fun sha256Hex(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun encryptString(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, portableAesKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptString(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val cipherText = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, portableAesKey, GCMParameterSpec(128, iv))
        val plain = cipher.doFinal(cipherText)
        return String(plain, Charsets.UTF_8)
    }
}
