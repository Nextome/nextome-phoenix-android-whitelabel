package com.nextome.test.map

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextome.localization.facade.state.FindFloorState
import com.nextome.localization.facade.state.GetPacketState
import com.nextome.localization.facade.state.IdleState
import com.nextome.localization.facade.state.LocalizationRunningState
import com.nextome.localization.facade.state.SearchVenueState
import com.nextome.localization.facade.state.StartedState
import com.nextome.nxt_data.data.NextomeEvent
import com.nextome.nxt_data.data.NextomePoi
import com.nextome.nxt_data.data.NextomePosition
import com.nextome.test.R
import com.nextome.test.SplashActivity
import com.nextome.test.data.NextomeSdkCredentials
import com.nextome.test.databinding.ActivityMapBinding
import com.nextome.test.helper.NmSerialization.asJson
import com.nextome.test.poilist.PoiListContract
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MapActivity : AppCompatActivity() {
    companion object {
        const val CREDENTIALS_EXTRA_KEY = "credentials_key"
        fun getIntent(
            credentials: NextomeSdkCredentials,
            ctx: Context
        ): Intent {
            return Intent(ctx, MapActivity::class.java).apply {
                putExtra(CREDENTIALS_EXTRA_KEY, credentials.asJson())
            }
        }
    }

    private val viewModel: MapViewModel by viewModel()

    private lateinit var binding: ActivityMapBinding

    private var mapFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.rootLayout)

        viewModel.initWithIntent(intent)

        registerContracts()

        initOpenStreetMaps()

        observeSdkResults()
        reactToEvents()

        binding.stopButton.setOnClickListener {
            viewModel.stopSdk()
            startActivity(SplashActivity.getIntent(false, this@MapActivity))
        }

        binding.exitNavigation.setOnClickListener {
            stopShowingPath()
        }

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.poiListButton -> {
                    viewModel.poiListActivity.launch(viewModel.poiList)
                }

                R.id.menuSettingsButton -> {
                    viewModel.openSettingsActivity(this)
                }
            }

            return@setOnMenuItemClickListener true
        }
    }

    private fun reactToEvents() {
        lifecycleScope.launchWhenStarted {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiEvents.collectLatest {
                        when (it) {
                            is MapViewModel.CloseActivityEvent -> {
                                navigateBackToSplashScreen()
                            }
                            is MapViewModel.ShowMessageEvent -> {
                                Toast.makeText(this@MapActivity, it.message, Toast.LENGTH_LONG).show()
                            }
                            is MapViewModel.NextomeServiceNotRunningDialog -> {
                                if (it.show) {
                                    showNextomeServiceNotRunningDialog()
                                } else {
                                    hideNextomeServiceNotRunningDialog()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
    private fun observeSdkResults() {
        showOpenStreetMap()

        viewModel.nextomeSdk.getStateObservable().asLiveData().observe(this) { state ->
            Log.e("serviceTest", "NEW EVENT ${state.toString()}")

            when (state) {
                is IdleState -> {
                    showOpenStreetMap()
                    updateState("Sdk is Idle")
                }
                is StartedState -> {
                    showOpenStreetMap()
                    updateState("Sdk Started")
                }

                is SearchVenueState -> {
                    showOpenStreetMap()
                    updateState("Searching Venue...")
                }

                is GetPacketState -> {
                    showOpenStreetMap()
                    viewModel.currentVenueId = state.venueId
                    updateState("Downloading venue ${state.venueId}...")
                }

                is FindFloorState -> {
                    showOpenStreetMap()
                    updateState("Finding current Floor on venue ${state.venueId}...")
                }

                is LocalizationRunningState -> {
                    updateState("Showing map of floor ${state.mapId}...")
                    showIndoorMap()

                    initMapFragment()
                    setIndoorMap(
                        state.mapId,
                        state.tilesZipPath,
                        state.mapHeight,
                        state.mapWidth
                    )

                    viewModel.poiList = state.venueData.allPois

                    observeFlutterMapEvents()
                }
            }

        }


        /**
         * Observes user location relative to a map and floor;
         */
        viewModel.nextomeSdk.getLocalizationObservable().asLiveData().observe(this) {
            viewModel.lastPosition = it
            updatePositionOnFlutterMap(it)
        }

        viewModel.nextomeSdk.getEnterEventObservable().asLiveData().observe(this) { event ->
            Log.e("event_test", "Received on enter with id ${event.event.id} and data: ${event.event.data}")
            showEventDialog(event.event, true)
        }

        viewModel.nextomeSdk.getExitEventObservable().asLiveData().observe(this) { event ->
            Log.e("event_test", "Received on exit with id ${event.event.id} and data: ${event.event.data}")
            showEventDialog(event.event, false)
        }
    }

    var eventDialog: AlertDialog? = null
    private fun showEventDialog(event: NextomeEvent, isEnter: Boolean) {
        if (viewModel.shouldShowEventAlert(event.id)) {
            val message = if (isEnter) {
                "Enter event ${event.id} (${event.data})"
            } else {
                "Exit event ${event.id} (${event.data})"
            }

            eventDialog?.dismiss()
            eventDialog = MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setNeutralButton("Don't show again") { dialog, _ ->
                    viewModel.hideEventAlert(event.id)
                    dialog.dismiss()
                }
                .show()
        }
    }

    var serviceNotRunningDialog: AlertDialog? = null
    private fun showNextomeServiceNotRunningDialog() {
        serviceNotRunningDialog = MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.service_not_running))
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
                navigateBackToSplashScreen()
            }
            .setCancelable(false)
            .show()
    }

    private fun hideNextomeServiceNotRunningDialog() {
        serviceNotRunningDialog?.dismiss()
    }

    private fun navigateBackToSplashScreen(){
        startActivity(SplashActivity.getIntent(true, this@MapActivity))
        finish()
    }

    /**
     * Notify a change of user position to Flutter.
     *
     * Flutter Map will automatically be updated with the user new location.
     */
    private fun updatePositionOnFlutterMap(it: NextomePosition) {
        viewModel.mapManager.updateBlueDotMarker(it, callApply = true)

        if (viewModel.isShowingPath) {
            viewModel.targetPathPoi?.let { poi ->
                viewModel.lastPosition?.let { position ->
                    showPathOnMap(position.venueId, position.x, position.y, position.mapId,
                        poi.x, poi.y, poi.map)
                }
            }
        }
    }

    /**
     * Example method to show a custom Path on the map
     */
    private fun showPathOnMap(venueId: Int, startX: Double, startY: Double, startMapId: Int,
                              targetX: Double, targetY: Double, targetMapId: Int) {

        lifecycleScope.launch {
            val path = viewModel.nextomeSdk.findPath(venueId,
                startX, startY, startMapId,
                targetX, targetY, targetMapId)
                .filter { it.z == viewModel.lastPosition?.mapId }

            runOnUiThread {
                binding.exitNavigation.visibility = View.VISIBLE
                viewModel.mapManager.drawPath(path, callApply = true)
            }
        }
    }

    private fun clearPathOnMap() {
        binding.exitNavigation.visibility = View.GONE

        runOnUiThread {
            viewModel.mapManager.clearPathAndMarker(callApply = true)
        }
    }

    /**
     * Callback for events happening on the Flutter map.
     */
    private var mapEventsJob: Job? = null

    private fun observeFlutterMapEvents() {
        if (mapEventsJob == null) {
            mapEventsJob = lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {


                    viewModel.mapManager.flutterMap.setOnMarkerTap { marker ->
                        viewModel.lastPosition?.let { position ->

                            var poi: NextomePoi? = viewModel.getNextomePoiFromMapMarker(marker)
                            poi?.let {
                                showPathOnMap(
                                    position.venueId,
                                    position.x, position.y, position.mapId,
                                    poi!!.x, poi!!.y, poi!!.map
                                )

                                viewModel.targetPathPoi = poi!!
                                viewModel.isShowingPath = true
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a specific map on Flutter.
     *
     * Nextome SDK users can call this method in the nextomeSdk.currentState observer.
     * mapTilesUrl and map sizes will be available in the observer's
     * NextomeLocalizationState object
     */
    private fun setIndoorMap(mapId: Int, mapTilesUrl: String, mapHeight: Int, mapWidth: Int) {
        lifecycleScope.launch {
            // Devo aspettare qualche istante dopo l'inizializzazione di flutter, altrimenti carica flutter ma non vedo niente.
            delay(1000)
            viewModel.mapManager.loadIndoorMapTiles(
                mapId,
                mapTilesUrl,
                mapHeight,
                mapWidth
            )
        }
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

        val locationOverlay = MyLocationNewOverlay(gpsLocationProvider, findViewById<MapView>(R.id.outdoor_map)).apply {
            enableMyLocation()
            enableFollowLocation()
        }

        with(findViewById<MapView>(R.id.outdoor_map)) {
            setTileSource(TileSourceFactory.MAPNIK)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setMultiTouchControls(true)
            overlays.add(locationOverlay)
            controller.setZoom(3.0)
        }

        locationOverlay.runOnFirstFix {
            runOnUiThread {
                findViewById<MapView>(R.id.outdoor_map).controller.animateTo(locationOverlay.myLocation)
                findViewById<MapView>(R.id.outdoor_map).controller.setZoom(12.5)
            }
        }
    }

    private fun showIndoorMap() {
        findViewById<MapView>(R.id.outdoor_map).visibility = View.GONE
        findViewById<FrameLayout>(R.id.indoor_map).visibility = View.VISIBLE

        findViewById<CardView>(R.id.stateCard).visibility = View.GONE
    }

    private fun showOpenStreetMap() {
        findViewById<MapView>(R.id.outdoor_map).visibility = View.VISIBLE
        findViewById<FrameLayout>(R.id.indoor_map).visibility = View.GONE
        findViewById<CardView>(R.id.stateCard).visibility = View.VISIBLE
        destroyMapFragment()
    }

    private fun updateState(message: String) { findViewById<TextView>(R.id.stateView).text = message }


    /**
     * Inits the map fragment
     */
    private fun initMapFragment() {

        if(mapFragment != null){
            return
        }

        val fragmentManager: FragmentManager = supportFragmentManager
        viewModel.mapManager.flutterMap.initialize(
            fragmentManager = fragmentManager,
            viewId = R.id.indoor_map,
            context = this
        )

        // Important to avoid MissingPluginException(No implementation found for method method_name on channel com.nextome.nextomemapview)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.mapManager.flutterMap.observeEvents().collect {
                    // do nothing. Just initialize the observer
                }
            }
        }

        // Called when all resources are loaded
        viewModel.mapManager.flutterMap.setOnMapReady {
            viewModel.mapManager.setIndoorMapItems(application)
            viewModel.mapManager.buildPois(viewModel.poiList)
        }
    }

    /**
     * Destroy the map fragment
     * */
    private fun destroyMapFragment() {
        if(mapFragment != null) {
            supportFragmentManager.beginTransaction().remove(mapFragment!!).commit()
            mapFragment = null
        }
        viewModel.mapManager.flutterMapReady = false
    }

    /**
     *  Handle poi list click
     */
    private fun registerContracts() {
        viewModel.poiListActivity = registerForActivityResult(PoiListContract()) {
            // Called when a new poi is selected in Poi List Activity
            it?.let {
                Log.e("registerContracts","LastPosition in ${viewModel.lastPosition}")
                viewModel.lastPosition?.let { position ->
                    showPathOnMap(position.venueId, position.x, position.y, position.mapId,
                        it.x, it.y, it.map)
                    viewModel.targetPathPoi = it
                    viewModel.isShowingPath = true
                }
            }
        }
    }

    private fun stopShowingPath() {
        viewModel.isShowingPath = false
        clearPathOnMap()
    }

    override fun onResume() {
        super.onResume()
        findViewById<MapView>(R.id.outdoor_map).onResume()
    }

    override fun onPause() {
        super.onPause()
        findViewById<MapView>(R.id.outdoor_map).onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}