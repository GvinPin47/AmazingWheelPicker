package com.test.testwheelviewlib

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE
import android.text.style.*
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


open class MainActivity : AppCompatActivity() {

    private var pickedHourText: String = ""
    private var pickedMinutesText: String = ""
    private val pickedHourAndTimeText get() = "$pickedHourText $pickedMinutesText"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pickedTimeTextView = findViewById<TextView>(R.id.pickedTimeTextView)
        val hoursWheelView = findViewById<WheelView<CharSequence>>(R.id.hoursWheelView)
        val minutesWheelView = findViewById<WheelView<String>>(R.id.minutesWheelView)

        val itemsHours = listOf(
            "1ч".changePartSize(1),
            "2ч".changePartSize(1),
            "3ч".changePartSize(1),
            "4ч".changePartSize(1)
        )

        val itemsMinutes = listOf(
            "Понедельник",
            "Вторник",
            "Среда",
            "Четверг",
            "Пятница",
            "Суббота",
            "Воскресенье",
        )


        hoursWheelView.visibleItems = 3
        hoursWheelView.isCyclic = true
        hoursWheelView.isCurved = false
        hoursWheelView.data  = itemsHours


        hoursWheelView.setOnItemSelectedListener { wheelView, data, position ->
            pickedHourText = data.toString()
            pickedTimeTextView.text = pickedHourAndTimeText
        }


        minutesWheelView.setNormalItemTextColorRes(R.color.test1_not_selected_color)
        minutesWheelView.setSelectedItemTextColorRes(R.color.test1_selected_color)
        minutesWheelView.lineSpacing = dp2px(10f)
        minutesWheelView.isCyclic = true
        minutesWheelView.isCurved = true
        minutesWheelView.textAlign = WheelView.TEXT_ALIGN_RIGHT
        minutesWheelView.visibleItems = 7
        minutesWheelView.refractRatio = 0.8f
        minutesWheelView.setTypeface(Typeface.DEFAULT, true)

        minutesWheelView.data  = itemsMinutes
        minutesWheelView.setTextSize(24f, true)

        minutesWheelView.setOnItemSelectedListener { wheelView, data, position ->
            pickedMinutesText = data.toString()
            pickedTimeTextView.text = pickedHourAndTimeText
        }
    }


    protected fun dp2px(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            Resources.getSystem().displayMetrics
        )
    }


    private fun String.changePartSize(lastSymbolsLength: Int): CharSequence {
        val spString = SpannableString(this)
        spString.setSpan(AbsoluteSizeSpan(64f.spToPx()), 0, this.length - lastSymbolsLength, SPAN_INCLUSIVE_INCLUSIVE)
        spString.setSpan(StyleSpan(Typeface.BOLD), 0, this.length - lastSymbolsLength, SPAN_INCLUSIVE_INCLUSIVE)
        spString.setSpan(AbsoluteSizeSpan(12f.spToPx()), this.length - lastSymbolsLength, this.length, SPAN_INCLUSIVE_INCLUSIVE)
        spString.setSpan(StyleSpan(Typeface.NORMAL), this.length - lastSymbolsLength, this.length, SPAN_INCLUSIVE_INCLUSIVE)
        spString.setSpan(ForegroundColorSpan(ContextCompat.getColor(this@MainActivity, R.color.test_color)), 0, this.length, SPAN_INCLUSIVE_INCLUSIVE)
        return spString
    }



    private fun Float.spToPx() : Int = (this * resources.displayMetrics.scaledDensity).toInt()
}