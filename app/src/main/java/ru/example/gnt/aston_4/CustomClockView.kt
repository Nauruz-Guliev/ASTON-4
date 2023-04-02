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
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.graphics.drawable.DrawableCompat
import java.util.*
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

    private var fontSize: Float? = null

    private lateinit var calendar: Calendar

    private var displayCurrentTime: Boolean? = null

    init {
        calendar = Calendar.getInstance()
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
        displayCurrentTime = typedArray.getBoolean(
            R.styleable.CustomClockView_display_current_time,
            false
        )
        var startSecondTime = typedArray.getColor(
            R.styleable.CustomClockView_second_start_time,
            DEFAULT_SECOND_START_TIME
        )

        var startMinuteTime = typedArray.getColor(
            R.styleable.CustomClockView_minute_start_time,
            DEFAULT_MINUTE_START_TIME
        )
        var startHourTime = typedArray.getColor(
            R.styleable.CustomClockView_hour_start_time,
            DEFAULT_HOUR_START_TIME
        )

        if (displayCurrentTime == true) {
            startHourTime = (calendar.get(Calendar.HOUR) * 5) - 1
            startMinuteTime = -startHourTime + calendar.get(Calendar.MINUTE) - 1
            startSecondTime = -startMinuteTime + calendar.get(Calendar.SECOND) - 1
        }


        hourHandInfo = ClockHandInfo(
            timeValue = startHourTime,
            drawable = context.getDrawable(R.drawable.hour_hand)!!,
            repeatIntervalInMilliseconds = 3600000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_hour_hand_color,
                DEFAULT_HOUR_HAND_COLOR
            ),
            isHour = true,
            angle = 0F
        )
        minuteHandInfo = ClockHandInfo(
            timeValue = startMinuteTime,
            drawable = context.getDrawable(R.drawable.minute_hand)!!,
            repeatIntervalInMilliseconds = 60000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_minute_hand_color,
                DEFAULT_MINUTE_HAND_COLOR
            ),
            isHour = false,
            angle = 0F
        )
        secondHandInfo = ClockHandInfo(
            timeValue = startSecondTime,
            drawable = context.getDrawable(R.drawable.second_hand)!!,
            repeatIntervalInMilliseconds = 1000,
            color = typedArray.getColor(
                R.styleable.CustomClockView_second_hand_color,
                DEFAULT_SECOND_HAND_COLOR
            ),
            isHour = false,
            angle = 0F
        )


        clockColor = typedArray.getColor(
            R.styleable.CustomClockView_clock_color,
            DEFAULT_CLOCK_COLOR
        )

        fontSize = typedArray.getDimension(
            R.styleable.CustomClockView_numbers_font_size,
            DEFAULT_FONT_SIZE
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
                left = (viewWidth.toFloat() / 2.9).toInt(),
                top = clockRadius.toInt() - DEFAULT_PADDING.toInt(),
                right = viewWidth - (viewWidth / 2.9).toInt(),
                bottom = clockRadius.toInt() + DEFAULT_PADDING.toInt() / 5
            )
        }
        hourHandInfo?.apply {
            dimension = ClockHandRectangleBounds(
                left = (viewWidth / (3.5)).toInt(),
                top = clockRadius.toInt() - DEFAULT_PADDING.toInt() * 4,
                right = viewWidth - (viewWidth / 3.5).toInt(),
                bottom = clockRadius.toInt() + DEFAULT_PADDING.toInt() / 2
            )
        }
    }

    /**
     * @return List of ClockNumber instances that contain necessary information to draw a number
     */
    private fun getClockNumberList(): List<ClockNumber> {
        viewPaint.textSize = fontSize ?: DEFAULT_FONT_SIZE
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
            canvas.rotate(angle, viewCenterY, viewCenterY)
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

    private inner class ClockHandInfo(
        var timeValue: Int,
        var drawable: Drawable,
        var color: Int? = null,
        var angle: Float,
        val isHour: Boolean,
        var dimension: ClockHandRectangleBounds? = null,
        var repeatIntervalInMilliseconds: Int,
    ) {
        init {
            DrawableCompat.setTint(this.drawable, color ?: Color.BLACK)
            myHandler.postDelayed(object : Runnable {
                override fun run() {
                    timeValue++
                    angle = if (isHour) {
                        timeValue * 6F
                    } else {
                        timeValue * 6F
                    }
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

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()!!
        val savedState = SavedState(superState)
        savedState.currentSecond = secondHandInfo?.timeValue ?: DEFAULT_SECOND_START_TIME
        savedState.currentMinute = minuteHandInfo?.timeValue ?: DEFAULT_MINUTE_START_TIME
        savedState.currentHour = hourHandInfo?.timeValue ?: DEFAULT_HOUR_HAND_COLOR
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        secondHandInfo!!.timeValue = savedState.currentSecond - 1
        minuteHandInfo!!.timeValue = savedState.currentMinute - 1
        hourHandInfo!!.timeValue = savedState.currentHour - 1
    }

    class SavedState : BaseSavedState {

        var currentSecond by Delegates.notNull<Int>()
        var currentMinute by Delegates.notNull<Int>()
        var currentHour by Delegates.notNull<Int>()


        constructor(superState: Parcelable) : super(superState)
        constructor(parcel: Parcel) : super(parcel) {
            currentSecond = parcel.readInt()
            currentMinute = parcel.readInt()
            currentHour = parcel.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(currentSecond)
            out.writeInt(currentMinute)
            out.writeInt(currentHour)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = Array(size) { null }
            }
        }

    }

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

        private const val DEFAULT_SECOND_START_TIME = 28
        private const val DEFAULT_MINUTE_START_TIME = 22
        private const val DEFAULT_HOUR_START_TIME = 7
    }
}
