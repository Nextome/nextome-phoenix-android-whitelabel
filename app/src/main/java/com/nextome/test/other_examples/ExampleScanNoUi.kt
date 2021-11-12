package com.nextome.test.other_examples

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import com.nextome.test.NextomeSettings
import com.nextome.test.R
import com.nextome.test.helper.Constants
import com.nextome.test.helper.ForegroundNotificationHelper
import net.nextome.phoenix_sdk.facade.NextomePhoenixSdk
import net.nextome.phoenix_sdk.facade.NextomePhoenixState

class ExampleScanNoUi : FragmentActivity() {
    private lateinit var nextomeSdk: NextomePhoenixSdk

    private lateinit var settings: NextomeSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        parseSettingsFromIntent()

        initNextomeSdk()
        startLocalization()
        observeResults()
    }

    private fun initNextomeSdk() {
        // Initialize Nextome SDK (make sure to use Application context)
        with (settings) {
            nextomeSdk = NextomePhoenixSdk().Builder(applicationContext)
                .withSecret(secret)
                .withDeveloperKey(developerKey)
                .withBundle(bundle)
                .withForegroundScanPeriod(scanPeriod)
                .withForegroundBetweenScanPeriod(betweenScanPeriod)
                .withBeaconListMaxSize(beaconListMaxSize)
                .withLocalizationMethod(localizationMethod)
                .withRssiThreshold(rssiThreshold)
                .build()
        }

        // Add other beacon format support
        // nextomeSdk.addBeaconLayout(NextomeBeaconLayouts.BEACON_LAYOUT_IBEACON)

        // Start SDK
        // nextomeSdk.start()

        // Start SDK with Foreground Service
        // nextomeSdk.startForegroundService()
    }

    private fun startLocalization() {
        nextomeSdk.startForegroundService(300,
            ForegroundNotificationHelper.createNotification(this))
    }

    private fun observeResults() {
        nextomeSdk.localizationLiveData.observe(this, Observer {
            val floor = it.floorId
            val map = it.mapId

            log( "User Position is ${it.x}, ${it.y}")
        })

        nextomeSdk.stateLiveData.observe(this, {
            with (it) {

                if (it.isOutdoor) {
                    log("Outdoor mode enabled")
                } else {
                    log("Outdoor mode disabled")
                }

                when (state) {
                    NextomePhoenixState.STARTED -> log("Sdk Started")
                    NextomePhoenixState.SEARCH_VENUE -> log("Searching Venue...")
                    NextomePhoenixState.GET_PACKET -> log("Downloading packet...")
                    NextomePhoenixState.FIND_FLOOR -> log("Finding current Floor...")

                    NextomePhoenixState.RUNNING -> {
                        log("Nextome is running")

                        log("""
                                Downloaded tiles url: $mapTilesUrl
                                Map Height: $mapHeight
                                Map Width: $mapWidth
                            """.trimIndent()
                        )
                    }

                    else -> log("An error occurred")
                }
            }
        })
    }

    override fun onDestroy() {
        nextomeSdk.stop()
        super.onDestroy()
    }

    private fun parseSettingsFromIntent() {
        settings = intent.extras?.getParcelable(Constants.INTENT_EXTRA_SETTINGS) ?: NextomeSettings()
    }

    private fun log(message: String) {
        Log.i("NextomeExample", message)
    }
}