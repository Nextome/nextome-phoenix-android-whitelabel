package com.nextome.test.map

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextome.localization.NextomeLocalizationSdk
import com.nextome.localization.background.NMNotification
import com.nextome.nextomemapview.models.NMMarker
import com.nextome.nxt_data.data.CriticalException
import com.nextome.nxt_data.data.GenericException
import com.nextome.nxt_data.data.InvalidCredentialException
import com.nextome.nxt_data.data.NextomeException
import com.nextome.nxt_data.data.NextomePoi
import com.nextome.nxt_data.data.NextomePosition
import com.nextome.nxt_data.data.NextomeSettings
import com.nextome.test.R
import com.nextome.test.data.NextomeSdkCredentials
import com.nextome.test.helper.NmSerialization.fromJson
import com.nextome.test.settings.AppOverriddenSettings
import com.nextome.test.settings.SettingsActivity
import com.nextome.test.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MapViewModel(
    private val settingsRepository: SettingsRepository,
    private val context: Application,
): ViewModel() {
    private val _uiEvents = MutableStateFlow<UiEvent?>(null)
    val uiEvents = _uiEvents.filterNotNull()

    lateinit var sdkCredentials: NextomeSdkCredentials
    lateinit var nextomeSdk: NextomeLocalizationSdk

    val settings by lazy { settingsRepository.getUserSettings() }

    var currentVenueId: Int? = null

    lateinit var poiListActivity: ActivityResultLauncher<List<NextomePoi>>
    var lastPosition: NextomePosition? = null
    var poiList: List<NextomePoi> = listOf()

    // Save if we're showing a path in map
    // In this case, we need to update the path each time the user moves
    var isShowingPath = false
    var targetPathPoi: NextomePoi? = null

    val mapManager = IndoorMapManager()

    fun initWithIntent(intent: Intent) {
        viewModelScope.launch {

            parseCredentialsFromIntent(intent)
            initNextomeSdk(settings)

            if (NextomeLocalizationSdk.isRunning()) {
                _uiEvents.value = ShowMessageEvent(
                    context.getString(R.string.service_running)
                )
            } else {
                startLocalization()
            }
        }
    }

    private fun ensureNextomeIsRunning() {
        viewModelScope.launch {
            NextomeLocalizationSdk.isBackgroundServiceRunningObservable(context).collectLatest { running ->
                _uiEvents.value = NextomeServiceNotRunningDialog(running.not())
            }
        }
    }

    private fun initNextomeSdk(settings: AppOverriddenSettings?) {
        val nextomeOverriddenSettings = settings ?: AppOverriddenSettings()

        // Initialize Nextome SDK (make sure to use Application context)
        nextomeSdk = NextomeLocalizationSdk(
            clientId = sdkCredentials.clientId,
            clientSecret = sdkCredentials.clientSecret,
            scanPeriod = nextomeOverriddenSettings.scanPeriod,
            betweenScanPeriod = nextomeOverriddenSettings.betweenScanPeriod,
            rssiThreshold = nextomeOverriddenSettings.rssiThreshold,
            beaconListMaxSize = nextomeOverriddenSettings.beaconListMaxSize,
            sendPositionToServer = nextomeOverriddenSettings.sendPositionToServer,
            sendAssetsToServer = nextomeOverriddenSettings.sendAssetsToServer,
            eventTimeoutDurationInSeconds = nextomeOverriddenSettings.eventTimeout,
            // initialData = AssetResource("resource_632.zip", 632, 1.0)
        )

        viewModelScope.launch {
            nextomeSdk.getErrorsObservable().collect {
                Log.e("nmlog", "New error received: ${it.message}")
                handleError(it)
            }
        }
    }

    /**
     * Starts Nextome Foreground Service.
     */
    private fun startLocalization() {
        nextomeSdk.start()
    }

    private fun startLocalizationWithBackgroundService() {
        nextomeSdk.startWithBackgroundService(300, NMNotification())
        ensureNextomeIsRunning()
    }


    private fun parseCredentialsFromIntent(intent: Intent){
        // parse credentials
        val encodedCredentials = intent.getStringExtra(MapActivity.CREDENTIALS_EXTRA_KEY)
        requireNotNull(encodedCredentials) { "Please provide credentials to initialize the SDK." }

        sdkCredentials = encodedCredentials.fromJson()
    }

    fun handleError(error: NextomeException) {
        when (error) {
            is GenericException -> {
                if(settings?.isDebugModeEnabled == true){
                    _uiEvents.value = ShowMessageEvent(message = error.message)
                }
            }

            is InvalidCredentialException -> {
                _uiEvents.value = ShowMessageEvent(message = error.message)
                _uiEvents.value = CloseActivityEvent
            }

            is CriticalException -> {
                _uiEvents.value = ShowMessageEvent(message = error.message)
            }
        }
    }

    fun stopSdk() {
        nextomeSdk.stopBackgroundService(context)
        nextomeSdk.stop()
    }

    fun openSettingsActivity(context: Context) {
        viewModelScope.launch {
            val venueId = currentVenueId
            val venueDefaultSettings: NextomeSettings? = if (venueId != null) {
                nextomeSdk.getVenueData(venueId) {
                    // Take download progress here
                }.settings
            } else { null }

            context.startActivity(SettingsActivity.getIntent(venueDefaultSettings,
                isReadOnly = true,
                context))
        }
    }

    private val hiddenEventAlerts = arrayListOf<String>()
    fun shouldShowEventAlert(eventId: String): Boolean {
        return hiddenEventAlerts.contains(eventId).not()
    }

    fun hideEventAlert(eventId: String) {
        hiddenEventAlerts.add(eventId)
    }

    /**
     * Helper function to match an NMarkr input, to NextomePoi stored in MapViewModel.poiList
     * */
    fun getNextomePoiFromMapMarker(marker: NMMarker): NextomePoi? {

        if(marker.id != null && marker.id!!.startsWith("poi_")){
            var idStr = marker.id!!.substring("poi_".length)
            try {
                var id = idStr.toInt()
                var res = poiList.filter { it.id == id}
                return if(res.isEmpty()) return null else res[0]
            }catch (e: Exception){
                return null
            }
        }

        return null
    }
    
    sealed class UiEvent
    object CloseActivityEvent: UiEvent()
    data class NextomeServiceNotRunningDialog(val show: Boolean): UiEvent()
    data class ShowMessageEvent(val message: String): UiEvent()
}