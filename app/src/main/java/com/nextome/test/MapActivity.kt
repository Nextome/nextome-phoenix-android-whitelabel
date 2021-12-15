package com.nextome.test

import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.bugsnag.android.Bugsnag
import com.google.gson.Gson
import com.nextome.test.helper.Constants
import com.nextome.test.helper.ForegroundNotificationHelper
import com.nextome.test.helper.flutter.FlutterPoi
import com.nextome.test.helper.flutter.FlutterUtils
import com.nextome.test.helper.flutter.asNextomePoi
import com.nextome.test.poilist.PoiListContract
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import kotlinx.android.synthetic.main.activity_example_outdoor_mode.*
import net.nextome.phoenix_sdk.core.helper.NMLogManager
import net.nextome.phoenix_sdk.facade.NextomePhoenixSdk
import net.nextome.phoenix_sdk.facade.NextomePhoenixState
import net.nextome.phoenix_sdk.legacy.nextome.data.models.NMPoi
import net.nextome.phoenix_sdk.models.NextomePosition
import net.nextome.phoenix_sdk.models.packages.NextomePoi
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapActivity : AppCompatActivity() {

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

    private lateinit var poiListActivity: ActivityResultLauncher<List<NextomePoi>>
    private var lastPosition = NextomePosition()
    private var poiList: List<NextomePoi> = listOf()

    // Save if we're showing a path in map
    // In this case, we need to update the path each time the user moves
    private var isShowingPath = false
    private var targetPathPoi: NextomePoi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example_outdoor_mode)

        registerContracts()

        parseSettingsFromIntent()

        initOpenStreetMaps()
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
                .withBackgroundScanPeriod(scanPeriod)
                .withBackgroundBetweenScanPeriod(betweenScanPeriod)
                .withRssiThreshold(rssiThreshold)
                .withBeaconListMaxSize(beaconListMaxSize)
                .withLocalizationMethod(localizationMethod)
                .withParticleActive(isParticleActive)
                .withSendPositionToServer(isSendPositionToServerEnabled)
                .withSendAssetsToServer(isSendAssetsToServerEnabled)
                .withEventTimeoutDurationInSeconds(10L)
                .build()
        }

        // Add other beacon format support
        // nextomeSdk.addBeaconLayout(NextomeBeaconLayouts.BEACON_LAYOUT_IBEACON)

        // Start SDK
        // nextomeSdk.start()

        // Start SDK with Foreground Service
        // nextomeSdk.startForegroundService()
    }

    /**
     * Starts Nextome Foreground Service.
     */
    private fun startLocalization() {
        nextomeSdk.startForegroundService(300,
                ForegroundNotificationHelper.createNotification(this))

        initFlutter()
    }

    private fun observeResults() {
        /**
         * Observes user location relative to a map and floor;
         */
        nextomeSdk.localizationLiveData.observe(this, {
            lastPosition = it
            updatePositionOnFlutterMap(it)
        })

        /**
         * Observes user outdoor location in (Lat, Lng)
         */
        nextomeSdk.outdoorLocalizationLiveData.observe(this, {
            NMLogManager.log( "Got outdoor position (${it.lat}, ${it.lng})")
        })

        /**
         * Observe changes in the Nextome SDK state.
         * Available states:
         * STARTED - Nextome has been correctly initialized and it's ready to scan beacons;
         *
         * SEARCH_VENUE - Nextome is currently scanning nearby beacons to determine in which venue
         * the user is;
         * If the SDK is stuck here, you're probably outdoor. You can listen to outdoor position updates
         * using nextomeSdk.outdoorLocalizationLiveData
         *
         * GET_PACKET - Nextome knows the venue of the user and it's downloading from the server
         * the associated resources (Maps, POIs, Patches...);
         *
         * FIND_FLOOR - All the venue resoruces have been downloaded. Nextome is now computing in
         * which floor the user is;
         *
         * RUNNING - Nextome SDK is computing user positions. You can observe live user location using
         * the observer nextomeSdk.locationLiveData;
         *
         * If the user changes floor, the SDK will resume from FIND_FLOOR state.
         * If the user goes outdoor, the SDK will resume from SEARCH_VENUE state.
         */

        nextomeSdk.stateLiveData.observe(this, {
            if (it.isOutdoor) {
                showOpenStreetMap()
            } else {
                showIndoorMap()
            }

            when (it.state) {
                NextomePhoenixState.STARTED -> updateState("Sdk Started")
                NextomePhoenixState.SEARCH_VENUE -> updateState("Searching Venue...")
                NextomePhoenixState.GET_PACKET -> updateState("Downloading packet...")
                NextomePhoenixState.FIND_FLOOR -> {
                    updateState("Finding current Floor...")
                }

                NextomePhoenixState.RUNNING -> {
                    updateState("Showing map...")

                    with(it) {
                        setIndoorMap(mapTilesUrl, mapHeight, mapWidth, venueResources.getPoisByMapId(mapId))
                        poiList = venueResources.allPois
                    }

                    observeMapEvents()
                }

                else -> { }
            }
        })

        nextomeSdk.errorObservable.observe(this) {
            Bugsnag.addMetadata("message", "description", it.customizedMessage)
            Bugsnag.notify(it.exception)
        }

        /**
         * Observe geofencing with events
         */
        nextomeSdk.enterEventObservable.observe(this) { event ->
            Log.e("event_test", "Received on enter with data: ${event.data}")
        }

        nextomeSdk.exitEventObservable.observe(this) { event ->
            Log.e("event_test", "Received on exit with data: ${event.data}")
        }

    }

    /**
     * Notify a change of user position to Flutter.
     *
     * Flutter Map will automatically be updated with the user new location.
     */
    private fun updatePositionOnFlutterMap(it: NextomePosition) {
        channel.invokeMethod("position", FlutterUtils.getPositionPayload(it.x, it.y))

        if (isShowingPath) {
            targetPathPoi?.let {
                showPathOnMap(lastPosition.x, lastPosition.y, lastPosition.mapId,
                    it.x!!, it.y!!, it.map!!)
            }
        }
    }

    /**
     * Example method to add a list of Points of Interest to the map
     */
    fun addExamplePoiOnMap() {
        showPoiOnMap(listOf(
                NextomePoi().apply {
                    id = 11
                    x = 5800.0
                    y = 4000.1
                    name = "Test poi name"
                    description = "Test Description"
                }
        ))
    }

    /**
     * Notify Flutter of new POIs to show on the map
     */
    private fun showPoiOnMap(poiList: List<NextomePoi>) {
        channel.invokeMethod("POI", FlutterUtils.getPoiPayload(poiList))
    }

    /**
     * Example method to show a custom Path on the map
     */
    private fun showPathOnMap(startX: Double, startY: Double, startMapId: Int,
                              targetX: Double, targetY: Double, targetMapId: Int) {

        val path = nextomeSdk.findPath(
                startX.toInt(), startY.toInt(), startMapId,
                targetX.toInt(), targetY.toInt(), targetMapId)
            .filter { it.map == lastPosition.mapId }

        channel.invokeMethod("path", FlutterUtils.getPathPayload(path))
    }

    /**
     * Example method to show path on the map starting from current position
     */
    private fun showPathOnMap(targetX: Double, targetY: Double, targetMapId: Int) {
        val path = nextomeSdk.findPath(
                lastPosition.x.toInt(), lastPosition.y.toInt(), lastPosition.mapId,
                targetX.toInt(), targetY.toInt(), targetMapId)

        channel.invokeMethod("path", FlutterUtils.getPathPayload(path))
    }

    /**
     * Callback for events happening on the Flutter map.
     */
    private fun observeMapEvents() {
        channel.setMethodCallHandler { methodCall, _ ->
            // On POI tapped
            if (methodCall.method == "poiData") {
                try {
                    val poiSerialized = methodCall.arguments as String
                    val poi = Gson().fromJson(poiSerialized, FlutterPoi::class.java)

                    // React here to calculate path click
                    // Build path
                    showPathOnMap(lastPosition.x, lastPosition.y, lastPosition.mapId,
                            poi.x!!, poi.y!!, poi.map!!)

                    targetPathPoi = poi.asNextomePoi()
                    isShowingPath = true
                } catch (e: Exception) {
                    Bugsnag.notify(e)
                }
            }
        }
    }

    /**
     * Forces displaying a specific map in the current venue.
     * User live positions will not be updated anymore
     *
     * User live position can be restored calling setLiveMap();
     */
    private fun setForcedMap(mapId: Int) {
        nextomeSdk.setForcedMap(mapId)
    }

    /**
     * Starts receiving again live map updates.
     *
     * The flutter map will automatically switch
     * to the map in which the user is in.
     */
    private fun setLiveMap() {
        nextomeSdk.setLiveMap()
    }

    /**
     * Shows a specific map on Flutter.
     *
     * Nextome SDK users can call this method in the nextomeSdk.currentState observer.
     * mapTilesUrl and map sizes will be available in the observer's
     * NextomeLocalizationState object
     */
    private fun setIndoorMap(mapTilesUrl: String, mapHeight: Int, mapWidth: Int, pois: MutableList<NextomePoi>) {
        Log.e("nextome", "Tiles url: $mapTilesUrl")
        Log.e("nextome", "Height: $mapHeight")
        Log.e("nextome", "Width: $mapWidth")

        // Send local package data to Flutter
        channel.invokeMethod("localPackageUrl",
                "$mapTilesUrl,$mapHeight,$mapWidth, 3")

        showPoiOnMap(pois)
    }

    /* Outdoor - Indoor map initialization */
    private fun initOpenStreetMaps() {
        Configuration.getInstance().load(applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext))

        val gpsLocationProvider = GpsMyLocationProvider(applicationContext).apply {
            locationUpdateMinTime = 1000
            addLocationSource(LocationManager.GPS_PROVIDER)
            addLocationSource(LocationManager.NETWORK_PROVIDER)
        }

        val locationOverlay = MyLocationNewOverlay(gpsLocationProvider, outdoor_map).apply {
            enableMyLocation()
            enableFollowLocation()
        }

        with(outdoor_map) {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            overlays.add(locationOverlay)
            controller.setZoom(3.0)
        }

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                outdoor_map.controller.animateTo(locationOverlay.myLocation)
                outdoor_map.controller.setZoom(12.5)
            }
        }
    }

    private fun showIndoorMap() {
        outdoor_map.visibility = View.GONE
        indoor_map.visibility = View.VISIBLE

        stateCard.visibility = View.GONE
    }

    private fun showOpenStreetMap() {
        outdoor_map.visibility = View.VISIBLE
        indoor_map.visibility = View.GONE

        stateCard.visibility = View.VISIBLE
    }

    private fun updateState(message: String) { stateView.text = message }


    lateinit var channel: MethodChannel

    /**
     * Inits a flutter fragment
     */
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
            var newFlutterFragment = FlutterFragment
                    .withCachedEngine("net.nextome.phoenix").build<FlutterFragment>()

            flutterFragment = newFlutterFragment

            fragmentManager
                    .beginTransaction()
                    .add(
                            R.id.indoor_map,
                            newFlutterFragment,
                            TAG_FLUTTER_FRAGMENT
                    ).commit()
        }

        channel = MethodChannel(engine.dartExecutor, "net.nextome.phoenix")
    }

    /**
     *  Handle poi list click
     */
    private fun registerContracts() {
        poiListActivity = registerForActivityResult(PoiListContract()) {
            // Called when a new poi is selected in Poi List Activity
            it?.let {
                showPathOnMap(lastPosition.x, lastPosition.y, lastPosition.mapId,
                    it.x, it.y, it.map)
                targetPathPoi = it
                isShowingPath = true
            }
        }
    }

    /** Logging
     */
    private lateinit var menuItems: Menu

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.poiListButton -> {
                poiListActivity.launch(poiList)
            }
            R.id.logStart -> {
                menuItems.findItem(R.id.logStart).isVisible = false
                menuItems.findItem(R.id.logStop).isVisible = true

                nextomeSdk.startLoggingOnFile()
            }
            R.id.logStop -> {
                menuItems.findItem(R.id.logStart).isVisible = true
                menuItems.findItem(R.id.logStop).isVisible = false

                nextomeSdk.stopAndShareLogs(this)
            }
        }

        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.map_menu, menu)
        menuItems = menu

        return true
    }

    private fun parseSettingsFromIntent() {
        settings = intent.extras?.getParcelable(Constants.INTENT_EXTRA_SETTINGS) ?: NextomeSettings()
    }

    override fun onResume() {
        super.onResume()
        outdoor_map.onResume()
    }

    override fun onPause() {
        super.onPause()
        outdoor_map.onPause()
    }

    override fun onDestroy() {
        nextomeSdk.stop()
        super.onDestroy()
    }
}