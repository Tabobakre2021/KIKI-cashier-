package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.PartyDao
import com.example.data.dao.PaymentAllocationDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseDao
import com.example.data.dao.ReturnDao
import com.example.data.dao.SaleDao
import com.example.data.dao.StockMovementDao
import com.example.data.dao.UserDao
import com.example.data.dao.WalletTransferDao
import com.example.data.dao.ZReportDao
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

class KikiTypeConverters {
    @TypeConverter
    fun fromProductCategory(value: ProductCategory): String = value.name

    @TypeConverter
    fun toProductCategory(value: String): ProductCategory =
        runCatching { ProductCategory.valueOf(value) }.getOrDefault(ProductCategory.POULTRY)

    @TypeConverter
    fun fromPackagingUnit(value: PackagingUnit): String = value.name

    @TypeConverter
    fun toPackagingUnit(value: String): PackagingUnit =
        runCatching { PackagingUnit.valueOf(value) }.getOrDefault(PackagingUnit.KILO)

    @TypeConverter
    fun fromWalletChannel(value: WalletChannel): String = value.name

    @TypeConverter
    fun toWalletChannel(value: String): WalletChannel =
        runCatching { WalletChannel.valueOf(value) }.getOrDefault(WalletChannel.CASH)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = when (value) {
        "BAGS_PACKAGING" -> ExpenseCategory.BAGS_SUPPLIES
        else -> runCatching { ExpenseCategory.valueOf(value) }.getOrDefault(ExpenseCategory.OTHER)
    }

    @TypeConverter
    fun fromPricingMode(value: PricingMode): String = value.name

    @TypeConverter
    fun toPricingMode(value: String): PricingMode =
        runCatching { PricingMode.valueOf(value) }.getOrDefault(PricingMode.MARKUP)

    @TypeConverter
    fun fromStockMovementType(value: StockMovementType): String = value.name

    @TypeConverter
    fun toStockMovementType(value: String): StockMovementType =
        runCatching { StockMovementType.valueOf(value) }.getOrDefault(StockMovementType.OPENING)

    @TypeConverter
    fun fromAuditActionType(value: AuditActionType): String = value.name

    @TypeConverter
    fun toAuditActionType(value: String): AuditActionType =
        runCatching { AuditActionType.valueOf(value) }.getOrDefault(AuditActionType.SALE_CREATED)
}

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
        PaymentAllocationEntity::class,
        StockMovementEntity::class,
        ReturnEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        DebtPaymentEntity::class,
        WalletTransferEntity::class,
        UserEntity::class,
        ZReportEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(KikiTypeConverters::class)
