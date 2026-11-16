package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.PaymentSplitLine
import com.example.domain.ExpenseCategory
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import com.example.ui.components.MultiPaymentSplitEditor
import com.example.ui.components.buildSplitLines
import com.example.ui.theme.KikiExpenseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    currentUser: UserEntity,
    onRecordExpense: (
        title: String,
        category: ExpenseCategory,
        description: String,
        amount: Long,
        allocations: List<PaymentSplitLine>,
        receiptNote: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canRecordExpenses && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية تسجيل المصروفات.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    var selectedCategory by remember { mutableStateOf(ExpenseCategory.ELECTRICITY) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isSplitMode by remember { mutableStateOf(false) }
    var singleChannel by remember { mutableStateOf(WalletChannel.CASH) }
    val splitAmounts = remember { mutableStateMapOf<WalletChannel, String>() }
    var receiptNumber by remember { mutableStateOf("") }
    var operationNumber by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }

    val amount = PricingEngine.parseLongAmount(amountText)
    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تسجيل مصروف تشغيلي", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KikiExpenseRed)

                    Text("تصنيف المصروف", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExpenseCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = cat == selectedCategory,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.arabicLabel) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم المصروف (مثلاً: فاتورة كهرباء المحل)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ (ج.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("ملاحظات / تفاصيل إضافية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    MultiPaymentSplitEditor(
                        totalDue = amount,
                        isSplitMode = isSplitMode,
                        onSplitModeChange = { isSplitMode = it },
                        singleChannel = singleChannel,
                        onSingleChannelChange = { singleChannel = it },
                        splitAmounts = splitAmounts,
                        onSplitAmountChange = { ch, txt -> splitAmounts[ch] = txt },
                        receiptNumber = receiptNumber,
                        onReceiptNumberChange = { receiptNumber = it },
                        operationNumber = operationNumber,
                        onOperationNumberChange = { operationNumber = it },
                        receiptNote = receiptNote,
                        onReceiptNoteChange = { receiptNote = it },
                        allowPartialCredit = false
                    )

                    Button(
                        onClick = {
                            if (amount <= 0L) return@Button
                            val allocs = buildSplitLines(amount, isSplitMode, singleChannel, splitAmounts, receiptNumber, operationNumber)
                            onRecordExpense(
                                title.ifBlank { selectedCategory.arabicLabel },
                                selectedCategory,
                                description,
                                amount,
                                allocs,
                                receiptNote
                            )
                            title = ""
                            amountText = ""
                            description = ""
                            receiptNote = ""
                            receiptNumber = ""
                            operationNumber = ""
                            splitAmounts.clear()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_expense_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ المصروف وخصمه من المحفظة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (expenses.isNotEmpty()) {
            item { Text("سجل المصروفات التشغيلية (${expenses.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(expenses.take(30), key = { it.id }) { e ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${e.expenseNumber} • ${e.title}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${e.category.arabicLabel} | ${e.paymentSummary} | ${timeFormatter.format(Date(e.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("- ${PricingEngine.formatCurrency(e.amount)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = KikiExpenseRed)
                    }
                }
            }
        }
    }
}
