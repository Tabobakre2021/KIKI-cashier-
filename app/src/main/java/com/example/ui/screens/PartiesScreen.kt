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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.entity.CustomerEntity
import com.example.data.entity.DebtPaymentEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiExpenseRedSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PartiesScreen(
    suppliers: List<SupplierEntity>,
    customers: List<CustomerEntity>,
    debtPayments: List<DebtPaymentEntity>,
    currentUser: UserEntity,
    onSaveSupplier: (SupplierEntity) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onRecordDebtPayment: (partyType: String, partyId: Long, amount: Long, wallet: WalletChannel, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canManageParties && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية إدارة الموردين والعملاء.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Suppliers, 1 = Customers
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentTarget by remember { mutableStateOf<Triple<String, Long, Pair<String, Long>>?>(null) }
    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }

    val totalSupplierDue = remember(suppliers) { suppliers.sumOf { it.balanceDue } }
    val totalCustomerDue = remember(customers) { customers.sumOf { it.balanceDue } }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("الموردون والعملاء والمديونيات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "متابعة الذمم الدائنة والمدينة وتسوية السداد عبر المحافظ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KikiExpenseRedSoft),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("متبقي للموردين", style = MaterialTheme.typography.labelMedium, color = KikiExpenseRed)
                            Text(PricingEngine.formatCurrency(totalSupplierDue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KikiExpenseRed)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KikiProfitGreenSoft),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("مستحق على العملاء", style = MaterialTheme.typography.labelMedium, color = KikiProfitGreen)
                            Text(PricingEngine.formatCurrency(totalCustomerDue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = KikiProfitGreen)
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("الموردون (${suppliers.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("العملاء (${customers.size})") })
                }
            }

            if (selectedTab == 0) {
                if (suppliers.isEmpty()) {
                    item { Text("لا يوجد موردون مسجلون. اضغط (+) لإضافة مورد.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(suppliers, key = { it.id }) { s ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (s.phone.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(s.phone, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    Surface(
                                        color = if (s.balanceDue > 0L) KikiExpenseRedSoft else KikiProfitGreenSoft,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "الرصيد: ${PricingEngine.formatCurrency(s.balanceDue)}",
                                            color = if (s.balanceDue > 0L) KikiExpenseRed else KikiProfitGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    "إجمالي المشتريات: ${PricingEngine.formatAmount(s.totalPurchased)} | المسدد: ${PricingEngine.formatAmount(s.totalPaid)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (s.balanceDue > 0L) {
                                    OutlinedButton(
                                        onClick = { paymentTarget = Triple("SUPPLIER", s.id, s.name to s.balanceDue) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Payments, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("تسجيل سداد للمورد")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (customers.isEmpty()) {
                    item { Text("لا يوجد عملاء مسجلون. اضغط (+) لإضافة عميل آجل.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(customers, key = { it.id }) { c ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        if (c.phone.isNotBlank()) {
                                            Text(c.phone, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Surface(
                                        color = if (c.balanceDue > 0L) KikiExpenseRedSoft else KikiProfitGreenSoft,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "المستحق: ${PricingEngine.formatCurrency(c.balanceDue)}",
                                            color = if (c.balanceDue > 0L) KikiExpenseRed else KikiProfitGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    "مبيعات آجل: ${PricingEngine.formatAmount(c.totalCreditSales)} | المحصل: ${PricingEngine.formatAmount(c.totalPaid)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (c.balanceDue > 0L) {
                                    OutlinedButton(
                                        onClick = { paymentTarget = Triple("CUSTOMER", c.id, c.name to c.balanceDue) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Payments, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("تحصيل دفعة من العميل")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (debtPayments.isNotEmpty()) {
                item {
                    Text("سجل سداد وتحصيل المديونيات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(debtPayments.take(20), key = { it.id }) { dp ->
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${if (dp.partyType == "CUSTOMER") "تحصيل من عميل" else "سداد لمورد"}: ${dp.partyName}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${dp.walletChannel.arabicLabel} • ${timeFormatter.format(Date(dp.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                PricingEngine.formatCurrency(dp.amount),
                                fontWeight = FontWeight.Bold,
                                color = if (dp.partyType == "CUSTOMER") KikiProfitGreen else KikiExpenseRed
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_party_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "إضافة طرف جديد")
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (selectedTab == 0) "إضافة مورد جديد" else "إضافة عميل جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        if (selectedTab == 0) {
                            onSaveSupplier(SupplierEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
                        } else {
                            onSaveCustomer(CustomerEntity(name = name.trim(), phone = phone.trim(), notes = notes.trim()))
                        }
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("إلغاء") }
            }
        )
    }

    paymentTarget?.let { (partyType, partyId, info) ->
        val (partyName, due) = info
        var amountText by remember { mutableStateOf(due.toString()) }
        var wallet by remember { mutableStateOf(WalletChannel.CASH) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { paymentTarget = null },
            title = {
                Text(if (partyType == "CUSTOMER") "تحصيل مديونية: $partyName" else "سداد مديونية: $partyName")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المبلغ المستحق الحالي: ${PricingEngine.formatCurrency(due)}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("المبلغ المدفوع (ج.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("المحفظة المستخدمة:", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WalletChannel.entries.forEach { ch ->
                            FilterChip(
                                selected = ch == wallet,
                                onClick = { wallet = ch },
                                label = { Text(ch.arabicLabel) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("رقم الإشعار / ملاحظات") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val amt = PricingEngine.parseLongAmount(amountText)
                Button(
                    onClick = {
                        if (amt > 0L) {
                            onRecordDebtPayment(partyType, partyId, amt, wallet, note.trim())
                            paymentTarget = null
                        }
                    },
                    enabled = amt > 0L
                ) {
                    Text("تأكيد العملية")
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentTarget = null }) { Text("إلغاء") }
            }
        )
    }
}
