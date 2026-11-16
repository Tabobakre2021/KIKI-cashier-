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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.UserEntity
import com.example.data.repository.AppSettings
import com.example.domain.BackupFrequency
import com.example.domain.ThemePreference

@Composable
fun SettingsScreen(
    settings: AppSettings,
    currentUser: UserEntity,
    onUpdateSettings: (
        themePreference: ThemePreference?,
        allowNegativeStock: Boolean?,
        allowCreditSales: Boolean?,
        pricingRoundingStep: Int?,
        maxPinAttempts: Int?,
        backupFrequency: BackupFrequency?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGuideDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("إعدادات النظام ودليل الاستخدام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "تخصيص المظهر وقواعد البيع والتسعير والأمان",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Theme Selector
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مظهر التطبيق (الثيم)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = pref == settings.themePreference,
                                onClick = { onUpdateSettings(pref, null, null, null, null, null) },
                                label = { Text(pref.arabicLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Pricing Rounding & POS Policies (Admin or privileged users)
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("سياسات التسعير والمخزون والكاشير", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    Text("خطوة تقريب أسعار البيع المقترحة (ج.س):", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 50, 100, 500).forEach { step ->
                            FilterChip(
                                selected = settings.pricingRoundingStep == step,
                                onClick = {
                                    if (currentUser.isAdmin || currentUser.canEditPrices) {
                                        onUpdateSettings(null, null, null, step, null, null)
                                    }
                                },
                                label = { Text(if (step == 1) "بدون" else "$step") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("السماح بالبيع مع رصيد مخزون سالب", fontWeight = FontWeight.SemiBold)
                            Text("عند التعطيل يمنع الكاشير البيع إذا نفد الصنف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.allowNegativeStock,
                            enabled = currentUser.isAdmin,
                            onCheckedChange = { onUpdateSettings(null, it, null, null, null, null) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل البيع الآجل والذمم في الكاشير", fontWeight = FontWeight.SemiBold)
                            Text("يسمح بدفع جزئي وتسجيل الباقي على حساب العميل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.allowCreditSales,
                            enabled = currentUser.isAdmin,
                            onCheckedChange = { onUpdateSettings(null, null, it, null, null, null) }
                        )
                    }

                    HorizontalDivider()

                    Text("حد المحاولات الخاطئة قبل قفل الحساب مؤقتاً:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { count ->
                            FilterChip(
                                selected = settings.maxPinAttempts == count,
                                onClick = {
                                    if (currentUser.isAdmin) {
                                        onUpdateSettings(null, null, null, null, count, null)
                                    }
                                },
                                label = { Text("$count محاولات") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // Arabic Built-in User Guide
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("دليل الاستخدام المتكامل — نظام KIKI ERP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "شرح سريع لجميع شاشات العمل: الكاشير، الدفع المشترك، التسعير الذكي (Markup / Margin)، تقفيل اليومية، وحماية الأرباح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = { showGuideDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("فتح دليل الاستخدام الكامل بالعربية")
                    }
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = { Text("دليل تشغيل نظام KIKI - مركز كي كي للدواجن") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuideSection("1. الدخول والأمان", "في التشغيل الأول ادخل بحساب المدير (1234) وسيطلب منك النظام فوراً تعيين رمز سري خاص جديد. الكاشير لا يرى أبداً أسعار الشراء أو الأرباح.")
                    GuideSection("2. شاشة الكاشير (POS)", "اختر الأصناف وحدد الكمية أو الوحدة. عند الدفع يمكنك اختيار محفظة واحدة (كاش/بنكك/فوري/أوكاش/ساهل) أو اختيار 'دفع مشترك' لتقسيم الفاتورة على أكثر من محفظة مع إمكانية مشاركة الفاتورة كصورة عبر واتساب.")
                    GuideSection("3. التسعير الذكي والمخزون", "يدعم النظام حساب سعر البيع إما بنسبة الزيادة على التكلفة (Markup) أو بهامش الربح من البيع (Margin) مع تحديث متوسط التكلفة المرجح تلقائياً عند كل فاتورة شراء.")
                    GuideSection("4. المحافظ الـ 5 والتحويلات", "كل قناة دفع تمتلك رصيداً مستقلاً. التحويل بين المحافظ (مثل إيداع الكاش في بنكك) ينقل السيولة دون التأثير على الإيرادات أو الأرباح.")
                    GuideSection("5. المرتجعات وتقفيل اليومية (Z-Report)", "لا تُحذف الفواتير أبداً؛ تُعكس عبر شاشة المرتجعات لتسجيل أثر دقيق. في نهاية اليوم، أدخل الجرد الفعلي لكل محفظة لإقفال اليومية ومطابقة الفارق.")
                    GuideSection("6. النسخ الاحتياطي", "احفظ نسخة مشفرة (.kikibackup) خارج الهاتف بانتظام أو فعّل النسخ التلقائي اليومي لاستعادتها على أي جهاز آخر.")
                }
            },
            confirmButton = {
                Button(onClick = { showGuideDialog = false }) { Text("إغلاق الدليل") }
            }
        )
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}
