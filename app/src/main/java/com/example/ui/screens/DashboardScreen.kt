package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.SaleWithItems
import com.example.data.entity.UserEntity
import com.example.domain.DateFilter
import com.example.domain.FinancialMetrics
import com.example.domain.PricingEngine
import com.example.ui.components.KikiChartsCard
import com.example.ui.components.WalletBalanceStrip
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiExpenseRedSoft
import com.example.ui.theme.KikiGold
import com.example.ui.theme.KikiInfoBlue
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    currentUser: UserEntity,
    dateFilter: DateFilter,
    onDateFilterChange: (DateFilter) -> Unit,
    metrics: FinancialMetrics,
    sales: List<SaleWithItems>,
    onShareSaleReceipt: (SaleWithItems) -> Unit,
    onOpenZReport: () -> Unit,
    onOpenReportsCenter: () -> Unit,
    onOpenReturns: () -> Unit,
    onCopyWhatsApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Period Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilter.entries.filter { it != DateFilter.CUSTOM }.forEach { filter ->
                    FilterChip(
                        selected = filter == dateFilter,
                        onClick = { onDateFilterChange(filter) },
                        label = { Text(filter.arabicLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Daily Operations KPIs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiStatCard(
                        title = "صافي المبيعات (${metrics.salesCount} فاتورة)",
                        value = PricingEngine.formatCurrency(metrics.netSales),
                        subtitle = "متوسط الفاتورة: ${PricingEngine.formatAmount(metrics.averageInvoiceValue)}",
                        icon = Icons.Filled.PointOfSale,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "صافي التحصيل",
                        value = PricingEngine.formatCurrency(metrics.netCollection),
                        subtitle = "كاش: ${PricingEngine.formatAmount(metrics.cashCollected)} | بنكي: ${PricingEngine.formatAmount(metrics.bankCollected)}",
                        icon = Icons.Filled.Payments,
                        accent = KikiProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiStatCard(
                        title = "المصروفات التشغيلية",
                        value = PricingEngine.formatCurrency(metrics.totalExpenses),
                        icon = Icons.Filled.Receipt,
                        accent = KikiExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "المرتجعات",
                        value = PricingEngine.formatCurrency(metrics.totalCustomerReturns),
                        icon = Icons.Filled.AssignmentReturn,
                        accent = KikiGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Profit & Valuation KPIs — STRICTLY GATED BY canViewProfits
        item {
            if (currentUser.canViewProfits) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiStatCard(
                            title = "مجمل الربح (Gross)",
                            value = PricingEngine.formatCurrency(metrics.grossProfit),
                            subtitle = "التكلفة (COGS): ${PricingEngine.formatAmount(metrics.totalCogs)}",
                            icon = Icons.Filled.TrendingUp,
                            accent = KikiProfitGreen,
                            containerColor = KikiProfitGreenSoft.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "صافي الربح التشغيلي",
                            value = PricingEngine.formatCurrency(metrics.netOperatingProfit),
                            subtitle = "بعد المصروفات التشغيلية",
                            icon = Icons.Filled.TrendingUp,
                            accent = if (metrics.netOperatingProfit >= 0L) KikiProfitGreen else KikiExpenseRed,
                            containerColor = if (metrics.netOperatingProfit >= 0L) KikiProfitGreenSoft.copy(alpha = 0.75f) else KikiExpenseRedSoft,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiStatCard(
                            title = "قيمة المخزون الحالي",
                            value = PricingEngine.formatCurrency(metrics.totalInventoryValue),
                            subtitle = "تنبيه انخفاض: ${metrics.lowStockCount} أصناف",
                            icon = Icons.Filled.Inventory2,
                            accent = KikiInfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = "الذمم والديون",
                            value = "عملاء: ${PricingEngine.formatAmount(metrics.totalCustomerDebt)}",
                            subtitle = "موردين: ${PricingEngine.formatAmount(metrics.totalSupplierDebt)}",
                            icon = Icons.Filled.People,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "حماية الأرباح مفعّلة: التكاليف ومجمل وصافي الربح مخفية تماماً عن هذا الحساب.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Low stock alert banner
        if (metrics.lowStockCount > 0) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            text = "تنبيه مخزون: يوجد ${metrics.lowStockCount} أصناف عند أو تحت حد التنبيه.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 5 Wallets Strip
        item {
            WalletBalanceStrip(metrics = metrics)
        }

        // Charts
        item {
            KikiChartsCard(
                dailySeries = metrics.dailySeries,
                expensesByCategory = metrics.expensesByCategory,
                canViewProfits = currentUser.canViewProfits
            )
        }

        // Top Selling & Slow Moving Products
        if (metrics.topSellingProducts.isNotEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("أفضل الأصناف مبيعاً", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        metrics.topSellingProducts.forEach { stat ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${stat.productName} (${PricingEngine.formatQty(stat.quantitySold)})", style = MaterialTheme.typography.bodySmall)
                                Text(PricingEngine.formatCurrency(stat.totalRevenue), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Quick ERP Actions
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إجراءات سريعة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentUser.canViewZReport || currentUser.isAdmin) {
                            Button(onClick = onOpenZReport, modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("open_zreport_button")) {
                                Text("تقفيل اليومية (Z)", maxLines = 1)
                            }
                        }
                        if (currentUser.canViewSalesReports || currentUser.canExportData || currentUser.isAdmin) {
                            OutlinedButton(onClick = onOpenReportsCenter, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                                Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مركز التقارير", maxLines = 1)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentUser.canReverseOrDelete || currentUser.isAdmin) {
                            OutlinedButton(onClick = onOpenReturns, modifier = Modifier.weight(1f).heightIn(min = 46.dp)) {
                                Icon(Icons.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("المرتجعات والتالف", maxLines = 1)
                            }
                        }
                        if (currentUser.canExportData || currentUser.canViewZReport || currentUser.isAdmin) {
                            OutlinedButton(onClick = onCopyWhatsApp, modifier = Modifier.weight(1f).heightIn(min = 46.dp).testTag("copy_whatsapp_button")) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ملخص واتساب", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Recent Sales Invoices
        item {
            Text("أحدث فواتير المبيعات (${sales.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        if (sales.isEmpty()) {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                    Text("لا توجد فواتير مبيعات في هذه الفترة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(sales.take(25), key = { it.sale.id }) { sw ->
                val s = sw.sale
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${s.invoiceNumber} • ${s.cashierName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${timeFormatter.format(Date(s.timestamp))} | ${s.paymentSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (currentUser.canViewProfits) {
                                Text("الربح: ${PricingEngine.formatCurrency(s.totalProfit)}", style = MaterialTheme.typography.labelMedium, color = KikiProfitGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(PricingEngine.formatCurrency(s.totalAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { onShareSaleReceipt(sw) }) {
                                Icon(Icons.Filled.Share, contentDescription = "مشاركة الفاتورة", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
