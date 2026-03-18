package com.yarick.orunner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import kotlin.math.roundToInt

class SplitsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Split(val index: Int, val secPerUnit: Float, val partialDistMeters: Float = 0f) {
        val isPartial get() = partialDistMeters > 0f
    }

    private var splits: List<Split> = emptyList()
    private var metric: Boolean = true

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val HEADER_H = dp(24f)
    private val ROW_H    = dp(36f)
    private val BAR_H    = dp(20f)
    private val LABEL_W  = dp(60f)
    private val PACE_W   = dp(76f)
    private val GAP      = dp(6f)
    private val CORNER   = dp(4f)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorPrimary,
            0xFF6750A4.toInt()
        )
    }

    private val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(11f)
        color = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            0xFF49454F.toInt()
        )
    }

    private val headerPacePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(11f)
        textAlign = Paint.Align.RIGHT
        color = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            0xFF49454F.toInt()
        )
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(12f)
        color = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            0xFF1C1B1F.toInt()
        )
    }

    private val pacePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dp(12f)
        textAlign = Paint.Align.RIGHT
        color = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            0xFF1C1B1F.toInt()
        )
    }

    private val rect = RectF()

    fun setSplits(splits: List<Split>, metric: Boolean) {
        this.splits = splits
        this.metric = metric
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = if (splits.isEmpty()) 0
                else (HEADER_H + splits.size * ROW_H).roundToInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h)
    }

    override fun onDraw(canvas: Canvas) {
        if (splits.isEmpty()) return

        // Header row
        val headerY = HEADER_H - (headerPaint.ascent() + headerPaint.descent()) / 2f - dp(4f)
        canvas.drawText(if (metric) "km" else "mi", 0f, headerY, headerPaint)
        canvas.drawText("Pace", width.toFloat(), headerY, headerPacePaint)

        val fastest = splits.minOf { it.secPerUnit }
        val barMaxW = width - LABEL_W - PACE_W - GAP * 2

        splits.forEachIndexed { i, split ->
            val rowMid = HEADER_H + i * ROW_H + ROW_H / 2f
            val textY = rowMid - (labelPaint.ascent() + labelPaint.descent()) / 2f

            // Split label — just the number, no unit
            val label = if (split.isPartial) {
                val dist = if (metric) split.partialDistMeters / 1000f
                           else        split.partialDistMeters / 1609.344f
                "%.2f".format(dist)
            } else {
                "${split.index}"
            }
            canvas.drawText(label, 0f, textY, labelPaint)

            // Bar — wider = faster
            val fraction = if (split.secPerUnit > 0f) fastest / split.secPerUnit else 0f
            val barW = (barMaxW * fraction).coerceAtLeast(dp(2f))
            val barL = LABEL_W + GAP
            rect.set(barL, rowMid - BAR_H / 2f, barL + barW, rowMid + BAR_H / 2f)
            canvas.drawRoundRect(rect, CORNER, CORNER, barPaint)

            // Pace text — right-aligned to the view edge
            val m = (split.secPerUnit / 60).toInt()
            val s = (split.secPerUnit % 60).toInt()
            canvas.drawText("%d:%02d".format(m, s), width.toFloat(), textY, pacePaint)
        }
    }
}
