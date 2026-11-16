package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.AuditActionType
import com.example.domain.ExpenseCategory
import com.example.domain.PackagingUnit
import com.example.domain.PricingMode
import com.example.domain.ProductCategory
import com.example.domain.StockMovementType
import com.example.domain.WalletChannel

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ProductCategory,
    val defaultUnit: PackagingUnit = PackagingUnit.KILO,
    val costPrice: Long = 0L,
    val lastPurchasePrice: Long = 0L,
    val averageCost: Long = 0L,
    val profitMarginPercent: Double = 0.0,
    val pricingMode: PricingMode = PricingMode.MARKUP,
    val sellingPrice: Long = 0L,
    val stockQuantity: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val unitConversionFactor: Double = 1.0,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val subtotalAmount: Long,
    val discountAmount: Long = 0L,
    val totalAmount: Long,
    val paidAmount: Long,
    val remainingDebt: Long = 0L,
    val totalCost: Long,
    val totalProfit: Long,
    val paymentSummary: String,
    val receiptNote: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val cashierUserId: Long,
    val cashierName: String,
    val isVoided: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val unit: PackagingUnit,
    val quantity: Double,
    val sellingPriceAtSale: Long,
    val costPriceAtSale: Long,
    val lineTotal: Long,
    val lineProfit: Long
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val productId: Long,
    val productName: String,
    val supplierId: Long? = null,
    val supplierName: String = "",
    val unit: PackagingUnit,
    val quantity: Double,
    val unitCostPrice: Long,
    val discountAmount: Long = 0L,
    val transportCost: Long = 0L,
    val totalCost: Long,
    val paidAmount: Long,
    val remainingDebt: Long = 0L,
    val paymentSummary: String,
    val receiptNote: String = "",
    val recordedByUserId: Long,
    val recordedByName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseNumber: String,
    val title: String,
    val category: ExpenseCategory,
    val description: String,
    val amount: Long,
    val paymentSummary: String,
    val receiptNote: String = "",
    val recordedByUserId: Long,
    val recordedByName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_allocations", indices = [Index("referenceType", "referenceId")])
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val referenceType: String, // "SALE", "PURCHASE", "EXPENSE"
    val referenceId: Long,
    val channel: WalletChannel,
    val amount: Long,
    val receiptNumber: String = "",
    val operationNumber: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements", indices = [Index("productId")])
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: StockMovementType,
    val unit: PackagingUnit,
    val quantityDelta: Double,
    val quantityBefore: Double,
    val quantityAfter: Double,
    val unitCostAtMovement: Long,
    val referenceCode: String = "",
    val reason: String = "",
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "returns")
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnCode: String,
    val returnType: String, // "CUSTOMER_RETURN", "SUPPLIER_RETURN", "DAMAGED", "EXPIRED", "ADJUSTMENT"
    val saleId: Long? = null,
    val saleInvoiceNumber: String = "",
    val productId: Long,
    val productName: String,
    val unit: PackagingUnit,
    val quantity: Double,
    val unitCost: Long,
    val refundAmount: Long,
    val reversedCostAmount: Long,
    val walletChannel: WalletChannel = WalletChannel.CASH,
    val restockEligible: Boolean = true,
    val reason: String = "",
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val totalPurchased: Long = 0L,
    val totalPaid: Long = 0L,
    val balanceDue: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val totalCreditSales: Long = 0L,
    val totalPaid: Long = 0L,
    val balanceDue: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyType: String, // "SUPPLIER" or "CUSTOMER"
    val partyId: Long,
    val partyName: String,
    val amount: Long,
    val walletChannel: WalletChannel,
    val receiptNote: String = "",
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transfers")
data class WalletTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromChannel: WalletChannel,
    val toChannel: WalletChannel,
    val amount: Long,
    val referenceNote: String = "",
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val roleTitle: String,
    val pinCode: String,
    val mustChangePin: Boolean = false,
    val isAdmin: Boolean = false,
    // 15 Granular Permissions + Exclusive Admin Profit Protection
    val canRecordSales: Boolean = true,
    val canRecordPurchases: Boolean = false,
    val canRecordExpenses: Boolean = false,
    val canEditPrices: Boolean = false,
    val canAddProducts: Boolean = false,
    val canEditProducts: Boolean = false,
    val canReverseOrDelete: Boolean = false,
    val canViewInventory: Boolean = true,
    val canManageParties: Boolean = false,
    val canViewSalesReports: Boolean = false,
    val canViewZReport: Boolean = false,
    val canExportData: Boolean = false,
    val canManageUsers: Boolean = false,
    val canReopenZReport: Boolean = false,
    val canManageBackup: Boolean = false,
    val canViewProfits: Boolean = false, // ADMIN ONLY PROFIT PROTECTION
    val isActive: Boolean = true,
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L,
    val lastLoginAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "z_reports")
data class ZReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateLabel: String,
    val openingCash: Long,
    val openingBankak: Long = 0L,
    val openingFawry: Long = 0L,
    val openingOcash: Long = 0L,
    val openingSahil: Long = 0L,
    val expectedCash: Long,
    val actualCash: Long,
    val expectedBankak: Long,
    val actualBankak: Long,
    val expectedFawry: Long,
    val actualFawry: Long,
    val expectedOcash: Long,
    val actualOcash: Long,
    val expectedSahil: Long,
    val actualSahil: Long,
    val totalVariance: Long,
    val notes: String = "",
    val isClosed: Boolean = true,
    val isReopened: Boolean = false,
    val reopenReason: String = "",
    val closedByUserId: Long,
    val closedByName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: AuditActionType,
    val entityType: String,
    val entityId: String,
    val summary: String,
    val beforeData: String = "",
    val afterData: String = "",
    val reason: String = "",
    val userId: Long,
    val userName: String,
    val timestamp: Long = System.currentTimeMillis()
)
