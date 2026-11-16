package com.example.domain

import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.SupplierEntity
import com.example.data.entity.WalletTransferEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

data class WalletSummary(
    val channel: WalletChannel,
    val openingBalance: Long = 0L,
    val salesReceipts: Long = 0L,
    val debtCollections: Long = 0L,
    val purchasePayments: Long = 0L,
    val expensePayments: Long = 0L,
    val customerRefunds: Long = 0L,
    val supplierPayments: Long = 0L,
    val transfersIn: Long = 0L,
    val transfersOut: Long = 0L
) {
    val totalInflows: Long
        get() = salesReceipts + debtCollections

    val totalOutflows: Long
        get() = purchasePayments + expensePayments + customerRefunds + supplierPayments

    val netBalance: Long
        get() = openingBalance + totalInflows - totalOutflows + transfersIn - transfersOut
}

data class DailyChartPoint(
    val dateLabel: String,
    val sales: Long,
    val profit: Long
)

data class ProductSalesStat(
    val productId: Long,
    val productName: String,
    val quantitySold: Double,
    val totalRevenue: Long
)

data class FinancialMetrics(
    val grossSales: Long = 0L,
    val totalDiscounts: Long = 0L,
    val netSales: Long = 0L,
    val salesCount: Int = 0,
    val averageInvoiceValue: Long = 0L,
    val totalCogs: Long = 0L,
    val grossProfit: Long = 0L,
    val totalExpenses: Long = 0L,
    val netOperatingProfit: Long = 0L,
    val totalCustomerReturns: Long = 0L,
    val totalPurchases: Long = 0L,
    val cashCollected: Long = 0L,
    val bankCollected: Long = 0L,
    val netCollection: Long = 0L,
    val totalInventoryValue: Long = 0L,
    val totalSupplierDebt: Long = 0L,
    val totalCustomerDebt: Long = 0L,
    val lowStockCount: Int = 0,
    val wallets: Map<WalletChannel, WalletSummary> = WalletChannel.entries.associateWith { WalletSummary(it) },
    val dailySeries: List<DailyChartPoint> = emptyList(),
    val expensesByCategory: Map<ExpenseCategory, Long> = emptyMap(),
    val salesByWallet: Map<WalletChannel, Long> = emptyMap(),
    val topSellingProducts: List<ProductSalesStat> = emptyList(),
    val slowMovingProducts: List<ProductEntity> = emptyList()
)

object FinancialCalculator {

    private val shortDateFormat = SimpleDateFormat("MM/dd", Locale.US)

    fun filterByDateRange(
        timestamp: Long,
        filter: DateFilter,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (filter == DateFilter.ALL) return true
        if (filter == DateFilter.CUSTOM) {
            val start = customStartMillis ?: 0L
            val end = customEndMillis ?: Long.MAX_VALUE
            return timestamp in start..end
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        return when (filter) {
            DateFilter.TODAY -> timestamp >= startOfToday
            DateFilter.YESTERDAY -> {
                val startOfYesterday = startOfToday - 24L * 60L * 60L * 1000L
                timestamp in startOfYesterday until startOfToday
            }
            DateFilter.LAST_7_DAYS -> {
                val sevenDaysAgo = startOfToday - 6L * 24L * 60L * 60L * 1000L
                timestamp >= sevenDaysAgo
            }
            DateFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                timestamp >= cal.timeInMillis
            }
            DateFilter.CUSTOM, DateFilter.ALL -> true
        }
    }

