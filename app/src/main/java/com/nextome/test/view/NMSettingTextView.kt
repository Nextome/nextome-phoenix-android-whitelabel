package com.nextome.test.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doOnTextChanged
import com.nextome.test.R

class NMSettingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {

    private val defaultValueString by lazy {
        context.getString(R.string.generic_default)
    }

    private var userDefinedDefaultValue: String? = null

    private var labelName: String
    private var negativeInt: Boolean

    init {
        inflate(context, R.layout.nm_setting_edittext, this)

        context.theme.obtainStyledAttributes(
            attrs, R.styleable.NMSettingEditTextView, 0, 0).apply {
                try {
                    labelName = getString(R.styleable.NMSettingEditTextView_labelName) ?: ""
                    negativeInt = getBoolean(R.styleable.NMSettingEditTextView_negativeInt, false)
                } finally {
                    recycle()
                }
            }

        populateLayout()
    }

    private fun populateLayout() {
        findViewById<TextView>(R.id.nmSettingLabel).text = labelName
        findViewById<EditText>(R.id.nextomeSettingInput).doOnTextChanged { text, start, before, count ->
            if (text.toString() == getDefaultValue()) return@doOnTextChanged
            Log.e("Test default", "$text")
            if (negativeInt) {
                val firstChar = text?.firstOrNull()
                if (firstChar != '-') {
                    Log.e("test", "Value was $text, changing to -$text")
                    setValue("-$text")
                }
            }
            handleDefaultIndicator(text.toString())
        }

        findViewById<EditText>(R.id.nextomeSettingInput).setOnFocusChangeListener { view, hasFocus ->
            val text = findViewById<EditText>(R.id.nextomeSettingInput).text.toString()

            if (text.isEmpty() || !text.isDigitsOnlyWithNegative()) {
                if (negativeInt && text == "-") {
                    setValue(getDefaultValue())
                    return@setOnFocusChangeListener
                } else {
                    setValue(if (hasFocus) "" else getDefaultValue())
                }
            } else if (negativeInt && text == "-") { if (!hasFocus) setValue(getDefaultValue()) }
        }
    }

    private fun String.isDigitsOnlyWithNegative(): Boolean {
        if (firstOrNull() == '-') { return replaceFirstChar { "" }.isDigitsOnly() }
        return isDigitsOnly()
    }

    fun getDefaultValue(): String = userDefinedDefaultValue ?: defaultValueString
    fun setDefaultValue(value: String) {
        userDefinedDefaultValue = value
    }

    fun setLabel(text: String) {
        findViewById<TextView>(R.id.nmSettingLabel).text = text
    }

    fun setValue(value: String){
        findViewById<EditText>(R.id.nextomeSettingInput).setText(value)
        handleDefaultIndicator(value)
    }

    fun getValue(): String = findViewById<EditText>(R.id.nextomeSettingInput).text.toString()

    override fun setEnabled(value: Boolean) {
        findViewById<EditText>(R.id.nextomeSettingInput).isEnabled = value
    }

    private fun handleDefaultIndicator(text: String?) {
        findViewById<ImageView>(R.id.editedImageView).visibility =
            if (text.isNullOrBlank() ||
                text.toString().equals(getDefaultValue(),
                    ignoreCase = true)) { GONE } else { VISIBLE }
    }
}