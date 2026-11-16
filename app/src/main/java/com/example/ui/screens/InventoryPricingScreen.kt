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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.entity.ProductEntity
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.UserEntity
import com.example.domain.PackagingUnit
import com.example.domain.PricingEngine
import com.example.domain.PricingMode
import com.example.domain.ProductCategory
import com.example.ui.components.PackagingUnitDropdown
import com.example.ui.components.SmartPricingCalculatorCardV3
import com.example.ui.theme.KikiExpenseRed
import com.example.ui.theme.KikiGold
import com.example.ui.theme.KikiGoldSoft
import com.example.ui.theme.KikiProfitGreen
import com.example.ui.theme.KikiProfitGreenSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InventoryPricingScreen(
    products: List<ProductEntity>,
    stockMovements: List<StockMovementEntity>,
    currentUser: UserEntity,
    roundingStep: Int,
    onSaveProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!currentUser.canViewInventory && !currentUser.isAdmin) {
        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize().padding(24.dp)) {
            Text("لا يملك هذا الحساب صلاحية الاطلاع على المخزون.", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var lowStockOnly by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var historyProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val canEdit = currentUser.canEditPrices || currentUser.canEditProducts || currentUser.isAdmin
    val canAdd = currentUser.canAddProducts || currentUser.isAdmin
    val canViewProfits = currentUser.canViewProfits

    val filteredProducts = remember(products, searchQuery, selectedCategory, lowStockOnly) {
        products.filter { p ->
            val matchesCat = selectedCategory == null || p.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() || p.name.contains(searchQuery.trim(), ignoreCase = true)
            val matchesLow = !lowStockOnly || p.stockQuantity <= p.lowStockThreshold
            matchesCat && matchesSearch && matchesLow
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث في الأصناف والمخزون...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "مسح")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { lowStockOnly = !lowStockOnly },
                        label = { Text("نواقص المخزون") },
                        leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KikiGold,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("الكل (${products.size})") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                    )
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.shortTitle) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val isLow = product.stockQuantity <= product.lowStockThreshold
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isLow) 1.5.dp else 1.dp,
                                color = if (isLow) KikiGold else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = canEdit) {
                                isCreatingNew = false
                                editingProduct = product
                            }
                            .testTag("inventory_product_${product.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(product.defaultUnit.arabicLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    if (isLow) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = KikiGoldSoft) {
                                            Text("منخفض", style = MaterialTheme.typography.labelSmall, color = Color(0xFF78350F), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Text(
                                    text = "${product.category.shortTitle} • المتاح: ${PricingEngine.formatQty(product.stockQuantity)} (حد التنبيه: ${PricingEngine.formatQty(product.lowStockThreshold)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (canViewProfits && product.averageCost > 0L) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("متوسط التكلفة: ${PricingEngine.formatAmount(product.averageCost)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Surface(color = KikiProfitGreenSoft, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "زيادة ${PricingEngine.formatPercent(PricingEngine.calculateMarkupPercent(product.averageCost, product.sellingPrice))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = KikiProfitGreen,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (product.sellingPrice > 0L) PricingEngine.formatCurrency(product.sellingPrice) else "غير مسعّر",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.sellingPrice > 0L) MaterialTheme.colorScheme.primary else KikiExpenseRed
                                )
                                Row {
                                    IconButton(onClick = { historyProduct = product }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Filled.History, contentDescription = "حركة الصنف", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    if (canEdit) {
                                        IconButton(
                                            onClick = {
                                                isCreatingNew = false
                                                editingProduct = product
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = KikiGold, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canAdd) {
            FloatingActionButton(
                onClick = {
                    isCreatingNew = true
                    editingProduct = ProductEntity(
                        name = "",
                        category = selectedCategory ?: ProductCategory.POULTRY,
                        defaultUnit = PackagingUnit.KILO,
                        sortOrder = products.size + 1
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp).testTag("add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "إضافة صنف")
            }
        }
    }

    editingProduct?.let { target ->
        ProductSmartPricingSheetV3(
            initialProduct = target,
            isNew = isCreatingNew,
            roundingStep = roundingStep,
            canViewProfits = canViewProfits,
            canDelete = currentUser.isAdmin && !isCreatingNew,
            onDismiss = { editingProduct = null },
            onSave = { updated ->
                onSaveProduct(updated)
                editingProduct = null
            },
            onDelete = {
                onDeleteProduct(target.id)
                editingProduct = null
            }
        )
    }

    historyProduct?.let { prod ->
        ProductMovementHistorySheet(
            product = prod,
            movements = stockMovements.filter { it.productId == prod.id },
            canViewCost = canViewProfits,
            onDismiss = { historyProduct = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSmartPricingSheetV3(
    initialProduct: ProductEntity,
    isNew: Boolean,
    roundingStep: Int,
    canViewProfits: Boolean,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct.name) }
    var category by remember { mutableStateOf(initialProduct.category) }
    var unit by remember { mutableStateOf(initialProduct.defaultUnit) }
    var stockText by remember { mutableStateOf(if (initialProduct.stockQuantity == 0.0) "0" else PricingEngine.formatQty(initialProduct.stockQuantity).replace(",", "")) }
    var thresholdText by remember { mutableStateOf(PricingEngine.formatQty(initialProduct.lowStockThreshold).replace(",", "")) }
    var conversionText by remember { mutableStateOf(PricingEngine.formatQty(initialProduct.unitConversionFactor).replace(",", "")) }
    var pricingMode by remember { mutableStateOf(initialProduct.pricingMode) }

    var costText by remember { mutableStateOf(if (initialProduct.costPrice == 0L) "" else initialProduct.costPrice.toString()) }
    var percentText by remember { mutableStateOf(if (initialProduct.profitMarginPercent == 0.0) "" else initialProduct.profitMarginPercent.toString()) }
    var sellingText by remember { mutableStateOf(if (initialProduct.sellingPrice == 0L) "" else initialProduct.sellingPrice.toString()) }
    var catMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNew) "إضافة صنف جديد" else "إدارة وتسعير: ${initialProduct.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (canDelete) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم الصنف") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { catMenuExpanded = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text("القسم", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(category.shortTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                        ProductCategory.entries.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.arabicTitle) }, onClick = { category = cat; catMenuExpanded = false })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    PackagingUnitDropdown(selectedUnit = unit, onUnitSelected = { unit = it })
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("الكمية المتاحة") },
                    leadingIcon = { Icon(Icons.Filled.Inventory2, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = { Text("حد التنبيه") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(0.8f)
                )
            }

            OutlinedTextField(
                value = conversionText,
                onValueChange = { conversionText = it },
                label = { Text("معامل التحويل الافتراضي (مثلاً: 12 قطعة في الكرتونة)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SmartPricingCalculatorCardV3(
                costPriceText = costText,
                percentText = percentText,
                sellingPriceText = sellingText,
                pricingMode = pricingMode,
                roundingStep = roundingStep,
                lastPurchasePrice = initialProduct.lastPurchasePrice,
                averageCost = initialProduct.averageCost,
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
                        val calcPct = if (pricingMode == PricingMode.MARKUP) PricingEngine.calculateMarkupPercent(c, s)
                        else PricingEngine.calculateMarginPercent(c, s)
                        percentText = calcPct.toString()
                    }
                },
                onPercentChange = { newPct ->
                    percentText = newPct
                    val c = PricingEngine.parseLongAmount(costText)
                    val p = PricingEngine.parseDoubleQty(newPct)
                    if (c > 0L) {
                        sellingText = PricingEngine.calculateSellingPrice(c, p, pricingMode, roundingStep).toString()
                    }
                },
                onSellingPriceChange = { newSell ->
                    sellingText = newSell
                    val c = PricingEngine.parseLongAmount(costText)
                    val s = PricingEngine.parseLongAmount(newSell)
                    if (c > 0L && s >= 0L) {
                        val calcPct = if (pricingMode == PricingMode.MARKUP) PricingEngine.calculateMarkupPercent(c, s)
                        else PricingEngine.calculateMarginPercent(c, s)
                        percentText = calcPct.toString()
                    }
                },
                canViewProfits = canViewProfits
            )

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val c = if (canViewProfits) PricingEngine.parseLongAmount(costText) else initialProduct.costPrice
                    val s = PricingEngine.parseLongAmount(sellingText)
                    val avg = if (canViewProfits && initialProduct.averageCost == 0L) c else initialProduct.averageCost
                    val pct = if (canViewProfits && c > 0L) {
                        if (pricingMode == PricingMode.MARKUP) PricingEngine.calculateMarkupPercent(c, s)
                        else PricingEngine.calculateMarginPercent(c, s)
                    } else initialProduct.profitMarginPercent

                    onSave(
                        initialProduct.copy(
                            name = name.trim(),
                            category = category,
                            defaultUnit = unit,
                            stockQuantity = PricingEngine.parseDoubleQty(stockText),
                            lowStockThreshold = PricingEngine.parseDoubleQty(thresholdText).coerceAtLeast(0.0),
                            unitConversionFactor = PricingEngine.parseDoubleQty(conversionText).coerceAtLeast(1.0),
                            costPrice = c,
                            averageCost = avg,
                            pricingMode = pricingMode,
                            profitMarginPercent = pct,
                            sellingPrice = s
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_product_pricing_button")
            ) {
                Text("حفظ الصنف والتسعير", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductMovementHistorySheet(
    product: ProductEntity,
    movements: List<StockMovementEntity>,
    canViewCost: Boolean,
    onDismiss: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("سجل حركة الصنف: ${product.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("الرصيد الحالي: ${PricingEngine.formatQty(product.stockQuantity)} ${product.defaultUnit.arabicLabel}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()

            if (movements.isEmpty()) {
                Text("لا توجد حركات مسجلة لهذا الصنف حتى الآن.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(movements, key = { it.id }) { m ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${m.movementType.arabicLabel} (${m.referenceCode})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${timeFormatter.format(Date(m.timestamp))} | ${m.userName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (m.reason.isNotBlank()) {
                                        Text(m.reason, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = (if (m.quantityDelta >= 0) "+" else "") + PricingEngine.formatQty(m.quantityDelta),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (m.quantityDelta >= 0) KikiProfitGreen else KikiExpenseRed
                                    )
                                    Text("بعد: ${PricingEngine.formatQty(m.quantityAfter)}", style = MaterialTheme.typography.labelSmall)
                                    if (canViewCost && m.unitCostAtMovement > 0L) {
                                        Text("تكلفة: ${PricingEngine.formatAmount(m.unitCostAtMovement)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
