package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.UserEntity
import com.example.data.repository.PaymentSplitLine
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.PricingMode
import com.example.domain.WalletChannel
import com.example.ui.components.MultiPaymentSplitEditor
import com.example.ui.components.PackagingUnitDropdown
import com.example.ui.components.SmartPricingCalculatorCardV3
import com.example.ui.components.buildSplitLines
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchasesScreen(
    products: List<ProductEntity>,
    suppliers: List<SupplierEntity>,
    purchases: List<PurchaseEntity>,
    currentUser: UserEntity,
    roundingStep: Int,
    onRecordPurchase: (
        product: ProductEntity,
        supplierId: Long?,
        supplierName: String,
        unit: PackagingUnit,
        quantity: Double,
        conversionMultiplier: Double,
        unitCostPrice: Long,
        discountAmount: Long,
        transportCost: Long,
        newTargetPercent: Double,
        newSellingPrice: Long,
        updateProductPricing: Boolean,
        allocations: List<PaymentSplitLine>,
        receiptNote: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canRecordPurchases && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية تسجيل المشتريات.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    var selectedProduct by remember(products) { mutableStateOf(products.firstOrNull()) }
    var productMenuOpen by remember { mutableStateOf(false) }
    var selectedSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var supplierMenuOpen by remember { mutableStateOf(false) }
    var customSupplierName by remember { mutableStateOf("") }

    var selectedUnit by remember(selectedProduct) { mutableStateOf(selectedProduct?.defaultUnit ?: PackagingUnit.KILO) }
    var quantityText by remember { mutableStateOf("1") }
    var conversionText by remember(selectedProduct) {
        val f = selectedProduct?.unitConversionFactor ?: 1.0
        mutableStateOf(if (f == 1.0) "1" else f.toString())
    }
    var pricingMode by remember(selectedProduct) { mutableStateOf(selectedProduct?.pricingMode ?: PricingMode.MARKUP) }
    var costText by remember(selectedProduct) {
        val c = selectedProduct?.costPrice ?: 0L
        mutableStateOf(if (c > 0L) c.toString() else "")
    }
    var percentText by remember(selectedProduct) {
        val m = selectedProduct?.profitMarginPercent ?: 0.0
        mutableStateOf(if (m > 0.0) m.toString() else "")
    }
    var sellingText by remember(selectedProduct) {
        val s = selectedProduct?.sellingPrice ?: 0L
        mutableStateOf(if (s > 0L) s.toString() else "")
    }
    var discountText by remember { mutableStateOf("") }
    var transportText by remember { mutableStateOf("") }
    var updateSellingToggle by remember { mutableStateOf(true) }

    var isSplitMode by remember { mutableStateOf(false) }
    var singleChannel by remember { mutableStateOf(WalletChannel.CASH) }
    val splitAmounts = remember { mutableStateMapOf<WalletChannel, String>() }
    var receiptNumber by remember { mutableStateOf("") }
    var operationNumber by remember { mutableStateOf("") }
    var receiptNote by remember { mutableStateOf("") }

    val qty = PricingEngine.parseDoubleQty(quantityText)
    val unitCost = PricingEngine.parseLongAmount(costText)
    val discount = PricingEngine.parseLongAmount(discountText)
    val transport = PricingEngine.parseLongAmount(transportText)
    val invoiceTotal = (PricingEngine.calculateLineTotal(unitCost, qty) - discount + transport).coerceAtLeast(0L)
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
                    Text("فاتورة توريد ومشتريات", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { productMenuOpen = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("purchase_product_picker")) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedProduct?.name ?: "اختر الصنف المورّد", fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = productMenuOpen, onDismissRequest = { productMenuOpen = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                            products.forEach { prod ->
                                DropdownMenuItem(text = { Text("${prod.name} (${prod.category.shortTitle})") }, onClick = {
                                    selectedProduct = prod
                                    selectedUnit = prod.defaultUnit
                                    productMenuOpen = false
                                })
                            }
                        }
                    }

                    if (suppliers.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { supplierMenuOpen = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(selectedSupplier?.name ?: "اختر مورداً مسجلاً (أو أدخل يدوياً أدناه)")
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = supplierMenuOpen, onDismissRequest = { supplierMenuOpen = false }) {
                                DropdownMenuItem(text = { Text("مورد نقدي / جديد") }, onClick = {
                                    selectedSupplier = null
                                    supplierMenuOpen = false
                                })
                                suppliers.forEach { s ->
                                    DropdownMenuItem(text = { Text("${s.name} (متبقي: ${PricingEngine.formatAmount(s.balanceDue)})") }, onClick = {
                                        selectedSupplier = s
                                        customSupplierName = s.name
                                        supplierMenuOpen = false
                                    })
                                }
                            }
                        }
                    }

                    if (selectedSupplier == null) {
                        OutlinedTextField(
                            value = customSupplierName,
                            onValueChange = { customSupplierName = it },
                            label = { Text("اسم المورد (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("الكمية") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            PackagingUnitDropdown(selectedUnit = selectedUnit, onUnitSelected = { selectedUnit = it })
                        }
                    }

                    if (selectedProduct != null && selectedUnit != selectedProduct?.defaultUnit) {
                        OutlinedTextField(
                            value = conversionText,
                            onValueChange = { conversionText = it },
                            label = { Text("معامل التحويل إلى (${selectedProduct!!.defaultUnit.arabicLabel})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
                            label = { Text("الخصم (ج.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = transportText,
                            onValueChange = { transportText = it },
                            label = { Text("تكاليف النقل (ج.س)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SmartPricingCalculatorCardV3(
                        costPriceText = costText,
                        percentText = percentText,
                        sellingPriceText = sellingText,
                        pricingMode = pricingMode,
                        roundingStep = roundingStep,
                        lastPurchasePrice = selectedProduct?.lastPurchasePrice ?: 0L,
                        averageCost = selectedProduct?.averageCost ?: 0L,
                        onModeChange = { newMode ->
                            pricingMode = newMode
                            val c = PricingEngine.parseLongAmount(costText)
                            val p = PricingEngine.parseDoubleQty(percentText)
                            if (c > 0L && percentText.isNotBlank()) {
                                sellingText = PricingEngine.calculateSellingPrice(c, p, newMode, roundingStep).toString()
                            }
                        },
                        onCostPriceChange = { newCost ->
                            costText = newCost
                            val c = PricingEngine.parseLongAmount(newCost)
                            val p = PricingEngine.parseDoubleQty(percentText)
                            val s = PricingEngine.parseLongAmount(sellingText)
                            if (c > 0L && percentText.isNotBlank()) {
                                sellingText = PricingEngine.calculateSellingPrice(c, p, pricingMode, roundingStep).toString()
                            } else if (c > 0L && s > 0L) {
                                val pct = if (pricingMode == PricingMode.MARKUP) PricingEngine.calculateMarkupPercent(c, s)
                                else PricingEngine.calculateMarginPercent(c, s)
                                percentText = pct.toString()
                            }
                        },
                        onPercentChange = { newPct ->
                            percentText = newPct
                            val c = PricingEngine.parseLongAmount(costText)
                            val p = PricingEngine.parseDoubleQty(newPct)
                            if (c > 0L) sellingText = PricingEngine.calculateSellingPrice(c, p, pricingMode, roundingStep).toString()
                        },
                        onSellingPriceChange = { newSell ->
                            sellingText = newSell
                            val c = PricingEngine.parseLongAmount(costText)
                            val s = PricingEngine.parseLongAmount(newSell)
                            if (c > 0L && s >= 0L) {
                                val pct = if (pricingMode == PricingMode.MARKUP) PricingEngine.calculateMarkupPercent(c, s)
                                else PricingEngine.calculateMarginPercent(c, s)
                                percentText = pct.toString()
                            }
                        },
                        canViewProfits = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = updateSellingToggle, onCheckedChange = { updateSellingToggle = it })
                        Text("تحديث سعر البيع في الكاشير بالقيمة الجديدة", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (invoiceTotal > 0L) {
                        Text(
                            text = "إجمالي فاتورة المشتريات: ${PricingEngine.formatCurrency(invoiceTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    MultiPaymentSplitEditor(
                        totalDue = invoiceTotal,
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
                        allowPartialCredit = selectedSupplier != null
                    )

                    Button(
                        onClick = {
                            val prod = selectedProduct ?: return@Button
                            if (qty <= 0.0 || unitCost <= 0L) return@Button
                            val mult = if (selectedUnit == prod.defaultUnit) 1.0 else PricingEngine.parseDoubleQty(conversionText).coerceAtLeast(1.0)
                            val allocs = buildSplitLines(invoiceTotal, isSplitMode, singleChannel, splitAmounts, receiptNumber, operationNumber)
                            onRecordPurchase(
                                prod,
                                selectedSupplier?.id,
                                customSupplierName,
                                selectedUnit,
                                qty,
                                mult,
                                unitCost,
                                discount,
                                transport,
                                PricingEngine.parseDoubleQty(percentText),
                                PricingEngine.parseLongAmount(sellingText),
                                updateSellingToggle,
                                allocs,
                                receiptNote
                            )
                            quantityText = "1"
                            discountText = ""
                            transportText = ""
                            receiptNote = ""
                            receiptNumber = ""
                            operationNumber = ""
                            splitAmounts.clear()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_purchase_button")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حفظ فاتورة التوريد وتحديث المخزون", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (purchases.isNotEmpty() && currentUser.canViewProfits) {
            item { Text("سجل فواتير المشتريات (${purchases.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(purchases.take(25), key = { it.id }) { p ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.invoiceNumber} • ${p.productName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${PricingEngine.formatQty(p.quantity)} ${p.unit.arabicLabel} | ${p.supplierName.ifBlank { "مورد نقدي" }} | ${timeFormatter.format(Date(p.timestamp))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (p.remainingDebt > 0L) {
                                Text("متبقي آجل للمورد: ${PricingEngine.formatCurrency(p.remainingDebt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(PricingEngine.formatCurrency(p.totalCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
