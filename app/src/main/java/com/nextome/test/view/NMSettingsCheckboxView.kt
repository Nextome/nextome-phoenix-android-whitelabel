package com.nextome.test.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import com.nextome.test.R

class NMSettingsCheckboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {

    private var userDefinedDefaultValue: Boolean? = null

    private var labelName: String
    private var defaultValue: Boolean

    init {
        inflate(context, R.layout.nm_settings_switch, this)

        context.theme.obtainStyledAttributes(
            attrs, R.styleable.NMSettingCheckboxView, 0, 0).apply {
                try {
                    labelName = getString(R.styleable.NMSettingCheckboxView_checkboxLabel) ?: ""
                    defaultValue = getBoolean(R.styleable.NMSettingCheckboxView_checkBoxDefaultValue, false)
                } finally {
                    recycle()
                }
            }

        populateLayout()
    }

    private fun populateLayout() {
        findViewById<TextView>(R.id.nmSettingsSwitchLabel).text = labelName
        findViewById<SwitchCompat>(R.id.nmSettingsSwitch).setOnCheckedChangeListener { _, isChecked ->
            findViewById<ImageView>(R.id.nmSettingsSwitchUpdatedImage).isVisible = defaultValue != isChecked

        }
    }

    fun getDefaultValue(): Boolean = userDefinedDefaultValue ?: defaultValue

    fun setDefaultValue(value: Boolean) {
        userDefinedDefaultValue = value
    }

    fun setLabel(text: String) {
        findViewById<TextView>(R.id.nmSettingsSwitchLabel).text = text
    }

    fun setValue(value: Boolean){
        findViewById<SwitchCompat>(R.id.nmSettingsSwitch).isChecked = value
        handleDefaultIndicator(value)
    }

    fun getValue(): Boolean = findViewById<SwitchCompat>(R.id.nmSettingsSwitch).isChecked

    override fun setEnabled(value: Boolean) {
        findViewById<EditText>(R.id.nextomeSettingInput).isEnabled = value
    }

    private fun handleDefaultIndicator(value: Boolean) {
        findViewById<ImageView>(R.id.nmSettingsSwitchUpdatedImage).isVisible =
            value != getDefaultValue()
    }
}