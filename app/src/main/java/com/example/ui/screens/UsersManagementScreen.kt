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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.AuditLogEntity
import com.example.data.entity.UserEntity
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiExpenseRedSoft
import com.example.ui.theme.KikiGoldSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsersManagementScreen(
    users: List<UserEntity>,
    auditLogs: List<AuditLogEntity>,
    currentUser: UserEntity,
    onSaveUser: (UserEntity) -> Unit,
    onDeleteUser: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canManageUsers && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = "هذه الشاشة حصرية للمدير العام (إدارة المستخدمين وسجل التدقيق).",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium
            )
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var auditQuery by remember { mutableStateOf("") }
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    val filteredLogs = remember(auditLogs, auditQuery) {
        if (auditQuery.isBlank()) auditLogs
        else {
            val q = auditQuery.trim()
            auditLogs.filter {
                it.summary.contains(q, ignoreCase = true) ||
                    it.userName.contains(q, ignoreCase = true) ||
                    it.actionType.arabicLabel.contains(q, ignoreCase = true)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("المستخدمون والصلاحيات وسجل التدقيق", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "15 صلاحية دقيقة + حماية حصرية للأرباح + رقابة شاملة على كل العمليات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("المستخدمون (${users.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("سجل التدقيق (${auditLogs.size})") })
                }
            }

            if (selectedTab == 0) {
                items(users, key = { it.id }) { user ->
                    val isLocked = user.lockedUntilMillis > System.currentTimeMillis()
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { editingUser = user }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (user.isAdmin) Icons.Filled.Shield else Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${user.roleTitle} • PIN: ${user.pinCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Surface(
                                        color = if (user.canViewProfits) KikiGoldSoft else KikiProfitGreenSoft,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (user.canViewProfits) "صلاحية كاملة: يرى التكاليف والأرباح" else "حماية الأرباح مفعّلة (بيع فقط)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (user.canViewProfits) Color(0xFF78350F) else KikiProfitGreen,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!user.isActive) {
                                    Text("موقوف", color = KikiExpenseRed, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                if (isLocked) {
                                    Surface(color = KikiExpenseRedSoft, shape = MaterialTheme.shapes.small) {
                                        Text(
                                            "مقفل",
                                            color = KikiExpenseRed,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = auditQuery,
                        onValueChange = { auditQuery = it },
                        placeholder = { Text("ابحث في سجل التدقيق (العملية، المستخدم، الفاتورة)...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(filteredLogs.take(120), key = { it.id }) { log ->
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.actionType.arabicLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(timeFormatter.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(log.summary, style = MaterialTheme.typography.bodySmall)
                            Text("بواسطة: ${log.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (log.reason.isNotBlank()) {
                                Text("السبب: ${log.reason}", style = MaterialTheme.typography.labelSmall, color = KikiExpenseRed)
                            }
                            if (log.beforeData.isNotBlank() || log.afterData.isNotBlank()) {
                                Text(
                                    "التغيير: ${log.beforeData} ⬅ ${log.afterData}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            FloatingActionButton(
                onClick = {
                    editingUser = UserEntity(
                        displayName = "",
                        roleTitle = "كاشير صباحي",
                        pinCode = "1111",
                        isAdmin = false,
                        canRecordSales = true,
                        canViewInventory = true,
                        isActive = true
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("add_user_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة مستخدم جديد")
            }
        }
    }

    editingUser?.let { target ->
        UserEditorBottomSheet(
            initialUser = target,
            canDelete = target.id != 0L && target.id != currentUser.id,
            onDismiss = { editingUser = null },
            onSave = { updated ->
                onSaveUser(updated)
                editingUser = null
            },
            onDelete = {
                onDeleteUser(target.id)
                editingUser = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserEditorBottomSheet(
    initialUser: UserEntity,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit,
    onDelete: () -> Unit
) {
    var displayName by remember { mutableStateOf(initialUser.displayName) }
    var roleTitle by remember { mutableStateOf(initialUser.roleTitle) }
    var pinCode by remember { mutableStateOf(initialUser.pinCode) }
    var isAdmin by remember { mutableStateOf(initialUser.isAdmin) }
    var mustChangePin by remember { mutableStateOf(initialUser.mustChangePin) }
    var isActive by remember { mutableStateOf(initialUser.isActive) }
    var failedAttempts by remember { mutableStateOf(initialUser.failedAttempts) }
    var lockedUntilMillis by remember { mutableStateOf(initialUser.lockedUntilMillis) }

    // All 15 Granular Permissions + Exclusive Admin Profit Protection
    var canRecordSales by remember { mutableStateOf(initialUser.canRecordSales) }
    var canRecordPurchases by remember { mutableStateOf(initialUser.canRecordPurchases) }
    var canRecordExpenses by remember { mutableStateOf(initialUser.canRecordExpenses) }
    var canEditPrices by remember { mutableStateOf(initialUser.canEditPrices) }
    var canAddProducts by remember { mutableStateOf(initialUser.canAddProducts) }
    var canEditProducts by remember { mutableStateOf(initialUser.canEditProducts) }
    var canReverseOrDelete by remember { mutableStateOf(initialUser.canReverseOrDelete) }
    var canViewInventory by remember { mutableStateOf(initialUser.canViewInventory) }
    var canManageParties by remember { mutableStateOf(initialUser.canManageParties) }
    var canViewSalesReports by remember { mutableStateOf(initialUser.canViewSalesReports) }
    var canViewZReport by remember { mutableStateOf(initialUser.canViewZReport) }
    var canExportData by remember { mutableStateOf(initialUser.canExportData) }
    var canManageUsers by remember { mutableStateOf(initialUser.canManageUsers) }
    var canReopenZReport by remember { mutableStateOf(initialUser.canReopenZReport) }
    var canManageBackup by remember { mutableStateOf(initialUser.canManageBackup) }
    var canViewProfits by remember { mutableStateOf(initialUser.canViewProfits) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialUser.id == 0L) "إنشاء مستخدم جديد" else "تعديل صلاحيات: ${initialUser.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("اسم المستخدم") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it },
                    label = { Text("المسمى الوظيفي") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = pinCode,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinCode = it },
                    label = { Text("PIN (4 أرقام)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            if (lockedUntilMillis > System.currentTimeMillis() || failedAttempts > 0) {
                OutlinedButton(
                    onClick = {
                        failedAttempts = 0
                        lockedUntilMillis = 0L
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("إلغاء قفل الحساب وتصفير المحاولات الخاطئة")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("حساب نشط", style = MaterialTheme.typography.titleSmall)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("دور المدير العام (Admin)", style = MaterialTheme.typography.titleSmall)
                Switch(checked = isAdmin, onCheckedChange = { isAdmin = it })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("إلزام بتغيير PIN عند الدخول التالي", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = mustChangePin, onCheckedChange = { mustChangePin = it })
            }

            HorizontalDivider()

            Text("الصلاحيات التفصيلية (15 صلاحية + حماية الأرباح):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            PermissionCheckboxRow("1. تسجيل المبيعات والكاشير", canRecordSales) { canRecordSales = it }
            PermissionCheckboxRow("2. تسجيل المشتريات والتوريد", canRecordPurchases) { canRecordPurchases = it }
            PermissionCheckboxRow("3. تسجيل المصروفات التشغيلية", canRecordExpenses) { canRecordExpenses = it }
            PermissionCheckboxRow("4. تعديل أسعار البيع", canEditPrices) { canEditPrices = it }
            PermissionCheckboxRow("5. إضافة أصناف جديدة", canAddProducts) { canAddProducts = it }
            PermissionCheckboxRow("6. تعديل الأصناف والمخزون", canEditProducts) { canEditProducts = it }
            PermissionCheckboxRow("7. تسجيل المرتجعات وعكس العمليات", canReverseOrDelete) { canReverseOrDelete = it }
            PermissionCheckboxRow("8. عرض شاشة المخزون وحركاته", canViewInventory) { canViewInventory = it }
            PermissionCheckboxRow("9. إدارة الموردين والعملاء والمديونيات", canManageParties) { canManageParties = it }
            PermissionCheckboxRow("10. الاطلاع على تقارير المبيعات", canViewSalesReports) { canViewSalesReports = it }
            PermissionCheckboxRow("11. تقفيل اليومية (Z-Report) والتحويل بين المحافظ", canViewZReport) { canViewZReport = it }
            PermissionCheckboxRow("12. إعادة فتح يومية مقفلة", canReopenZReport) { canReopenZReport = it }
            PermissionCheckboxRow("13. تصدير البيانات والطباعة (PDF/XLSX/CSV)", canExportData) { canExportData = it }
            PermissionCheckboxRow("14. إدارة المستخدمين وسجل التدقيق", canManageUsers) { canManageUsers = it }
            PermissionCheckboxRow("15. النسخ الاحتياطي واستعادة البيانات", canManageBackup) { canManageBackup = it }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                PermissionCheckboxRow(
                    label = "[حصرية للأدمن - حماية الأرباح]: رؤية أسعار الشراء ومتوسط التكلفة ومجمل/صافي الربح وتقارير الربحية",
                    checked = canViewProfits,
                    onCheckedChange = { canViewProfits = it }
                )
            }

            Button(
                onClick = {
                    if (displayName.isBlank() || pinCode.length != 4) return@Button
                    onSave(
                        initialUser.copy(
                            displayName = displayName.trim(),
                            roleTitle = roleTitle.ifBlank { "موظف" }.trim(),
                            pinCode = pinCode,
                            mustChangePin = mustChangePin,
                            isAdmin = isAdmin,
                            canRecordSales = canRecordSales,
                            canRecordPurchases = canRecordPurchases,
                            canRecordExpenses = canRecordExpenses,
                            canEditPrices = canEditPrices,
                            canAddProducts = canAddProducts,
                            canEditProducts = canEditProducts,
                            canReverseOrDelete = canReverseOrDelete,
                            canViewInventory = canViewInventory,
                            canManageParties = canManageParties,
                            canViewSalesReports = canViewSalesReports,
                            canViewZReport = canViewZReport,
                            canExportData = canExportData,
                            canManageUsers = canManageUsers,
                            canReopenZReport = canReopenZReport,
                            canManageBackup = canManageBackup,
                            canViewProfits = canViewProfits,
                            isActive = isActive,
                            failedAttempts = failedAttempts,
                            lockedUntilMillis = lockedUntilMillis
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("save_user_button")
            ) {
                Text("حفظ المستخدم والصلاحيات", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
