package com.example

import com.example.data.SeedData
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SaleEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SaleWithItems
import com.example.domain.ExpenseCategory
import com.example.domain.FinancialCalculator
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.PricingMode
import com.example.domain.ProductCategory
import com.example.domain.WalletChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KikiDomainUnitTest {

    @Test
    fun qa1_smartPricing_markupMode_andReverseCalculation() {
        // Section 21 Test 1a: Cost = 10,000, Markup = 20% => Selling = 12,000
        val selling = PricingEngine.calculateSellingPrice(
            costPrice = 10000L,
            targetPercent = 20.0,
            mode = PricingMode.MARKUP,
            roundingStep = 1
        )
        assertEquals(12000L, selling)

        // Reverse Markup
        val reverseMarkup = PricingEngine.calculateMarkupPercent(costPrice = 10000L, sellingPrice = 12000L)
        assertEquals(20.0, reverseMarkup, 0.01)
    }

    @Test
    fun qa1_smartPricing_marginMode_andReverseCalculation() {
        // Section 21 Test 1b: Cost = 10,000, Margin = 20% => 10,000 / 0.8 = 12,500
        val selling = PricingEngine.calculateSellingPrice(
            costPrice = 10000L,
            targetPercent = 20.0,
            mode = PricingMode.MARGIN,
            roundingStep = 1
        )
        assertEquals(12500L, selling)

        val reverseMargin = PricingEngine.calculateMarginPercent(costPrice = 10000L, sellingPrice = 12500L)
        assertEquals(20.0, reverseMargin, 0.01)
    }

    @Test
    fun qa2_inventory_movingWeightedAverageCost_andLineProfit() {
        // 50 units @ 10,000 + 50 units @ 12,000 => Weighted Average = 11,000
        val newAvg = PricingEngine.calculateMovingWeightedAverageCost(
            previousQty = 50.0,
            previousAvgCost = 10000L,
            purchasedQty = 50.0,
            purchasedTotalNetCost = 50L * 12000L
        )
        assertEquals(11000L, newAvg)

        // Stock 100 -> sell 10 => remaining 90, line COGS = 110,000, sell @ 14,000 => profit = 30,000
        val lineTotal = PricingEngine.calculateLineTotal(unitPrice = 14000L, quantity = 10.0)
        val lineProfit = PricingEngine.calculateLineProfit(sellingUnitPrice = 14000L, costUnitPrice = newAvg, quantity = 10.0)
        assertEquals(140000L, lineTotal)
        assertEquals(30000L, lineProfit)
        assertEquals(90.0, 100.0 - 10.0, 0.001)
    }

    @Test
    fun qa3_splitPayment_50000_acrossThreeWallets_balancesAccurately() {
        val sale = SaleWithItems(
            sale = SaleEntity(
                id = 1L,
                invoiceNumber = "KIKI-1001",
                subtotalAmount = 50000L,
                totalAmount = 50000L,
                paidAmount = 50000L,
                totalCost = 35000L,
                totalProfit = 15000L,
                paymentSummary = "دفع مشترك",
                cashierUserId = 1L,
                cashierName = "المدير العام"
            ),
            items = listOf(
                SaleItemEntity(
                    id = 1L,
                    saleId = 1L,
                    productId = 1L,
                    productName = "فراخ",
                    unit = PackagingUnit.KILO,
                    quantity = 2.0,
                    sellingPriceAtSale = 25000L,
                    costPriceAtSale = 17500L,
                    lineTotal = 50000L,
                    lineProfit = 15000L
                )
            )
        )
        val allocations = listOf(
            PaymentAllocationEntity(id = 1L, referenceType = "SALE", referenceId = 1L, channel = WalletChannel.CASH, amount = 20000L),
            PaymentAllocationEntity(id = 2L, referenceType = "SALE", referenceId = 1L, channel = WalletChannel.BANKAK, amount = 20000L),
            PaymentAllocationEntity(id = 3L, referenceType = "SALE", referenceId = 1L, channel = WalletChannel.FAWRY, amount = 10000L)
        )

        val metrics = FinancialCalculator.computeMetrics(
            sales = listOf(sale),
            purchases = emptyList(),
            expenses = emptyList(),
            returns = emptyList(),
            allocations = allocations,
            debtPayments = emptyList(),
            transfers = emptyList(),
            products = emptyList(),
            suppliers = emptyList(),
            customers = emptyList()
        )

        assertEquals(50000L, metrics.netSales)
        assertEquals(20000L, metrics.wallets[WalletChannel.CASH]?.netBalance)
        assertEquals(20000L, metrics.wallets[WalletChannel.BANKAK]?.netBalance)
        assertEquals(10000L, metrics.wallets[WalletChannel.FAWRY]?.netBalance)
        assertEquals(0L, metrics.wallets[WalletChannel.OCASH]?.netBalance)
        assertEquals(0L, metrics.wallets[WalletChannel.SAHIL]?.netBalance)
    }

    @Test
    fun qa4_profitCalculation_grossAndNetOperatingProfit() {
        // Sales = 100,000, COGS = 70,000, Expenses = 5,000 => Gross Profit = 30,000, Net Profit = 25,000
        val sale = SaleWithItems(
            sale = SaleEntity(
                id = 10L,
                invoiceNumber = "KIKI-1010",
                subtotalAmount = 100000L,
                totalAmount = 100000L,
                paidAmount = 100000L,
                totalCost = 70000L,
                totalProfit = 30000L,
                paymentSummary = "كاش",
                cashierUserId = 1L,
                cashierName = "المدير"
            ),
            items = emptyList()
        )
        val expense = ExpenseEntity(
            id = 1L,
            expenseNumber = "EXP-1",
            title = "كهرباء",
            category = ExpenseCategory.ELECTRICITY,
            description = "فاتورة",
            amount = 5000L,
            paymentSummary = "كاش",
            recordedByUserId = 1L,
            recordedByName = "المدير"
        )
        val allocations = listOf(
            PaymentAllocationEntity(referenceType = "SALE", referenceId = 10L, channel = WalletChannel.CASH, amount = 100000L),
            PaymentAllocationEntity(referenceType = "EXPENSE", referenceId = 1L, channel = WalletChannel.CASH, amount = 5000L)
        )

        val metrics = FinancialCalculator.computeMetrics(
            sales = listOf(sale),
            purchases = emptyList(),
            expenses = listOf(expense),
            returns = emptyList(),
            allocations = allocations,
            debtPayments = emptyList(),
            transfers = emptyList(),
            products = emptyList(),
            suppliers = emptyList(),
            customers = emptyList()
        )

        assertEquals(100000L, metrics.netSales)
        assertEquals(70000L, metrics.totalCogs)
        assertEquals(30000L, metrics.grossProfit)
        assertEquals(5000L, metrics.totalExpenses)
        assertEquals(25000L, metrics.netOperatingProfit)
    }

    @Test
    fun qa5_zReportReconciliation_shortageDetection() {
        // Opening Cash = 20,000 + Cash Sales = 50,000 - Cash Expenses = 10,000 => Expected Cash = 60,000
        // Actual Count = 58,000 => Variance = -2,000 (Shortage)
        val allocations = listOf(
            PaymentAllocationEntity(referenceType = "SALE", referenceId = 1L, channel = WalletChannel.CASH, amount = 50000L),
            PaymentAllocationEntity(referenceType = "EXPENSE", referenceId = 1L, channel = WalletChannel.CASH, amount = 10000L)
        )
        val metrics = FinancialCalculator.computeMetrics(
            sales = emptyList(),
            purchases = emptyList(),
            expenses = emptyList(),
            returns = emptyList(),
            allocations = allocations,
            debtPayments = emptyList(),
            transfers = emptyList(),
            products = emptyList(),
            suppliers = emptyList(),
            customers = emptyList(),
            openingBalances = mapOf(WalletChannel.CASH to 20000L)
        )

        val expectedCash = metrics.wallets[WalletChannel.CASH]?.netBalance ?: 0L
        assertEquals(60000L, expectedCash)
        val actualCount = 58000L
        val variance = actualCount - expectedCash
        assertEquals(-2000L, variance)
    }

    @Test
    fun qa6_seedData_35Products_andProfitProtectionRoles() {
        val products = SeedData.defaultProducts()
        assertEquals(35, products.size)
        assertEquals(26000L, products.first { it.name == "فراخ" }.sellingPrice)
        assertEquals(60000L, products.first { it.name == "جبنة موزريلا" }.sellingPrice)
        assertEquals(34000L, products.first { it.name == "دكوة" }.sellingPrice)

        val users = SeedData.defaultUsers()
        val admin = users.first { it.isAdmin }
        val cashier = users.first { !it.isAdmin }
        assertTrue(admin.mustChangePin)
        assertTrue(admin.canViewProfits)
        assertFalse(cashier.canViewProfits)
        assertFalse(cashier.canManageUsers)
    }
}
