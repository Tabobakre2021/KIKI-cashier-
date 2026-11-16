package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.entity.ZReportEntity
import com.example.domain.FinancialMetrics
import com.example.export.ExportManager
import com.example.export.ReportCategory
import com.example.ui.theme.KikiGoldSoft

@Composable
fun ReportsCenterScreen(
    currentUser: UserEntity,
    periodLabel: String,
    metrics: FinancialMetrics,
    sales: List<SaleWithItems>,
    purchases: List<PurchaseEntity>,
    expenses: List<ExpenseEntity>,
    products: List<ProductEntity>,
    movements: List<StockMovementEntity>,
    returns: List<ReturnEntity>,
    suppliers: List<SupplierEntity>,
    customers: List<CustomerEntity>,
    zReports: List<ZReportEntity>,
    onExportXlsx: () -> Unit,
    onExportCsv: () -> Unit,
    onCopyWhatsApp: () -> Unit,
    onExportPdf: (ReportCategory) -> Unit,
    onPrintReport: (ReportCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAccess = currentUser.canViewSalesReports || currentUser.canExportData || currentUser.isAdmin
    if (!canAccess) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية عرض أو تصدير التقارير.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    var expandedCategory by remember { mutableStateOf<ReportCategory?>(ReportCategory.DAILY_SALES) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("مركز التقارير والتصدير الشامل", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("الفترة المحددة: $periodLabel — 13 تقريراً احترافياً (PDF / XLSX / CSV / طباعة)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تصدير الحزمة المحاسبية الكاملة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onExportXlsx,
                            modifier = Modifier.weight(1f).testTag("export_xlsx_button")
                        ) {
                            Icon(Icons.Filled.TableView, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Excel (XLSX)")
                        }
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f).testTag("export_csv_button")
                        ) {
                            Text("ملف CSV")
                        }
                    }
                    OutlinedButton(onClick = onCopyWhatsApp, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("نسخ ملخص التقرير الجاهز للواتساب")
                    }
                }
            }
        }

        items(ReportCategory.entries) { category ->
            val restricted = category.requiresProfitAccess && !currentUser.canViewProfits
            val isExpanded = expandedCategory == category

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.medium
                    )
                    .clickable(enabled = !restricted) {
                        expandedCategory = if (isExpanded) null else category
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.arabicTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (restricted) {
                            Surface(color = KikiGoldSoft, shape = MaterialTheme.shapes.small) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("للأدمن فقط", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isExpanded && !restricted) {
                        val lines = remember(category, metrics, sales, purchases, expenses, products, movements, returns, suppliers, customers, zReports) {
                            ExportManager.buildReportLines(
                                category = category,
                                metrics = metrics,
                                sales = sales,
                                purchases = purchases,
                                expenses = expenses,
                                products = products,
                                movements = movements,
                                returns = returns,
                                suppliers = suppliers,
                                customers = customers,
                                zReports = zReports,
                                canViewProfits = currentUser.canViewProfits
                            )
                        }
                        HorizontalDivider()
                        lines.take(15).forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall)
                        }
                        if (lines.size > 15) {
                            Text(
                                "... وعدد ${lines.size - 15} سطراً إضافياً في التصدير الكامل.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onExportPdf(category) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("تصدير PDF")
                            }
                            OutlinedButton(
                                onClick = { onPrintReport(category) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("طباعة")
                            }
                        }
                    }
                }
            }
        }
    }
}
