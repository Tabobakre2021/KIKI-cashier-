package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.UserEntity
import com.example.data.repository.PaymentSplitLine
import com.example.domain.DailyChartPoint
import com.example.domain.ExpenseCategory
import com.example.domain.FinancialMetrics
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.PricingMode
import com.example.domain.WalletChannel
import com.example.ui.theme.KikiBurgundy
import com.example.ui.theme.KikiBurgundyDeep
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiGold
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft

@Composable
fun KikiBrandHeader(
    currentUser: UserEntity?,
    onOpenDrawer: () -> Unit,
    onSwitchUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = listOf(KikiBurgundyDeep, KikiBurgundy)))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("open_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "القائمة الرئيسية",
                        tint = Color.White
                    )
                }

                Image(
                    painter = painterResource(id = R.drawable.img_kiki_logo),
                    contentDescription = "شعار مركز كي كي للدواجن",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, KikiGold, RoundedCornerShape(12.dp))
                        .padding(2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "KIKI - مركز كي كي للدواجن",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "دائماً فكر بالمذاق الطازج",
                        style = MaterialTheme.typography.labelSmall,
                        color = KikiGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (currentUser != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSwitchUser() }
                        .testTag("switch_user_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUser.canViewProfits) Icons.Filled.Shield else Icons.Filled.AccountBalanceWallet,
                            contentDescription = "حساب المستخدم",
                            tint = KikiGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = currentUser.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "خروج",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletBalanceStrip(
    metrics: FinancialMetrics,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "أرصدة المحافظ الـ 5 المستقلة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WalletChannel.entries.forEach { channel ->
                val summary = metrics.wallets[channel]
                val balance = summary?.netBalance ?: 0L
                val icon = when (channel) {
                    WalletChannel.CASH -> Icons.Filled.Payments
                    WalletChannel.BANKAK -> Icons.Filled.AccountBalance
                    WalletChannel.FAWRY -> Icons.Filled.PhoneAndroid
                    WalletChannel.OCASH -> Icons.Filled.AccountBalanceWallet
                    WalletChannel.SAHIL -> Icons.Filled.AttachMoney
                }
                val accent = when (channel) {
                    WalletChannel.CASH -> KikiProfitGreen
                    WalletChannel.BANKAK -> MaterialTheme.colorScheme.primary
                    WalletChannel.FAWRY -> MaterialTheme.colorScheme.secondary
                    WalletChannel.OCASH -> MaterialTheme.colorScheme.tertiary
                    WalletChannel.SAHIL -> Color(0xFF0369A1)
                }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .width(156.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = channel.arabicLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.14f))
                            ) {
                                Icon(icon, contentDescription = channel.arabicLabel, tint = accent, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = PricingEngine.formatCurrency(balance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else KikiExpenseRed,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "وارد: ${PricingEngine.formatAmount(summary?.totalInflows ?: 0L)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KikiProfitGreen
                        )
                        Text(
                            text = "صادر: ${PricingEngine.formatAmount(summary?.totalOutflows ?: 0L)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = KikiExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiPaymentSplitEditor(
    totalDue: Long,
    isSplitMode: Boolean,
    onSplitModeChange: (Boolean) -> Unit,
    singleChannel: WalletChannel,
    onSingleChannelChange: (WalletChannel) -> Unit,
    splitAmounts: Map<WalletChannel, String>,
    onSplitAmountChange: (WalletChannel, String) -> Unit,
    receiptNumber: String,
    onReceiptNumberChange: (String) -> Unit,
    operationNumber: String,
    onOperationNumberChange: (String) -> Unit,
    receiptNote: String,
    onReceiptNoteChange: (String) -> Unit,
    allowPartialCredit: Boolean,
    modifier: Modifier = Modifier
) {
    val totalEntered = if (isSplitMode) {
        WalletChannel.entries.sumOf { PricingEngine.parseLongAmount(splitAmounts[it].orEmpty()) }
    } else {
        totalDue
    }
    val remaining = (totalDue - totalEntered)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "وسيلة الدفع والمحفظة",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WalletChannel.entries.forEach { ch ->
                val selected = !isSplitMode && singleChannel == ch
                FilterChip(
                    selected = selected,
                    onClick = {
                        onSplitModeChange(false)
                        onSingleChannelChange(ch)
                    },
                    label = { Text(ch.arabicLabel, style = MaterialTheme.typography.labelLarge) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.heightIn(min = 42.dp).testTag("pay_chip_${ch.name.lowercase()}")
                )
            }
            FilterChip(
                selected = isSplitMode,
                onClick = { onSplitModeChange(!isSplitMode) },
                label = { Text("دفع مشترك (متعدد)", style = MaterialTheme.typography.labelLarge) },
                leadingIcon = { Icon(Icons.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                modifier = Modifier.heightIn(min = 42.dp).testTag("pay_chip_split")
            )
        }

        AnimatedVisibility(visible = isSplitMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تقسيم المبلغ على المحافظ (أدخل القيمة لكل قناة مستخدمة):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    WalletChannel.entries.forEach { ch ->
                        OutlinedTextField(
                            value = splitAmounts[ch].orEmpty(),
                            onValueChange = { onSplitAmountChange(ch, it) },
                            label = { Text("${ch.arabicLabel} (ج.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("split_input_${ch.name.lowercase()}")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "مجموع المدفوع: ${PricingEngine.formatCurrency(totalEntered)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = KikiProfitGreen
                        )
                        Text(
                            text = when {
                                remaining == 0L -> "مطابق تماماً"
                                remaining > 0L && allowPartialCredit -> "متبقي (آجل): ${PricingEngine.formatAmount(remaining)}"
                                remaining > 0L -> "ناقص: ${PricingEngine.formatAmount(remaining)}"
                                else -> "زيادة: ${PricingEngine.formatAmount(-remaining)}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining == 0L) KikiProfitGreen else KikiExpenseRed
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = receiptNumber,
                onValueChange = onReceiptNumberChange,
                label = { Text("رقم الإشعار") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = operationNumber,
                onValueChange = onOperationNumberChange,
                label = { Text("رقم العملية") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = receiptNote,
            onValueChange = onReceiptNoteChange,
            label = { Text("ملاحظات التحصيل / الدفع") },
            leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun buildSplitLines(
    totalDue: Long,
    isSplitMode: Boolean,
    singleChannel: WalletChannel,
    splitAmounts: Map<WalletChannel, String>,
    receiptNumber: String,
    operationNumber: String
): List<PaymentSplitLine> {
    return if (!isSplitMode) {
        if (totalDue > 0L) {
            listOf(PaymentSplitLine(singleChannel, totalDue, receiptNumber, operationNumber))
        } else emptyList()
    } else {
        WalletChannel.entries.mapNotNull { ch ->
            val amt = PricingEngine.parseLongAmount(splitAmounts[ch].orEmpty())
            if (amt > 0L) PaymentSplitLine(ch, amt, receiptNumber, operationNumber) else null
        }
    }
}

@Composable
fun SmartPricingCalculatorCardV3(
    costPriceText: String,
    percentText: String,
    sellingPriceText: String,
    pricingMode: PricingMode,
    roundingStep: Int,
    lastPurchasePrice: Long,
    averageCost: Long,
    onModeChange: (PricingMode) -> Unit,
    onCostPriceChange: (String) -> Unit,
    onPercentChange: (String) -> Unit,
    onSellingPriceChange: (String) -> Unit,
    canViewProfits: Boolean,
    modifier: Modifier = Modifier
) {
    val cost = PricingEngine.parseLongAmount(costPriceText)
    val sell = PricingEngine.parseLongAmount(sellingPriceText)
    val liveMarkup = PricingEngine.calculateMarkupPercent(cost, sell)
    val liveMargin = PricingEngine.calculateMarginPercent(cost, sell)
    val unitProfit = if (cost > 0L && sell > 0L) sell - cost else 0L

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (canViewProfits) "محرك التسعير الذكي (Markup / Margin)" else "تحديد سعر البيع للجمهور",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (canViewProfits) {
                if (lastPurchasePrice > 0L || averageCost > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "آخر شراء: ${PricingEngine.formatCurrency(lastPurchasePrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "متوسط التكلفة: ${PricingEngine.formatCurrency(averageCost)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Mode Switcher: Markup vs Margin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PricingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == pricingMode,
                            onClick = { onModeChange(mode) },
                            label = {
                                Text(
                                    text = if (mode == PricingMode.MARKUP) "زيادة على التكلفة" else "هامش من البيع",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = onCostPriceChange,
                    label = { Text("سعر الشراء / التكلفة للوحدة (ج.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("smart_cost_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = percentText,
                        onValueChange = onPercentChange,
                        label = {
                            Text(if (pricingMode == PricingMode.MARKUP) "نسبة الزيادة %" else "هامش الربح %")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("smart_margin_input")
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = onSellingPriceChange,
                        label = { Text("سعر البيع (ج.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1.25f).testTag("smart_selling_input")
                    )
                }

                Text(
                    text = "التقريب المفعل: لأقرب ${PricingEngine.formatAmount(roundingStep.toLong())} ج.س",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cost > 0L && sell > 0L) {
                    Surface(
                        color = if (unitProfit >= 0L) KikiProfitGreenSoft else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الربح للوحدة:", style = MaterialTheme.typography.labelLarge, color = KikiProfitGreen)
                                Text(
                                    text = PricingEngine.formatCurrency(unitProfit),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unitProfit >= 0L) KikiProfitGreen else KikiExpenseRed
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نسبة الزيادة (Markup): ${PricingEngine.formatPercent(liveMarkup)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "هامش البيع (Margin): ${PricingEngine.formatPercent(liveMargin)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = sellingPriceText,
                    onValueChange = onSellingPriceChange,
                    label = { Text("سعر البيع للوحدة (ج.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("smart_selling_input")
                )
            }
        }
    }
}

@Composable
fun PackagingUnitDropdown(
    selectedUnit: PackagingUnit,
    onUnitSelected: (PackagingUnit) -> Unit,
    label: String = "الوحدة / العبوة",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("unit_dropdown_button")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(selectedUnit.arabicLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "اختر الوحدة")
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PackagingUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = {
                        Text(unit.arabicLabel, fontWeight = if (unit == selectedUnit) FontWeight.Bold else FontWeight.Normal)
                    },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun KikiChartsCard(
    dailySeries: List<DailyChartPoint>,
    expensesByCategory: Map<ExpenseCategory, Long>,
    canViewProfits: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (canViewProfits) "الرسم البياني: المبيعات والأرباح اليومية" else "الرسم البياني: المبيعات اليومية",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (dailySeries.isEmpty()) {
                Text(
                    text = "ستظهر الرسوم البيانية فور تسجيل فواتير مبيعات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = dailySeries.maxOf { maxOf(it.sales, if (canViewProfits) it.profit else 0L) }.coerceAtLeast(1L)
                val primaryColor = MaterialTheme.colorScheme.primary
                val profitColor = KikiProfitGreen

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val groupWidth = size.width / dailySeries.size.coerceAtLeast(1)
                    val barWidth = if (canViewProfits) (groupWidth * 0.28f) else (groupWidth * 0.45f)
                    dailySeries.forEachIndexed { idx, pt ->
                        val centerX = (idx + 0.5f) * groupWidth
                        val salesH = (pt.sales.toFloat() / maxVal.toFloat()) * (size.height - 16f)
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(
                                x = if (canViewProfits) centerX - barWidth - 3f else centerX - barWidth / 2f,
                                y = size.height - salesH
                            ),
                            size = Size(barWidth, salesH),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        if (canViewProfits && pt.profit > 0L) {
                            val profH = (pt.profit.toFloat() / maxVal.toFloat()) * (size.height - 16f)
                            drawRoundRect(
                                color = profitColor,
                                topLeft = Offset(x = centerX + 3f, y = size.height - profH),
                                size = Size(barWidth, profH),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    dailySeries.forEach { pt ->
                        Text(pt.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (expensesByCategory.isNotEmpty()) {
                val maxExp = expensesByCategory.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                Text(
                    text = "توزيع المصروفات حسب التصنيف",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                expensesByCategory.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat.arabicLabel, style = MaterialTheme.typography.bodySmall)
                            Text(PricingEngine.formatCurrency(amt), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (amt.toFloat() / maxExp.toFloat()).coerceIn(0f, 1f) },
                            color = KikiExpenseRed,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
