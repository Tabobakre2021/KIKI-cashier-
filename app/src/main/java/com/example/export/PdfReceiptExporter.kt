package com.example.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.entity.SaleWithItems
import com.example.domain.FinancialMetrics
import com.example.domain.PricingEngine
import com.example.domain.WalletChannel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun generateInvoiceReceiptImage(
        context: Context,
        saleWithItems: SaleWithItems
    ): File {
        val sale = saleWithItems.sale
        val width = 720
        val height = 420 + (saleWithItems.items.size * 68) + 180
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val burgundyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#741F3D") }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            textSize = 20f
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252525")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252525")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val leftAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#741F3D")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#DCDCDC")
            strokeWidth = 2f
        }

        // Header banner
        canvas.drawRect(0f, 0f, width.toFloat(), 150f, burgundyPaint)
        canvas.drawRect(0f, 144f, width.toFloat(), 150f, goldPaint)

        runCatching {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.img_kiki_logo)
            canvas.drawBitmap(logo, null, Rect(28, 22, 134, 128), null)
        }

        canvas.drawText("KIKI - مركز كي كي للدواجن", width - 28f, 65f, titlePaint)
        canvas.drawText("دائماً فكر بالمذاق الطازج", width - 28f, 108f, subTitlePaint)

        var y = 195f
        canvas.drawText("فاتورة رقم: ${sale.invoiceNumber}", width - 28f, y, boldBodyPaint)
        y += 34f
        canvas.drawText("التاريخ: ${dateTimeFormat.format(Date(sale.timestamp))}", width - 28f, y, bodyPaint)
        y += 34f
        canvas.drawText("الكاشير: ${sale.cashierName}" + if (sale.customerName.isNotBlank()) " | العميل: ${sale.customerName}" else "", width - 28f, y, bodyPaint)
        y += 24f
        canvas.drawLine(28f, y, width - 28f, y, linePaint)
        y += 36f

        saleWithItems.items.forEach { item ->
            canvas.drawText("${item.productName} (${PricingEngine.formatQty(item.quantity)} ${item.unit.arabicLabel})", width - 28f, y, boldBodyPaint)
            canvas.drawText(PricingEngine.formatCurrency(item.lineTotal), 28f, y, leftAmountPaint)
            y += 28f
            canvas.drawText("سعر الوحدة: ${PricingEngine.formatCurrency(item.sellingPriceAtSale)}", width - 28f, y, bodyPaint)
            y += 36f
        }

        canvas.drawLine(28f, y, width - 28f, y, linePaint)
        y += 36f

        if (sale.discountAmount > 0L) {
            canvas.drawText("المجموع الفرعي:", width - 28f, y, bodyPaint)
            canvas.drawText(PricingEngine.formatCurrency(sale.subtotalAmount), 28f, y, leftAmountPaint)
            y += 32f
            canvas.drawText("الخصم:", width - 28f, y, bodyPaint)
            canvas.drawText("- ${PricingEngine.formatCurrency(sale.discountAmount)}", 28f, y, leftAmountPaint)
            y += 34f
        }

        canvas.drawText("الإجمالي الصافي:", width - 28f, y, boldBodyPaint)
        canvas.drawText(PricingEngine.formatCurrency(sale.totalAmount), 28f, y, leftAmountPaint)
        y += 34f
        canvas.drawText("المدفوع (${sale.paymentSummary}):", width - 28f, y, bodyPaint)
        canvas.drawText(PricingEngine.formatCurrency(sale.paidAmount), 28f, y, leftAmountPaint)
        if (sale.remainingDebt > 0L) {
            y += 34f
            canvas.drawText("المتبقي (مديونية):", width - 28f, y, boldBodyPaint)
            canvas.drawText(PricingEngine.formatCurrency(sale.remainingDebt), 28f, y, leftAmountPaint)
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "${sale.invoiceNumber}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return file
    }

    fun generateFinancialPdfReport(
        context: Context,
        reportTitle: String,
        periodLabel: String,
        metrics: FinancialMetrics,
        canViewProfits: Boolean,
        lines: List<String>
    ): File {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas

        val burgundyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#741F3D") }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F59E0B") }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#741F3D")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#252525")
            textSize = 11.5f
            textAlign = Paint.Align.RIGHT
        }

        // Draw Header
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, burgundyPaint)
        canvas.drawRect(0f, 86f, pageWidth.toFloat(), 90f, goldPaint)
        runCatching {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.img_kiki_logo)
            canvas.drawBitmap(logo, null, Rect(24, 15, 84, 75), null)
        }
        canvas.drawText("KIKI - مركز كي كي للدواجن", pageWidth - 24f, 38f, headerTextPaint)
        canvas.drawText("$reportTitle | الفترة: $periodLabel | ${dateTimeFormat.format(Date())}", pageWidth - 24f, 64f, subHeaderPaint)

        var y = 120f
        canvas.drawText("الملخص المالي العام", pageWidth - 24f, y, sectionTitlePaint)
        y += 22f

        val summaryLines = buildList {
            add("صافي المبيعات (${metrics.salesCount} فاتورة): ${PricingEngine.formatCurrency(metrics.netSales)}")
            add("تحصيل الكاش: ${PricingEngine.formatCurrency(metrics.cashCollected)} | التطبيقات البنكية: ${PricingEngine.formatCurrency(metrics.bankCollected)}")
            add("إجمالي المصروفات: ${PricingEngine.formatCurrency(metrics.totalExpenses)} | المرتجعات: ${PricingEngine.formatCurrency(metrics.totalCustomerReturns)}")
            if (canViewProfits) {
                add("تكلفة البضاعة المباعة (COGS): ${PricingEngine.formatCurrency(metrics.totalCogs)}")
                add("مجمل الربح: ${PricingEngine.formatCurrency(metrics.grossProfit)}")
                add("صافي الربح التشغيلي: ${PricingEngine.formatCurrency(metrics.netOperatingProfit)}")
                add("إجمالي المشتريات: ${PricingEngine.formatCurrency(metrics.totalPurchases)} | قيمة المخزون: ${PricingEngine.formatCurrency(metrics.totalInventoryValue)}")
            }
            add("أرصدة المحافظ: " + WalletChannel.entries.joinToString(" | ") { ch ->
                "${ch.arabicLabel}: ${PricingEngine.formatAmount(metrics.wallets[ch]?.netBalance ?: 0L)}"
            })
        }

        summaryLines.forEach { text ->
            canvas.drawText(text, pageWidth - 24f, y, bodyPaint)
            y += 18f
        }

        y += 12f
        canvas.drawText("تفاصيل التقرير", pageWidth - 24f, y, sectionTitlePaint)
        y += 20f

        lines.forEach { line ->
            if (y > pageHeight - 40f) {
                doc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = 44f
            }
            canvas.drawText(line, pageWidth - 24f, y, bodyPaint)
            y += 17f
        }

        doc.finishPage(page)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "KIKI_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()
        return file
    }
}
