package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.UserEntity
import com.example.data.entity.ZReportEntity
import com.example.domain.FinancialMetrics
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiExpenseRedSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft

@Composable
fun ZReportScreen(
    currentUser: UserEntity,
    metrics: FinancialMetrics,
    openingBalances: Map<WalletChannel, Long>,
    pastReports: List<ZReportEntity>,
    onSaveZReport: (Map<WalletChannel, Long>, Map<WalletChannel, Long>, String) -> Unit,
    onReopenZReport: (ZReportEntity, String) -> Unit,
    onCopyWhatsApp: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    if (!currentUser.canViewZReport && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية تقفيل اليومية.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val openingInputs = remember(openingBalances) {
        mutableStateMapOf<WalletChannel, String>().apply {
            WalletChannel.entries.forEach { ch ->
                val v = openingBalances[ch] ?: 0L
                put(ch, if (v == 0L) "0" else v.toString())
            }
        }
    }
    val actualInputs = remember { mutableStateMapOf<WalletChannel, String>() }
    var notes by remember { mutableStateOf("") }
    var reopenTarget by remember { mutableStateOf<ZReportEntity?>(null) }
    var reopenReason by remember { mutableStateOf("") }

    val parsedOpenings = WalletChannel.entries.associateWith {
        PricingEngine.parseLongAmount(openingInputs[it].orEmpty())
    }
    val expectedMap = WalletChannel.entries.associateWith { ch ->
        val w = metrics.wallets[ch]
        (parsedOpenings[ch] ?: 0L) + (w?.totalInflows ?: 0L) - (w?.totalOutflows ?: 0L) + (w?.transfersIn ?: 0L) - (w?.transfersOut ?: 0L)
    }
    val actualMap = WalletChannel.entries.associateWith { ch ->
        val txt = actualInputs[ch].orEmpty()
        if (txt.isBlank()) (expectedMap[ch] ?: 0L) else PricingEngine.parseLongAmount(txt)
    }
    val totalVariance = WalletChannel.entries.sumOf { ch ->
        (actualMap[ch] ?: 0L) - (expectedMap[ch] ?: 0L)
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Column {
                    Text("تقفيل اليومية والمطابقة (Z-Report)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("الأرصدة الافتتاحية والحركة والجرد الفعلي للمحافظ الـ 5", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("أولاً: الأرصدة الافتتاحية وبداية اليوم", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    WalletChannel.entries.forEach { ch ->
                        val expected = expectedMap[ch] ?: 0L
                        val actual = actualMap[ch] ?: 0L
                        val diff = actual - expected
                        val w = metrics.wallets[ch]

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ch.arabicLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "وارد: ${PricingEngine.formatAmount(w?.totalInflows ?: 0L)} | صادر: ${PricingEngine.formatAmount(w?.totalOutflows ?: 0L)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = openingInputs[ch].orEmpty(),
                                    onValueChange = { openingInputs[ch] = it },
                                    label = { Text("افتتاحي") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = actualInputs[ch].orEmpty(),
                                    onValueChange = { actualInputs[ch] = it },
                                    placeholder = { Text(PricingEngine.formatAmount(expected)) },
                                    label = { Text("الفعلي المعدود") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المتوقع: ${PricingEngine.formatCurrency(expected)}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = when {
                                        diff == 0L -> "متطابق"
                                        diff > 0L -> "زيادة: +${PricingEngine.formatAmount(diff)}"
                                        else -> "عجز: ${PricingEngine.formatAmount(diff)}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff < 0L) KikiExpenseRed else KikiProfitGreen
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    Surface(
                        color = when {
                            totalVariance == 0L -> KikiProfitGreenSoft
                            totalVariance > 0L -> MaterialTheme.colorScheme.secondaryContainer
                            else -> KikiExpenseRedSoft
                        },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    totalVariance == 0L -> "المطابقة الإجمالية تامة"
                                    totalVariance > 0L -> "إجمالي الزيادة:"
                                    else -> "إجمالي العجز:"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (totalVariance < 0L) KikiExpenseRed else KikiProfitGreen
                            )
                            Text(
                                text = PricingEngine.formatCurrency(totalVariance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalVariance < 0L) KikiExpenseRed else KikiProfitGreen
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات الإغلاق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveZReport(parsedOpenings, actualMap, notes)
                                notes = ""
                            },
                            modifier = Modifier.weight(1.3f).heightIn(min = 50.dp).testTag("save_zreport_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إغلاق واعتماد اليومية", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onCopyWhatsApp, modifier = Modifier.weight(0.9f).heightIn(min = 50.dp)) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("واتساب")
                        }
                    }
                }
            }
        }

        if (pastReports.isNotEmpty()) {
            item { Text("سجل تقارير التقفيل (${pastReports.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(pastReports, key = { it.id }) { r ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${r.dateLabel} • ${r.closedByName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (r.isReopened) "أُعيد فتحها: ${r.reopenReason}" else "مقفلة نهائياً | كاش فعلي: ${PricingEngine.formatAmount(r.actualCash)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = PricingEngine.formatCurrency(r.totalVariance),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (r.totalVariance < 0L) KikiExpenseRed else KikiProfitGreen
                            )
                            if (r.isClosed && !r.isReopened && (currentUser.canReopenZReport || currentUser.isAdmin)) {
                                TextButton(onClick = { reopenTarget = r }) {
                                    Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إعادة فتح", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reopenTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { reopenTarget = null },
            title = { Text("إعادة فتح اليومية (${target.dateLabel})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سيتم تسجيل عملية إعادة الفتح في سجل التدقيق (Audit Log). أدخل السبب:")
                    OutlinedTextField(
                        value = reopenReason,
                        onValueChange = { reopenReason = it },
                        label = { Text("سبب إعادة فتح اليومية") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (reopenReason.isNotBlank()) {
                        onReopenZReport(target, reopenReason)
                        reopenReason = ""
                        reopenTarget = null
                    }
                }) {
                    Text("تأكيد إعادة الفتح")
                }
            },
            dismissButton = {
                TextButton(onClick = { reopenTarget = null }) { Text("إلغاء") }
            }
        )
    }
}
