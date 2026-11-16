package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.ProductEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleItemEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.UserEntity
import com.example.domain.PricingEngine
import com.example.domain.StockMovementType
import com.example.domain.WalletChannel
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiExpenseRedSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReturnTabMode(val arabicLabel: String) {
    CUSTOMER_RETURN("مرتجع عميل"),
    WASTE_ADJUSTMENT("تالف / تسوية / مرتجع مورد")
}

@Composable
fun ReturnsAdjustmentsScreen(
    sales: List<SaleWithItems>,
    products: List<ProductEntity>,
    returns: List<ReturnEntity>,
    stockMovements: List<StockMovementEntity>,
    currentUser: UserEntity,
    onRecordCustomerReturn: (
        sale: SaleWithItems,
        item: SaleItemEntity,
        returnQty: Double,
        restockEligible: Boolean,
        refundChannel: WalletChannel,
        reason: String
    ) -> Unit,
    onRecordStockAdjustmentOrWaste: (
        product: ProductEntity,
        movementType: StockMovementType,
        quantity: Double,
        reason: String,
        refundWallet: WalletChannel
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAccess = currentUser.canReverseOrDelete || currentUser.canEditProducts || currentUser.isAdmin
    if (!canAccess) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text(
                "لا يملك هذا الحساب صلاحية المرتجعات وتسوية المخزون.",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val timeFormatter = remember { SimpleDateFormat("MM-dd HH:mm", Locale.US) }
    var tabMode by remember { mutableStateOf(ReturnTabMode.CUSTOMER_RETURN) }

    // Customer return state
    val activeSales = remember(sales) { sales.filter { !it.sale.isVoided && it.items.isNotEmpty() } }
    var selectedSale by remember(activeSales) { mutableStateOf(activeSales.firstOrNull()) }
    var saleDropdownOpen by remember { mutableStateOf(false) }

    val saleItems = selectedSale?.items.orEmpty()
    var selectedSaleItem by remember(selectedSale) { mutableStateOf(saleItems.firstOrNull()) }
    var itemDropdownOpen by remember { mutableStateOf(false) }

    var returnQtyText by remember { mutableStateOf("1") }
    var restockEligible by remember { mutableStateOf(true) }
    var refundChannel by remember { mutableStateOf(WalletChannel.CASH) }
    var returnReason by remember { mutableStateOf("") }

    // Waste / Adjustment state
    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var prodDropdownOpen by remember { mutableStateOf(false) }
    val adjTypes = remember {
        listOf(
            StockMovementType.DAMAGED,
            StockMovementType.EXPIRED,
            StockMovementType.SUPPLIER_RETURN,
            StockMovementType.ADJUSTMENT_IN,
            StockMovementType.ADJUSTMENT_OUT
        )
    }
    var selectedAdjType by remember { mutableStateOf(StockMovementType.DAMAGED) }
    var adjQtyText by remember { mutableStateOf("1") }
    var adjWallet by remember { mutableStateOf(WalletChannel.CASH) }
    var adjReason by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AssignmentReturn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("المرتجعات والتالف وتسويات المخزون", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "عكس مالي ومخزني موثق دون حذف الفواتير الأصلية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReturnTabMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == tabMode,
                        onClick = { tabMode = mode },
                        label = { Text(mode.arabicLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (tabMode == ReturnTabMode.CUSTOMER_RETURN) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("تسجيل مرتجع من فاتورة بيع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        if (activeSales.isEmpty()) {
                            Text(
                                "لا توجد فواتير مبيعات في الفترة المحددة لعمل مرتجع منها.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box {
                                OutlinedButton(
                                    onClick = { saleDropdownOpen = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            selectedSale?.let {
                                                "${it.sale.invoiceNumber} — ${PricingEngine.formatCurrency(it.sale.totalAmount)}"
                                            } ?: "اختر الفاتورة"
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(expanded = saleDropdownOpen, onDismissRequest = { saleDropdownOpen = false }) {
                                    activeSales.take(25).forEach { sw ->
                                        DropdownMenuItem(
                                            text = { Text("${sw.sale.invoiceNumber} (${PricingEngine.formatCurrency(sw.sale.totalAmount)})") },
                                            onClick = {
                                                selectedSale = sw
                                                saleDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box {
                                OutlinedButton(
                                    onClick = { itemDropdownOpen = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            selectedSaleItem?.let {
                                                "${it.productName} (${PricingEngine.formatQty(it.quantity)} ${it.unit.arabicLabel})"
                                            } ?: "اختر الصنف"
                                        )
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(expanded = itemDropdownOpen, onDismissRequest = { itemDropdownOpen = false }) {
                                    saleItems.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text("${item.productName} — المباع: ${PricingEngine.formatQty(item.quantity)}") },
                                            onClick = {
                                                selectedSaleItem = item
                                                itemDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = returnQtyText,
                                onValueChange = { returnQtyText = it },
                                label = { Text("الكمية المرتجعة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("صالح لإعادة الدخول للمخزون؟", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = restockEligible, onCheckedChange = { restockEligible = it })
                            }

                            Text("محفظة استرداد المبلغ للعميل:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WalletChannel.entries.forEach { ch ->
                                    FilterChip(
                                        selected = ch == refundChannel,
                                        onClick = { refundChannel = ch },
                                        label = { Text(ch.arabicLabel) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = returnReason,
                                onValueChange = { returnReason = it },
                                label = { Text("سبب المرتجع (إلزامي)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val qty = PricingEngine.parseDoubleQty(returnQtyText)
                            val item = selectedSaleItem
                            val refundPreview = if (item != null && qty > 0.0) {
                                PricingEngine.calculateLineTotal(item.sellingPriceAtSale, qty.coerceAtMost(item.quantity))
                            } else 0L

                            Text(
                                "المبلغ المسترد المتوقع: ${PricingEngine.formatCurrency(refundPreview)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KikiExpenseRed
                            )

                            Button(
                                onClick = {
                                    val sale = selectedSale ?: return@Button
                                    val targetItem = selectedSaleItem ?: return@Button
                                    val safeQty = qty.coerceAtMost(targetItem.quantity)
                                    if (safeQty <= 0.0 || returnReason.isBlank()) return@Button
                                    onRecordCustomerReturn(sale, targetItem, safeQty, restockEligible, refundChannel, returnReason.trim())
                                    returnQtyText = "1"
                                    returnReason = ""
                                },
                                enabled = selectedSale != null && selectedSaleItem != null && qty > 0.0 && returnReason.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_customer_return_button")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("تأكيد المرتجع وعكس الحسابات", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("تسوية مخزون / تالف / مرتجع لمورد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Box {
                            OutlinedButton(
                                onClick = { prodDropdownOpen = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedProduct?.let {
                                            "${it.name} (المتاح: ${PricingEngine.formatQty(it.stockQuantity)})"
                                        } ?: "اختر الصنف"
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = prodDropdownOpen, onDismissRequest = { prodDropdownOpen = false }) {
                                products.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} — ${PricingEngine.formatQty(p.stockQuantity)} ${p.defaultUnit.arabicLabel}") },
                                        onClick = {
                                            selectedProduct = p
                                            prodDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            adjTypes.forEach { t ->
                                FilterChip(
                                    selected = t == selectedAdjType,
                                    onClick = { selectedAdjType = t },
                                    label = { Text(t.arabicLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = adjQtyText,
                            onValueChange = { adjQtyText = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (selectedAdjType == StockMovementType.SUPPLIER_RETURN) {
                            Text("استرداد قيمة المورد في محفظة:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WalletChannel.entries.forEach { ch ->
                                    FilterChip(
                                        selected = ch == adjWallet,
                                        onClick = { adjWallet = ch },
                                        label = { Text(ch.arabicLabel) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = adjReason,
                            onValueChange = { adjReason = it },
                            label = { Text("السبب / الملاحظة (إلزامي)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        val q = PricingEngine.parseDoubleQty(adjQtyText)
                        Button(
                            onClick = {
                                val p = selectedProduct ?: return@Button
                                if (q <= 0.0 || adjReason.isBlank()) return@Button
                                onRecordStockAdjustmentOrWaste(p, selectedAdjType, q, adjReason.trim(), adjWallet)
                                adjQtyText = "1"
                                adjReason = ""
                            },
                            enabled = selectedProduct != null && q > 0.0 && adjReason.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_adjustment_button")
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("حفظ حركة المخزون", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("سجل المرتجعات والتالف (${returns.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(returns.take(30), key = { it.id }) { r ->
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${r.returnCode} • ${r.productName}", fontWeight = FontWeight.Bold)
                        Text(
                            "${r.returnType} | ${PricingEngine.formatQty(r.quantity)} ${r.unit.arabicLabel} • ${r.userName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (r.reason.isNotBlank()) {
                            Text("السبب: ${r.reason}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = if (r.restockEligible) KikiProfitGreenSoft else KikiExpenseRedSoft,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = PricingEngine.formatCurrency(r.refundAmount),
                                color = if (r.refundAmount > 0L) KikiExpenseRed else KikiProfitGreen,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(timeFormatter.format(Date(r.timestamp)), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            Text("آخر حركات المخزون (${stockMovements.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(stockMovements.take(25), key = { it.id }) { m ->
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${m.productName} • ${m.movementType.arabicLabel}", fontWeight = FontWeight.SemiBold)
                        Text("${m.referenceCode} | ${m.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${if (m.quantityDelta >= 0) "+" else ""}${PricingEngine.formatQty(m.quantityDelta)} -> ${PricingEngine.formatQty(m.quantityAfter)}",
                        fontWeight = FontWeight.Bold,
                        color = if (m.quantityDelta >= 0) KikiProfitGreen else KikiExpenseRed
                    )
                }
            }
        }
    }
}
