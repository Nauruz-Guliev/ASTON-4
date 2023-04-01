package ru.example.gnt.aston_4

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates


class CustomClockView(
    context: Context,
    attributesSet: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attributesSet, defStyleAttr, defStyleRes) {

    private var viewWidth by Delegates.notNull<Int>()
    private var viewHeight by Delegates.notNull<Int>()

    private var viewCenterY by Delegates.notNull<Float>()
    private var viewCenterX by Delegates.notNull<Float>()

    private var clockRadius by Delegates.notNull<Float>()
    private var hourNumbersRadius by Delegates.notNull<Float>()

    private var viewPaint: Paint
    private var viewNumbers: IntArray
    private var viewRectangle: Rect

    private var listOfClockNumberInfo = listOf<ClockNumber>()

    private var myHandler: Handler

    private var secondHandInfo: ClockHandInfo? = null
    private var minuteHandInfo: ClockHandInfo? = null
    private var hourHandInfo: ClockHandInfo? = null


    private var clockColor: Int? = null

    init {
        myHandler = Handler(Looper.getMainLooper())
        if (attributesSet != null) {
            initAttributes(attributesSet, defStyleAttr, defStyleRes)
        }
        viewPaint = Paint(ANTI_ALIAS_FLAG)
        viewNumbers = IntRange(1, 12).step(1).toList().toIntArray()
        viewRectangle = Rect()
        updateViewSizes()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initAttributes(attributesSet: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val typedArray = context.obtainStyledAttributes(
            attributesSet,
            R.styleable.CustomClockView,
            defStyleAttr,
            defStyleRes
        )
        secondHandInfo = ClockHandInfo(
            timeValue = typedArray.getColor(
                R.styleable.CustomClockView_second_start_time,
                DEFAULT_SECOND_START_TIME
            ),
            drawable = context.getDrawable(R.drawable.second_hand)!!,
            repeatIntervalInMilliseconds = 1000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_second_hand_color,
                DEFAULT_SECOND_HAND_COLOR
            ),
            myHandler = myHandler
        )

        minuteHandInfo = ClockHandInfo(
            timeValue = typedArray.getColor(
                R.styleable.CustomClockView_minute_start_time,
                DEFAULT_MINUTE_START_TIME
            ),
            drawable = context.getDrawable(R.drawable.minute_hand)!!,
            repeatIntervalInMilliseconds = 60000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_minute_hand_color,
                DEFAULT_MINUTE_HAND_COLOR
            ),
            myHandler = myHandler
        )

        hourHandInfo = ClockHandInfo(
            timeValue = typedArray.getColor(
                R.styleable.CustomClockView_hour_start_time,
                DEFAULT_HOUR_START_TIME
            ),
            drawable = context.getDrawable(R.drawable.hour_hand)!!,
            repeatIntervalInMilliseconds = 3600000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_hour_hand_color,
                DEFAULT_HOUR_HAND_COLOR
            ),
            myHandler = myHandler
        )
        clockColor = typedArray.getColor(
            R.styleable.CustomClockView_clock_color,
            DEFAULT_CLOCK_COLOR
        )
        typedArray.recycle()
    }

    constructor(context: Context, attributesSet: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attributesSet,
        defStyleAttr,
        R.style.CustomClockDefaultStyle
    )

    constructor(context: Context, attributesSet: AttributeSet?) : this(
        context,
        attributesSet,
        R.style.CustomClockDefaultStyle
    )

    constructor(context: Context) : this(context, null)

    private fun drawCircle(canvas: Canvas) {
        viewPaint.apply {
            reset()
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = DEFAULT_STROKE_WIDTH
            isAntiAlias = true
            color = clockColor ?: Color.BLACK
        }
        canvas.drawCircle(
            viewCenterX, viewCenterY,
            clockRadius, viewPaint
        )
    }

    private fun drawNumbers(canvas: Canvas) {
        listOfClockNumberInfo.forEach { clockNumber ->
            with(clockNumber) {
                canvas.drawText(
                    number,
                    xPosition,
                    yPosition,
                    paint
                )
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateViewSizes()
    }

    private fun updateViewSizes() {
        viewHeight = height
        viewWidth = width

        viewCenterY = viewWidth / HALF_DIVIDER
        viewCenterX = viewHeight / HALF_DIVIDER

        clockRadius = min(viewHeight / HALF_DIVIDER, viewWidth / HALF_DIVIDER) - DEFAULT_PADDING
        hourNumbersRadius = clockRadius - DEFAULT_CLOCK_NUMBERS_SPACING

        listOfClockNumberInfo = getClockNumberList()

        calculateHandBounds()
    }

    private fun calculateHandBounds() {
        secondHandInfo?.apply {
            dimension = ClockHandRectangleBounds(
                left = viewWidth / 3 + clockRadius.toInt() / 10,
                top = clockRadius.toInt() - DEFAULT_PADDING.toInt(),
                right = viewWidth - viewWidth / 3 - clockRadius.toInt() / 10,
                bottom = clockRadius.toInt()
            )
        }

        minuteHandInfo?.apply {
            dimension = ClockHandRectangleBounds(
                left = viewWidth / 3,
                top = clockRadius.toInt() - DEFAULT_PADDING.toInt(),
                right = viewWidth - viewWidth / 3,
                bottom = clockRadius.toInt() + DEFAULT_PADDING.toInt() / 2
            )
        }
        hourHandInfo?.apply {
            dimension = ClockHandRectangleBounds(
                left = (viewWidth / (3.4)).toInt(),
                top = clockRadius.toInt() - DEFAULT_PADDING.toInt() * 4,
                right = viewWidth - (viewWidth / 3.4).toInt(),
                bottom = clockRadius.toInt() + DEFAULT_PADDING.toInt() / 2
            )
        }
    }

    /**
     * @return List of ClockNumber instances that contain necessary information to draw a number
     */
    private fun getClockNumberList(): List<ClockNumber> {
        viewPaint.textSize = DEFAULT_FONT_SIZE
        viewPaint.apply {
            strokeWidth = HOUR_NUMBERS_STROKE_WIDTH
        }
        val list = mutableListOf<ClockNumber>()
        for (number in viewNumbers) {
            (Math.PI / 6 * (number - 3)).also { angle ->
                viewPaint.getTextBounds(
                    number.toString(),
                    0,
                    number.toString().length,
                    viewRectangle
                )
                list.add(
                    ClockNumber(
                        number = number.toString(),
                        xPosition = (viewCenterY + cos(angle) * hourNumbersRadius - viewRectangle.width() / 2).toFloat(),
                        yPosition = (viewCenterY + sin(angle) * hourNumbersRadius + viewRectangle.height() / 2).toFloat(),
                        paint = Paint(viewPaint)
                    )
                )
            }
        }

        return list
    }

    private fun drawHand(
        clockHand: ClockHandInfo,
        canvas: Canvas,
    ) {
        clockHand.run {
            with(this.dimension!!) {
                drawable.setBounds(
                    left,
                    top,
                    right,
                    bottom
                )
            }
            canvas.rotate(timeValue * 6f, viewCenterY, viewCenterY);
            this.drawable.draw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
        drawNumbers(canvas)

        hourHandInfo?.run { drawHand(this, canvas) }
        minuteHandInfo?.run { drawHand(this, canvas) }
        secondHandInfo?.run { drawHand(this, canvas) }

        postInvalidateDelayed(500)
    }

    private data class ClockHandInfo(
        var timeValue: Int,
        var drawable: Drawable,
        var color: Int? = null,
        var dimension: ClockHandRectangleBounds? = null,
        var repeatIntervalInMilliseconds: Int,
        val myHandler: Handler
    ) {
        init {
            DrawableCompat.setTint(this.drawable, color ?: Color.BLACK)
            myHandler.postDelayed(object : Runnable {
                override fun run() {
                    timeValue++
                    myHandler.postDelayed(this, repeatIntervalInMilliseconds.toLong())
                }
            }, 0)
        }
    }

    private inner class ClockNumber(
        val xPosition: Float,
        val yPosition: Float,
        val number: String,
        val paint: Paint
    )

    /** // себе для наглядности
     * @param left The X coordinate of the left side of the rectangle
     * @param top The Y coordinate of the top of the rectangle
     * @param right The X coordinate of the right side of the rectangle
     * @param bottom The Y coordinate of the bottom of the rectangle
     */
    private data class ClockHandRectangleBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    companion object {
        private const val HALF_DIVIDER = 2F
        private const val DEFAULT_STROKE_WIDTH = 14F
        private const val HOUR_NUMBERS_STROKE_WIDTH = 4F
        private const val DEFAULT_FONT_SIZE = 40F
        private const val DEFAULT_PADDING = DEFAULT_STROKE_WIDTH + 2F
        private const val DEFAULT_CLOCK_NUMBERS_SPACING = 36F

        private const val DEFAULT_SECOND_HAND_COLOR = Color.BLUE
        private const val DEFAULT_MINUTE_HAND_COLOR = Color.RED
        private const val DEFAULT_HOUR_HAND_COLOR = Color.BLACK
        private const val DEFAULT_CLOCK_COLOR = Color.BLACK

        private const val DEFAULT_SECOND_START_TIME = -1
        private const val DEFAULT_MINUTE_START_TIME = -1
        private const val DEFAULT_HOUR_START_TIME = -1
    }
}
