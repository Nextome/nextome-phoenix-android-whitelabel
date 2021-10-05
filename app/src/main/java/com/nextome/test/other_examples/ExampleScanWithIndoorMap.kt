package com.nextome.test.other_examples

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.nextome.test.NextomeSettings
import com.nextome.test.R
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import kotlinx.android.synthetic.main.activity_scanning.*
import com.nextome.test.helper.Constants.Companion.INTENT_EXTRA_SETTINGS
import com.nextome.test.helper.ForegroundNotificationHelper
import net.nextome.phoenix_sdk.facade.NextomePhoenixSdk
import net.nextome.phoenix_sdk.facade.NextomePhoenixState
import net.nextome.phoenix_sdk.models.NextomePosition

class ExampleScanWithIndoorMap : AppCompatActivity() {

    companion object {
        // Define a tag String to represent the FlutterFragment within this
        // Activity's FragmentManager. This value can be whatever you'd like.
        private const val TAG_FLUTTER_FRAGMENT = "flutter_fragment"
    }

    // Declare a local variable to reference the FlutterFragment so that you
    // can forward calls to it later.
    private var flutterFragment: FlutterFragment? = null

    private lateinit var settings: NextomeSettings

    private lateinit var nextomeSdk: NextomePhoenixSdk

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
            val mapId = it.mapId
            updatePositionOnFlutterMap(it)
        })

        nextomeSdk.currentState.asLiveData().observe(this, Observer {
            updateState("Is outdoor: ${it.isOutdoor}")

                when (it.state) {
                    NextomePhoenixState.STARTED -> updateState("Sdk Started")
                    NextomePhoenixState.SEARCH_VENUE -> updateState("Searching Venue...")
                    NextomePhoenixState.GET_PACKET -> updateState("Downloading packet...")
                    NextomePhoenixState.FIND_FLOOR -> updateState("Finding current Floor...")

                    NextomePhoenixState.RUNNING -> {
                        updateState("Showing map...")

                        with (it) {
                            initFlutter()
                            showFlutterMap(mapTilesUrl, mapHeight, mapWidth)
                        }
                    }

                    else -> { }
                }
        })
    }

    private fun showFlutterMap(mapTilesUrl: String, mapHeight: Int, mapWidth: Int) {
        Toast.makeText(applicationContext,
                "Cambio piano rilevato: $mapTilesUrl", Toast.LENGTH_LONG).show()

        showMap(mapTilesUrl, mapHeight, mapWidth)
    }

    private fun updatePositionOnFlutterMap(it: NextomePosition) {
        channel.invokeMethod("position", "${it.x},${it.y}")
    }

    lateinit var channel: MethodChannel

    private fun initFlutter() {
        val engine = FlutterEngine(this)

        // Start executing Dart code in the FlutterEngine.
        engine.dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault()
        )

        // Cache the pre-warmed FlutterEngine to be used later by FlutterFragment.
        FlutterEngineCache
                .getInstance()
                .put("net.nextome.phoenix", engine)

        val fragmentManager: FragmentManager = supportFragmentManager

        // Attempt to find an existing FlutterFragment, in case this is not the
        // first time that onCreate() was run.
        flutterFragment = fragmentManager
                .findFragmentByTag(TAG_FLUTTER_FRAGMENT) as FlutterFragment?


        // Create and attach a FlutterFragment if one does not exist.
        if (flutterFragment == null) {
            var newFlutterFragment = FlutterFragment.
            withCachedEngine("net.nextome.phoenix").build<FlutterFragment>()

            flutterFragment = newFlutterFragment
            fragmentManager
                    .beginTransaction()
                    .add(
                        R.id.rootLayout,
                            newFlutterFragment,
                            TAG_FLUTTER_FRAGMENT
                    ).commit()
        }

        channel = MethodChannel(engine.dartExecutor, "net.nextome.phoenix")
    }

    private fun showMap(mapTilesUrl: String, mapHeight: Int, mapWidth: Int) {
        Log.e("nextome", "Tiles url: $mapTilesUrl")
        Log.e("nextome", "Height: $mapHeight")
        Log.e("nextome", "Width: $mapWidth")

        // Send local package data to Flutter
        channel.invokeMethod("localPackageUrl",
                "$mapTilesUrl,$mapHeight,$mapWidth,3")


        // Handle method call from Flutter side
        channel.setMethodCallHandler { methodCall, result -> }
    }

    private fun updateState(message: String) { stateView.text = message }

    private fun parseSettingsFromIntent() {
        settings = intent.extras?.getParcelable(INTENT_EXTRA_SETTINGS) ?: NextomeSettings()
    }

    override fun onDestroy() {
        nextomeSdk.stop()
        super.onDestroy()
    }
}
