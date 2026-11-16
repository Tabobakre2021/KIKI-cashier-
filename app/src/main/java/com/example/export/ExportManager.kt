package com.example.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ExpenseEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseEntity
import com.example.data.entity.ReturnEntity
import com.example.data.entity.SaleWithItems
import com.example.data.entity.StockMovementEntity
import com.example.data.entity.SupplierEntity
import com.example.data.entity.ZReportEntity
import com.example.domain.FinancialMetrics
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportCategory(val arabicTitle: String, val requiresProfitAccess: Boolean = false) {
    DAILY_SALES("1. تقرير المبيعات اليومية"),
    MONTHLY_SALES("2. تقرير المبيعات الشهرية"),
    PROFIT_REPORT("3. تقرير الأرباح والتكاليف (للإدارة)", requiresProfitAccess = true),
    EXPENSES_REPORT("4. تقرير المصروفات التشغيلية"),
    PURCHASES_REPORT("5. تقرير المشتريات والتوريد", requiresProfitAccess = true),
    INVENTORY_REPORT("6. تقرير أرصدة المخزون"),
    STOCK_MOVEMENTS("7. تقرير حركة الأصناف"),
    PAYMENT_CHANNELS("8. تقرير وسائل الدفع"),
    WALLET_BALANCES("9. تقرير أرصدة المحافظ الـ 5"),
    SUPPLIERS_REPORT("10. تقرير الموردين والمديونيات"),
    CUSTOMERS_REPORT("11. تقرير العملاء والذمم"),
    ZREPORT_HISTORY("12. تقرير تقفيل اليومية (Z-Report)"),
    RETURNS_DAMAGED("13. تقرير المرتجعات والتالف")
}

