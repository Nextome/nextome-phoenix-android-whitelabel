package com.nextome.test.settings

import kotlinx.serialization.Serializable

@Serializable
data class AppOverriddenSettings(
    val scanPeriod: Long? = null,
    val betweenScanPeriod: Long? = null,
    val beaconListMaxSize: Int? = null,
    val rssiThreshold: Int? = null,
    val sendPositionToServer: Boolean? = null,
    val sendAssetsToServer: Boolean? = null,
    val eventTimeout: Long? = null,
    val isDebugModeEnabled: Boolean = false
) {
    fun hasEditedSettings() =
        scanPeriod != null ||
        betweenScanPeriod != null ||
        beaconListMaxSize != null ||
        rssiThreshold != null ||
        sendPositionToServer != null ||
        sendAssetsToServer != null ||
        eventTimeout != null ||
        isDebugModeEnabled
}