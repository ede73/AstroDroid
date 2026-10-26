package com.ede.astrodroid.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class SkyLabel(val text: String, val x: Float, val y: Float, val priority: Int)

class SkyLabelOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xDD000000.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        textSize = 13f * resources.displayMetrics.scaledDensity
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
        textSize = 13f * resources.displayMetrics.scaledDensity
    }
    @Volatile private var skyLabels: List<SkyLabel> = emptyList()

    init {
        isClickable = false
        isFocusable = false
    }

    fun setSkyLabels(labels: List<SkyLabel>) {
        if (skyLabels == labels) return
        skyLabels = labels
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val accepted = mutableListOf<RectF>()
        val density = resources.displayMetrics.density
        val padding = 5f * density
        for (label in skyLabels.sortedByDescending { it.priority }) {
            val left = label.x + 7f * density
            val baseline = label.y - 5f * density
            val textWidth = textPaint.measureText(label.text)
            val bounds = RectF(left, baseline - textPaint.textSize, left + textWidth, baseline)
            if (bounds.left < 0 || bounds.right > this.width || bounds.top < 0 || bounds.bottom > height) continue
            if (accepted.any { RectF.intersects(it, RectF(bounds).apply { inset(-padding, -padding) }) }) continue
            accepted += bounds
            canvas.drawText(label.text, left, baseline, outlinePaint)
            canvas.drawText(label.text, left, baseline, textPaint)
        }
    }
}
