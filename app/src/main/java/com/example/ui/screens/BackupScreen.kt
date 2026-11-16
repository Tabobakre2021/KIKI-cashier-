package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.data.BackupRecordInfo
import com.example.data.entity.UserEntity
import com.example.domain.BackupFrequency
import com.example.ui.theme.KikiExpenseRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    currentUser: UserEntity,
    backupFrequency: BackupFrequency,
    localBackups: List<BackupRecordInfo>,
    onChangeBackupFrequency: (BackupFrequency) -> Unit,
    onCreateManualBackup: () -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onRestoreUri: (Uri) -> Unit,
    onRestoreLocalFile: (String) -> Unit,
    onShareLocalBackup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canManageBackup && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية إدارة النسخ الاحتياطي والاستعادة.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    var pendingRestoreLocal by remember { mutableStateOf<BackupRecordInfo?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("النسخ الاحتياطي المشفر والاستعادة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "تشفير AES-256-GCM + فحص سلامة SHA-256 لنقل البيانات بأمان بين الأجهزة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إجراءات النسخ الاحتياطي الفوري", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = onCreateManualBackup,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("create_manual_backup_btn")
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("إنشاء نسخة احتياطية محلية الآن", fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("KIKI_Backup_${System.currentTimeMillis()}.kikibackup") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("حفظ للهاتف / خارجي")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("استيراد من ملف")
                        }
                    }
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
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("جدولة النسخ التلقائي في الخلفية (WorkManager)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BackupFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = freq == backupFrequency,
                                onClick = { onChangeBackupFrequency(freq) },
                                label = { Text(freq.arabicLabel) },
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

        item {
            Text("النسخ الاحتياطية المحفوظة (${localBackups.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (localBackups.isEmpty()) {
            item {
                Text(
                    "لا توجد نسخ محلية بعد. اضغط على (إنشاء نسخة احتياطية محلية الآن).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(localBackups, key = { it.absolutePath }) { backup ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(backup.fileName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${timeFormatter.format(Date(backup.createdAt))} • ${(backup.sizeBytes / 1024L).coerceAtLeast(1L)} كيلوبايت",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onShareLocalBackup(backup.absolutePath) }) {
                            Icon(Icons.Filled.Share, contentDescription = "مشاركة النسخة")
                        }
                        IconButton(onClick = { pendingRestoreLocal = backup }) {
                            Icon(Icons.Filled.Restore, contentDescription = "استعادة النسخة", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    pendingRestoreLocal?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestoreLocal = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية") },
            text = {
                Text(
                    "سيتم التحقق من التوقيع الرقمي (SHA-256) للملف (${backup.fileName}) ثم استبدال كافة البيانات الحالية ببيانات هذه النسخة. هل أنت متأكد؟",
                    color = KikiExpenseRed
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRestoreLocalFile(backup.absolutePath)
                    pendingRestoreLocal = null
                }) {
                    Text("استعادة الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreLocal = null }) { Text("إلغاء") }
            }
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("تأكيد الاستيراد من ملف خارجي") },
            text = {
                Text(
                    "سيتم فك تشفير الملف والتحقق من سلامته عبر SHA-256 ثم استبدال جميع بيانات النظام الحالية بالنسخة المستوردة.",
                    color = KikiExpenseRed
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRestoreUri(uri)
                    pendingRestoreUri = null
                }) {
                    Text("تأكيد الاستعادة")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("إلغاء") }
            }
        )
    }
}
