package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.example.data.KikiDatabase
import com.example.data.SeedData
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
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WalletTransferEntity
import com.example.data.entity.ZReportEntity
import com.example.domain.AuditActionType
import com.example.domain.BackupFrequency
import com.example.domain.ExpenseCategory
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.StockMovementType
import com.example.domain.ThemePreference
import com.example.domain.WalletChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.roundToLong

private val Context.kikiDataStore: DataStore<Preferences> by preferencesDataStore(name = "kiki_erp_prefs")

data class CartItemDraft(
    val product: ProductEntity,
    val unit: PackagingUnit,
    val quantity: Double,
    val conversionMultiplier: Double = 1.0,
    val customUnitPrice: Long? = null
) {
    val effectiveUnitPrice: Long
        get() = customUnitPrice?.takeIf { it > 0L } ?: product.sellingPrice
}

data class PaymentSplitLine(
    val channel: WalletChannel,
    val amount: Long,
    val receiptNumber: String = "",
    val operationNumber: String = ""
)

data class AppSettings(
    val themePreference: ThemePreference = ThemePreference.LIGHT,
    val allowNegativeStock: Boolean = false,
    val allowCreditSales: Boolean = true,
    val pricingRoundingStep: Int = 100,
    val maxPinAttempts: Int = 5,
    val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
    val openingBalances: Map<WalletChannel, Long> = WalletChannel.entries.associateWith { 0L }
)

