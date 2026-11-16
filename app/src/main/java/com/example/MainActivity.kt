package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.UserEntity
import com.example.domain.ThemePreference
import com.example.ui.MainViewModel
import com.example.ui.components.KikiBrandHeader
import com.example.ui.screens.BackupScreen
import com.example.ui.screens.CashierPosScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryPricingScreen
import com.example.ui.screens.MandatoryPinChangeScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.PinLoginScreen
import com.example.ui.screens.PurchasesScreen
import com.example.ui.screens.ReportsCenterScreen
import com.example.ui.screens.ReturnsAdjustmentsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UsersManagementScreen
import com.example.ui.screens.WalletsScreen
import com.example.ui.screens.ZReportScreen
import com.example.ui.theme.KikiBurgundy
import com.example.ui.theme.KikiBurgundyDeep
import com.example.ui.theme.KikiGold
import com.example.ui.theme.KikiTheme
import kotlinx.coroutines.launch

private enum class AppSection(
    val title: String,
    val shortTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("الرئيسية والمؤشرات", "الرئيسية", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    POS("الكاشير (نقطة البيع)", "الكاشير", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    PURCHASES("المشتريات والتوريد", "المشتريات", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    INVENTORY("المخزون والتسعير", "المخزون", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    EXPENSES("المصروفات التشغيلية", "المصروفات", Icons.Filled.Receipt, Icons.Outlined.Receipt),
    RETURNS("المرتجعات والتالف", "المرتجعات", Icons.Filled.AssignmentReturn, Icons.Outlined.AssignmentReturn),
    PARTIES("الموردون والعملاء", "الأطراف", Icons.Filled.People, Icons.Outlined.People),
    WALLETS("المحافظ والتحويلات", "المحافظ", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    ZREPORT("تقفيل اليومية (Z-Report)", "التقفيل", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
    REPORTS("مركز التقارير والتصدير", "التقارير", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    USERS("المستخدمون وسجل التدقيق", "المستخدمون", Icons.Filled.ManageAccounts, Icons.Outlined.ManageAccounts),
    BACKUP("النسخ الاحتياطي المشفر", "النسخ", Icons.Filled.Backup, Icons.Outlined.Backup),
    SETTINGS("الإعدادات ودليل الاستخدام", "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                KikiRootApp()
            }
        }
    }
}

@Composable
private fun KikiRootApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isDark = when (uiState.settings.themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    KikiTheme(darkTheme = isDark) {
        var showSplash by rememberSaveable { mutableStateOf(true) }
        var currentSection by rememberSaveable { mutableStateOf(AppSection.POS) }

        LaunchedEffect(Unit) {
            viewModel.toastEvents.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        if (showSplash) {
            SplashScreen(
                isDataReady = uiState.isReady,
                onFinished = { showSplash = false }
            )
            return@KikiTheme
        }

        val currentUser = uiState.currentUser
        if (currentUser == null) {
            PinLoginScreen(
                users = uiState.users,
                onLoginAttempt = { user, pin ->
                    val res = viewModel.attemptPinLogin(user, pin)
                    if (res.first) {
                        currentSection = if (user.canRecordSales) AppSection.POS else AppSection.DASHBOARD
                    }
                    res
                }
            )
            return@KikiTheme
        }

        if (currentUser.mustChangePin) {
            MandatoryPinChangeScreen(
                user = currentUser,
                onConfirmNewPin = viewModel::completeMandatoryPinChange
            )
            return@KikiTheme
        }

        val allowedSections = remember(currentUser) { computeAllowedSections(currentUser) }
        if (currentSection !in allowedSections) {
            currentSection = allowedSections.first()
        }

        val bottomBarSections = remember(allowedSections) {
            listOfNotNull(
                AppSection.DASHBOARD.takeIf { it in allowedSections },
                AppSection.POS.takeIf { it in allowedSections },
                AppSection.INVENTORY.takeIf { it in allowedSections },
                AppSection.WALLETS.takeIf { it in allowedSections },
                AppSection.REPORTS.takeIf { it in allowedSections }
            ).take(4)
        }

        val defaultHome = if (AppSection.POS in allowedSections) AppSection.POS else allowedSections.first()
        BackHandler(enabled = currentSection != defaultHome) {
            currentSection = defaultHome
        }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(KikiBurgundyDeep, KikiBurgundy)))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.img_kiki_logo),
                                contentDescription = "شعار كي كي",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, KikiGold, RoundedCornerShape(12.dp))
                                    .padding(2.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("KIKI - مركز كي كي للدواجن", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("دائماً فكر بالمذاق الطازج", color = KikiGold, style = MaterialTheme.typography.labelSmall)
                                Text("${currentUser.displayName} • ${currentUser.roleTitle}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        allowedSections.forEach { section ->
                            val selected = section == currentSection
                            NavigationDrawerItem(
                                label = { Text(section.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                selected = selected,
                                onClick = {
                                    currentSection = section
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                                        contentDescription = section.title
                                    )
                                },
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .testTag("drawer_item_${section.name.lowercase()}")
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        KikiBrandHeader(
                            currentUser = currentUser,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onSwitchUser = { viewModel.logout() }
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen && bottomBarSections.isNotEmpty()) {
                            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                                bottomBarSections.forEach { section ->
                                    val selected = section == currentSection
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentSection = section },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                                                contentDescription = section.shortTitle
                                            )
                                        },
                                        label = { Text(section.shortTitle, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_tab_${section.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                allowedSections.forEach { section ->
                                    val selected = section == currentSection
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentSection = section },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                                                contentDescription = section.shortTitle
                                            )
                                        },
                                        label = { Text(section.shortTitle, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (currentSection) {
                                AppSection.DASHBOARD -> DashboardScreen(
                                    currentUser = currentUser,
                                    dateFilter = uiState.dateFilter,
                                    onDateFilterChange = { viewModel.setDateFilter(it) },
                                    metrics = uiState.metrics,
                                    sales = uiState.filteredSales,
                                    onShareSaleReceipt = { viewModel.shareSaleReceipt(context, it) },
                                    onOpenZReport = { currentSection = AppSection.ZREPORT },
                                    onOpenReportsCenter = { currentSection = AppSection.REPORTS },
                                    onOpenReturns = { currentSection = AppSection.RETURNS },
                                    onCopyWhatsApp = { viewModel.copyWhatsAppSummary(context) }
                                )

                                AppSection.POS -> CashierPosScreen(
                                    products = uiState.products,
                                    customers = uiState.customers,
                                    topSellingProducts = uiState.metrics.topSellingProducts,
                                    cart = uiState.cart,
                                    heldCarts = uiState.heldCarts,
                                    cartSubtotal = uiState.cartSubtotal,
                                    currentUser = currentUser,
                                    allowCreditSales = uiState.settings.allowCreditSales,
                                    lastCompletedSale = uiState.lastCompletedSale,
                                    onDismissLastSale = viewModel::clearLastCompletedSale,
                                    onShareSaleReceipt = { viewModel.shareSaleReceipt(context, it) },
                                    onPrintSaleInvoice = { viewModel.printSaleInvoice(context, it) },
                                    onAddToCart = viewModel::addToCart,
                                    onDecrementProduct = viewModel::decrementProductFromCart,
                                    onUpdateCartItem = viewModel::updateCartItem,
                                    onHoldCart = viewModel::holdCurrentCart,
                                    onResumeHeldCart = viewModel::resumeHeldCart,
                                    onDiscardHeldCart = viewModel::discardHeldCart,
                                    onClearCart = viewModel::clearCart,
                                    onCompleteSale = viewModel::completeSale,
                                    onNavigateToPricing = { currentSection = AppSection.INVENTORY }
                                )

                                AppSection.PURCHASES -> PurchasesScreen(
                                    products = uiState.products,
                                    suppliers = uiState.suppliers,
                                    purchases = uiState.filteredPurchases,
                                    currentUser = currentUser,
                                    roundingStep = uiState.settings.pricingRoundingStep,
                                    onRecordPurchase = viewModel::recordPurchase
                                )

                                AppSection.INVENTORY -> InventoryPricingScreen(
                                    products = uiState.products,
                                    stockMovements = uiState.stockMovements,
                                    currentUser = currentUser,
                                    roundingStep = uiState.settings.pricingRoundingStep,
                                    onSaveProduct = viewModel::saveProduct,
                                    onDeleteProduct = viewModel::deleteProduct
                                )

                                AppSection.EXPENSES -> ExpensesScreen(
                                    expenses = uiState.filteredExpenses,
                                    currentUser = currentUser,
                                    onRecordExpense = viewModel::recordExpense
                                )

                                AppSection.RETURNS -> ReturnsAdjustmentsScreen(
                                    sales = uiState.filteredSales,
                                    products = uiState.products,
                                    returns = uiState.filteredReturns,
                                    stockMovements = uiState.stockMovements,
                                    currentUser = currentUser,
                                    onRecordCustomerReturn = viewModel::recordCustomerReturn,
                                    onRecordStockAdjustmentOrWaste = viewModel::recordStockAdjustmentOrWaste
                                )

                                AppSection.PARTIES -> PartiesScreen(
                                    suppliers = uiState.suppliers,
                                    customers = uiState.customers,
                                    debtPayments = uiState.filteredDebtPayments,
                                    currentUser = currentUser,
                                    onSaveSupplier = viewModel::saveSupplier,
                                    onSaveCustomer = viewModel::saveCustomer,
                                    onRecordDebtPayment = viewModel::recordDebtPayment
                                )

                                AppSection.WALLETS -> WalletsScreen(
                                    metrics = uiState.metrics,
                                    transfers = uiState.filteredTransfers,
                                    currentUser = currentUser,
                                    onRecordTransfer = viewModel::recordWalletTransfer
                                )

                                AppSection.ZREPORT -> ZReportScreen(
                                    currentUser = currentUser,
                                    metrics = uiState.metrics,
                                    openingBalances = uiState.settings.openingBalances,
                                    pastReports = uiState.allZReports,
                                    onSaveZReport = viewModel::saveZReport,
                                    onReopenZReport = viewModel::reopenZReport,
                                    onCopyWhatsApp = { viewModel.copyWhatsAppSummary(context) },
                                    onBack = { currentSection = defaultHome }
                                )

                                AppSection.REPORTS -> ReportsCenterScreen(
                                    currentUser = currentUser,
                                    periodLabel = uiState.dateFilter.arabicLabel,
                                    metrics = uiState.metrics,
                                    sales = uiState.filteredSales,
                                    purchases = uiState.filteredPurchases,
                                    expenses = uiState.filteredExpenses,
                                    products = uiState.products,
                                    movements = uiState.stockMovements,
                                    returns = uiState.filteredReturns,
                                    suppliers = uiState.suppliers,
                                    customers = uiState.customers,
                                    zReports = uiState.allZReports,
                                    onExportXlsx = { viewModel.exportXlsx(context) },
                                    onExportCsv = { viewModel.exportCsv(context) },
                                    onCopyWhatsApp = { viewModel.copyWhatsAppSummary(context) },
                                    onExportPdf = { viewModel.exportPdfReport(context, it) },
                                    onPrintReport = { viewModel.printReport(context, it) }
                                )

                                AppSection.USERS -> UsersManagementScreen(
                                    users = uiState.users,
                                    auditLogs = uiState.auditLogs,
                                    currentUser = currentUser,
                                    onSaveUser = viewModel::saveUser,
                                    onDeleteUser = viewModel::deleteUser
                                )

                                AppSection.BACKUP -> BackupScreen(
                                    currentUser = currentUser,
                                    backupFrequency = uiState.settings.backupFrequency,
                                    localBackups = uiState.localBackups,
                                    onChangeBackupFrequency = { freq ->
                                        viewModel.updateSettings(backupFrequency = freq)
                                    },
                                    onCreateManualBackup = viewModel::createManualBackup,
                                    onExportBackupUri = viewModel::exportBackupToUri,
                                    onRestoreUri = viewModel::restoreFromUri,
                                    onRestoreLocalFile = viewModel::restoreFromLocalBackup,
                                    onShareLocalBackup = { path -> viewModel.shareBackupFile(context, path) }
                                )

                                AppSection.SETTINGS -> SettingsScreen(
                                    settings = uiState.settings,
                                    currentUser = currentUser,
                                    onUpdateSettings = viewModel::updateSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeAllowedSections(user: UserEntity): List<AppSection> {
    return buildList {
        add(AppSection.DASHBOARD)
        if (user.canRecordSales || user.isAdmin) add(AppSection.POS)
        if (user.canRecordPurchases || user.isAdmin) add(AppSection.PURCHASES)
        if (user.canViewInventory || user.canEditPrices || user.canEditProducts || user.isAdmin) {
            add(AppSection.INVENTORY)
        }
        if (user.canRecordExpenses || user.isAdmin) add(AppSection.EXPENSES)
        if (user.canReverseOrDelete || user.canEditProducts || user.isAdmin) add(AppSection.RETURNS)
        if (user.canManageParties || user.isAdmin) add(AppSection.PARTIES)
        add(AppSection.WALLETS)
        if (user.canViewZReport || user.isAdmin) add(AppSection.ZREPORT)
        if (user.canViewSalesReports || user.canExportData || user.isAdmin) add(AppSection.REPORTS)
        if (user.canManageUsers || user.isAdmin) add(AppSection.USERS)
        if (user.canManageBackup || user.isAdmin) add(AppSection.BACKUP)
        add(AppSection.SETTINGS)
    }
}
