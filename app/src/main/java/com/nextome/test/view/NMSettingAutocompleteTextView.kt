package com.nextome.test.view

import android.content.Context
import android.util.AttributeSet
import android.widget.*
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputLayout
import com.nextome.test.R

class NMSettingAutocompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {

    private val defaultValueString by lazy {
        context.getString(R.string.generic_default)
    }

    private var labelName: String

    private var userDefinedDefaultValue: String? = null

    init {
        inflate(context, R.layout.nm_setting_autocomplete, this)

        context.theme.obtainStyledAttributes(
            attrs, R.styleable.NMSettingAutocompleteTextView, 0, 0).apply {
            try {
                labelName = getString(R.styleable.NMSettingAutocompleteTextView_autocompleteLabelName) ?: ""
            } finally {
                recycle()
            }
        }

        popolateLayout()
    }

    private fun popolateLayout() {
        findViewById<TextView>(R.id.nmSettingLabel).text = labelName
        findViewById<EditText>(R.id.autocompleteTextInput).doOnTextChanged { text, start, before, count ->
            handleDefaultIndicator(text.toString())
        }
    }

    fun getDefaultValue(): String = userDefinedDefaultValue ?: defaultValueString
    fun setDefaultValue(value: String) {
        userDefinedDefaultValue = value
    }

    fun setLabel(text: String) {
        findViewById<TextView>(R.id.nmSettingLabel).text = text
    }

    override fun setEnabled(enabled: Boolean) {
        findViewById<EditText>(R.id.autocompleteTextInput).isEnabled = enabled
        findViewById<TextInputLayout>(R.id.nmTextInputLayout).isEnabled = enabled
    }

    fun setValue(value: String){
        findViewById<EditText>(R.id.autocompleteTextInput).setText(value)
        handleDefaultIndicator(value)
    }

    fun getValue(): String = findViewById<EditText>(R.id.autocompleteTextInput).text.toString()

    fun setAdapterWithItems(items: List<String>, context: Context) {
        findViewById<AutoCompleteTextView>(R.id.autocompleteTextInput).setAdapter(
            ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line, items.toTypedArray())
        )
    }

    private fun handleDefaultIndicator(text: String?) {
        findViewById<ImageView>(R.id.editedImageView).visibility =
            if (text.isNullOrBlank() ||
                text.toString().equals(getDefaultValue(), ignoreCase = true)) { GONE } else { VISIBLE }
    }
}