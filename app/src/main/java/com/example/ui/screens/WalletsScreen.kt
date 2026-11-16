package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.UserEntity
import com.example.data.entity.WalletTransferEntity
import com.example.domain.FinancialMetrics
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiProfitGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletsScreen(
    metrics: FinancialMetrics,
    transfers: List<WalletTransferEntity>,
    currentUser: UserEntity,
    onRecordTransfer: (from: WalletChannel, to: WalletChannel, amount: Long, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }
    var fromChannel by remember { mutableStateOf(WalletChannel.CASH) }
    var toChannel by remember { mutableStateOf(WalletChannel.BANKAK) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val canTransfer = currentUser.canViewZReport || currentUser.isAdmin
    val parsedAmount = PricingEngine.parseLongAmount(amountText)

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("المحافظ المالية الخمس والتحويلات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "التحويلات الداخلية تنقل السيولة بين المحافظ دون التأثير على الأرباح أو المصروفات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(WalletChannel.entries) { ch ->
            val w = metrics.wallets[ch]
            val net = w?.netBalance ?: 0L
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ch.arabicLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            PricingEngine.formatCurrency(net),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (net >= 0L) KikiProfitGreen else KikiExpenseRed
                        )
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("افتتاحي: ${PricingEngine.formatAmount(w?.openingBalance ?: 0L)}", style = MaterialTheme.typography.labelMedium)
                        Text("مبيعات: ${PricingEngine.formatAmount(w?.salesReceipts ?: 0L)}", style = MaterialTheme.typography.labelMedium, color = KikiProfitGreen)
                        Text("تحصيل ديون: ${PricingEngine.formatAmount(w?.debtCollections ?: 0L)}", style = MaterialTheme.typography.labelMedium, color = KikiProfitGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("مشتريات/سداد: ${PricingEngine.formatAmount((w?.purchasePayments ?: 0L) + (w?.supplierPayments ?: 0L))}", style = MaterialTheme.typography.labelMedium, color = KikiExpenseRed)
                        Text("مصروفات/رد: ${PricingEngine.formatAmount((w?.expensePayments ?: 0L) + (w?.customerRefunds ?: 0L))}", style = MaterialTheme.typography.labelMedium, color = KikiExpenseRed)
                    }
                    if ((w?.transfersIn ?: 0L) > 0L || (w?.transfersOut ?: 0L) > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تحويلات واردة: +${PricingEngine.formatAmount(w?.transfersIn ?: 0L)}", style = MaterialTheme.typography.labelSmall, color = KikiProfitGreen)
                            Text("تحويلات صادرة: -${PricingEngine.formatAmount(w?.transfersOut ?: 0L)}", style = MaterialTheme.typography.labelSmall, color = KikiExpenseRed)
                        }
                    }
                }
            }
        }

        if (canTransfer) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("تحويل رصيد بين المحافظ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text("من محفظة:", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WalletChannel.entries.forEach { ch ->
                                FilterChip(
                                    selected = ch == fromChannel,
                                    onClick = { fromChannel = ch },
                                    label = { Text(ch.arabicLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Text("إلى محفظة:", style = MaterialTheme.typography.labelLarge)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WalletChannel.entries.forEach { ch ->
                                FilterChip(
                                    selected = ch == toChannel,
                                    onClick = { toChannel = ch },
                                    label = { Text(ch.arabicLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("المبلغ المحول (ج.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("wallet_transfer_amount")
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("السبب / رقم الإشعار (مثال: توريد نقدية للبنك)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (fromChannel == toChannel || parsedAmount <= 0L) return@Button
                                onRecordTransfer(fromChannel, toChannel, parsedAmount, note.trim())
                                amountText = ""
                                note = ""
                            },
                            enabled = fromChannel != toChannel && parsedAmount > 0L,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("wallet_transfer_submit")
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تنفيذ التحويل الداخلي", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (transfers.isNotEmpty()) {
            item {
                Text("سجل التحويلات الداخلية (${transfers.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(transfers, key = { it.id }) { tr ->
                Card(
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${tr.fromChannel.arabicLabel} ⬅ ${tr.toChannel.arabicLabel}", fontWeight = FontWeight.Bold)
                            Text("${tr.userName} • ${timeFormatter.format(Date(tr.timestamp))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (tr.referenceNote.isNotBlank()) {
                                Text(tr.referenceNote, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(PricingEngine.formatCurrency(tr.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
