package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.UserEntity
import com.example.data.repository.CartItemDraft
import com.example.data.repository.PaymentSplitLine
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.ProductCategory
import com.example.domain.ProductSalesStat
import com.example.domain.WalletChannel
import com.example.ui.HeldInvoiceDraft
import com.example.ui.components.MultiPaymentSplitEditor
import com.example.ui.components.PackagingUnitDropdown
import com.example.ui.components.buildSplitLines
import com.example.ui.theme.KikiBurgundy
import com.example.ui.theme.KikiBurgundyDeep
import com.example.ui.theme.KikiErrorBg
import com.example.ui.theme.KikiErrorText
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiGold
import com.example.ui.theme.KikiGoldSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import com.example.ui.theme.KikiSuccessBg
import com.example.ui.theme.KikiSuccessText
import com.example.ui.theme.KikiWarningAccent
import com.example.ui.theme.KikiWarningBg
import com.example.ui.theme.KikiWarningText

private val PosEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private enum class PosMobileTab(val label: String) {
    CATALOG("قوائم الأصناف"),
    INVOICE("الفاتورة الحالية")
}

private fun normalizeArabicForSearch(input: String): String {
    return input.trim()
        .lowercase()
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace('ى', 'ي')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierPosScreen(
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    topSellingProducts: List<ProductSalesStat>,
    cart: List<CartItemDraft>,
    heldCarts: List<HeldInvoiceDraft>,
    cartSubtotal: Long,
    currentUser: UserEntity,
    allowCreditSales: Boolean,
    lastCompletedSale: SaleWithItems?,
    onDismissLastSale: () -> Unit,
    onShareSaleReceipt: (SaleWithItems) -> Unit,
    onPrintSaleInvoice: (SaleWithItems) -> Unit,
    onAddToCart: (ProductEntity, Double, PackagingUnit, Double, Long?) -> Unit,
    onDecrementProduct: (Long, Double) -> Unit,
    onUpdateCartItem: (Int, Double, PackagingUnit, Double?, Long?, Boolean) -> Unit,
    onHoldCart: (String) -> Unit,
    onResumeHeldCart: (Long) -> Unit,
    onDiscardHeldCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onCompleteSale: (Long, List<PaymentSplitLine>, Long?, String) -> Unit,
    onNavigateToPricing: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canRecordSales && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = "لا يملك هذا الحساب صلاحية تسجيل المبيعات. راجع المدير العام.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(ProductCategory.POULTRY) }
    var quickStep by remember { mutableDoubleStateOf(1.0) }
    var quickAddProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var mobileTab by remember { mutableStateOf(PosMobileTab.CATALOG) }

    val canEditInvoicePrice = currentUser.isAdmin || currentUser.canEditPrices

    val categoryCounts = remember(products) {
        products.groupingBy { it.category }.eachCount()
    }

    val categoryProducts = remember(products, selectedCategory) {
        if (selectedCategory == null) products
        else products.filter { it.category == selectedCategory }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        val normQuery = normalizeArabicForSearch(searchQuery)
        if (normQuery.isNotEmpty()) {
            products.filter { product ->
                normalizeArabicForSearch(product.name).contains(normQuery) ||
                    normalizeArabicForSearch(product.category.arabicTitle).contains(normQuery)
            }
        } else {
            categoryProducts
        }
    }

    // Shared Invoice Checkout State
    var discountText by remember { mutableStateOf("") }
    var isSplitMode by remember { mutableStateOf(false) }
    var singleChannel by remember { mutableStateOf(WalletChannel.CASH) }
    val splitAmounts = remember { mutableStateMapOf<WalletChannel, String>() }
    var receiptNumber by remember { mutableStateOf("") }
    var operationNumber by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val resetCheckoutFields: () -> Unit = {
        discountText = ""
        isSplitMode = false
        singleChannel = WalletChannel.CASH
        splitAmounts.clear()
        receiptNumber = ""
        operationNumber = ""
        receiptNote = ""
        selectedCustomer = null
    }

    val cartItemsCount = remember(cart) { cart.size }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideSplitPos = maxWidth >= 720.dp

        if (isWideSplitPos) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    PosCatalogPane(
                        allProductsCount = products.size,
                        categoryProducts = categoryProducts,
                        filteredProducts = filteredProducts,
                        categoryCounts = categoryCounts,
                        cart = cart,
                        heldCarts = heldCarts,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        quickStep = quickStep,
                        onQuickStepChange = { quickStep = it },
                        bottomPadding = 16.dp,
                        onAddProductToCart = { prod, qty, unit, mult ->
                            if (prod.sellingPrice > 0L) {
                                onAddToCart(prod, qty, unit, mult, null)
                            } else if (canEditInvoicePrice) {
                                onNavigateToPricing()
                            } else {
                                onAddToCart(prod, qty, unit, mult, null)
                            }
                        },
                        onDecrementProduct = { prodId -> onDecrementProduct(prodId, quickStep) },
                        onOpenKeypadForProduct = { quickAddProduct = it },
                        onResumeHeldCart = onResumeHeldCart,
                        onDiscardHeldCart = onDiscardHeldCart
                    )
                }

                VerticalDivider()

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                ) {
                    PosLiveInvoicePane(
                        cart = cart,
                        cartSubtotal = cartSubtotal,
                        customers = customers,
                        canApplyDiscount = canEditInvoicePrice,
                        allowCreditSales = allowCreditSales,
                        discountText = discountText,
                        onDiscountChange = { discountText = it },
                        selectedCustomer = selectedCustomer,
                        onCustomerChange = { selectedCustomer = it },
                        isSplitMode = isSplitMode,
                        onSplitModeChange = { isSplitMode = it },
                        singleChannel = singleChannel,
                        onSingleChannelChange = { singleChannel = it },
                        splitAmounts = splitAmounts,
                        onSplitAmountChange = { ch, v -> splitAmounts[ch] = v },
                        receiptNumber = receiptNumber,
                        onReceiptNumberChange = { receiptNumber = it },
                        operationNumber = operationNumber,
                        onOperationNumberChange = { operationNumber = it },
                        receiptNote = receiptNote,
                        onReceiptNoteChange = { receiptNote = it },
                        onUpdateCartItem = { idx, q, u -> onUpdateCartItem(idx, q, u, null, null, true) },
                        onOpenEditLine = { idx -> editingLineIndex = idx },
                        onHoldCart = {
                            onHoldCart("")
                            resetCheckoutFields()
                        },
                        onClearCart = {
                            onClearCart()
                            resetCheckoutFields()
                        },
                        onConfirmSale = { discount, allocs ->
                            onCompleteSale(discount, allocs, selectedCustomer?.id, receiptNote)
                            resetCheckoutFields()
                        },
                        onSwitchToCatalog = null
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // M3 Single-Select Segmented Button with Pill Corners & Cart Badge
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            PosMobileTab.entries.forEachIndexed { index, tab ->
                                val selected = tab == mobileTab
                                SegmentedButton(
                                    selected = selected,
                                    onClick = { mobileTab = tab },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = PosMobileTab.entries.size
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = selected) {
                                            Icon(
                                                imageVector = if (tab == PosMobileTab.CATALOG) Icons.Filled.Category else Icons.Filled.ReceiptLong,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (tab == PosMobileTab.INVOICE && cartItemsCount > 0) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ) {
                                                    Text("$cartItemsCount")
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("pos_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = mobileTab,
                        transitionSpec = {
                            fadeIn(tween(200, easing = PosEasing)) togetherWith
                                fadeOut(tween(150, easing = PosEasing))
                        },
                        label = "pos_mobile_tab",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { activeTab ->
                        if (activeTab == PosMobileTab.CATALOG) {
                            PosCatalogPane(
                                allProductsCount = products.size,
                                categoryProducts = categoryProducts,
                                filteredProducts = filteredProducts,
                                categoryCounts = categoryCounts,
                                cart = cart,
                                heldCarts = heldCarts,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                quickStep = quickStep,
                                onQuickStepChange = { quickStep = it },
                                bottomPadding = if (cart.isNotEmpty()) 112.dp else 18.dp,
                                onAddProductToCart = { prod, qty, unit, mult ->
                                    if (prod.sellingPrice > 0L) {
                                        onAddToCart(prod, qty, unit, mult, null)
                                    } else if (canEditInvoicePrice) {
                                        onNavigateToPricing()
                                    } else {
                                        onAddToCart(prod, qty, unit, mult, null)
                                    }
                                },
                                onDecrementProduct = { prodId -> onDecrementProduct(prodId, quickStep) },
                                onOpenKeypadForProduct = { quickAddProduct = it },
                                onResumeHeldCart = onResumeHeldCart,
                                onDiscardHeldCart = onDiscardHeldCart
                            )
                        } else {
                            PosLiveInvoicePane(
                                cart = cart,
                                cartSubtotal = cartSubtotal,
                                customers = customers,
                                canApplyDiscount = canEditInvoicePrice,
                                allowCreditSales = allowCreditSales,
                                discountText = discountText,
                                onDiscountChange = { discountText = it },
                                selectedCustomer = selectedCustomer,
                                onCustomerChange = { selectedCustomer = it },
                                isSplitMode = isSplitMode,
                                onSplitModeChange = { isSplitMode = it },
                                singleChannel = singleChannel,
                                onSingleChannelChange = { singleChannel = it },
                                splitAmounts = splitAmounts,
                                onSplitAmountChange = { ch, v -> splitAmounts[ch] = v },
                                receiptNumber = receiptNumber,
                                onReceiptNumberChange = { receiptNumber = it },
                                operationNumber = operationNumber,
                                onOperationNumberChange = { operationNumber = it },
                                receiptNote = receiptNote,
                                onReceiptNoteChange = { receiptNote = it },
                                onUpdateCartItem = { idx, q, u -> onUpdateCartItem(idx, q, u, null, null, true) },
                                onOpenEditLine = { idx -> editingLineIndex = idx },
                                onHoldCart = {
                                    onHoldCart("")
                                    resetCheckoutFields()
                                    mobileTab = PosMobileTab.CATALOG
                                },
                                onClearCart = {
                                    onClearCart()
                                    resetCheckoutFields()
                                },
                                onConfirmSale = { discount, allocs ->
                                    onCompleteSale(discount, allocs, selectedCustomer?.id, receiptNote)
                                    resetCheckoutFields()
                                    mobileTab = PosMobileTab.CATALOG
                                },
                                onSwitchToCatalog = { mobileTab = PosMobileTab.CATALOG }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = mobileTab == PosMobileTab.CATALOG && cart.isNotEmpty(),
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = Color.Transparent,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(Brush.horizontalGradient(listOf(KikiBurgundyDeep, KikiBurgundy)))
                            .clickable { showCheckoutSheet = true }
                            .testTag("open_checkout_bar")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(KikiGold)
                                ) {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    val lastAdded = cart.lastOrNull()
                                    Text(
                                        text = "${cart.size} أصناف • آخر صنف: ${lastAdded?.product?.name.orEmpty()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = PricingEngine.formatCurrency(cartSubtotal),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        onHoldCart("")
                                        resetCheckoutFields()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .testTag("hold_cart_quick_button")
                                ) {
                                    Icon(Icons.Filled.PauseCircleOutline, contentDescription = "تعليق الفاتورة", tint = KikiGold, modifier = Modifier.size(20.dp))
                                }

                                Surface(shape = RoundedCornerShape(50), color = KikiGold) {
                                    Text(
                                        text = "الفاتورة والدفع",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    quickAddProduct?.let { prod ->
        ProQuantityUnitSheet(
            title = prod.name,
            defaultUnit = prod.defaultUnit,
            stockAvailable = prod.stockQuantity,
            baseSellingPrice = prod.sellingPrice,
            initialQty = quickStep,
            initialUnit = prod.defaultUnit,
            initialConversionFactor = prod.unitConversionFactor,
            initialCustomPrice = null,
            canEditPrice = canEditInvoicePrice,
            confirmLabel = "إضافة للفاتورة",
            onDismiss = { quickAddProduct = null },
            onConfirm = { qty, unit, mult, customPrice ->
                onAddToCart(prod, qty, unit, mult, customPrice)
                quickAddProduct = null
            }
        )
    }

    editingLineIndex?.let { idx ->
        val draft = cart.getOrNull(idx)
        if (draft == null) {
            editingLineIndex = null
        } else {
            ProQuantityUnitSheet(
                title = "تعديل: ${draft.product.name}",
                defaultUnit = draft.product.defaultUnit,
                stockAvailable = draft.product.stockQuantity,
                baseSellingPrice = draft.product.sellingPrice,
                initialQty = draft.quantity,
                initialUnit = draft.unit,
                initialConversionFactor = draft.conversionMultiplier,
                initialCustomPrice = draft.customUnitPrice,
                canEditPrice = canEditInvoicePrice,
                confirmLabel = "تحديث الصنف",
                onDismiss = { editingLineIndex = null },
                onConfirm = { qty, unit, mult, customPrice ->
                    onUpdateCartItem(idx, qty, unit, mult, customPrice, false)
                    editingLineIndex = null
                }
            )
        }
    }

    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)) {
                PosLiveInvoicePane(
                    cart = cart,
                    cartSubtotal = cartSubtotal,
                    customers = customers,
                    canApplyDiscount = canEditInvoicePrice,
                    allowCreditSales = allowCreditSales,
                    discountText = discountText,
                    onDiscountChange = { discountText = it },
                    selectedCustomer = selectedCustomer,
                    onCustomerChange = { selectedCustomer = it },
                    isSplitMode = isSplitMode,
                    onSplitModeChange = { isSplitMode = it },
                    singleChannel = singleChannel,
                    onSingleChannelChange = { singleChannel = it },
                    splitAmounts = splitAmounts,
                    onSplitAmountChange = { ch, v -> splitAmounts[ch] = v },
                    receiptNumber = receiptNumber,
                    onReceiptNumberChange = { receiptNumber = it },
                    operationNumber = operationNumber,
                    onOperationNumberChange = { operationNumber = it },
                    receiptNote = receiptNote,
                    onReceiptNoteChange = { receiptNote = it },
                    onUpdateCartItem = { idx, q, u -> onUpdateCartItem(idx, q, u, null, null, true) },
                    onOpenEditLine = { idx ->
                        showCheckoutSheet = false
                        editingLineIndex = idx
                    },
                    onHoldCart = {
                        onHoldCart("")
                        resetCheckoutFields()
                        showCheckoutSheet = false
                    },
                    onClearCart = {
                        onClearCart()
                        resetCheckoutFields()
                        showCheckoutSheet = false
                    },
                    onConfirmSale = { discount, allocs ->
                        onCompleteSale(discount, allocs, selectedCustomer?.id, receiptNote)
                        resetCheckoutFields()
                        showCheckoutSheet = false
                    },
                    onSwitchToCatalog = { showCheckoutSheet = false }
                )
            }
        }
    }

    lastCompletedSale?.let { saved ->
        AlertDialog(
            onDismissRequest = onDismissLastSale,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kiki_logo),
                        contentDescription = "شعار كي كي",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.5.dp, KikiGold, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    )
                    Column {
                        Text("فاتورة ${saved.sale.invoiceNumber}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("KIKI - مركز كي كي للدواجن", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("عدد الأصناف: ${saved.items.size}")
                    Text("الإجمالي الصافي: ${PricingEngine.formatCurrency(saved.sale.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("المدفوع: ${PricingEngine.formatCurrency(saved.sale.paidAmount)}")
                    if (saved.sale.remainingDebt > 0L) {
                        Text("المتبقي (آجل): ${PricingEngine.formatCurrency(saved.sale.remainingDebt)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Text("قناة الدفع: ${saved.sale.paymentSummary}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onShareSaleReceipt(saved)
                                onDismissLastSale()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب PNG", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                onPrintSaleInvoice(saved)
                                onDismissLastSale()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة", maxLines = 1)
                        }
                    }
                    TextButton(onClick = onDismissLastSale, modifier = Modifier.align(Alignment.End)) {
                        Text("فاتورة جديدة")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosCatalogPane(
    allProductsCount: Int,
    categoryProducts: List<ProductEntity>,
    filteredProducts: List<ProductEntity>,
    categoryCounts: Map<ProductCategory, Int>,
    cart: List<CartItemDraft>,
    heldCarts: List<HeldInvoiceDraft>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: ProductCategory?,
    onCategoryChange: (ProductCategory?) -> Unit,
    quickStep: Double,
    onQuickStepChange: (Double) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onAddProductToCart: (ProductEntity, Double, PackagingUnit, Double) -> Unit,
    onDecrementProduct: (Long) -> Unit,
    onOpenKeypadForProduct: (ProductEntity) -> Unit,
    onResumeHeldCart: (Long) -> Unit,
    onDiscardHeldCart: (Long) -> Unit
) {
    var mainMenuExpanded by remember { mutableStateOf(false) }
    var subProductMenuExpanded by remember { mutableStateOf(false) }
    var pickedProduct by remember(categoryProducts) { mutableStateOf(categoryProducts.firstOrNull()) }
    var pickedQty by remember(pickedProduct) { mutableDoubleStateOf(quickStep) }
    var pickedUnit by remember(pickedProduct) { mutableStateOf(pickedProduct?.defaultUnit ?: PackagingUnit.KILO) }

    LaunchedEffect(categoryProducts) {
        if (pickedProduct == null || categoryProducts.none { it.id == pickedProduct?.id }) {
            pickedProduct = categoryProducts.firstOrNull()
        }
    }

    val presetWeights = remember { listOf(0.25, 0.5, 1.0, 1.5, 2.0, 5.0) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 158.dp),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Held Invoices Quick Restore Bar
            if (heldCarts.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فواتير معلقة:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        heldCarts.forEach { held ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KikiWarningBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onResumeHeldCart(held.id) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "${held.label} (${held.items.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = KikiWarningText
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "حذف الفاتورة المعلقة",
                                        tint = KikiWarningText,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDiscardHeldCart(held.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // M3 Cascading Exposed Dropdown Menus + Quick Add Stepper Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Compact single-line header to preserve vertical screen space
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_kiki_logo),
                                contentDescription = "شعار كي كي",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                    .padding(1.dp)
                            )
                            Text(
                                text = "اختيار الصنف المتدرج",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 1. M3 Exposed Dropdown Menu — القائمة الرئيسية (القسم)
                        ExposedDropdownMenuBox(
                            expanded = mainMenuExpanded,
                            onExpandedChange = { mainMenuExpanded = !mainMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val catLabel = selectedCategory?.let {
                                "${it.arabicTitle} (${categoryCounts[it] ?: 0} أصناف)"
                            } ?: "عرض جميع الأقسام ($allProductsCount صنف)"

                            OutlinedTextField(
                                value = catLabel,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("1. القائمة الرئيسية (القسم)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = mainMenuExpanded)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .testTag("main_category_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = mainMenuExpanded,
                                onDismissRequest = { mainMenuExpanded = false }
                            ) {
                                ProductCategory.entries.forEach { category ->
                                    val count = categoryCounts[category] ?: 0
                                    val isSelected = selectedCategory == category
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.arabicTitle,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = "$count",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onCategoryChange(category)
                                            mainMenuExpanded = false
                                            subProductMenuExpanded = true
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "عرض جميع الأقسام ($allProductsCount صنف)",
                                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onCategoryChange(null)
                                        mainMenuExpanded = false
                                        subProductMenuExpanded = true
                                    }
                                )
                            }
                        }

                        // 2. M3 Exposed Dropdown Menu — قائمة أصناف القسم المختار
                        ExposedDropdownMenuBox(
                            expanded = subProductMenuExpanded,
                            onExpandedChange = { subProductMenuExpanded = !subProductMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentProdText = pickedProduct?.let { p ->
                                "${p.name} — ${if (p.sellingPrice > 0L) PricingEngine.formatCurrency(p.sellingPrice) else "غير مسعّر"}"
                            } ?: "اضغط لاختيار الصنف"

                            OutlinedTextField(
                                value = currentProdText,
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                label = { Text("2. أصناف القسم (${selectedCategory?.shortTitle ?: "الكل"})") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = subProductMenuExpanded)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .testTag("sub_product_dropdown")
                            )

                            ExposedDropdownMenu(
                                expanded = subProductMenuExpanded,
                                onDismissRequest = { subProductMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 320.dp)
                            ) {
                                if (categoryProducts.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("لا توجد أصناف في هذا القسم") },
                                        onClick = { subProductMenuExpanded = false }
                                    )
                                } else {
                                    categoryProducts.forEach { prod ->
                                        val isPicked = pickedProduct?.id == prod.id
                                        val hasPrice = prod.sellingPrice > 0L
                                        val isLow = prod.stockQuantity <= prod.lowStockThreshold
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = prod.name,
                                                            fontWeight = if (isPicked) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "المتاح: ${PricingEngine.formatQty(prod.stockQuantity)} ${prod.defaultUnit.arabicLabel}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isLow) KikiErrorText else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (hasPrice) KikiSuccessBg else KikiErrorBg
                                                    ) {
                                                        Text(
                                                            text = if (hasPrice) PricingEngine.formatCurrency(prod.sellingPrice) else "غير مسعّر",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (hasPrice) KikiSuccessText else KikiErrorText,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                pickedProduct = prod
                                                pickedUnit = prod.defaultUnit
                                                pickedQty = quickStep
                                                subProductMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. بطاقة تسعير وإضافة الصنف: Suggestion Chips + M3 Tonal Stepper (48dp) + FilledButton (48dp)
                        pickedProduct?.let { prod ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "السعر: ${PricingEngine.formatCurrency(prod.sellingPrice)} • المتاح: ${PricingEngine.formatQty(prod.stockQuantity)} ${prod.defaultUnit.arabicLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val previewTotal = PricingEngine.calculateLineTotal(prod.sellingPrice, pickedQty)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = KikiSuccessBg
                                        ) {
                                            Text(
                                                text = PricingEngine.formatCurrency(previewTotal),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = KikiSuccessText,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Uniform M3 Suggestion / Filter Chips for quick weights & keypad
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        presetWeights.forEach { w ->
                                            val active = pickedQty == w
                                            FilterChip(
                                                selected = active,
                                                onClick = { pickedQty = w },
                                                label = {
                                                    Text(
                                                        text = PricingEngine.formatQty(w),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                modifier = Modifier.height(36.dp)
                                            )
                                        }
                                        SuggestionChip(
                                            onClick = { onOpenKeypadForProduct(prod) },
                                            label = {
                                                Text(
                                                    text = "لوحة أرقام",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Filled.Tune,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                labelColor = MaterialTheme.colorScheme.primary,
                                                iconContentColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.height(36.dp)
                                        )
                                    }

                                    // Aligned 48dp M3 Tonal Stepper + 48dp FilledButton
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (pickedQty > 0.25) {
                                                            pickedQty = (pickedQty - 0.5).coerceAtLeast(0.25)
                                                        }
                                                    },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Remove,
                                                        contentDescription = "إنقاص",
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "${PricingEngine.formatQty(pickedQty)} ${pickedUnit.arabicLabel}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                                IconButton(
                                                    onClick = { pickedQty += 0.5 },
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = "زيادة",
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                onAddProductToCart(prod, pickedQty, pickedUnit, 1.0)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = KikiSuccessText,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("cascading_add_to_invoice_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddShoppingCart,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "إضافة للفاتورة",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // M3 Pill Search Bar + Quick Step Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "بحث",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "بحث فوري في جميع الأصناف...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pos_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "مسح البحث",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "نتائج البحث (${filteredProducts.size})" else "أصناف: ${selectedCategory?.arabicTitle ?: "الكل"} (${filteredProducts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0.5, 1.0, 2.0).forEach { step ->
                                val active = quickStep == step
                                FilterChip(
                                    selected = active,
                                    onClick = { onQuickStepChange(step) },
                                    label = {
                                        Text(
                                            text = "×${PricingEngine.formatQty(step)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Text("لا توجد أصناف مطابقة للبحث الحالي.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val qtyInCart = cart.filter { it.product.id == product.id }.sumOf { it.quantity }
                    PosProductCard(
                        product = product,
                        qtyInCart = qtyInCart,
                        quickStep = quickStep,
                        onQuickTap = { onAddProductToCart(product, quickStep, product.defaultUnit, 1.0) },
                        onDecrementTap = { onDecrementProduct(product.id) },
                        onCustomUnitTap = { onOpenKeypadForProduct(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PosProductCard(
    product: ProductEntity,
    qtyInCart: Double,
    quickStep: Double,
    onQuickTap: () -> Unit,
    onDecrementTap: () -> Unit,
    onCustomUnitTap: () -> Unit
) {
    val hasPrice = product.sellingPrice > 0L
    val isOut = product.stockQuantity <= 0.0
    val isLowStock = !isOut && product.stockQuantity <= product.lowStockThreshold

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (qtyInCart > 0) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (qtyInCart > 0) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (qtyInCart > 0) 1.5.dp else 1.dp,
                color = if (qtyInCart > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onQuickTap() }
            .testTag("pos_product_${product.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val badgeBg = when {
                    isOut -> KikiErrorBg
                    isLowStock -> KikiWarningBg
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val badgeFg = when {
                    isOut -> KikiErrorText
                    isLowStock -> KikiWarningText
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                    Text(
                        text = "${product.defaultUnit.arabicLabel} • ${PricingEngine.formatQty(product.stockQuantity)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                if (qtyInCart > 0) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = PricingEngine.formatQty(qtyInCart),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (isLowStock || isOut) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "تنبيه مخزون",
                        tint = if (isOut) KikiErrorText else KikiWarningAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasPrice) KikiSuccessBg else KikiErrorBg,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = if (hasPrice) PricingEngine.formatCurrency(product.sellingPrice) else "غير مسعّر",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasPrice) KikiSuccessText else KikiErrorText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                FilledTonalIconButton(
                    onClick = onCustomUnitTap,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "تحديد وزن أو وحدة مخصصة",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (qtyInCart > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDecrementTap,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Remove,
                                contentDescription = "إنقاص ${PricingEngine.formatQty(quickStep)}",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "${PricingEngine.formatQty(qtyInCart)} ${product.defaultUnit.arabicLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(
                            onClick = onQuickTap,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "إضافة ${PricingEngine.formatQty(quickStep)}",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosLiveInvoicePane(
    cart: List<CartItemDraft>,
    cartSubtotal: Long,
    customers: List<CustomerEntity>,
    canApplyDiscount: Boolean,
    allowCreditSales: Boolean,
    discountText: String,
    onDiscountChange: (String) -> Unit,
    selectedCustomer: CustomerEntity?,
    onCustomerChange: (CustomerEntity?) -> Unit,
    isSplitMode: Boolean,
    onSplitModeChange: (Boolean) -> Unit,
    singleChannel: WalletChannel,
    onSingleChannelChange: (WalletChannel) -> Unit,
    splitAmounts: Map<WalletChannel, String>,
    onSplitAmountChange: (WalletChannel, String) -> Unit,
    receiptNumber: String,
    onReceiptNumberChange: (String) -> Unit,
    operationNumber: String,
    onOperationNumberChange: (String) -> Unit,
    receiptNote: String,
    onReceiptNoteChange: (String) -> Unit,
    onUpdateCartItem: (Int, Double, PackagingUnit) -> Unit,
    onOpenEditLine: (Int) -> Unit,
    onHoldCart: () -> Unit,
    onClearCart: () -> Unit,
    onConfirmSale: (Long, List<PaymentSplitLine>) -> Unit,
    onSwitchToCatalog: (() -> Unit)?
) {
    var customerMenuOpen by remember { mutableStateOf(false) }
    val discount = if (canApplyDiscount) PricingEngine.parseLongAmount(discountText).coerceIn(0L, cartSubtotal) else 0L
    val netTotal = (cartSubtotal - discount).coerceAtLeast(0L)

    if (cart.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_kiki_logo),
                    contentDescription = "شعار كي كي",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(2.dp, KikiGold, RoundedCornerShape(18.dp))
                        .padding(4.dp)
                )
                Text("فاتورة مبيعات KIKI فارغة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "اختر القائمة الرئيسية (القسم) ثم اختر الصنف من قائمته المنسدلة لإضافته فوراً للفاتورة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onSwitchToCatalog != null) {
                    Button(onClick = onSwitchToCatalog) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("اختيار الأصناف للفاتورة")
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Branded Invoice Header with Official KIKI Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_kiki_logo),
                    contentDescription = "شعار كي كي",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, KikiGold, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                )
                Column {
                    Text(
                        text = "فاتورة المبيعات (${cart.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "KIKI - مركز كي كي للدواجن",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onHoldCart, modifier = Modifier.testTag("hold_cart_button")) {
                    Icon(Icons.Filled.PauseCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تعليق")
                }
                TextButton(onClick = onClearCart) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = KikiExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إفراغ", color = KikiExpenseRed)
                }
            }
        }
        HorizontalDivider()

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            itemsIndexed(cart) { index, item ->
                val lineUnitPrice = item.effectiveUnitPrice
                val lineTotal = PricingEngine.calculateLineTotal(lineUnitPrice, item.quantity)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEditLine(index) }
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${PricingEngine.formatCurrency(lineUnitPrice)} × ${PricingEngine.formatQty(item.quantity)} (${item.unit.arabicLabel})" +
                                        if (item.customUnitPrice != null) " • سعر خاص" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = PricingEngine.formatCurrency(lineTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onUpdateCartItem(index, item.quantity - 1.0, item.unit) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "إنقاص")
                                }
                                Text(PricingEngine.formatQty(item.quantity), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { onUpdateCartItem(index, item.quantity + 1.0, item.unit) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "زيادة")
                                }
                            }

                            OutlinedButton(
                                onClick = { onOpenEditLine(index) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("وزن / وحدة / سعر", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (canApplyDiscount) {
                item {
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = onDiscountChange,
                        label = { Text("خصم على الفاتورة (ج.س) - اختياري") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (customers.isNotEmpty() && allowCreditSales) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { customerMenuOpen = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(selectedCustomer?.name ?: "عميل نقدي (أو اختر عميلاً للآجل)")
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = customerMenuOpen, onDismissRequest = { customerMenuOpen = false }) {
                            DropdownMenuItem(text = { Text("عميل نقدي (بدون حساب)") }, onClick = {
                                onCustomerChange(null)
                                customerMenuOpen = false
                            })
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text("${c.name} (رصيد: ${PricingEngine.formatAmount(c.balanceDue)})") }, onClick = {
                                    onCustomerChange(c)
                                    customerMenuOpen = false
                                })
                            }
                        }
                    }
                }
            }

            item {
                MultiPaymentSplitEditor(
                    totalDue = netTotal,
                    isSplitMode = isSplitMode,
                    onSplitModeChange = onSplitModeChange,
                    singleChannel = singleChannel,
                    onSingleChannelChange = onSingleChannelChange,
                    splitAmounts = splitAmounts,
                    onSplitAmountChange = onSplitAmountChange,
                    receiptNumber = receiptNumber,
                    onReceiptNumberChange = onReceiptNumberChange,
                    operationNumber = operationNumber,
                    onOperationNumberChange = onOperationNumberChange,
                    receiptNote = receiptNote,
                    onReceiptNoteChange = onReceiptNoteChange,
                    allowPartialCredit = allowCreditSales && selectedCustomer != null
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإجمالي الصافي:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        PricingEngine.formatCurrency(netTotal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = {
                        val allocs = buildSplitLines(netTotal, isSplitMode, singleChannel, splitAmounts, receiptNumber, operationNumber)
                        onConfirmSale(discount, allocs)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("confirm_sale_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد وإصدار الفاتورة (${PricingEngine.formatCurrency(netTotal)})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProQuantityUnitSheet(
    title: String,
    defaultUnit: PackagingUnit,
    stockAvailable: Double,
    baseSellingPrice: Long,
    initialQty: Double,
    initialUnit: PackagingUnit,
    initialConversionFactor: Double,
    initialCustomPrice: Long?,
    canEditPrice: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, PackagingUnit, Double, Long?) -> Unit
) {
    var qtyText by remember { mutableStateOf(PricingEngine.formatQty(initialQty).replace(",", "")) }
    var selectedUnit by remember { mutableStateOf(initialUnit) }
    var conversionText by remember {
        mutableStateOf(if (initialConversionFactor != 1.0) initialConversionFactor.toString() else "1")
    }
    var customPriceText by remember {
        mutableStateOf(initialCustomPrice?.toString() ?: "")
    }

    val parsedQty = PricingEngine.parseDoubleQty(qtyText)
    val effectiveUnitPrice = PricingEngine.parseLongAmount(customPriceText).takeIf { it > 0L } ?: baseSellingPrice
    val linePreviewTotal = if (parsedQty > 0.0 && effectiveUnitPrice > 0L) {
        PricingEngine.calculateLineTotal(effectiveUnitPrice, parsedQty)
    } else 0L

    val presetWeights = remember { listOf("0.25", "0.5", "0.75", "1", "1.5", "2", "3", "5", "10") }
    val keypadRows = remember {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "DEL")
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "سعر البيع: ${PricingEngine.formatCurrency(baseSellingPrice)} | المتاح: ${PricingEngine.formatQty(stockAvailable)} ${defaultUnit.arabicLabel}",
                style = MaterialTheme.typography.labelLarge,
                color = KikiProfitGreen
            )

            Surface(
                color = KikiProfitGreenSoft,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الكمية / الوزن المحدد", style = MaterialTheme.typography.labelSmall, color = KikiProfitGreen)
                        Text(
                            "${qtyText.ifBlank { "0" }} ${selectedUnit.arabicLabel}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("إجمالي السطر", style = MaterialTheme.typography.labelSmall, color = KikiProfitGreen)
                        Text(
                            PricingEngine.formatCurrency(linePreviewTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KikiProfitGreen
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetWeights.forEach { preset ->
                    val selected = qtyText == preset
                    FilterChip(
                        selected = selected,
                        onClick = { qtyText = preset },
                        label = { Text(preset, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keypadRows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { key ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        qtyText = when (key) {
                                            "DEL" -> if (qtyText.isNotEmpty()) qtyText.dropLast(1) else ""
                                            "." -> if (qtyText.contains('.')) qtyText else (if (qtyText.isEmpty()) "0." else "$qtyText.")
                                            else -> if (qtyText == "0") key else qtyText + key
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "مسح")
                                    } else {
                                        Text(key, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PackagingUnitDropdown(selectedUnit = selectedUnit, onUnitSelected = { selectedUnit = it })

            if (selectedUnit != defaultUnit) {
                OutlinedTextField(
                    value = conversionText,
                    onValueChange = { conversionText = it },
                    label = { Text("معامل التحويل إلى (${defaultUnit.arabicLabel})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (canEditPrice) {
                OutlinedTextField(
                    value = customPriceText,
                    onValueChange = { customPriceText = it },
                    label = { Text("تعديل سعر الوحدة لهذه الفاتورة (اختياري - ج.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    val qty = PricingEngine.parseDoubleQty(qtyText)
                    val mult = if (selectedUnit == defaultUnit) 1.0 else PricingEngine.parseDoubleQty(conversionText).coerceAtLeast(0.01)
                    val customP = PricingEngine.parseLongAmount(customPriceText).takeIf { it > 0L && canEditPrice }
                    if (qty > 0.0) onConfirm(qty, selectedUnit, mult, customP)
                },
                enabled = parsedQty > 0.0,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("$confirmLabel (${PricingEngine.formatCurrency(linePreviewTotal)})", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
