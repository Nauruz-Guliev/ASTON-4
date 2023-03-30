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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates


@SuppressLint("UseCompatLoadingForDrawables")
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

    private var hourHandSize by Delegates.notNull<Float>()
    private var handSize by Delegates.notNull<Float>()

    private var seconds: Int = -1
    private var minutes: Int = -1

    private var myHandler: Handler

    private lateinit var secondHandDrawable: Drawable
    private lateinit var minuteHandDrawable: Drawable
    private lateinit var hourHandDrawable: Drawable



    init {
        viewPaint = Paint(ANTI_ALIAS_FLAG)
        viewNumbers = IntRange(1, 12).step(1).toList().toIntArray()
        viewRectangle = Rect()
        updateViewSizes()
        myHandler = Handler(Looper.getMainLooper())
        initSecondHand()
        initMinuteHand()
    }

    private fun initSecondHand() {
        secondHandDrawable = context.getDrawable(R.drawable.second_hand)!!
        DrawableCompat.setTint(secondHandDrawable, ContextCompat.getColor(context, R.color.blue))
        myHandler.postDelayed(object : Runnable {
            override fun run() {
                seconds++
                myHandler.postDelayed(this, 1000)
            }
        }, 0)
    }

    private fun initMinuteHand() {
        minuteHandDrawable = context.getDrawable(R.drawable.second_hand)!!
        DrawableCompat.setTint(minuteHandDrawable, ContextCompat.getColor(context, R.color.black))
        myHandler.postDelayed(object : Runnable {
            override fun run() {
                minutes++
                myHandler.postDelayed(this, 60000)
            }
        }, 0)
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
            color = resources.getColor(R.color.black, context.theme)
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

        hourHandSize = clockRadius - clockRadius / 2
        handSize = clockRadius - clockRadius / 4
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

    private fun drawSecondsHand(canvas: Canvas, location: Float) {
        secondHandDrawable.setBounds(
            viewCenterX.toInt() - (clockRadius / 4).toInt(),
            viewCenterY.toInt() - 12,
            viewCenterX.toInt() + clockRadius.toInt() - 20,
            viewCenterY.toInt() + 12
        )
        canvas.rotate(location * 6f, viewCenterX, viewCenterY);
        secondHandDrawable.draw(canvas)
    }

    private fun drawMinutesHand(canvas: Canvas, location: Float) {
        minuteHandDrawable.setBounds(
            viewCenterX.toInt() - (clockRadius / 4).toInt(),
            viewCenterY.toInt() - 12,
            viewCenterX.toInt() + clockRadius.toInt() - 20,
            viewCenterY.toInt() + 12
        )
        canvas.rotate(location * 6f, viewCenterY, viewCenterY);
        minuteHandDrawable.draw(canvas)
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
        drawNumbers(canvas)
    //    drawMinutesHand(canvas, minutes.toFloat())
        canvas.rotate(-45F, (secondHandDrawable.intrinsicWidth/2).toFloat(),
            (secondHandDrawable.intrinsicHeight/2).toFloat()
        )
        drawSecondsHand(canvas, seconds.toFloat())
        postInvalidateDelayed(500)
    }

    companion object {
        private const val HALF_DIVIDER = 2F
        private const val DEFAULT_STROKE_WIDTH = 14F
        private const val HOUR_NUMBERS_STROKE_WIDTH = 4F
        private const val DEFAULT_FONT_SIZE = 40F
        private const val DEFAULT_PADDING = DEFAULT_STROKE_WIDTH + 2F
        private const val DEFAULT_CLOCK_NUMBERS_SPACING = 36F
    }

    inner class ClockNumber(
        val xPosition: Float,
        val yPosition: Float,
        val number: String,
        val paint: Paint
    )
}