object ExportManager {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)

    fun buildWhatsAppSummary(
        periodLabel: String,
        metrics: FinancialMetrics,
        canViewProfits: Boolean,
        latestZReport: ZReportEntity? = null
    ): String {
        val sb = StringBuilder()
        sb.appendLine("🐓 *KIKI - مركز كي كي للدواجن*")
        sb.appendLine("_دائماً فكر بالمذاق الطازج_")
        sb.appendLine("📅 التقرير: $periodLabel (${dateTimeFormat.format(Date())})")
        sb.appendLine("────────────────────")
        sb.appendLine("💰 *ملخص المبيعات (${metrics.salesCount} فاتورة):*")
        sb.appendLine("• صافي المبيعات: ${PricingEngine.formatCurrency(metrics.netSales)}")
        sb.appendLine("• تحصيل الكاش: ${PricingEngine.formatCurrency(metrics.cashCollected)}")
        sb.appendLine("• تحصيل التطبيقات: ${PricingEngine.formatCurrency(metrics.bankCollected)}")
        sb.appendLine("• المصروفات: ${PricingEngine.formatCurrency(metrics.totalExpenses)}")
        sb.appendLine("• المرتجعات: ${PricingEngine.formatCurrency(metrics.totalCustomerReturns)}")

        if (canViewProfits) {
            sb.appendLine("────────────────────")
            sb.appendLine("📈 *مؤشرات الربحية (حصرية للإدارة):*")
            sb.appendLine("• تكلفة البضاعة (COGS): ${PricingEngine.formatCurrency(metrics.totalCogs)}")
            sb.appendLine("• مجمل الربح: ${PricingEngine.formatCurrency(metrics.grossProfit)}")
            sb.appendLine("• صافي الربح التشغيلي: ${PricingEngine.formatCurrency(metrics.netOperatingProfit)}")
            sb.appendLine("• إجمالي المشتريات: ${PricingEngine.formatCurrency(metrics.totalPurchases)}")
        }

        sb.appendLine("────────────────────")
        sb.appendLine("💳 *أرصدة المحافظ الـ 5:*")
        WalletChannel.entries.forEach { wallet ->
            val summary = metrics.wallets[wallet]
            sb.appendLine("• ${wallet.arabicLabel}: ${PricingEngine.formatCurrency(summary?.netBalance ?: 0L)}")
        }

        if (latestZReport != null) {
            sb.appendLine("────────────────────")
            sb.appendLine("🧾 *آخر تقفيل يومية:* ${latestZReport.dateLabel} | الفارق: ${PricingEngine.formatCurrency(latestZReport.totalVariance)}")
        }
        return sb.toString()
    }

    fun copyTextToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareInvoiceReceiptImage(context: Context, saleWithItems: SaleWithItems) {
        val file = PdfReceiptExporter.generateInvoiceReceiptImage(context, saleWithItems)
        shareFile(context, file, "image/png", "مشاركة الفاتورة (${saleWithItems.sale.invoiceNumber})")
    }

    fun printSaleInvoice(context: Context, saleWithItems: SaleWithItems) {
        val sale = saleWithItems.sale
        val logoBase64 = encodeLogoBase64(context)
        val logoHtml = if (logoBase64.isNotEmpty()) {
            """<img src="data:image/jpeg;base64,$logoBase64" style="width:56px;height:56px;border-radius:10px;border:2px solid #F59E0B;" />"""
        } else ""

        val rowsHtml = saleWithItems.items.joinToString("") { item ->
            """
            <tr>
              <td>${item.productName}</td>
              <td>${PricingEngine.formatQty(item.quantity)} ${item.unit.arabicLabel}</td>
              <td>${PricingEngine.formatAmount(item.sellingPriceAtSale)}</td>
              <td><b>${PricingEngine.formatCurrency(item.lineTotal)}</b></td>
            </tr>
            """.trimIndent()
        }
        val discountRow = if (sale.discountAmount > 0L) {
            """<div>الخصم: <b>- ${PricingEngine.formatCurrency(sale.discountAmount)}</b></div>"""
        } else ""
        val debtRow = if (sale.remainingDebt > 0L) {
            """<div style="color:#B91C1C;">المتبقي (آجل): <b>${PricingEngine.formatCurrency(sale.remainingDebt)}</b></div>"""
        } else ""
        val customerInfo = if (sale.customerName.isNotBlank()) " | العميل: ${sale.customerName}" else ""

        val html = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head><meta charset="utf-8" />
        <style>
          body { font-family: sans-serif; color: #252525; padding: 14px; direction: rtl; }
          .header { display: flex; align-items: center; gap: 12px; border-bottom: 2px solid #741F3D; padding-bottom: 8px; margin-bottom: 10px; }
          h2 { margin: 0; color: #741F3D; font-size: 18px; }
          .sub { color: #D97706; font-size: 12px; font-weight: bold; }
          .meta { font-size: 12px; margin-bottom: 10px; line-height: 1.6; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 10px; }
          th, td { border: 1px solid #DCDCDC; padding: 6px; text-align: right; }
          th { background: #741F3D; color: #FFF; }
          .totals { border-top: 2px solid #741F3D; padding-top: 8px; font-size: 13px; line-height: 1.7; }
        </style></head>
        <body>
          <div class="header">$logoHtml<div><h2>KIKI - مركز كي كي للدواجن</h2><div class="sub">دائماً فكر بالمذاق الطازج</div></div></div>
          <div class="meta">
            <b>فاتورة مبيعات رقم:</b> ${sale.invoiceNumber}<br/>
            <b>التاريخ:</b> ${dateTimeFormat.format(Date(sale.timestamp))}<br/>
            <b>الكاشير:</b> ${sale.cashierName}$customerInfo
          </div>
          <table>
            <tr><th>الصنف</th><th>الكمية</th><th>السعر</th><th>الإجمالي</th></tr>
            $rowsHtml
          </table>
          <div class="totals">
            <div>المجموع الفرعي: ${PricingEngine.formatCurrency(sale.subtotalAmount)}</div>
            $discountRow
            <div style="font-size:16px;color:#741F3D;"><b>الإجمالي الصافي: ${PricingEngine.formatCurrency(sale.totalAmount)}</b></div>
            <div>المدفوع (${sale.paymentSummary}): <b>${PricingEngine.formatCurrency(sale.paidAmount)}</b></div>
            $debtRow
          </div>
        </body></html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "KIKI_Invoice_${sale.invoiceNumber}"
                printManager?.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    fun buildReportLines(
        category: ReportCategory,
        metrics: FinancialMetrics,
        sales: List<SaleWithItems>,
        purchases: List<PurchaseEntity>,
        expenses: List<ExpenseEntity>,
        products: List<ProductEntity>,
        movements: List<StockMovementEntity>,
        returns: List<ReturnEntity>,
        suppliers: List<SupplierEntity>,
        customers: List<CustomerEntity>,
        zReports: List<ZReportEntity>,
        canViewProfits: Boolean
    ): List<String> {
        if (category.requiresProfitAccess && !canViewProfits) {
            return listOf("هذه البيانات محمية بصلاحية حماية الأرباح (حصرية للمدير العام).")
        }
        return when (category) {
            ReportCategory.DAILY_SALES, ReportCategory.MONTHLY_SALES -> {
                if (sales.isEmpty()) listOf("لا توجد فواتير مبيعات مسجلة.")
                else sales.map { sw ->
                    val s = sw.sale
                    val profitPart = if (canViewProfits) " | ربح: ${PricingEngine.formatAmount(s.totalProfit)}" else ""
                    "${s.invoiceNumber} | ${dateTimeFormat.format(Date(s.timestamp))} | ${s.paymentSummary} | ${PricingEngine.formatCurrency(s.totalAmount)}$profitPart"
                }
            }
            ReportCategory.PROFIT_REPORT -> {
                listOf(
                    "صافي المبيعات: ${PricingEngine.formatCurrency(metrics.netSales)}",
                    "تكلفة البضاعة المباعة: ${PricingEngine.formatCurrency(metrics.totalCogs)}",
                    "مجمل الربح: ${PricingEngine.formatCurrency(metrics.grossProfit)}",
                    "المصروفات التشغيلية: ${PricingEngine.formatCurrency(metrics.totalExpenses)}",
                    "صافي الربح التشغيلي: ${PricingEngine.formatCurrency(metrics.netOperatingProfit)}"
                ) + sales.map { sw ->
                    "${sw.sale.invoiceNumber}: بيع ${PricingEngine.formatAmount(sw.sale.totalAmount)} - تكلفة ${PricingEngine.formatAmount(sw.sale.totalCost)} = ربح ${PricingEngine.formatCurrency(sw.sale.totalProfit)}"
                }
            }
            ReportCategory.EXPENSES_REPORT -> {
                if (expenses.isEmpty()) listOf("لا توجد مصروفات مسجلة.")
                else expenses.map { e ->
                    "${e.expenseNumber} | ${e.category.arabicLabel}: ${e.title} | ${PricingEngine.formatCurrency(e.amount)} | ${e.paymentSummary}"
                }
            }
            ReportCategory.PURCHASES_REPORT -> {
                if (purchases.isEmpty()) listOf("لا توجد فواتير مشتريات.")
                else purchases.map { p ->
                    "${p.invoiceNumber} | ${p.productName} (${PricingEngine.formatQty(p.quantity)} ${p.unit.arabicLabel}) | ${PricingEngine.formatCurrency(p.totalCost)} | متبقي: ${PricingEngine.formatAmount(p.remainingDebt)}"
                }
            }
            ReportCategory.INVENTORY_REPORT -> products.map { p ->
                val costStr = if (canViewProfits) " | متوسط التكلفة: ${PricingEngine.formatAmount(p.averageCost)}" else ""
                "${p.name} (${p.category.shortTitle}) | المتاح: ${PricingEngine.formatQty(p.stockQuantity)} ${p.defaultUnit.arabicLabel} | البيع: ${PricingEngine.formatAmount(p.sellingPrice)}$costStr"
            }
            ReportCategory.STOCK_MOVEMENTS -> {
                if (movements.isEmpty()) listOf("لا توجد حركات مخزون.")
                else movements.take(60).map { m ->
                    "${m.referenceCode} | ${m.productName} | ${m.movementType.arabicLabel} (${PricingEngine.formatQty(m.quantityDelta)}) | بعد: ${PricingEngine.formatQty(m.quantityAfter)}"
                }
            }
            ReportCategory.PAYMENT_CHANNELS -> WalletChannel.entries.map { ch ->
                "${ch.arabicLabel}: تحصيل مبيعات ${PricingEngine.formatCurrency(metrics.salesByWallet[ch] ?: 0L)}"
            }
            ReportCategory.WALLET_BALANCES -> WalletChannel.entries.map { ch ->
                val w = metrics.wallets[ch]
                "${ch.arabicLabel}: افتتاحي ${PricingEngine.formatAmount(w?.openingBalance ?: 0L)} | وارد ${PricingEngine.formatAmount(w?.totalInflows ?: 0L)} | صادر ${PricingEngine.formatAmount(w?.totalOutflows ?: 0L)} | الصافي ${PricingEngine.formatCurrency(w?.netBalance ?: 0L)}"
            }
            ReportCategory.SUPPLIERS_REPORT -> {
                if (suppliers.isEmpty()) listOf("لا يوجد موردون مسجلون.")
                else suppliers.map { s -> "${s.name} | المشتريات: ${PricingEngine.formatAmount(s.totalPurchased)} | المدفوع: ${PricingEngine.formatAmount(s.totalPaid)} | المتبقي: ${PricingEngine.formatCurrency(s.balanceDue)}" }
            }
            ReportCategory.CUSTOMERS_REPORT -> {
                if (customers.isEmpty()) listOf("لا يوجد عملاء مسجلون.")
                else customers.map { c -> "${c.name} | الآجل: ${PricingEngine.formatAmount(c.totalCreditSales)} | المحصل: ${PricingEngine.formatAmount(c.totalPaid)} | المستحق: ${PricingEngine.formatCurrency(c.balanceDue)}" }
            }
            ReportCategory.ZREPORT_HISTORY -> {
                if (zReports.isEmpty()) listOf("لا توجد تقارير تقفيل يومية.")
                else zReports.map { z -> "${z.dateLabel} | ${z.closedByName} | الفارق: ${PricingEngine.formatCurrency(z.totalVariance)} | ${if (z.isReopened) "أُعيد فتحها" else "مقفلة"}" }
            }
            ReportCategory.RETURNS_DAMAGED -> {
                if (returns.isEmpty()) listOf("لا توجد مرتجعات أو تالف.")
                else returns.map { r -> "${r.returnCode} | ${r.productName} (${PricingEngine.formatQty(r.quantity)}) | السبب: ${r.reason} | مسترد: ${PricingEngine.formatCurrency(r.refundAmount)}" }
            }
        }
    }

    fun exportXlsxAndShare(
        context: Context,
        periodLabel: String,
        metrics: FinancialMetrics,
        sales: List<SaleWithItems>,
        purchases: List<PurchaseEntity>,
        expenses: List<ExpenseEntity>,
        products: List<ProductEntity>,
        suppliers: List<SupplierEntity>,
        customers: List<CustomerEntity>,
        canViewProfits: Boolean
    ) {
        val summaryRows = mutableListOf(
            listOf("البند المالي", "القيمة (ج.س)"),
            listOf("صافي المبيعات", metrics.netSales.toString()),
            listOf("عدد الفواتير", metrics.salesCount.toString()),
            listOf("تحصيل الكاش", metrics.cashCollected.toString()),
            listOf("تحصيل التطبيقات", metrics.bankCollected.toString()),
            listOf("المصروفات", metrics.totalExpenses.toString()),
            listOf("المرتجعات", metrics.totalCustomerReturns.toString())
        )
        if (canViewProfits) {
            summaryRows.add(listOf("تكلفة البضاعة المباعة", metrics.totalCogs.toString()))
            summaryRows.add(listOf("مجمل الربح", metrics.grossProfit.toString()))
            summaryRows.add(listOf("صافي الربح التشغيلي", metrics.netOperatingProfit.toString()))
            summaryRows.add(listOf("إجمالي المشتريات", metrics.totalPurchases.toString()))
            summaryRows.add(listOf("قيمة المخزون", metrics.totalInventoryValue.toString()))
        }

        val walletRows = listOf(listOf("المحفظة", "الافتتاحي", "الوارد", "الصادر", "الصافي")) +
            WalletChannel.entries.map { ch ->
                val w = metrics.wallets[ch]
                listOf(
                    ch.arabicLabel,
                    (w?.openingBalance ?: 0L).toString(),
                    (w?.totalInflows ?: 0L).toString(),
                    (w?.totalOutflows ?: 0L).toString(),
                    (w?.netBalance ?: 0L).toString()
                )
            }

        val salesRows = if (canViewProfits) {
            listOf(listOf("رقم الفاتورة", "التاريخ", "الكاشير", "العميل", "الدفع", "الإجمالي", "المدفوع", "المتبقي", "التكلفة", "الربح")) +
                sales.map { sw ->
                    val s = sw.sale
                    listOf(s.invoiceNumber, dateTimeFormat.format(Date(s.timestamp)), s.cashierName, s.customerName, s.paymentSummary, s.totalAmount.toString(), s.paidAmount.toString(), s.remainingDebt.toString(), s.totalCost.toString(), s.totalProfit.toString())
                }
        } else {
            listOf(listOf("رقم الفاتورة", "التاريخ", "الكاشير", "العميل", "الدفع", "الإجمالي", "المدفوع", "المتبقي")) +
                sales.map { sw ->
                    val s = sw.sale
                    listOf(s.invoiceNumber, dateTimeFormat.format(Date(s.timestamp)), s.cashierName, s.customerName, s.paymentSummary, s.totalAmount.toString(), s.paidAmount.toString(), s.remainingDebt.toString())
                }
        }

        val inventoryRows = if (canViewProfits) {
            listOf(listOf("الصنف", "القسم", "الوحدة", "المخزون", "متوسط التكلفة", "سعر البيع")) +
                products.map { listOf(it.name, it.category.shortTitle, it.defaultUnit.arabicLabel, it.stockQuantity.toString(), it.averageCost.toString(), it.sellingPrice.toString()) }
        } else {
            listOf(listOf("الصنف", "القسم", "الوحدة", "المخزون", "سعر البيع")) +
                products.map { listOf(it.name, it.category.shortTitle, it.defaultUnit.arabicLabel, it.stockQuantity.toString(), it.sellingPrice.toString()) }
        }

        val expenseRows = listOf(listOf("الرقم", "التاريخ", "البند", "العنوان", "المبلغ", "الدفع")) +
            expenses.map { listOf(it.expenseNumber, dateTimeFormat.format(Date(it.timestamp)), it.category.arabicLabel, it.title, it.amount.toString(), it.paymentSummary) }

        val sheets = mutableListOf(
            XlsxSheetData("الملخص المالي", summaryRows),
            XlsxSheetData("المحافظ الـ 5", walletRows),
            XlsxSheetData("المبيعات", salesRows),
            XlsxSheetData("المخزون والأصناف", inventoryRows),
            XlsxSheetData("المصروفات", expenseRows)
        )

        if (canViewProfits) {
            val purchaseRows = listOf(listOf("رقم الفاتورة", "التاريخ", "الصنف", "المورد", "الكمية", "التكلفة الكلية", "المدفوع", "المتبقي")) +
                purchases.map { listOf(it.invoiceNumber, dateTimeFormat.format(Date(it.timestamp)), it.productName, it.supplierName, it.quantity.toString(), it.totalCost.toString(), it.paidAmount.toString(), it.remainingDebt.toString()) }
            sheets.add(XlsxSheetData("المشتريات", purchaseRows))
        }

        val partyRows = listOf(listOf("النوع", "الاسم", "الهاتف", "الرصيد المستحق")) +
            suppliers.map { listOf("مورد", it.name, it.phone, it.balanceDue.toString()) } +
            customers.map { listOf("عميل", it.name, it.phone, it.balanceDue.toString()) }
        sheets.add(XlsxSheetData("الموردون والعملاء", partyRows))

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "KIKI_ERP_${fileDateFormat.format(Date())}.xlsx")
        XlsxWriter.writeWorkbook(file, sheets)
        shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "تصدير ملف Excel XLSX ($periodLabel)")
    }

    fun exportCsvAndShare(
        context: Context,
        periodLabel: String,
        metrics: FinancialMetrics,
        sales: List<SaleWithItems>,
        purchases: List<PurchaseEntity>,
        expenses: List<ExpenseEntity>,
        canViewProfits: Boolean
    ) {
        val sb = StringBuilder().append('\uFEFF')
        sb.appendLine("KIKI - مركز كي كي للدواجن ($periodLabel)")
        sb.appendLine("التاريخ,${dateTimeFormat.format(Date())}")
        sb.appendLine()
        sb.appendLine("الملخص المالي,القيمة (ج.س)")
        sb.appendLine("صافي المبيعات,${metrics.netSales}")
        sb.appendLine("تحصيل الكاش,${metrics.cashCollected}")
        sb.appendLine("تحصيل التطبيقات,${metrics.bankCollected}")
        sb.appendLine("إجمالي المصروفات,${metrics.totalExpenses}")
        if (canViewProfits) {
            sb.appendLine("تكلفة البضاعة,${metrics.totalCogs}")
            sb.appendLine("مجمل الربح,${metrics.grossProfit}")
            sb.appendLine("صافي الربح التشغيلي,${metrics.netOperatingProfit}")
            sb.appendLine("إجمالي المشتريات,${metrics.totalPurchases}")
        }
        sb.appendLine()
        sb.appendLine("المحفظة,الافتتاحي,الوارد,الصادر,الصافي")
        WalletChannel.entries.forEach { ch ->
            val w = metrics.wallets[ch]
            sb.appendLine("${ch.arabicLabel},${w?.openingBalance ?: 0},${w?.totalInflows ?: 0},${w?.totalOutflows ?: 0},${w?.netBalance ?: 0}")
        }
        sb.appendLine()
        if (canViewProfits) {
            sb.appendLine("الفواتير,التاريخ,الكاشير,الدفع,الإجمالي,التكلفة,الربح")
            sales.forEach { sw ->
                val s = sw.sale
                sb.appendLine("${s.invoiceNumber},${dateTimeFormat.format(Date(s.timestamp))},${s.cashierName},\"${s.paymentSummary}\",${s.totalAmount},${s.totalCost},${s.totalProfit}")
            }
            sb.appendLine()
            sb.appendLine("المشتريات,الصنف,المورد,الكمية,الإجمالي")
            purchases.forEach { p ->
                sb.appendLine("${p.invoiceNumber},${p.productName},${p.supplierName},${p.quantity},${p.totalCost}")
            }
        } else {
            sb.appendLine("الفواتير,التاريخ,الكاشير,الدفع,الإجمالي")
            sales.forEach { sw ->
                val s = sw.sale
                sb.appendLine("${s.invoiceNumber},${dateTimeFormat.format(Date(s.timestamp))},${s.cashierName},\"${s.paymentSummary}\",${s.totalAmount}")
            }
        }
        sb.appendLine()
        sb.appendLine("المصروفات,البند,العنوان,المبلغ,الدفع")
        expenses.forEach { e ->
            sb.appendLine("${e.expenseNumber},${e.category.arabicLabel},\"${e.title}\",${e.amount},\"${e.paymentSummary}\"")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "KIKI_Report_${fileDateFormat.format(Date())}.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, file, "text/csv", "تصدير CSV ($periodLabel)")
    }

    fun exportPdfAndShare(
        context: Context,
        category: ReportCategory,
        periodLabel: String,
        metrics: FinancialMetrics,
        lines: List<String>,
        canViewProfits: Boolean
    ) {
        val file = PdfReceiptExporter.generateFinancialPdfReport(
            context = context,
            reportTitle = category.arabicTitle,
            periodLabel = periodLabel,
            metrics = metrics,
            canViewProfits = canViewProfits,
            lines = lines
        )
        shareFile(context, file, "application/pdf", "تصدير ملف PDF")
    }

    fun printFinancialReport(
        context: Context,
        reportTitle: String,
        periodLabel: String,
        metrics: FinancialMetrics,
        lines: List<String>,
        canViewProfits: Boolean
    ) {
        val logoBase64 = encodeLogoBase64(context)
        val logoHtml = if (logoBase64.isNotEmpty()) {
            """<img src="data:image/jpeg;base64,$logoBase64" style="width:68px;height:68px;border-radius:12px;border:2px solid #F59E0B;" />"""
        } else ""

        val profitHtml = if (canViewProfits) {
            """
            <div class="kpi"><span class="label">تكلفة البضاعة</span><span class="val">${PricingEngine.formatCurrency(metrics.totalCogs)}</span></div>
            <div class="kpi"><span class="label">مجمل الربح</span><span class="val green">${PricingEngine.formatCurrency(metrics.grossProfit)}</span></div>
            <div class="kpi"><span class="label">صافي الربح التشغيلي</span><span class="val green">${PricingEngine.formatCurrency(metrics.netOperatingProfit)}</span></div>
            """
        } else ""

        val rowsHtml = lines.joinToString("") { "<tr><td>$it</td></tr>" }
        val html = """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head><meta charset="utf-8" />
        <style>
          body { font-family: sans-serif; color: #252525; padding: 18px; direction: rtl; }
          .header { display: flex; align-items: center; gap: 14px; border-bottom: 3px solid #741F3D; padding-bottom: 10px; margin-bottom: 14px; }
          h1 { margin: 0; color: #741F3D; font-size: 20px; }
          .tag { color: #D97706; font-weight: bold; font-size: 13px; margin: 3px 0; }
          .grid { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
          .kpi { border: 1px solid #DCDCDC; border-radius: 8px; padding: 8px 12px; background: #F7F7F7; }
          .kpi .label { display: block; font-size: 11px; color: #666; }
          .kpi .val { font-size: 14px; font-weight: bold; color: #741F3D; }
          .kpi .val.green { color: #15803D; }
          table { width: 100%; border-collapse: collapse; font-size: 12px; }
          th, td { border: 1px solid #DCDCDC; padding: 7px; text-align: right; }
          th { background: #741F3D; color: #FFF; }
        </style></head>
        <body>
          <div class="header">$logoHtml<div><h1>KIKI - مركز كي كي للدواجن</h1><div class="tag">دائماً فكر بالمذاق الطازج | $reportTitle ($periodLabel)</div><small>${dateTimeFormat.format(Date())}</small></div></div>
          <div class="grid">
            <div class="kpi"><span class="label">صافي المبيعات (${metrics.salesCount})</span><span class="val">${PricingEngine.formatCurrency(metrics.netSales)}</span></div>
            <div class="kpi"><span class="label">تحصيل الكاش</span><span class="val">${PricingEngine.formatCurrency(metrics.cashCollected)}</span></div>
            <div class="kpi"><span class="label">تحصيل التطبيقات</span><span class="val">${PricingEngine.formatCurrency(metrics.bankCollected)}</span></div>
            <div class="kpi"><span class="label">المصروفات</span><span class="val">${PricingEngine.formatCurrency(metrics.totalExpenses)}</span></div>
            $profitHtml
          </div>
          <table><tr><th>بيانات التقرير</th></tr>$rowsHtml</table>
        </body></html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "KIKI_Report_${fileDateFormat.format(Date())}"
                printManager?.print(jobName, view.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    private fun encodeLogoBase64(context: Context): String {
        return try {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_kiki_logo)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}
