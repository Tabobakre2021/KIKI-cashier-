package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.KikiApplication
import com.example.data.AutoBackupWorker
import com.example.data.BackupManager
import com.example.data.BackupRecordInfo
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.PaymentAllocationEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.WalletTransferEntity
import com.example.data.entity.ZReportEntity
import com.example.data.repository.AppSettings
import com.example.data.repository.CartItemDraft
import com.example.data.repository.PaymentSplitLine
import com.example.domain.BackupFrequency
import com.example.domain.DateFilter
import com.example.domain.ExpenseCategory
import com.example.domain.FinancialCalculator
import com.example.domain.FinancialMetrics
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.StockMovementType
import com.example.domain.ThemePreference
import com.example.domain.WalletChannel
import com.example.export.ExportManager
import com.example.export.ReportCategory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HeldInvoiceDraft(
    val id: Long,
    val label: String,
    val items: List<CartItemDraft>,
    val createdAt: Long = System.currentTimeMillis()
)

data class KikiUiState(
    val isReady: Boolean = false,
    val products: List<ProductEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val currentUser: UserEntity? = null,
    val settings: AppSettings = AppSettings(),
    val dateFilter: DateFilter = DateFilter.TODAY,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null,
    val filteredSales: List<SaleWithItems> = emptyList(),
    val filteredPurchases: List<PurchaseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val filteredReturns: List<ReturnEntity> = emptyList(),
    val filteredAllocations: List<PaymentAllocationEntity> = emptyList(),
    val filteredDebtPayments: List<DebtPaymentEntity> = emptyList(),
    val filteredTransfers: List<WalletTransferEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val allZReports: List<ZReportEntity> = emptyList(),
    val auditLogs: List<AuditLogEntity> = emptyList(),
    val localBackups: List<BackupRecordInfo> = emptyList(),
    val metrics: FinancialMetrics = FinancialMetrics(),
    val cart: List<CartItemDraft> = emptyList(),
    val heldCarts: List<HeldInvoiceDraft> = emptyList(),
    val lastCompletedSale: SaleWithItems? = null
) {
    val cartSubtotal: Long
        get() = cart.sumOf { PricingEngine.calculateLineTotal(it.effectiveUnitPrice, it.quantity) }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as KikiApplication
    private val repository = app.repository
    private val db = app.database

    private val dateFilterFlow = MutableStateFlow(DateFilter.TODAY)
    private val customRangeFlow = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    private val cartFlow = MutableStateFlow<List<CartItemDraft>>(emptyList())
    private val heldCartsFlow = MutableStateFlow<List<HeldInvoiceDraft>>(emptyList())
    private val lastSaleFlow = MutableStateFlow<SaleWithItems?>(null)
    private val seededFlow = MutableStateFlow(false)
    private val backupsFlow = MutableStateFlow<List<BackupRecordInfo>>(emptyList())

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshBackups()
            seededFlow.value = true
        }
    }

    private data class CoreBundle(
        val ready: Boolean,
        val products: List<ProductEntity>,
        val users: List<UserEntity>,
        val activeUserId: Long?,
        val settings: AppSettings
    )

    private data class FilterBundle(
        val filter: DateFilter,
        val customRange: Pair<Long?, Long?>,
        val cart: List<CartItemDraft>,
        val heldCarts: List<HeldInvoiceDraft>,
        val lastSale: SaleWithItems?,
        val backups: List<BackupRecordInfo>
    )

    private data class LedgerA(
        val sales: List<SaleWithItems>,
        val purchases: List<PurchaseEntity>,
        val expenses: List<ExpenseEntity>,
        val returns: List<ReturnEntity>,
        val allocations: List<PaymentAllocationEntity>
    )

    private data class LedgerB(
        val debtPayments: List<DebtPaymentEntity>,
        val transfers: List<WalletTransferEntity>,
        val movements: List<StockMovementEntity>,
        val suppliers: List<SupplierEntity>,
        val customers: List<CustomerEntity>,
        val zReports: List<ZReportEntity>,
        val auditLogs: List<AuditLogEntity>
    )

    private val coreFlow = combine(
        seededFlow,
        repository.products,
        repository.users,
        repository.activeUserIdFlow,
        repository.settingsFlow
    ) { ready, products, users, activeId, settings ->
        CoreBundle(ready, products, users, activeId, settings)
    }

    private val cartsPairFlow = combine(cartFlow, heldCartsFlow) { cart, held -> cart to held }

    private val filterFlow = combine(
        dateFilterFlow,
        customRangeFlow,
        cartsPairFlow,
        lastSaleFlow,
        backupsFlow
    ) { f, range, cartsPair, lastSale, backups ->
        FilterBundle(f, range, cartsPair.first, cartsPair.second, lastSale, backups)
    }

    private val ledgerAFlow = combine(
        repository.salesWithItems,
        repository.purchases,
        repository.expenses,
        repository.returns,
        repository.paymentAllocations
    ) { s, p, e, r, a -> LedgerA(s, p, e, r, a) }

    private val ledgerBFlow = combine(
        repository.debtPayments,
        repository.walletTransfers,
        repository.stockMovements,
        repository.suppliers,
        repository.customers,
        repository.zReports,
        repository.auditLogs
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        LedgerB(
            debtPayments = arr[0] as List<DebtPaymentEntity>,
            transfers = arr[1] as List<WalletTransferEntity>,
            movements = arr[2] as List<StockMovementEntity>,
            suppliers = arr[3] as List<SupplierEntity>,
            customers = arr[4] as List<CustomerEntity>,
            zReports = arr[5] as List<ZReportEntity>,
            auditLogs = arr[6] as List<AuditLogEntity>
        )
    }

    val uiState: StateFlow<KikiUiState> = combine(
        coreFlow,
        filterFlow,
        ledgerAFlow,
        ledgerBFlow
    ) { core, filt, la, lb ->
        val currentUser = core.users.firstOrNull { it.id == core.activeUserId && it.isActive }
        val now = System.currentTimeMillis()
        val (cStart, cEnd) = filt.customRange

        val fSales = la.sales.filter {
            FinancialCalculator.filterByDateRange(it.sale.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fPurchases = la.purchases.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fExpenses = la.expenses.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fReturns = la.returns.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fAllocs = la.allocations.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fDebtPays = lb.debtPayments.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }
        val fTransfers = lb.transfers.filter {
            FinancialCalculator.filterByDateRange(it.timestamp, filt.filter, cStart, cEnd, now)
        }

        val rawMetrics = FinancialCalculator.computeMetrics(
            sales = fSales,
            purchases = fPurchases,
            expenses = fExpenses,
            returns = fReturns,
            allocations = fAllocs,
            debtPayments = fDebtPays,
            transfers = fTransfers,
            products = core.products,
            suppliers = lb.suppliers,
            customers = lb.customers,
            openingBalances = core.settings.openingBalances
        )

        // Business-layer Profit Protection Enforcement (Section 16)
        val canViewProfits = currentUser?.canViewProfits == true
        val guardedMetrics = if (canViewProfits) rawMetrics else rawMetrics.copy(
            totalCogs = 0L,
            grossProfit = 0L,
            netOperatingProfit = 0L,
            totalPurchases = 0L,
            totalInventoryValue = 0L,
            dailySeries = rawMetrics.dailySeries.map { it.copy(profit = 0L) }
        )

        val guardedProducts = if (canViewProfits) core.products else core.products.map {
            it.copy(costPrice = 0L, lastPurchasePrice = 0L, averageCost = 0L, profitMarginPercent = 0.0)
        }

        val guardedSales = if (canViewProfits) fSales else fSales.map { sw ->
            sw.copy(
                sale = sw.sale.copy(totalCost = 0L, totalProfit = 0L),
                items = sw.items.map { it.copy(costPriceAtSale = 0L, lineProfit = 0L) }
            )
        }

        KikiUiState(
            isReady = core.ready,
            products = guardedProducts,
            users = core.users,
            currentUser = currentUser,
            settings = core.settings,
            dateFilter = filt.filter,
            customStartMillis = cStart,
            customEndMillis = cEnd,
            filteredSales = guardedSales,
            filteredPurchases = if (canViewProfits || currentUser?.canRecordPurchases == true) fPurchases else emptyList(),
            filteredExpenses = fExpenses,
            filteredReturns = fReturns,
            filteredAllocations = fAllocs,
            filteredDebtPayments = fDebtPays,
            filteredTransfers = fTransfers,
            stockMovements = lb.movements,
            suppliers = lb.suppliers,
            customers = lb.customers,
            allZReports = lb.zReports,
            auditLogs = lb.auditLogs,
            localBackups = filt.backups,
            metrics = guardedMetrics,
            cart = filt.cart,
            heldCarts = filt.heldCarts,
            lastCompletedSale = filt.lastSale
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KikiUiState()
    )

    fun refreshBackups() {
        backupsFlow.value = BackupManager.listLocalBackups(app)
    }

    fun setDateFilter(filter: DateFilter, customStart: Long? = null, customEnd: Long? = null) {
        dateFilterFlow.value = filter
        if (filter == DateFilter.CUSTOM) {
            customRangeFlow.value = customStart to customEnd
        }
    }

    // --- Authentication ---
    suspend fun attemptPinLogin(user: UserEntity, pin: String): Pair<Boolean, String?> {
        val now = System.currentTimeMillis()
        if (!user.isActive) return false to "هذا الحساب موقوف حالياً"
        if (user.lockedUntilMillis > now) {
            val sec = ((user.lockedUntilMillis - now) / 1000L).coerceAtLeast(1L)
            return false to "الحساب مقفل مؤقتاً بعد محاولات خاطئة. انتظر $sec ثانية"
        }
        return if (user.pinCode == pin) {
            repository.recordLoginResult(user, true, uiState.value.settings.maxPinAttempts)
            repository.setActiveUserId(user.id)
            true to null
        } else {
            val updated = repository.recordLoginResult(user, false, uiState.value.settings.maxPinAttempts)
            if (updated.lockedUntilMillis > now) {
                false to "تم قفل الحساب لمدة دقيقة لتجاوز الحد المسموح"
            } else {
                val left = (uiState.value.settings.maxPinAttempts - updated.failedAttempts).coerceAtLeast(0)
                false to "رمز PIN غير صحيح (المحاولات المتبقية: $left)"
            }
        }
    }

    fun completeMandatoryPinChange(newPin: String) {
        val user = uiState.value.currentUser ?: return
        if (newPin.length < 4) return
        viewModelScope.launch {
            repository.updateUserPin(user, newPin)
            emitMessage("تم تعيين رمز PIN الجديد بنجاح")
        }
    }

    fun logout() {
        val user = uiState.value.currentUser
        viewModelScope.launch {
            if (user != null) {
                repository.logAudit(
                    action = com.example.domain.AuditActionType.USER_LOGOUT,
                    entityType = "USER",
                    entityId = user.id.toString(),
                    summary = "تسجيل خروج المستخدم: ${user.displayName}",
                    user = user
                )
            }
            cartFlow.value = emptyList()
            repository.setActiveUserId(null)
        }
    }

    // --- POS Cart & Sales ---
    fun addToCart(
        product: ProductEntity,
        quantity: Double = 1.0,
        unit: PackagingUnit = product.defaultUnit,
        conversionMultiplier: Double = 1.0,
        customUnitPrice: Long? = null
    ) {
        val effectivePrice = customUnitPrice?.takeIf { it > 0L } ?: product.sellingPrice
        if (effectivePrice <= 0L) {
            emitMessage("يرجى تسعير الصنف (${product.name}) أولاً")
            return
        }
        val allowNegative = uiState.value.settings.allowNegativeStock
        val current = cartFlow.value.toMutableList()
        val idx = current.indexOfFirst {
            it.product.id == product.id && it.unit == unit && it.customUnitPrice == customUnitPrice
        }
        val existingQty = current.filter { it.product.id == product.id }.sumOf { it.quantity * it.conversionMultiplier }
        val newNeeded = existingQty + (quantity * conversionMultiplier)
        if (!allowNegative && newNeeded > product.stockQuantity) {
            emitMessage("الرصيد المتاح من (${product.name}) هو ${PricingEngine.formatQty(product.stockQuantity)} فقط")
            return
        }
        if (idx >= 0) {
            current[idx] = current[idx].copy(quantity = current[idx].quantity + quantity)
        } else {
            current.add(CartItemDraft(product, unit, quantity, conversionMultiplier, customUnitPrice))
        }
        cartFlow.value = current
    }

    fun decrementProductFromCart(productId: Long, step: Double = 1.0) {
        val current = cartFlow.value.toMutableList()
        val idx = current.indexOfLast { it.product.id == productId }
        if (idx < 0) return
        val item = current[idx]
        val nextQty = item.quantity - step
        if (nextQty <= 0.0001) {
            current.removeAt(idx)
        } else {
            current[idx] = item.copy(quantity = nextQty)
        }
        cartFlow.value = current
    }

    fun updateCartItem(
        index: Int,
        quantity: Double,
        unit: PackagingUnit,
        conversionMultiplier: Double? = null,
        customUnitPrice: Long? = null,
        keepExistingPrice: Boolean = true
    ) {
        val current = cartFlow.value.toMutableList()
        if (index !in current.indices) return
        if (quantity <= 0.0) {
            current.removeAt(index)
            cartFlow.value = current
            return
        }
        val old = current[index]
        val mult = conversionMultiplier ?: old.conversionMultiplier
        val allowNegative = uiState.value.settings.allowNegativeStock
        val otherQty = current.filterIndexed { i, item -> i != index && item.product.id == old.product.id }
            .sumOf { it.quantity * it.conversionMultiplier }
        val totalNeeded = otherQty + (quantity * mult)
        if (!allowNegative && totalNeeded > old.product.stockQuantity) {
            emitMessage("الرصيد المتاح من (${old.product.name}) هو ${PricingEngine.formatQty(old.product.stockQuantity)} فقط")
            return
        }
        current[index] = old.copy(
            quantity = quantity,
            unit = unit,
            conversionMultiplier = mult,
            customUnitPrice = if (keepExistingPrice) old.customUnitPrice else customUnitPrice
        )
        cartFlow.value = current
    }

    fun holdCurrentCart(customLabel: String = "") {
        val items = cartFlow.value
        if (items.isEmpty()) return
        val heldList = heldCartsFlow.value.toMutableList()
        val num = heldList.size + 1
        val label = customLabel.trim().ifBlank { "فاتورة معلقة #$num" }
        heldList.add(HeldInvoiceDraft(id = System.currentTimeMillis(), label = label, items = items))
        heldCartsFlow.value = heldList
        cartFlow.value = emptyList()
        emitMessage("تم تعليق الفاتورة ($label)")
    }

    fun resumeHeldCart(heldId: Long) {
        val heldList = heldCartsFlow.value.toMutableList()
        val target = heldList.firstOrNull { it.id == heldId } ?: return
        heldList.removeAll { it.id == heldId }
        if (cartFlow.value.isNotEmpty()) {
            heldList.add(
                HeldInvoiceDraft(
                    id = System.currentTimeMillis(),
                    label = "فاتورة معلقة #${heldList.size + 1}",
                    items = cartFlow.value
                )
            )
        }
        heldCartsFlow.value = heldList
        cartFlow.value = target.items
    }

    fun discardHeldCart(heldId: Long) {
        heldCartsFlow.value = heldCartsFlow.value.filterNot { it.id == heldId }
    }

    fun clearCart() {
        cartFlow.value = emptyList()
    }

    fun clearLastCompletedSale() {
        lastSaleFlow.value = null
    }

    fun completeSale(
        discountAmount: Long,
        allocations: List<PaymentSplitLine>,
        customerId: Long?,
        receiptNote: String
    ) {
        val user = uiState.value.currentUser ?: return
        val items = cartFlow.value
        if (items.isEmpty()) return
        if (!user.canRecordSales) {
            emitMessage("لا تملك صلاحية تسجيل المبيعات")
            return
        }
        viewModelScope.launch {
            val result = repository.recordSale(
                cartItems = items,
                discountAmount = discountAmount,
                allocations = allocations,
                customerId = customerId,
                receiptNote = receiptNote,
                allowNegativeStock = uiState.value.settings.allowNegativeStock,
                cashier = user
            )
            result.onSuccess { savedSale ->
                cartFlow.value = emptyList()
                lastSaleFlow.value = savedSale
                emitMessage("تم إصدار الفاتورة ${savedSale.sale.invoiceNumber} بنجاح")
            }.onFailure { err ->
                emitMessage(err.message ?: "تعذر إتمام الفاتورة")
            }
        }
    }

    // --- Purchases ---
    fun recordPurchase(
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
        receiptNote: String
    ) {
        val user = uiState.value.currentUser ?: return
        if (!user.canRecordPurchases) {
            emitMessage("لا تملك صلاحية تسجيل المشتريات")
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.recordPurchase(
                    product = product,
                    supplierId = supplierId,
                    supplierName = supplierName,
                    unit = unit,
                    quantity = quantity,
                    conversionMultiplier = conversionMultiplier,
                    unitCostPrice = unitCostPrice,
                    discountAmount = discountAmount,
                    transportCost = transportCost,
                    newTargetPercent = newTargetPercent,
                    newSellingPrice = newSellingPrice,
                    updateProductPricing = updateProductPricing,
                    allocations = allocations,
                    receiptNote = receiptNote,
                    recordedBy = user
                )
            }.onSuccess {
                emitMessage("تم حفظ فاتورة التوريد وتحديث متوسط التكلفة والمخزون")
            }.onFailure {
                emitMessage(it.message ?: "تعذر حفظ المشتريات")
            }
        }
    }

    // --- Expenses ---
    fun recordExpense(
        title: String,
        category: ExpenseCategory,
        description: String,
        amount: Long,
        allocations: List<PaymentSplitLine>,
        receiptNote: String
    ) {
        val user = uiState.value.currentUser ?: return
        if (!user.canRecordExpenses) {
            emitMessage("لا تملك صلاحية تسجيل المصروفات")
            return
        }
        viewModelScope.launch {
            repository.recordExpense(title, category, description, amount, allocations, receiptNote, user)
            emitMessage("تم تسجيل المصروف وخصمه من المحفظة المحددة")
        }
    }

    // --- Products ---
    fun saveProduct(product: ProductEntity) {
        val user = uiState.value.currentUser ?: return
        val canSave = if (product.id == 0L) (user.canAddProducts || user.isAdmin) else (user.canEditProducts || user.canEditPrices || user.isAdmin)
        if (!canSave) {
            emitMessage("لا تملك صلاحية تعديل أو إضافة الأصناف")
            return
        }
        viewModelScope.launch {
            repository.saveProduct(product, user)
            emitMessage("تم حفظ بيانات الصنف (${product.name})")
        }
    }

    fun deleteProduct(productId: Long) {
        val user = uiState.value.currentUser ?: return
        if (!user.isAdmin && !user.canEditProducts) return
        viewModelScope.launch {
            repository.deleteProduct(productId, user)
            emitMessage("تم حذف الصنف")
        }
    }

    // --- Returns & Adjustments ---
    fun recordCustomerReturn(
        sale: SaleWithItems,
        item: SaleItemEntity,
        returnQty: Double,
        restockEligible: Boolean,
        refundChannel: WalletChannel,
        reason: String
    ) {
        val user = uiState.value.currentUser ?: return
        if (!user.canReverseOrDelete && !user.isAdmin) {
            emitMessage("لا تملك صلاحية تسجيل المرتجعات")
            return
        }
        viewModelScope.launch {
            repository.recordCustomerReturn(sale, item, returnQty, restockEligible, refundChannel, reason, user)
            emitMessage("تم تسجيل مرتجع العميل وعكس الأثر المالي والمخزني")
        }
    }

    fun recordStockAdjustmentOrWaste(
        product: ProductEntity,
        movementType: StockMovementType,
        quantity: Double,
        reason: String,
        refundWallet: WalletChannel = WalletChannel.CASH
    ) {
        val user = uiState.value.currentUser ?: return
        if (!user.canEditProducts && !user.canReverseOrDelete && !user.isAdmin) {
            emitMessage("لا تملك صلاحية تسوية المخزون")
            return
        }
        viewModelScope.launch {
            repository.recordStockAdjustmentOrWaste(product, movementType, quantity, reason, refundWallet, user)
            emitMessage("تم تسجيل حركة المخزون (${movementType.arabicLabel})")
        }
    }

    // --- Parties & Wallet Transfers ---
    fun saveSupplier(supplier: SupplierEntity) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageParties && !user.isAdmin) return
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            emitMessage("تم حفظ بيانات المورد (${supplier.name})")
        }
    }

    fun saveCustomer(customer: CustomerEntity) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageParties && !user.isAdmin) return
        viewModelScope.launch {
            repository.saveCustomer(customer)
            emitMessage("تم حفظ بيانات العميل (${customer.name})")
        }
    }

    fun recordDebtPayment(partyType: String, partyId: Long, amount: Long, wallet: WalletChannel, note: String) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageParties && !user.isAdmin) {
            emitMessage("لا تملك صلاحية إدارة الموردين والعملاء")
            return
        }
        viewModelScope.launch {
            repository.recordDebtPayment(partyType, partyId, amount, wallet, note, user)
            emitMessage("تم تسجيل السداد/التحصيل وتحديث رصيد المحفظة")
        }
    }

    fun recordWalletTransfer(from: WalletChannel, to: WalletChannel, amount: Long, note: String) {
        val user = uiState.value.currentUser ?: return
        if (!user.canViewZReport && !user.isAdmin) {
            emitMessage("لا تملك صلاحية التحويل بين المحافظ")
            return
        }
        viewModelScope.launch {
            repository.recordWalletTransfer(from, to, amount, note, user)
            emitMessage("تم التحويل بين المحافظ بنجاح")
        }
    }

    // --- Z-Report ---
    fun saveOpeningBalances(balances: Map<WalletChannel, Long>) {
        viewModelScope.launch {
            repository.setOpeningBalances(balances)
            emitMessage("تم تحديث الأرصدة الافتتاحية")
        }
    }

    fun saveZReport(
        openingBalances: Map<WalletChannel, Long>,
        actualBalances: Map<WalletChannel, Long>,
        notes: String
    ) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canViewZReport && !user.isAdmin) {
            emitMessage("لا تملك صلاحية تقفيل اليومية")
            return
        }
        val expectedMap = WalletChannel.entries.associateWith { ch ->
            val w = state.metrics.wallets[ch]
            val baseOpen = openingBalances[ch] ?: 0L
            baseOpen + (w?.totalInflows ?: 0L) - (w?.totalOutflows ?: 0L) + (w?.transfersIn ?: 0L) - (w?.transfersOut ?: 0L)
        }
        val totalVariance = WalletChannel.entries.sumOf { ch ->
            (actualBalances[ch] ?: 0L) - (expectedMap[ch] ?: 0L)
        }
        val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        viewModelScope.launch {
            repository.setOpeningBalances(openingBalances)
            repository.saveZReport(
                ZReportEntity(
                    dateLabel = dateLabel,
                    openingCash = openingBalances[WalletChannel.CASH] ?: 0L,
                    openingBankak = openingBalances[WalletChannel.BANKAK] ?: 0L,
                    openingFawry = openingBalances[WalletChannel.FAWRY] ?: 0L,
                    openingOcash = openingBalances[WalletChannel.OCASH] ?: 0L,
                    openingSahil = openingBalances[WalletChannel.SAHIL] ?: 0L,
                    expectedCash = expectedMap[WalletChannel.CASH] ?: 0L,
                    actualCash = actualBalances[WalletChannel.CASH] ?: 0L,
                    expectedBankak = expectedMap[WalletChannel.BANKAK] ?: 0L,
                    actualBankak = actualBalances[WalletChannel.BANKAK] ?: 0L,
                    expectedFawry = expectedMap[WalletChannel.FAWRY] ?: 0L,
                    actualFawry = actualBalances[WalletChannel.FAWRY] ?: 0L,
                    expectedOcash = expectedMap[WalletChannel.OCASH] ?: 0L,
                    actualOcash = actualBalances[WalletChannel.OCASH] ?: 0L,
                    expectedSahil = expectedMap[WalletChannel.SAHIL] ?: 0L,
                    actualSahil = actualBalances[WalletChannel.SAHIL] ?: 0L,
                    totalVariance = totalVariance,
                    notes = notes.trim(),
                    isClosed = true,
                    closedByUserId = user.id,
                    closedByName = user.displayName
                ),
                actor = user
            )
            emitMessage("تم إغلاق وحفظ تقرير اليومية (Z-Report)")
        }
    }

    fun reopenZReport(report: ZReportEntity, reason: String) {
        val user = uiState.value.currentUser ?: return
        if (!user.canReopenZReport && !user.isAdmin) {
            emitMessage("لا تملك صلاحية إعادة فتح اليومية")
            return
        }
        if (reason.isBlank()) {
            emitMessage("يرجى كتابة سبب إعادة فتح اليومية للتوثيق")
            return
        }
        viewModelScope.launch {
            repository.reopenZReport(report, reason, user)
            emitMessage("تمت إعادة فتح اليومية وتسجيل السبب في سجل التدقيق")
        }
    }

    // --- Users ---
    fun saveUser(user: UserEntity) {
        val current = uiState.value.currentUser ?: return
        if (!current.canManageUsers && !current.isAdmin) return
        viewModelScope.launch {
            repository.saveUser(user, current)
            emitMessage("تم حفظ بيانات المستخدم (${user.displayName})")
        }
    }

    fun deleteUser(userId: Long) {
        val current = uiState.value.currentUser ?: return
        if (!current.canManageUsers && !current.isAdmin) return
        if (current.id == userId) {
            emitMessage("لا يمكن حذف الحساب النشط حالياً")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(userId, current)
            emitMessage("تم حذف المستخدم")
        }
    }

    // --- Settings & Backups ---
    fun updateSettings(
        themePreference: ThemePreference? = null,
        allowNegativeStock: Boolean? = null,
        allowCreditSales: Boolean? = null,
        pricingRoundingStep: Int? = null,
        maxPinAttempts: Int? = null,
        backupFrequency: BackupFrequency? = null
    ) {
        viewModelScope.launch {
            repository.updateSettings(
                themePreference, allowNegativeStock, allowCreditSales, pricingRoundingStep, maxPinAttempts, backupFrequency
            )
            backupFrequency?.let { AutoBackupWorker.schedule(app, it) }
            emitMessage("تم حفظ الإعدادات")
        }
    }

    fun createManualBackup() {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageBackup && !user.isAdmin) {
            emitMessage("لا تملك صلاحية النسخ الاحتياطي")
            return
        }
        viewModelScope.launch {
            runCatching { BackupManager.createLocalBackup(app, db, user, false) }
                .onSuccess {
                    refreshBackups()
                    emitMessage("تم إنشاء نسخة احتياطية مشفرة بنجاح")
                }
                .onFailure { emitMessage(it.message ?: "فشل إنشاء النسخة الاحتياطية") }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageBackup && !user.isAdmin) return
        viewModelScope.launch {
            runCatching { BackupManager.exportBackupToUri(app, db, uri, user) }
                .onSuccess {
                    refreshBackups()
                    emitMessage("تم تصدير ملف النسخة الاحتياطية بنجاح")
                }
                .onFailure { emitMessage(it.message ?: "تعذر تصدير النسخة") }
        }
    }

    fun restoreFromLocalBackup(filePath: String) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageBackup && !user.isAdmin) return
        viewModelScope.launch {
            val res = BackupManager.restoreFromLocalFile(db, File(filePath), user)
            res.onSuccess { msg ->
                refreshBackups()
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "فشلت استعادة النسخة الاحتياطية")
            }
        }
    }

    fun restoreFromUri(uri: Uri) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageBackup && !user.isAdmin) return
        viewModelScope.launch {
            val res = BackupManager.restoreFromUri(app, db, uri, user)
            res.onSuccess { msg ->
                refreshBackups()
                emitMessage(msg)
            }.onFailure { err ->
                emitMessage(err.message ?: "فشلت استعادة النسخة الاحتياطية")
            }
        }
    }

    fun shareBackupFile(context: Context, filePath: String) {
        val user = uiState.value.currentUser ?: return
        if (!user.canManageBackup && !user.isAdmin) return
        ExportManager.shareFile(context, File(filePath), "application/octet-stream", "مشاركة نسخة KIKI الاحتياطية")
    }

    // --- Exports ---
    fun copyWhatsAppSummary(context: Context) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canExportData && !user.isAdmin && !user.canViewZReport) {
            emitMessage("لا تملك صلاحية التصدير")
            return
        }
        val text = ExportManager.buildWhatsAppSummary(
            periodLabel = state.dateFilter.arabicLabel,
            metrics = state.metrics,
            canViewProfits = user.canViewProfits,
            latestZReport = state.allZReports.firstOrNull()
        )
        ExportManager.copyTextToClipboard(context, "KIKI Daily Report", text)
        emitMessage("تم نسخ ملخص التقرير للحافظة للواتساب")
    }

    fun shareSaleReceipt(context: Context, saleWithItems: SaleWithItems) {
        ExportManager.shareInvoiceReceiptImage(context, saleWithItems)
    }

    fun printSaleInvoice(context: Context, saleWithItems: SaleWithItems) {
        ExportManager.printSaleInvoice(context, saleWithItems)
    }

    fun exportXlsx(context: Context) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canExportData && !user.isAdmin) {
            emitMessage("لا تملك صلاحية التصدير")
            return
        }
        ExportManager.exportXlsxAndShare(
            context = context,
            periodLabel = state.dateFilter.arabicLabel,
            metrics = state.metrics,
            sales = state.filteredSales,
            purchases = state.filteredPurchases,
            expenses = state.filteredExpenses,
            products = state.products,
            suppliers = state.suppliers,
            customers = state.customers,
            canViewProfits = user.canViewProfits
        )
    }

    fun exportCsv(context: Context) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canExportData && !user.isAdmin) {
            emitMessage("لا تملك صلاحية التصدير")
            return
        }
        ExportManager.exportCsvAndShare(
            context = context,
            periodLabel = state.dateFilter.arabicLabel,
            metrics = state.metrics,
            sales = state.filteredSales,
            purchases = state.filteredPurchases,
            expenses = state.filteredExpenses,
            canViewProfits = user.canViewProfits
        )
    }

    fun exportPdfReport(context: Context, category: ReportCategory) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canExportData && !user.canViewSalesReports && !user.isAdmin) {
            emitMessage("لا تملك صلاحية تصدير التقارير")
            return
        }
        if (category.requiresProfitAccess && !user.canViewProfits) {
            emitMessage("هذا التقرير محمي بصلاحية الأرباح")
            return
        }
        val lines = ExportManager.buildReportLines(
            category = category,
            metrics = state.metrics,
            sales = state.filteredSales,
            purchases = state.filteredPurchases,
            expenses = state.filteredExpenses,
            products = state.products,
            movements = state.stockMovements,
            returns = state.filteredReturns,
            suppliers = state.suppliers,
            customers = state.customers,
            zReports = state.allZReports,
            canViewProfits = user.canViewProfits
        )
        ExportManager.exportPdfAndShare(context, category, state.dateFilter.arabicLabel, state.metrics, lines, user.canViewProfits)
    }

    fun printReport(context: Context, category: ReportCategory) {
        val state = uiState.value
        val user = state.currentUser ?: return
        if (!user.canExportData && !user.canViewZReport && !user.isAdmin) {
            emitMessage("لا تملك صلاحية الطباعة")
            return
        }
        if (category.requiresProfitAccess && !user.canViewProfits) {
            emitMessage("هذا التقرير محمي بصلاحية الأرباح")
            return
        }
        val lines = ExportManager.buildReportLines(
            category = category,
            metrics = state.metrics,
            sales = state.filteredSales,
            purchases = state.filteredPurchases,
            expenses = state.filteredExpenses,
            products = state.products,
            movements = state.stockMovements,
            returns = state.filteredReturns,
            suppliers = state.suppliers,
            customers = state.customers,
            zReports = state.allZReports,
            canViewProfits = user.canViewProfits
        )
        ExportManager.printFinancialReport(context, category.arabicTitle, state.dateFilter.arabicLabel, state.metrics, lines, user.canViewProfits)
    }

    fun emitMessage(msg: String) {
        viewModelScope.launch {
            _toastEvents.emit(msg)
        }
    }
}
