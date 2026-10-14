package com.example.bankapputomo.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

class StatisticsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ChartType {
        LINE, BAR, PIE
    }

    var chartType: ChartType = ChartType.LINE
        set(value) {
            field = value
            startAnimation()
        }

    private var animProgress = 1f
    private var months = listOf("Oct", "Nov", "Dec", "Jan", "Feb", "Mar")
    private var lineValues = listOf(0.45f, 0.70f, 0.50f, 0.88f, 0.65f, 0.80f)

    var activeMonthIndex: Int = 3 // Jan (default)
        set(value) {
            if (field != value && value in months.indices) {
                field = value
                startAnimation()
            }
        }

    val monthNames: List<String> get() = months

    var onMonthSelectedListener: ((index: Int, monthName: String) -> Unit)? = null

    var monthlyExpenseLabels: List<String> = listOf("Rp 6,7M", "Rp 10,5M", "Rp 7,5M", "Rp 1,9M", "Rp 9,7M", "Rp 12,0M")
        set(value) {
            field = value
            invalidate()
        }

    fun setMonthlyExpenses(labels: List<String>, expenses: List<Long>) {
        if (labels.isEmpty() || labels.size != expenses.size) return
        months = labels
        val maxExpense = expenses.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        lineValues = expenses.map { (it.toDouble() / maxExpense.toDouble()).toFloat().coerceIn(0.08f, 1.0f) }
        monthlyExpenseLabels = expenses.map { value ->
            if (value >= 1_000_000L) {
                "Rp ${String.format(java.util.Locale.US, "%.1fM", value / 1_000_000.0)}"
            } else {
                "Rp ${value / 1000}K"
            }
        }
        activeMonthIndex = activeMonthIndex.coerceIn(0, months.lastIndex)
        startAnimation()
    }

    // Line paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F66F6")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F66F6")
        style = Paint.Style.FILL
    }

    private val dotInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Guide Line & Tooltip paints
    private val guideLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#332F66F6")
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2135")
        style = Paint.Style.FILL
    }

    private val tooltipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Bar paints
    private val barInactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6EDFC")
        style = Paint.Style.FILL
    }

    private val barActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F66F6")
        style = Paint.Style.FILL
    }

    // Pie slices
    private val pieColors = intArrayOf(
        Color.parseColor("#2F66F6"), // Shopping
        Color.parseColor("#F2994A"), // Entertainment
        Color.parseColor("#27AE60"), // Bills
        Color.parseColor("#9B51E0")  // Food
    )
    private var piePercentages = floatArrayOf(0.35f, 0.25f, 0.20f, 0.20f)
    private val piePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    // Text paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E94A4")
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val activeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val centerTextValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1B2135")
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val centerTextLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E94A4")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    private val sliceTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    var centerPercentageText: String = "55%"
        set(value) {
            field = value
            invalidate()
        }

    var centerLabelText: String = "Spent This Month"
        set(value) {
            field = value
            invalidate()
        }

    fun setPieSummary(percent: Int, label: String) {
        centerPercentageText = "$percent%"
        centerLabelText = label
        invalidate()
    }

    fun setPiePercentages(values: List<Float>) {
        if (values.size != 4) return
        val total = values.sum().takeIf { it > 0f } ?: 1f
        piePercentages = values.map { (it / total).coerceAtLeast(0f) }.toFloatArray()
        invalidate()
    }

    init {
        startAnimation()
    }

    fun startAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
        }
        animator.start()
    }

    private var touchStartX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (chartType == ChartType.PIE) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - touchStartX
                    if (Math.abs(dx) > 60f) {
                        if (dx > 0 && activeMonthIndex > 0) {
                            activeMonthIndex--
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onMonthSelectedListener?.invoke(activeMonthIndex, months[activeMonthIndex])
                        } else if (dx < 0 && activeMonthIndex < months.size - 1) {
                            activeMonthIndex++
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onMonthSelectedListener?.invoke(activeMonthIndex, months[activeMonthIndex])
                        }
                    }
                    performClick()
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val selected = findClosestMonthIndex(event.x)
                if (selected != -1 && selected != activeMonthIndex) {
                    activeMonthIndex = selected
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onMonthSelectedListener?.invoke(selected, months[selected])
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun findClosestMonthIndex(touchX: Float): Int {
        val w = width.toFloat()
        if (w <= 0) return -1
        val stepX = w / (months.size + 1)
        var closestIdx = -1
        var minDistance = Float.MAX_VALUE

        for (i in months.indices) {
            val x = stepX * (i + 1)
            val dist = Math.abs(touchX - x)
            if (dist < minDistance && dist < stepX * 0.95f) {
                minDistance = dist
                closestIdx = i
            }
        }
        return closestIdx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        when (chartType) {
            ChartType.LINE -> drawLineChart(canvas, w, h)
            ChartType.BAR -> drawBarChart(canvas, w, h)
            ChartType.PIE -> drawPieChart(canvas, w, h)
        }
    }

    private fun drawLineChart(canvas: Canvas, w: Float, h: Float) {
        val paddingBottom = 70f
        val chartHeight = h - paddingBottom - 50f
        val stepX = w / (months.size + 1)

        val points = mutableListOf<PointF>()
        for (i in lineValues.indices) {
            val x = stepX * (i + 1)
            val y = (h - paddingBottom) - (lineValues[i] * chartHeight * animProgress)
            points.add(PointF(x, y))
        }

        // Draw gradient fill
        val fillPath = Path()
        fillPath.moveTo(points.first().x, h - paddingBottom)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2
            fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }
        fillPath.lineTo(points.last().x, h - paddingBottom)
        fillPath.close()

        val gradient = LinearGradient(
            0f, 0f, 0f, h - paddingBottom,
            Color.parseColor("#442F66F6"), Color.parseColor("#002F66F6"),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradient
        canvas.drawPath(fillPath, fillPaint)

        // Draw smooth line
        val linePath = Path()
        linePath.moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val cx = (p0.x + p1.x) / 2
            linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
        }
        canvas.drawPath(linePath, linePaint)

        // Draw vertical dashed guide line for active point
        if (activeMonthIndex in points.indices) {
            val activePt = points[activeMonthIndex]
            canvas.drawLine(activePt.x, 35f, activePt.x, h - paddingBottom, guideLinePaint)
        }

        // Draw points and month labels
        for (i in points.indices) {
            val pt = points[i]
            val month = months[i]

            if (i == activeMonthIndex) {
                // Active point with ring
                dotPaint.color = Color.parseColor("#2F66F6")
                canvas.drawCircle(pt.x, pt.y, 16f, dotPaint)
                canvas.drawCircle(pt.x, pt.y, 8f, dotInnerPaint)

                // Tooltip bubble above point
                val label = monthlyExpenseLabels.getOrElse(i) { "" }
                if (label.isNotEmpty()) {
                    val tooltipW = 120f
                    val tooltipH = 38f
                    val tooltipLeft = (pt.x - tooltipW / 2f).coerceIn(12f, w - tooltipW - 12f)
                    val tooltipTop = (pt.y - tooltipH - 18f).coerceAtLeast(8f)
                    val tooltipRect = RectF(tooltipLeft, tooltipTop, tooltipLeft + tooltipW, tooltipTop + tooltipH)
                    canvas.drawRoundRect(tooltipRect, 12f, 12f, tooltipBgPaint)
                    canvas.drawText(label, tooltipLeft + tooltipW / 2f, tooltipTop + 26f, tooltipTextPaint)

                    val arrowPath = Path().apply {
                        val arrowTop = tooltipTop + tooltipH
                        moveTo(pt.x - 7f, arrowTop)
                        lineTo(pt.x, arrowTop + 7f)
                        lineTo(pt.x + 7f, arrowTop)
                        close()
                    }
                    canvas.drawPath(arrowPath, tooltipBgPaint)
                }

                // Active Month Pill background
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2F66F6")
                }
                val pillRect = RectF(pt.x - 38f, h - paddingBottom + 12f, pt.x + 38f, h - paddingBottom + 56f)
                canvas.drawRoundRect(pillRect, 22f, 22f, pillPaint)
                canvas.drawText(month, pt.x, h - paddingBottom + 45f, activeTextPaint)
            } else {
                canvas.drawCircle(pt.x, pt.y, 7f, dotPaint)
                canvas.drawText(month, pt.x, h - paddingBottom + 45f, textPaint)
            }
        }
    }

    private fun drawBarChart(canvas: Canvas, w: Float, h: Float) {
        val paddingBottom = 70f
        val chartHeight = h - paddingBottom - 50f
        val stepX = w / (months.size + 1)
        val barWidth = 32f

        for (i in lineValues.indices) {
            val x = stepX * (i + 1)
            val barHeight = lineValues[i] * chartHeight * animProgress
            val top = (h - paddingBottom) - barHeight
            val bottom = h - paddingBottom

            val rect = RectF(x - barWidth / 2, top, x + barWidth / 2, bottom)
            val paint = if (i == activeMonthIndex) barActivePaint else barInactivePaint
            canvas.drawRoundRect(rect, 14f, 14f, paint)

            val month = months[i]
            if (i == activeMonthIndex) {
                // Tooltip bubble above active bar
                val label = monthlyExpenseLabels.getOrElse(i) { "" }
                if (label.isNotEmpty()) {
                    val tooltipW = 120f
                    val tooltipH = 38f
                    val tooltipLeft = (x - tooltipW / 2f).coerceIn(12f, w - tooltipW - 12f)
                    val tooltipTop = (top - tooltipH - 18f).coerceAtLeast(8f)
                    val tooltipRect = RectF(tooltipLeft, tooltipTop, tooltipLeft + tooltipW, tooltipTop + tooltipH)
                    canvas.drawRoundRect(tooltipRect, 12f, 12f, tooltipBgPaint)
                    canvas.drawText(label, tooltipLeft + tooltipW / 2f, tooltipTop + 26f, tooltipTextPaint)

                    val arrowPath = Path().apply {
                        val arrowTop = tooltipTop + tooltipH
                        moveTo(x - 7f, arrowTop)
                        lineTo(x, arrowTop + 7f)
                        lineTo(x + 7f, arrowTop)
                        close()
                    }
                    canvas.drawPath(arrowPath, tooltipBgPaint)
                }

                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2F66F6")
                }
                val pillRect = RectF(x - 38f, h - paddingBottom + 12f, x + 38f, h - paddingBottom + 56f)
                canvas.drawRoundRect(pillRect, 22f, 22f, pillPaint)
                canvas.drawText(month, x, h - paddingBottom + 45f, activeTextPaint)
            } else {
                canvas.drawText(month, x, h - paddingBottom + 45f, textPaint)
            }
        }
    }

    private fun drawPieChart(canvas: Canvas, w: Float, h: Float) {
        val cx = w / 2f
        val cy = (h - 20f) / 2f
        val radius = Math.min(cx, cy) - 34f
        val strokeW = 46f
        piePaint.strokeWidth = strokeW

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        var startAngle = -90f

        val slicePercentagesLabels = piePercentages.map { "${(it * 100).toInt()}%" }

        for (i in piePercentages.indices) {
            val sweep = piePercentages[i] * 360f * animProgress
            piePaint.color = pieColors[i]
            canvas.drawArc(rect, startAngle + 2f, sweep - 4f, false, piePaint)

            // Draw percentage label directly inside the arc band
            if (animProgress > 0.6f && sweep > 24f) {
                val midAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
                val textX = cx + (radius * Math.cos(midAngleRad)).toFloat()
                val textY = cy + (radius * Math.sin(midAngleRad)).toFloat() + 8f
                canvas.drawText(slicePercentagesLabels[i], textX, textY, sliceTextPaint)
            }

            startAngle += sweep
        }

        // Center Percentage
        canvas.drawText(centerPercentageText, cx, cy + 10f, centerTextValuePaint)
        canvas.drawText(centerLabelText, cx, cy + 46f, centerTextLabelPaint)
    }
}
