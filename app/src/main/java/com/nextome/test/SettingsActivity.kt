package com.nextome.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.widget.ArrayAdapter
import com.nextome.test.helper.Constants.Companion.INTENT_EXTRA_SETTINGS
import kotlinx.android.synthetic.main.activity_settings.*
import net.nextome.phoenix_sdk.facade.NextomeLocalizationMethod


class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        populateLocalizationSpinner()

        startScanningButton.setOnClickListener {
            try {
                startExample()
            } catch (e: Exception) {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startExample() {
        val intent = Intent(this, MapActivity::class.java)

        val inputScanPeriod = inputScanPeriod.getLongValue()
        val inputBetweenScanPeriod = inputBetweenScanPeriod.getLongValue()
        val inputRssiThreshold = inputRssiThreshold.getIntValue()
        val inputBeaconListMaxSize = inputBeaconListMaxSize.getIntValue()
        val particleActive = activateParticle.isChecked
        val sendPositionToServer = sendToServer.isChecked
        val sendAssetsToServer = sendAssetsToServer.isChecked

        val inputLocalizationMethod = when (localizationSpinner.selectedItemPosition) {
            0 -> NextomeLocalizationMethod.LINEAR_SVD
            1 -> NextomeLocalizationMethod.NON_LINEAR_DA
            2 -> NextomeLocalizationMethod.PARTICLE
            else -> NextomeLocalizationMethod.LINEAR_SVD
        }

        with(intent) {
            // Pass scan parameters
            putExtra(INTENT_EXTRA_SETTINGS, NextomeSettings(
                    scanPeriod = inputScanPeriod,
                    betweenScanPeriod = inputBetweenScanPeriod,
                    beaconListMaxSize = inputBeaconListMaxSize,
                    rssiThreshold = inputRssiThreshold,
                    localizationMethod = inputLocalizationMethod,
                    isParticleActive = particleActive,
                    isSendPositionToServerEnabled = sendPositionToServer,
                    isSendAssetsToServerEnabled = sendAssetsToServer,
            ))
        }

        startActivity(intent)
        finish()
    }

    private fun populateLocalizationSpinner() {
        ArrayAdapter.createFromResource(
                this,
                R.array.localization_method_array,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            localizationSpinner.adapter = adapter
        }
    }
}

private fun EditText.getIntValue(): Int {
    return this.text.toString().toInt()
}

private fun EditText.getLongValue(): Long {
    return this.text.toString().toLong()
}
