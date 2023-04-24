package com.nextome.test.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import net.nextome.phoenix.models.packages.NextomeSettings
import com.nextome.test.R
import com.nextome.test.databinding.ActivitySettingsBinding
import com.nextome.test.helper.NmSerialization.asJson
import com.nextome.test.helper.NmSerialization.fromJson
import org.koin.androidx.viewmodel.ext.android.viewModel


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModel()

    private val stringDefaultValue by lazy { getString(R.string.generic_default) }

    companion object {
        private const val DEFAULT_SETTINGS_KEY = "DEF_SETTINGS_KEY"
        private const val DEFAULT_READONLY_KEY = "READONLY_SETTINGS_KEY"

        fun getIntent(
            defaultSettings: NextomeSettings? = null,
            isReadOnly: Boolean = false,
            ctx: Context,
        ): Intent {
            return Intent(ctx, SettingsActivity::class.java).apply {
                putExtra(DEFAULT_SETTINGS_KEY, defaultSettings.asJson())
                putExtra(DEFAULT_READONLY_KEY, isReadOnly)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateDropdowns()
        setVenueDefaultSettingsFromIntentIfAvailable(intent)

        populateUiWithSettings(viewModel.getCachedSettings())

        setReadOnlyWithIntent(intent)

        with(binding) {
            toolbar.setNavigationOnClickListener {
                finish()
            }

            saveButton.setOnClickListener {
                viewModel.saveSettings(getSettingsFromInputValues())
                finish()
            }

            restoreButton.setOnClickListener {
                viewModel.clearSettings()
                finish()
            }
        }
    }

    private fun setReadOnlyWithIntent(intent: Intent?) {
        intent?.getBooleanExtra(DEFAULT_READONLY_KEY, false)?.let { readOnly ->
            with (binding) {
                tipTextView.text = if (readOnly) {
                    getString(R.string.settings_tip_readonly)
                } else {
                    getString(R.string.settings_tip)
                }

                scanPeriodSetting.isEnabled = !readOnly
                betweenScanPeriodSetting.isEnabled = !readOnly
                beaconListMaxSizeSetting.isEnabled = !readOnly
                rssiSetting.isEnabled = !readOnly
                sendAssetsSetting.isEnabled = !readOnly
                sendPositionsSetting.isEnabled = !readOnly
                eventTimeoutSetting.isEnabled = !readOnly

                saveButton.isVisible = !readOnly
                restoreButton.isVisible = !readOnly
            }
        }
    }

    private fun setVenueDefaultSettingsFromIntentIfAvailable(intent: Intent?) {
        try {
            val settings = intent?.getStringExtra(DEFAULT_SETTINGS_KEY)?.fromJson<NextomeSettings>()
                ?: return

            with(binding) {
                settings.scanPeriod.toStringOrNull()?.let {
                    scanPeriodSetting.setDefaultValue(it)
                    scanPeriodSetting.setValue(it)
                }

                settings.betweenScanPeriod.toStringOrNull()?.let {
                    betweenScanPeriodSetting.setDefaultValue(it)
                    betweenScanPeriodSetting.setValue(it)
                }

                settings.beaconListMaxSize.toString()?.let {
                    beaconListMaxSizeSetting.setDefaultValue(it)
                    beaconListMaxSizeSetting.setValue(it)
                }

                settings.rssiThreshold.toString()?.let {
                    rssiSetting.setDefaultValue(it)
                    rssiSetting.setValue(it)
                }

                settings.eventsTimeout.toStringOrNull()?.let {
                    eventTimeoutSetting.setDefaultValue(it)
                    eventTimeoutSetting.setValue(it)
                }

                settings.sendAssetsToServer.toInputSetting().let {
                    sendAssetsSetting.setDefaultValue(it)
                    sendAssetsSetting.setValue(it)
                }

                settings.sendPositionsToServer.toInputSetting().let {
                    sendPositionsSetting.setDefaultValue(it)
                    sendPositionsSetting.setValue(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // no default settings
        }
    }

    private fun populateUiWithSettings(cachedSettings: AppOverriddenSettings?) {
        if (cachedSettings == null) return else setInputValuesFromSettings(cachedSettings)
    }

    private fun getSettingsFromInputValues(): AppOverriddenSettings = with(binding) {
        return AppOverriddenSettings(
            scanPeriod = scanPeriodSetting.getValue().getLongSettingOrNull(),
            betweenScanPeriod = betweenScanPeriodSetting.getValue().getLongSettingOrNull(),
            beaconListMaxSize =  beaconListMaxSizeSetting.getValue().getIntSettingOrNull(),
            rssiThreshold = rssiSetting.getValue().getIntSettingOrNull(),
            sendAssetsToServer = sendAssetsSetting.getValue().getBoolSettingOrNull(),
            sendPositionToServer = sendPositionsSetting.getValue().getBoolSettingOrNull(),
            eventTimeout = eventTimeoutSetting.getValue().getLongSettingOrNull(),
            isDebugModeEnabled = debugMode.getValue()
        )
    }

    private fun setInputValuesFromSettings(settings: AppOverriddenSettings) = with (binding) {

        settings.scanPeriod?.toString()?.let { scanPeriodSetting.setValue(it) }
        settings.betweenScanPeriod?.toString()?.let { betweenScanPeriodSetting.setValue(it) }
        settings.beaconListMaxSize?.toString()?.let { beaconListMaxSizeSetting.setValue(it) }
        settings.rssiThreshold?.toString()?.let { rssiSetting.setValue(it) }
        settings.eventTimeout?.toString()?.let { eventTimeoutSetting.setValue(it) }

        settings.sendAssetsToServer?.let { sendAssetsSetting.setValue(it.toInputSetting()) }
        settings.sendPositionToServer?.let { sendPositionsSetting.setValue(it.toInputSetting()) }
        debugMode.setValue(settings.isDebugModeEnabled)
    }.also { populateDropdowns() }

    private fun populateDropdowns() {
        with(binding) {
            sendAssetsSetting.setAdapterWithItems(
                listOf(stringDefaultValue, getString(R.string.generic_yes), getString(R.string.generic_no)),
                this@SettingsActivity)

            sendPositionsSetting.setAdapterWithItems(
                listOf(stringDefaultValue, getString(R.string.generic_yes), getString(R.string.generic_no)),
                this@SettingsActivity)
        }
    }

    private fun Long?.toStringOrNull(): String? = this?.toString()
    private fun String.getBoolSettingOrNull(): Boolean? {
        return when (this) {
            getString(R.string.generic_yes) -> true
            getString(R.string.generic_no) -> false
            else -> { null }
        }
    }

    private fun Boolean?.toInputSetting(): String {
        return when (this) {
            true -> { getString(R.string.generic_yes) }
            false -> { getString(R.string.generic_no) }
            else -> stringDefaultValue
        }
    }

    private fun String.getLongSettingOrNull(): Long? {
        return if (this == stringDefaultValue) {
            null
        } else {
            this.toLongOrNull()
        }
    }

    private fun String.getIntSettingOrNull(): Int? {
        return if (this == stringDefaultValue) {
            null
        } else {
            this.toIntOrNull()
        }
    }
}