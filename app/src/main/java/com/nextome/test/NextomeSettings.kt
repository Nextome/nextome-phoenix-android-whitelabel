package com.nextome.test

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import net.nextome.phoenix_sdk.facade.NextomeLocalizationMethod

@Parcelize
data class NextomeSettings(
    // TODO: Add Secret and Developer Key here.
    val secret: String = "secret_here",
    val developerKey: String = "developer_key_here",
    val scanPeriod: Long = 1000,
    val betweenScanPeriod: Long = 250,
    val beaconListMaxSize: Int = 12,
    val rssiThreshold: Int = -75,
    val localizationMethod: NextomeLocalizationMethod = NextomeLocalizationMethod.LINEAR_SVD,
    val isParticleActive: Boolean = true,
    val isSendPositionToServerEnabled: Boolean = false,
): Parcelable