class KikiRepository(
    private val context: Context,
    private val db: KikiDatabase
) {
    private object PrefKeys {
        val ACTIVE_USER_ID = longPreferencesKey("active_user_id")
        val THEME_PREF = stringPreferencesKey("theme_pref")
        val ALLOW_NEGATIVE_STOCK = booleanPreferencesKey("allow_negative_stock")
        val ALLOW_CREDIT_SALES = booleanPreferencesKey("allow_credit_sales")
        val PRICING_ROUNDING_STEP = intPreferencesKey("pricing_rounding_step")
        val MAX_PIN_ATTEMPTS = intPreferencesKey("max_pin_attempts")
        val BACKUP_FREQ = stringPreferencesKey("backup_freq")

        val OPEN_CASH = longPreferencesKey("open_cash")
        val OPEN_BANKAK = longPreferencesKey("open_bankak")
        val OPEN_FAWRY = longPreferencesKey("open_fawry")
        val OPEN_OCASH = longPreferencesKey("open_ocash")
        val OPEN_SAHIL = longPreferencesKey("open_sahil")
    }

    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val salesWithItems: Flow<List<SaleWithItems>> = db.saleDao().observeAllWithItems()
    val purchases: Flow<List<PurchaseEntity>> = db.purchaseDao().observeAll()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    val paymentAllocations: Flow<List<PaymentAllocationEntity>> = db.paymentAllocationDao().observeAll()
    val stockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().observeAll()
    val returns: Flow<List<ReturnEntity>> = db.returnDao().observeAll()
    val suppliers: Flow<List<SupplierEntity>> = db.partyDao().observeSuppliers()
    val customers: Flow<List<CustomerEntity>> = db.partyDao().observeCustomers()
    val debtPayments: Flow<List<DebtPaymentEntity>> = db.partyDao().observeDebtPayments()
    val walletTransfers: Flow<List<WalletTransferEntity>> = db.walletTransferDao().observeAll()
    val users: Flow<List<UserEntity>> = db.userDao().observeAll()
    val zReports: Flow<List<ZReportEntity>> = db.zReportDao().observeAll()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().observeAll()

    val activeUserIdFlow: Flow<Long?> = context.kikiDataStore.data.map { it[PrefKeys.ACTIVE_USER_ID] }

    val settingsFlow: Flow<AppSettings> = context.kikiDataStore.data.map { prefs ->
        val theme = prefs[PrefKeys.THEME_PREF]?.let {
            runCatching { ThemePreference.valueOf(it) }.getOrNull()
        } ?: ThemePreference.LIGHT
        val freq = prefs[PrefKeys.BACKUP_FREQ]?.let {
            runCatching { BackupFrequency.valueOf(it) }.getOrNull()
        } ?: BackupFrequency.DAILY
        AppSettings(
            themePreference = theme,
            allowNegativeStock = prefs[PrefKeys.ALLOW_NEGATIVE_STOCK] ?: false,
            allowCreditSales = prefs[PrefKeys.ALLOW_CREDIT_SALES] ?: true,
            pricingRoundingStep = prefs[PrefKeys.PRICING_ROUNDING_STEP] ?: 100,
            maxPinAttempts = prefs[PrefKeys.MAX_PIN_ATTEMPTS] ?: 5,
            backupFrequency = freq,
            openingBalances = mapOf(
                WalletChannel.CASH to (prefs[PrefKeys.OPEN_CASH] ?: 0L),
                WalletChannel.BANKAK to (prefs[PrefKeys.OPEN_BANKAK] ?: 0L),
                WalletChannel.FAWRY to (prefs[PrefKeys.OPEN_FAWRY] ?: 0L),
                WalletChannel.OCASH to (prefs[PrefKeys.OPEN_OCASH] ?: 0L),
                WalletChannel.SAHIL to (prefs[PrefKeys.OPEN_SAHIL] ?: 0L)
            )
        )
    }

    suspend fun ensureSeeded() {
        db.withTransaction {
            if (db.productDao().count() == 0) {
                db.productDao().insertAll(SeedData.defaultProducts())
            }
            if (db.userDao().count() == 0) {
                db.userDao().insertAll(SeedData.defaultUsers())
            }
        }
    }

    suspend fun setActiveUserId(userId: Long?) {
        context.kikiDataStore.edit { prefs ->
            if (userId == null) prefs.remove(PrefKeys.ACTIVE_USER_ID)
            else prefs[PrefKeys.ACTIVE_USER_ID] = userId
        }
    }

    suspend fun updateSettings(
        themePreference: ThemePreference? = null,
        allowNegativeStock: Boolean? = null,
        allowCreditSales: Boolean? = null,
        pricingRoundingStep: Int? = null,
        maxPinAttempts: Int? = null,
        backupFrequency: BackupFrequency? = null
    ) {
        context.kikiDataStore.edit { prefs ->
            themePreference?.let { prefs[PrefKeys.THEME_PREF] = it.name }
            allowNegativeStock?.let { prefs[PrefKeys.ALLOW_NEGATIVE_STOCK] = it }
            allowCreditSales?.let { prefs[PrefKeys.ALLOW_CREDIT_SALES] = it }
            pricingRoundingStep?.let { prefs[PrefKeys.PRICING_ROUNDING_STEP] = it }
            maxPinAttempts?.let { prefs[PrefKeys.MAX_PIN_ATTEMPTS] = it }
            backupFrequency?.let { prefs[PrefKeys.BACKUP_FREQ] = it.name }
        }
    }

    suspend fun setOpeningBalances(balances: Map<WalletChannel, Long>) {
        context.kikiDataStore.edit { prefs ->
            prefs[PrefKeys.OPEN_CASH] = balances[WalletChannel.CASH] ?: 0L
            prefs[PrefKeys.OPEN_BANKAK] = balances[WalletChannel.BANKAK] ?: 0L
            prefs[PrefKeys.OPEN_FAWRY] = balances[WalletChannel.FAWRY] ?: 0L
            prefs[PrefKeys.OPEN_OCASH] = balances[WalletChannel.OCASH] ?: 0L
            prefs[PrefKeys.OPEN_SAHIL] = balances[WalletChannel.SAHIL] ?: 0L
        }
    }

    suspend fun logAudit(
        action: AuditActionType,
        entityType: String,
        entityId: String,
        summary: String,
        beforeData: String = "",
        afterData: String = "",
        reason: String = "",
        user: UserEntity
    ) {
        db.auditLogDao().insert(
            AuditLogEntity(
                actionType = action,
                entityType = entityType,
                entityId = entityId,
                summary = summary,
                beforeData = beforeData,
                afterData = afterData,
                reason = reason,
                userId = user.id,
                userName = user.displayName
            )
        )
    }

    // --- Authentication & Security ---
    suspend fun recordLoginResult(user: UserEntity, success: Boolean, maxAttempts: Int): UserEntity {
        val now = System.currentTimeMillis()
        val updated = if (success) {
            user.copy(failedAttempts = 0, lockedUntilMillis = 0L, lastLoginAt = now)
        } else {
            val attempts = user.failedAttempts + 1
            val lockUntil = if (attempts >= maxAttempts) now + 60_000L else 0L
            user.copy(failedAttempts = attempts, lockedUntilMillis = lockUntil)
        }
        db.userDao().update(updated)
        if (success) {
            logAudit(
                action = AuditActionType.USER_LOGIN,
                entityType = "USER",
                entityId = user.id.toString(),
                summary = "تسجيل دخول المستخدم: ${user.displayName}",
                user = updated
            )
        }
        return updated
    }

    suspend fun updateUserPin(user: UserEntity, newPin: String): UserEntity {
        val updated = user.copy(pinCode = newPin, mustChangePin = false)
        db.userDao().update(updated)
        logAudit(
            action = AuditActionType.USER_UPDATED,
            entityType = "USER",
            entityId = user.id.toString(),
            summary = "تحديث رمز PIN للمستخدم ${user.displayName}",
            user = updated
        )
        return updated
    }

    // --- Products & Smart Pricing ---
    suspend fun saveProduct(product: ProductEntity, actor: UserEntity): Long {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            if (product.id == 0L) {
                val id = db.productDao().insert(product.copy(updatedAt = now))
                if (product.stockQuantity > 0.0) {
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = id,
                            productName = product.name,
                            movementType = StockMovementType.OPENING,
                            unit = product.defaultUnit,
                            quantityDelta = product.stockQuantity,
                            quantityBefore = 0.0,
                            quantityAfter = product.stockQuantity,
                            unitCostAtMovement = product.averageCost.takeIf { it > 0L } ?: product.costPrice,
                            referenceCode = "INIT-$id",
                            reason = "رصيد افتتاحي عند إضافة الصنف",
                            userName = actor.displayName,
                            timestamp = now
                        )
                    )
                }
                logAudit(
                    action = AuditActionType.STOCK_ADJUSTED,
                    entityType = "PRODUCT",
                    entityId = id.toString(),
                    summary = "إضافة صنف جديد: ${product.name}",
                    afterData = "بيع: ${product.sellingPrice} | رصيد: ${product.stockQuantity}",
                    user = actor
                )
                id
            } else {
                val old = db.productDao().getById(product.id)
                db.productDao().update(product.copy(updatedAt = now))
                if (old != null) {
                    if (old.sellingPrice != product.sellingPrice) {
                        logAudit(
                            action = AuditActionType.PRICE_CHANGED,
                            entityType = "PRODUCT",
                            entityId = product.id.toString(),
                            summary = "تعديل سعر البيع: ${product.name}",
                            beforeData = old.sellingPrice.toString(),
                            afterData = product.sellingPrice.toString(),
                            user = actor
                        )
                    }
                    if (old.costPrice != product.costPrice) {
                        logAudit(
                            action = AuditActionType.COST_CHANGED,
                            entityType = "PRODUCT",
                            entityId = product.id.toString(),
                            summary = "تعديل تكلفة الشراء: ${product.name}",
                            beforeData = old.costPrice.toString(),
                            afterData = product.costPrice.toString(),
                            user = actor
                        )
                    }
                    val stockDiff = product.stockQuantity - old.stockQuantity
                    if (stockDiff != 0.0) {
                        db.stockMovementDao().insert(
                            StockMovementEntity(
                                productId = product.id,
                                productName = product.name,
                                movementType = if (stockDiff > 0) StockMovementType.ADJUSTMENT_IN else StockMovementType.ADJUSTMENT_OUT,
                                unit = product.defaultUnit,
                                quantityDelta = stockDiff,
                                quantityBefore = old.stockQuantity,
                                quantityAfter = product.stockQuantity,
                                unitCostAtMovement = product.averageCost,
                                referenceCode = "ADJ-${now % 100000}",
                                reason = "تعديل رصيد المخزون من بطاقة الصنف",
                                userName = actor.displayName,
                                timestamp = now
                            )
                        )
                    }
                }
                product.id
            }
        }
    }

    suspend fun deleteProduct(productId: Long, actor: UserEntity) {
        val old = db.productDao().getById(productId)
        db.productDao().deleteById(productId)
        if (old != null) {
            logAudit(
                action = AuditActionType.STOCK_ADJUSTED,
                entityType = "PRODUCT",
                entityId = productId.toString(),
                summary = "حذف الصنف: ${old.name}",
                user = actor
            )
        }
    }

    // --- Sales & POS ---
    suspend fun recordSale(
        cartItems: List<CartItemDraft>,
        discountAmount: Long,
        allocations: List<PaymentSplitLine>,
        customerId: Long?,
        receiptNote: String,
        allowNegativeStock: Boolean,
        cashier: UserEntity
    ): Result<SaleWithItems> {
        return runCatching {
            db.withTransaction {
                val now = System.currentTimeMillis()
                val latestClosed = db.zReportDao().getLatestActiveClosed()
                if (latestClosed != null && now <= latestClosed.timestamp) {
                    throw IllegalStateException("اليومية مقفلة، يرجى إعادة الفتح من المدير العام")
                }

                val nextSeq = db.saleDao().count() + 1
                val invoiceNumber = "KIKI-S-${nextSeq.toString().padStart(4, '0')}"

                var subtotal = 0L
                var totalCost = 0L
                val saleItems = mutableListOf<SaleItemEntity>()

                for (draft in cartItems) {
                    val latestProduct = db.productDao().getById(draft.product.id)
                        ?: throw IllegalStateException("الصنف غير موجود: ${draft.product.name}")

                    val effectiveQty = draft.quantity * draft.conversionMultiplier
                    if (!allowNegativeStock && effectiveQty > latestProduct.stockQuantity) {
                        throw IllegalStateException(
                            "الرصيد المتاح من (${latestProduct.name}) هو ${PricingEngine.formatQty(latestProduct.stockQuantity)} فقط"
                        )
                    }

                    val unitCost = latestProduct.averageCost.takeIf { it > 0L } ?: latestProduct.costPrice
                    val sellPrice = draft.customUnitPrice?.takeIf { it > 0L } ?: latestProduct.sellingPrice
                    val lineTotal = PricingEngine.calculateLineTotal(sellPrice, draft.quantity)
                    val lineCost = PricingEngine.calculateLineTotal(unitCost, effectiveQty)

                    subtotal += lineTotal
                    totalCost += lineCost

                    val qtyBefore = latestProduct.stockQuantity
                    val qtyAfter = qtyBefore - effectiveQty
                    db.productDao().adjustStock(latestProduct.id, -effectiveQty, now)

                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = latestProduct.id,
                            productName = latestProduct.name,
                            movementType = StockMovementType.SALE,
                            unit = draft.unit,
                            quantityDelta = -effectiveQty,
                            quantityBefore = qtyBefore,
                            quantityAfter = qtyAfter,
                            unitCostAtMovement = unitCost,
                            referenceCode = invoiceNumber,
                            reason = "بيع فاتورة $invoiceNumber",
                            userName = cashier.displayName,
                            timestamp = now
                        )
                    )

                    saleItems.add(
                        SaleItemEntity(
                            saleId = 0L,
                            productId = latestProduct.id,
                            productName = latestProduct.name,
                            unit = draft.unit,
                            quantity = draft.quantity,
                            sellingPriceAtSale = sellPrice,
                            costPriceAtSale = unitCost,
                            lineTotal = lineTotal,
                            lineProfit = lineTotal - lineCost
                        )
                    )
                }

                val safeDiscount = discountAmount.coerceIn(0L, subtotal)
                val netTotal = (subtotal - safeDiscount).coerceAtLeast(0L)
                val paidTotal = allocations.sumOf { it.amount }.coerceAtMost(netTotal)
                val remainingDebt = (netTotal - paidTotal).coerceAtLeast(0L)

                if (remainingDebt > 0L && customerId == null) {
                    throw IllegalStateException("مجموع المدفوعات أقل من الإجمالي. اختر عميلاً لتسجيل المتبقي كمديونية آجل")
                }

                val customer = customerId?.let { db.partyDao().getCustomerById(it) }
                if (remainingDebt > 0L && customer != null) {
                    db.partyDao().updateCustomer(
                        customer.copy(
                            totalCreditSales = customer.totalCreditSales + remainingDebt,
                            balanceDue = customer.balanceDue + remainingDebt
                        )
                    )
                }

                val paymentSummary = buildString {
                    val parts = allocations.filter { it.amount > 0L }
                        .map { "${it.channel.arabicLabel}: ${PricingEngine.formatAmount(it.amount)}" }
                        .toMutableList()
                    if (remainingDebt > 0L) parts.add("آجل: ${PricingEngine.formatAmount(remainingDebt)}")
                    append(parts.joinToString(" | ").ifBlank { "كاش" })
                }

                val netProfit = netTotal - totalCost
                val saleEntity = SaleEntity(
                    invoiceNumber = invoiceNumber,
                    subtotalAmount = subtotal,
                    discountAmount = safeDiscount,
                    totalAmount = netTotal,
                    paidAmount = paidTotal,
                    remainingDebt = remainingDebt,
                    totalCost = totalCost,
                    totalProfit = netProfit,
                    paymentSummary = paymentSummary,
                    receiptNote = receiptNote.trim(),
                    customerId = customer?.id,
                    customerName = customer?.name ?: "",
                    cashierUserId = cashier.id,
                    cashierName = cashier.displayName,
                    isVoided = false,
                    timestamp = now
                )

                val saleId = db.saleDao().insertSaleWithItems(saleEntity, saleItems)

                val allocationEntities = allocations.filter { it.amount > 0L }.map { line ->
                    PaymentAllocationEntity(
                        referenceType = "SALE",
                        referenceId = saleId,
                        channel = line.channel,
                        amount = line.amount,
                        receiptNumber = line.receiptNumber.trim(),
                        operationNumber = line.operationNumber.trim(),
                        note = receiptNote.trim(),
                        timestamp = now
                    )
                }
                if (allocationEntities.isNotEmpty()) {
                    db.paymentAllocationDao().insertAll(allocationEntities)
                }

                logAudit(
                    action = AuditActionType.SALE_CREATED,
                    entityType = "SALE",
                    entityId = invoiceNumber,
                    summary = "فاتورة بيع $invoiceNumber بقيمة ${PricingEngine.formatCurrency(netTotal)}",
                    afterData = paymentSummary,
                    user = cashier
                )

                val savedSale = saleEntity.copy(id = saleId)
                val savedItems = saleItems.map { it.copy(saleId = saleId) }
                SaleWithItems(savedSale, savedItems)
            }
        }
    }

    // --- Purchases & Moving Weighted Average Cost ---
    suspend fun recordPurchase(
        product: ProductEntity,
        supplierId: Long?,
        supplierName: String,
        unit: PackagingUnit,
        quantity: Double,
        conversionMultiplier: Double,
        unitCostPrice: Long,
        discountAmount: Long,
        transportCost: Long,
        newTargetPercent: Double,
        newSellingPrice: Long,
        updateProductPricing: Boolean,
        allocations: List<PaymentSplitLine>,
        receiptNote: String,
        recordedBy: UserEntity
    ): Long {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val latestProduct = db.productDao().getById(product.id)
                ?: throw IllegalStateException("الصنف غير موجود")

            val nextSeq = db.purchaseDao().count() + 1
            val invoiceNumber = "KIKI-P-${nextSeq.toString().padStart(4, '0')}"

            val itemGrossCost = PricingEngine.calculateLineTotal(unitCostPrice, quantity)
            val totalCost = (itemGrossCost - discountAmount + transportCost).coerceAtLeast(0L)
            val paidTotal = allocations.sumOf { it.amount }.coerceAtMost(totalCost)
            val remainingDebt = (totalCost - paidTotal).coerceAtLeast(0L)

            val effectiveQty = quantity * conversionMultiplier.coerceAtLeast(1.0)
            val newAvgCost = PricingEngine.calculateMovingWeightedAverageCost(
                previousQty = latestProduct.stockQuantity,
                previousAvgCost = latestProduct.averageCost.takeIf { it > 0L } ?: latestProduct.costPrice,
                purchasedQty = effectiveQty,
                purchasedTotalNetCost = totalCost
            )
            val effectiveUnitCost = if (effectiveQty > 0.0) {
                (totalCost.toDouble() / effectiveQty).roundToLong()
            } else unitCostPrice

            val supplier = supplierId?.let { db.partyDao().getSupplierById(it) }
            val resolvedSupplierName = supplier?.name ?: supplierName.trim()
            if (supplier != null) {
                db.partyDao().updateSupplier(
                    supplier.copy(
                        totalPurchased = supplier.totalPurchased + totalCost,
                        totalPaid = supplier.totalPaid + paidTotal,
                        balanceDue = supplier.balanceDue + remainingDebt
                    )
                )
            }

            val paymentSummary = buildString {
                val parts = allocations.filter { it.amount > 0L }
                    .map { "${it.channel.arabicLabel}: ${PricingEngine.formatAmount(it.amount)}" }
                    .toMutableList()
                if (remainingDebt > 0L) parts.add("آجل للمورد: ${PricingEngine.formatAmount(remainingDebt)}")
                append(parts.joinToString(" | ").ifBlank { "آجل" })
            }

            val purchaseId = db.purchaseDao().insert(
                PurchaseEntity(
                    invoiceNumber = invoiceNumber,
                    productId = latestProduct.id,
                    productName = latestProduct.name,
                    supplierId = supplier?.id,
                    supplierName = resolvedSupplierName,
                    unit = unit,
                    quantity = quantity,
                    unitCostPrice = unitCostPrice,
                    discountAmount = discountAmount,
                    transportCost = transportCost,
                    totalCost = totalCost,
                    paidAmount = paidTotal,
                    remainingDebt = remainingDebt,
                    paymentSummary = paymentSummary,
                    receiptNote = receiptNote.trim(),
                    recordedByUserId = recordedBy.id,
                    recordedByName = recordedBy.displayName,
                    timestamp = now
                )
            )

            val allocs = allocations.filter { it.amount > 0L }.map { line ->
                PaymentAllocationEntity(
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    channel = line.channel,
                    amount = line.amount,
                    receiptNumber = line.receiptNumber.trim(),
                    operationNumber = line.operationNumber.trim(),
                    note = receiptNote.trim(),
                    timestamp = now
                )
            }
            if (allocs.isNotEmpty()) db.paymentAllocationDao().insertAll(allocs)

            val qtyBefore = latestProduct.stockQuantity
            val qtyAfter = qtyBefore + effectiveQty

            val updatedSelling = if (updateProductPricing && newSellingPrice > 0L) {
                newSellingPrice
            } else latestProduct.sellingPrice

            val updatedProduct = latestProduct.copy(
                costPrice = effectiveUnitCost,
                lastPurchasePrice = unitCostPrice,
                averageCost = newAvgCost,
                profitMarginPercent = if (updatedSelling > 0L && newAvgCost > 0L) {
                    PricingEngine.calculateMarkupPercent(newAvgCost, updatedSelling)
                } else newTargetPercent,
                sellingPrice = updatedSelling,
                stockQuantity = qtyAfter,
                updatedAt = now
            )
            db.productDao().update(updatedProduct)

            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = latestProduct.id,
                    productName = latestProduct.name,
                    movementType = StockMovementType.PURCHASE,
                    unit = unit,
                    quantityDelta = effectiveQty,
                    quantityBefore = qtyBefore,
                    quantityAfter = qtyAfter,
                    unitCostAtMovement = effectiveUnitCost,
                    referenceCode = invoiceNumber,
                    reason = "توريد من ${resolvedSupplierName.ifBlank { "مورد" }}",
                    userName = recordedBy.displayName,
                    timestamp = now
                )
            )

            logAudit(
                action = AuditActionType.PURCHASE_CREATED,
                entityType = "PURCHASE",
                entityId = invoiceNumber,
                summary = "فاتورة توريد $invoiceNumber (${latestProduct.name}) بتكلفة ${PricingEngine.formatCurrency(totalCost)}",
                afterData = "متوسط التكلفة الجديد: $newAvgCost",
                user = recordedBy
            )

            purchaseId
        }
    }

    // --- Expenses ---
    suspend fun recordExpense(
        title: String,
        category: ExpenseCategory,
        description: String,
        amount: Long,
        allocations: List<PaymentSplitLine>,
        receiptNote: String,
        recordedBy: UserEntity
    ): Long {
        return db.withTransaction {
            val now = System.currentTimeMillis()
            val seq = db.expenseDao().count() + 1
            val expNumber = "KIKI-E-${seq.toString().padStart(4, '0')}"

            val paymentSummary = allocations.filter { it.amount > 0L }
                .joinToString(" | ") { "${it.channel.arabicLabel}: ${PricingEngine.formatAmount(it.amount)}" }

            val expId = db.expenseDao().insert(
                ExpenseEntity(
                    expenseNumber = expNumber,
                    title = title.trim().ifBlank { category.arabicLabel },
                    category = category,
                    description = description.trim(),
                    amount = amount,
                    paymentSummary = paymentSummary,
                    receiptNote = receiptNote.trim(),
                    recordedByUserId = recordedBy.id,
                    recordedByName = recordedBy.displayName,
                    timestamp = now
                )
            )

            val allocs = allocations.filter { it.amount > 0L }.map { line ->
                PaymentAllocationEntity(
                    referenceType = "EXPENSE",
                    referenceId = expId,
                    channel = line.channel,
                    amount = line.amount,
                    receiptNumber = line.receiptNumber.trim(),
                    operationNumber = line.operationNumber.trim(),
                    note = receiptNote.trim(),
                    timestamp = now
                )
            }
            if (allocs.isNotEmpty()) db.paymentAllocationDao().insertAll(allocs)

            logAudit(
                action = AuditActionType.EXPENSE_CREATED,
                entityType = "EXPENSE",
                entityId = expNumber,
                summary = "تسجيل مصروف (${category.arabicLabel}): ${PricingEngine.formatCurrency(amount)}",
                user = recordedBy
            )
            expId
        }
    }

    // --- Returns, Damaged, Expired & Stock Adjustments ---
    suspend fun recordCustomerReturn(
        sale: SaleWithItems,
        item: SaleItemEntity,
        returnQty: Double,
        restockEligible: Boolean,
        refundChannel: WalletChannel,
        reason: String,
        actor: UserEntity
    ) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val code = "RET-${(now / 1000L) % 100000L}"
            val refundAmount = PricingEngine.calculateLineTotal(item.sellingPriceAtSale, returnQty)
            val reversedCost = PricingEngine.calculateLineTotal(item.costPriceAtSale, returnQty)

            val product = db.productDao().getById(item.productId)
            if (product != null && restockEligible) {
                val before = product.stockQuantity
                val after = before + returnQty
                db.productDao().adjustStock(product.id, returnQty, now)
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        movementType = StockMovementType.CUSTOMER_RETURN,
                        unit = item.unit,
                        quantityDelta = returnQty,
                        quantityBefore = before,
                        quantityAfter = after,
                        unitCostAtMovement = item.costPriceAtSale,
                        referenceCode = code,
                        reason = reason.ifBlank { "مرتجع فاتورة ${sale.sale.invoiceNumber}" },
                        userName = actor.displayName,
                        timestamp = now
                    )
                )
            }

            db.returnDao().insert(
                ReturnEntity(
                    returnCode = code,
                    returnType = "CUSTOMER_RETURN",
                    saleId = sale.sale.id,
                    saleInvoiceNumber = sale.sale.invoiceNumber,
                    productId = item.productId,
                    productName = item.productName,
                    unit = item.unit,
                    quantity = returnQty,
                    unitCost = item.costPriceAtSale,
                    refundAmount = refundAmount,
                    reversedCostAmount = reversedCost,
                    walletChannel = refundChannel,
                    restockEligible = restockEligible,
                    reason = reason.trim(),
                    userName = actor.displayName,
                    timestamp = now
                )
            )

            logAudit(
                action = AuditActionType.SALE_RETURNED,
                entityType = "SALE",
                entityId = sale.sale.invoiceNumber,
                summary = "مرتجع عميل (${item.productName} × $returnQty) بقيمة ${PricingEngine.formatCurrency(refundAmount)}",
                reason = reason,
                user = actor
            )
        }
    }

    suspend fun recordStockAdjustmentOrWaste(
        product: ProductEntity,
        movementType: StockMovementType,
        quantity: Double,
        reason: String,
        walletChannelForSupplierRefund: WalletChannel = WalletChannel.CASH,
        actor: UserEntity
    ) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val delta = if (movementType.isInbound) quantity else -quantity
            val before = product.stockQuantity
            val after = before + delta
            val unitCost = product.averageCost.takeIf { it > 0L } ?: product.costPrice

            db.productDao().adjustStock(product.id, delta, now)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    movementType = movementType,
                    unit = product.defaultUnit,
                    quantityDelta = delta,
                    quantityBefore = before,
                    quantityAfter = after,
                    unitCostAtMovement = unitCost,
                    referenceCode = "STK-${(now / 1000L) % 100000L}",
                    reason = reason.ifBlank { movementType.arabicLabel },
                    userName = actor.displayName,
                    timestamp = now
                )
            )

            if (movementType == StockMovementType.DAMAGED ||
                movementType == StockMovementType.EXPIRED ||
                movementType == StockMovementType.SUPPLIER_RETURN
            ) {
                db.returnDao().insert(
                    ReturnEntity(
                        returnCode = "STK-${(now / 1000L) % 100000L}",
                        returnType = movementType.name,
                        productId = product.id,
                        productName = product.name,
                        unit = product.defaultUnit,
                        quantity = quantity,
                        unitCost = unitCost,
                        refundAmount = if (movementType == StockMovementType.SUPPLIER_RETURN) {
                            PricingEngine.calculateLineTotal(unitCost, quantity)
                        } else 0L,
                        reversedCostAmount = PricingEngine.calculateLineTotal(unitCost, quantity),
                        walletChannel = walletChannelForSupplierRefund,
                        restockEligible = false,
                        reason = reason.ifBlank { movementType.arabicLabel },
                        userName = actor.displayName,
                        timestamp = now
                    )
                )
            }

            logAudit(
                action = AuditActionType.STOCK_ADJUSTED,
                entityType = "PRODUCT",
                entityId = product.id.toString(),
                summary = "${movementType.arabicLabel}: ${product.name} (${PricingEngine.formatQty(quantity)})",
                beforeData = PricingEngine.formatQty(before),
                afterData = PricingEngine.formatQty(after),
                reason = reason,
                user = actor
            )
        }
    }

    // --- Parties & Debt Payments ---
    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        return if (supplier.id == 0L) db.partyDao().insertSupplier(supplier)
        else {
            db.partyDao().updateSupplier(supplier)
            supplier.id
        }
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) db.partyDao().insertCustomer(customer)
        else {
            db.partyDao().updateCustomer(customer)
            customer.id
        }
    }

    suspend fun recordDebtPayment(
        partyType: String, // "SUPPLIER" or "CUSTOMER"
        partyId: Long,
        amount: Long,
        walletChannel: WalletChannel,
        receiptNote: String,
        actor: UserEntity
    ) {
        db.withTransaction {
            val partyName = if (partyType == "SUPPLIER") {
                val s = db.partyDao().getSupplierById(partyId) ?: return@withTransaction
                val safeAmt = amount.coerceAtMost(s.balanceDue)
                db.partyDao().updateSupplier(
                    s.copy(
                        totalPaid = s.totalPaid + safeAmt,
                        balanceDue = (s.balanceDue - safeAmt).coerceAtLeast(0L)
                    )
                )
                s.name
            } else {
                val c = db.partyDao().getCustomerById(partyId) ?: return@withTransaction
                val safeAmt = amount.coerceAtMost(c.balanceDue)
                db.partyDao().updateCustomer(
                    c.copy(
                        totalPaid = c.totalPaid + safeAmt,
                        balanceDue = (c.balanceDue - safeAmt).coerceAtLeast(0L)
                    )
                )
                c.name
            }

            db.partyDao().insertDebtPayment(
                DebtPaymentEntity(
                    partyType = partyType,
                    partyId = partyId,
                    partyName = partyName,
                    amount = amount,
                    walletChannel = walletChannel,
                    receiptNote = receiptNote.trim(),
                    userName = actor.displayName
                )
            )

            logAudit(
                action = AuditActionType.DEBT_PAYMENT,
                entityType = partyType,
                entityId = partyId.toString(),
                summary = if (partyType == "SUPPLIER") {
                    "سداد للمورد ($partyName) مبلغ ${PricingEngine.formatCurrency(amount)}"
                } else {
                    "تحصيل من العميل ($partyName) مبلغ ${PricingEngine.formatCurrency(amount)}"
                },
                user = actor
            )
        }
    }

    // --- Wallet Transfers ---
    suspend fun recordWalletTransfer(
        fromChannel: WalletChannel,
        toChannel: WalletChannel,
        amount: Long,
        referenceNote: String,
        actor: UserEntity
    ) {
        if (fromChannel == toChannel || amount <= 0L) return
        db.withTransaction {
            db.walletTransferDao().insert(
                WalletTransferEntity(
                    fromChannel = fromChannel,
                    toChannel = toChannel,
                    amount = amount,
                    referenceNote = referenceNote.trim(),
                    userName = actor.displayName
                )
            )
            logAudit(
                action = AuditActionType.WALLET_TRANSFER,
                entityType = "WALLET",
                entityId = "${fromChannel.name}->${toChannel.name}",
                summary = "تحويل ${PricingEngine.formatCurrency(amount)} من ${fromChannel.arabicLabel} إلى ${toChannel.arabicLabel}",
                reason = referenceNote,
                user = actor
            )
        }
    }

    // --- Z-Reports ---
    suspend fun saveZReport(report: ZReportEntity, actor: UserEntity): Long {
        return db.withTransaction {
            val id = db.zReportDao().insert(report)
            logAudit(
                action = AuditActionType.ZREPORT_CLOSED,
                entityType = "Z_REPORT",
                entityId = id.toString(),
                summary = "تقفيل يومية (${report.dateLabel}) | الفارق: ${PricingEngine.formatCurrency(report.totalVariance)}",
                user = actor
            )
            id
        }
    }

    suspend fun reopenZReport(report: ZReportEntity, reason: String, actor: UserEntity) {
        db.withTransaction {
            db.zReportDao().update(
                report.copy(
                    isClosed = false,
                    isReopened = true,
                    reopenReason = reason.trim()
                )
            )
            logAudit(
                action = AuditActionType.ZREPORT_REOPENED,
                entityType = "Z_REPORT",
                entityId = report.id.toString(),
                summary = "إعادة فتح اليومية (${report.dateLabel})",
                reason = reason.trim(),
                user = actor
            )
        }
    }

    // --- Users ---
    suspend fun saveUser(user: UserEntity, actor: UserEntity): Long {
        val id = if (user.id == 0L) db.userDao().insert(user) else {
            db.userDao().update(user)
            user.id
        }
        logAudit(
            action = AuditActionType.USER_UPDATED,
            entityType = "USER",
            entityId = id.toString(),
            summary = "حفظ/تحديث صلاحيات المستخدم: ${user.displayName}",
            user = actor
        )
        return id
    }

    suspend fun deleteUser(userId: Long, actor: UserEntity) {
        val u = db.userDao().getById(userId)
        db.userDao().deleteById(userId)
        if (u != null) {
            logAudit(
                action = AuditActionType.USER_UPDATED,
                entityType = "USER",
                entityId = userId.toString(),
                summary = "حذف المستخدم: ${u.displayName}",
                user = actor
            )
        }
    }
}
