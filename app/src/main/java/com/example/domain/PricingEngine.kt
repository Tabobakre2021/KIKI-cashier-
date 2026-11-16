package com.example.domain

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

object PricingEngine {

    private val currencyFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
    private val percentFormat = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
    private val qtyFormat = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun roundToStep(rawValue: Double, step: Int = 1): Long {
        if (rawValue <= 0.0) return 0L
        val safeStep = step.coerceAtLeast(1)
        if (safeStep == 1) return rawValue.roundToLong()
        return ((rawValue / safeStep).roundToLong()) * safeStep
    }

    /**
     * Markup mode:
     * Selling Price = Cost Price * (1 + Markup% / 100)
     * Example: 10,000 * (1 + 20/100) = 12,000
     */
    fun calculateSellingFromMarkup(
        costPrice: Long,
        markupPercent: Double,
        roundingStep: Int = 1
    ): Long {
        if (costPrice <= 0L) return 0L
        val raw = costPrice.toDouble() * (1.0 + markupPercent / 100.0)
        return roundToStep(raw, roundingStep)
    }

    /**
     * Margin mode (Gross Margin from Selling Price):
     * Selling Price = Cost Price / (1 - Margin% / 100)
     * Example: 10,000 / (1 - 20/100) = 12,500
     */
    fun calculateSellingFromMargin(
        costPrice: Long,
        marginPercent: Double,
        roundingStep: Int = 1
    ): Long {
        if (costPrice <= 0L) return 0L
        if (marginPercent >= 99.9) return costPrice * 100L
        val raw = costPrice.toDouble() / (1.0 - marginPercent / 100.0)
        return roundToStep(raw, roundingStep)
    }

    fun calculateSellingPrice(
        costPrice: Long,
        targetPercent: Double,
        mode: PricingMode,
        roundingStep: Int = 1
    ): Long = when (mode) {
        PricingMode.MARKUP -> calculateSellingFromMarkup(costPrice, targetPercent, roundingStep)
        PricingMode.MARGIN -> calculateSellingFromMargin(costPrice, targetPercent, roundingStep)
    }

    /**
     * Markup % = ((Selling - Cost) / Cost) * 100
     */
    fun calculateMarkupPercent(costPrice: Long, sellingPrice: Long): Double {
        if (costPrice <= 0L) return 0.0
        val raw = ((sellingPrice - costPrice).toDouble() / costPrice.toDouble()) * 100.0
        return (raw * 100.0).roundToLong() / 100.0
    }

    /**
     * Profit Margin % = ((Selling - Cost) / Selling) * 100
     */
    fun calculateMarginPercent(costPrice: Long, sellingPrice: Long): Double {
        if (sellingPrice <= 0L || costPrice <= 0L) return 0.0
        val raw = ((sellingPrice - costPrice).toDouble() / sellingPrice.toDouble()) * 100.0
        return (raw * 100.0).roundToLong() / 100.0
    }

    /**
     * Moving Weighted Average Cost:
     * NewAvg = (PreviousQty * PreviousAvgCost + NewPurchaseNetCost) / (PreviousQty + NewPurchasedQty)
     */
    fun calculateMovingWeightedAverageCost(
        previousQty: Double,
        previousAvgCost: Long,
        purchasedQty: Double,
        purchasedTotalNetCost: Long
    ): Long {
        if (purchasedQty <= 0.0) return previousAvgCost
        if (previousQty <= 0.0 || previousAvgCost <= 0L) {
            return (purchasedTotalNetCost.toDouble() / purchasedQty).roundToLong()
        }
        val prevValue = previousQty * previousAvgCost.toDouble()
        val totalQty = previousQty + purchasedQty
        if (totalQty <= 0.0) return previousAvgCost
        return ((prevValue + purchasedTotalNetCost.toDouble()) / totalQty).roundToLong()
    }

    fun calculateLineTotal(unitPrice: Long, quantity: Double): Long {
        return (unitPrice.toDouble() * quantity).roundToLong()
    }

    fun calculateLineProfit(
        sellingUnitPrice: Long,
        costUnitPrice: Long,
        quantity: Double,
        lineDiscount: Long = 0L
    ): Long {
        val revenue = calculateLineTotal(sellingUnitPrice, quantity) - lineDiscount
        val cost = calculateLineTotal(costUnitPrice, quantity)
        return revenue - cost
    }

    fun formatAmount(amount: Long): String = currencyFormat.format(amount)

    fun formatCurrency(amount: Long): String = "${currencyFormat.format(amount)} ج.س"

    fun formatPercent(percent: Double): String = "${percentFormat.format(percent)}%"

    fun formatQty(qty: Double): String = qtyFormat.format(qty)

    fun parseLongAmount(input: String): Long {
        val cleaned = input.trim().replace(",", "")
        return cleaned.toDoubleOrNull()?.roundToLong() ?: 0L
    }

    fun parseDoubleQty(input: String): Double {
        val cleaned = input.trim().replace(",", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