abstract class KikiDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun returnDao(): ReturnDao
    abstract fun partyDao(): PartyDao
    abstract fun walletTransferDao(): WalletTransferDao
    abstract fun userDao(): UserDao
    abstract fun zReportDao(): ZReportDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: KikiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe upgrade from v1 prototype schema to v2 Mini-ERP schema
                db.execSQL("DROP TABLE IF EXISTS sale_items")
                db.execSQL("DROP TABLE IF EXISTS sales")
                db.execSQL("DROP TABLE IF EXISTS purchases")
                db.execSQL("DROP TABLE IF EXISTS expenses")
                db.execSQL("DROP TABLE IF EXISTS z_reports")
                db.execSQL("DROP TABLE IF EXISTS products")
                db.execSQL("DROP TABLE IF EXISTS users")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        defaultUnit TEXT NOT NULL,
                        costPrice INTEGER NOT NULL,
                        lastPurchasePrice INTEGER NOT NULL,
                        averageCost INTEGER NOT NULL,
                        profitMarginPercent REAL NOT NULL,
                        pricingMode TEXT NOT NULL,
                        sellingPrice INTEGER NOT NULL,
                        stockQuantity REAL NOT NULL,
                        lowStockThreshold REAL NOT NULL,
                        unitConversionFactor REAL NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sales (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber TEXT NOT NULL,
                        subtotalAmount INTEGER NOT NULL,
                        discountAmount INTEGER NOT NULL,
                        totalAmount INTEGER NOT NULL,
                        paidAmount INTEGER NOT NULL,
                        remainingDebt INTEGER NOT NULL,
                        totalCost INTEGER NOT NULL,
                        totalProfit INTEGER NOT NULL,
                        paymentSummary TEXT NOT NULL,
                        receiptNote TEXT NOT NULL,
                        customerId INTEGER,
                        customerName TEXT NOT NULL,
                        cashierUserId INTEGER NOT NULL,
                        cashierName TEXT NOT NULL,
                        isVoided INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sale_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        saleId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        sellingPriceAtSale INTEGER NOT NULL,
                        costPriceAtSale INTEGER NOT NULL,
                        lineTotal INTEGER NOT NULL,
                        lineProfit INTEGER NOT NULL,
                        FOREIGN KEY(saleId) REFERENCES sales(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_saleId ON sale_items(saleId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        supplierId INTEGER,
                        supplierName TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitCostPrice INTEGER NOT NULL,
                        discountAmount INTEGER NOT NULL,
                        transportCost INTEGER NOT NULL,
                        totalCost INTEGER NOT NULL,
                        paidAmount INTEGER NOT NULL,
                        remainingDebt INTEGER NOT NULL,
                        paymentSummary TEXT NOT NULL,
                        receiptNote TEXT NOT NULL,
                        recordedByUserId INTEGER NOT NULL,
                        recordedByName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        expenseNumber TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        description TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        paymentSummary TEXT NOT NULL,
                        receiptNote TEXT NOT NULL,
                        recordedByUserId INTEGER NOT NULL,
                        recordedByName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS payment_allocations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        referenceType TEXT NOT NULL,
                        referenceId INTEGER NOT NULL,
                        channel TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        receiptNumber TEXT NOT NULL,
                        operationNumber TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_referenceType_referenceId ON payment_allocations(referenceType, referenceId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        movementType TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        quantityDelta REAL NOT NULL,
                        quantityBefore REAL NOT NULL,
                        quantityAfter REAL NOT NULL,
                        unitCostAtMovement INTEGER NOT NULL,
                        referenceCode TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS returns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        returnCode TEXT NOT NULL,
                        returnType TEXT NOT NULL,
                        saleId INTEGER,
                        saleInvoiceNumber TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitCost INTEGER NOT NULL,
                        refundAmount INTEGER NOT NULL,
                        reversedCostAmount INTEGER NOT NULL,
                        walletChannel TEXT NOT NULL,
                        restockEligible INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        totalPurchased INTEGER NOT NULL,
                        totalPaid INTEGER NOT NULL,
                        balanceDue INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS customers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        totalCreditSales INTEGER NOT NULL,
                        totalPaid INTEGER NOT NULL,
                        balanceDue INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS debt_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        partyType TEXT NOT NULL,
                        partyId INTEGER NOT NULL,
                        partyName TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        walletChannel TEXT NOT NULL,
                        receiptNote TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS wallet_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fromChannel TEXT NOT NULL,
                        toChannel TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        referenceNote TEXT NOT NULL,
                        userName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        displayName TEXT NOT NULL,
                        roleTitle TEXT NOT NULL,
                        pinCode TEXT NOT NULL,
                        mustChangePin INTEGER NOT NULL,
                        isAdmin INTEGER NOT NULL,
                        canRecordSales INTEGER NOT NULL,
                        canRecordPurchases INTEGER NOT NULL,
                        canRecordExpenses INTEGER NOT NULL,
                        canEditPrices INTEGER NOT NULL,
                        canAddProducts INTEGER NOT NULL,
                        canEditProducts INTEGER NOT NULL,
                        canReverseOrDelete INTEGER NOT NULL,
                        canViewInventory INTEGER NOT NULL,
                        canManageParties INTEGER NOT NULL,
                        canViewSalesReports INTEGER NOT NULL,
                        canViewZReport INTEGER NOT NULL,
                        canExportData INTEGER NOT NULL,
                        canManageUsers INTEGER NOT NULL,
                        canReopenZReport INTEGER NOT NULL,
                        canManageBackup INTEGER NOT NULL,
                        canViewProfits INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        failedAttempts INTEGER NOT NULL,
                        lockedUntilMillis INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS z_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dateLabel TEXT NOT NULL,
                        openingCash INTEGER NOT NULL,
                        openingBankak INTEGER NOT NULL,
                        openingFawry INTEGER NOT NULL,
                        openingOcash INTEGER NOT NULL,
                        openingSahil INTEGER NOT NULL,
                        expectedCash INTEGER NOT NULL,
                        actualCash INTEGER NOT NULL,
                        expectedBankak INTEGER NOT NULL,
                        actualBankak INTEGER NOT NULL,
                        expectedFawry INTEGER NOT NULL,
                        actualFawry INTEGER NOT NULL,
                        expectedOcash INTEGER NOT NULL,
                        actualOcash INTEGER NOT NULL,
                        expectedSahil INTEGER NOT NULL,
                        actualSahil INTEGER NOT NULL,
                        totalVariance INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        isClosed INTEGER NOT NULL,
                        isReopened INTEGER NOT NULL,
                        reopenReason TEXT NOT NULL,
                        closedByUserId INTEGER NOT NULL,
                        closedByName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        actionType TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        beforeData TEXT NOT NULL,
                        afterData TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        userId INTEGER NOT NULL,
                        userName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): KikiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KikiDatabase::class.java,
                    "kiki_poultry_pos.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