    fun computeMetrics(
        sales: List<SaleWithItems>,
        purchases: List<PurchaseEntity>,
        expenses: List<ExpenseEntity>,
        returns: List<ReturnEntity>,
        allocations: List<PaymentAllocationEntity>,
        debtPayments: List<DebtPaymentEntity>,
        transfers: List<WalletTransferEntity>,
        products: List<ProductEntity>,
        suppliers: List<SupplierEntity>,
        customers: List<CustomerEntity>,
        openingBalances: Map<WalletChannel, Long> = emptyMap()
    ): FinancialMetrics {
        val activeSales = sales.filter { !it.sale.isVoided }

        val grossSales = activeSales.sumOf { it.sale.subtotalAmount }
        val totalDiscounts = activeSales.sumOf { it.sale.discountAmount }
        val rawSalesTotal = activeSales.sumOf { it.sale.totalAmount }
        val rawSalesCogs = activeSales.sumOf { it.sale.totalCost }

        val customerReturns = returns.filter { it.returnType == "CUSTOMER_RETURN" }
        val totalCustomerReturns = customerReturns.sumOf { it.refundAmount }
        val returnedCogs = customerReturns.sumOf { it.reversedCostAmount }

        val netSales = rawSalesTotal - totalCustomerReturns
        val totalCogs = (rawSalesCogs - returnedCogs).coerceAtLeast(0L)
        val grossProfit = netSales - totalCogs
        val totalExpenses = expenses.sumOf { it.amount }
        val netOperatingProfit = grossProfit - totalExpenses
        val totalPurchases = purchases.sumOf { it.totalCost }

        val salesCount = activeSales.size
        val averageInvoiceValue = if (salesCount > 0) netSales / salesCount else 0L

        // Wallets computation from PaymentAllocations + DebtPayments + Returns + Transfers
        val salesReceiptsMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val purchasePayMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val expensePayMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val debtCollMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val supplierPayMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val refundMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val transInMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()
        val transOutMap = WalletChannel.entries.associateWith { 0L }.toMutableMap()

        allocations.forEach { alloc ->
            when (alloc.referenceType) {
                "SALE" -> salesReceiptsMap[alloc.channel] = (salesReceiptsMap[alloc.channel] ?: 0L) + alloc.amount
                "PURCHASE" -> purchasePayMap[alloc.channel] = (purchasePayMap[alloc.channel] ?: 0L) + alloc.amount
                "EXPENSE" -> expensePayMap[alloc.channel] = (expensePayMap[alloc.channel] ?: 0L) + alloc.amount
            }
        }

        debtPayments.forEach { dp ->
            if (dp.partyType == "CUSTOMER") {
                debtCollMap[dp.walletChannel] = (debtCollMap[dp.walletChannel] ?: 0L) + dp.amount
            } else if (dp.partyType == "SUPPLIER") {
                supplierPayMap[dp.walletChannel] = (supplierPayMap[dp.walletChannel] ?: 0L) + dp.amount
            }
        }

        customerReturns.forEach { ret ->
            if (ret.refundAmount > 0L) {
                refundMap[ret.walletChannel] = (refundMap[ret.walletChannel] ?: 0L) + ret.refundAmount
            }
        }

        transfers.forEach { tr ->
            transOutMap[tr.fromChannel] = (transOutMap[tr.fromChannel] ?: 0L) + tr.amount
            transInMap[tr.toChannel] = (transInMap[tr.toChannel] ?: 0L) + tr.amount
        }

        val wallets = WalletChannel.entries.associateWith { ch ->
            WalletSummary(
                channel = ch,
                openingBalance = openingBalances[ch] ?: 0L,
                salesReceipts = salesReceiptsMap[ch] ?: 0L,
                debtCollections = debtCollMap[ch] ?: 0L,
                purchasePayments = purchasePayMap[ch] ?: 0L,
                expensePayments = expensePayMap[ch] ?: 0L,
                customerRefunds = refundMap[ch] ?: 0L,
                supplierPayments = supplierPayMap[ch] ?: 0L,
                transfersIn = transInMap[ch] ?: 0L,
                transfersOut = transOutMap[ch] ?: 0L
            )
        }

        val cashCollected = (salesReceiptsMap[WalletChannel.CASH] ?: 0L) + (debtCollMap[WalletChannel.CASH] ?: 0L)
        val bankCollected = WalletChannel.entries
            .filter { it != WalletChannel.CASH }
            .sumOf { (salesReceiptsMap[it] ?: 0L) + (debtCollMap[it] ?: 0L) }
        val netCollection = cashCollected + bankCollected - totalCustomerReturns

        val totalInventoryValue = products.sumOf {
            (it.stockQuantity.coerceAtLeast(0.0) * it.averageCost.toDouble()).roundToLong()
        }
        val lowStockCount = products.count { it.stockQuantity <= it.lowStockThreshold }
        val totalSupplierDebt = suppliers.sumOf { it.balanceDue }
        val totalCustomerDebt = customers.sumOf { it.balanceDue }

        // Daily Sales & Profit Series
        val groupedByDay = activeSales.groupBy { shortDateFormat.format(Date(it.sale.timestamp)) }
        val dailySeries = groupedByDay.entries
            .take(7)
            .map { (dayLabel, daySales) ->
                val dayRev = daySales.sumOf { it.sale.totalAmount }
                val dayProfit = daySales.sumOf { it.sale.totalProfit }
                DailyChartPoint(dayLabel, dayRev, dayProfit)
            }
            .reversed()

        val expensesByCategory = expenses
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val productStatMap = mutableMapOf<Long, ProductSalesStat>()
        activeSales.forEach { sw ->
            sw.items.forEach { item ->
                val existing = productStatMap[item.productId]
                productStatMap[item.productId] = if (existing == null) {
                    ProductSalesStat(
                        productId = item.productId,
                        productName = item.productName,
                        quantitySold = item.quantity,
                        totalRevenue = item.lineTotal
                    )
                } else {
                    existing.copy(
                        quantitySold = existing.quantitySold + item.quantity,
                        totalRevenue = existing.totalRevenue + item.lineTotal
                    )
                }
            }
        }
        val topSelling = productStatMap.values.sortedByDescending { it.totalRevenue }.take(5)
        val soldIds = productStatMap.keys
        val slowMoving = products.filter { it.id !in soldIds }.take(5)

        return FinancialMetrics(
            grossSales = grossSales,
            totalDiscounts = totalDiscounts,
            netSales = netSales,
            salesCount = salesCount,
            averageInvoiceValue = averageInvoiceValue,
            totalCogs = totalCogs,
            grossProfit = grossProfit,
            totalExpenses = totalExpenses,
            netOperatingProfit = netOperatingProfit,
            totalCustomerReturns = totalCustomerReturns,
            totalPurchases = totalPurchases,
            cashCollected = cashCollected,
            bankCollected = bankCollected,
            netCollection = netCollection,
            totalInventoryValue = totalInventoryValue,
            totalSupplierDebt = totalSupplierDebt,
            totalCustomerDebt = totalCustomerDebt,
            lowStockCount = lowStockCount,
            wallets = wallets,
            dailySeries = dailySeries,
            expensesByCategory = expensesByCategory,
            salesByWallet = salesReceiptsMap,
            topSellingProducts = topSelling,
            slowMovingProducts = slowMoving
        )
    }
}
