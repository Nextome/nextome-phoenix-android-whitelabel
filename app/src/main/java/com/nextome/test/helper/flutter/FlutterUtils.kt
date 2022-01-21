package com.nextome.test.helper.flutter

import com.google.gson.Gson
import net.nextome.phoenix_sdk.legacy.nextome.data.models.Vertex
import net.nextome.phoenix_sdk.models.packages.NextomePoi
import net.nextome.phoenix_sdk.models.packages.NextomePoiDescriptions
import io.flutter.plugin.common.MethodChannel

object FlutterUtils {

    fun setMapViewSettings(channel: MethodChannel, fabEnabled: Boolean = false, customPositionResourceUrl: String? = null) {
        channel.invokeMethod("showCenterPositionFab", fabEnabled.toString())

        customPositionResourceUrl?.let {
            channel.invokeMethod("customPositionResourceUrl", it)
        }
    }

    fun getPositionPayload(x: Double, y: Double) = "$x,$y"
    fun getPathPayload(vertexList: List<Vertex>) = Gson().toJson(vertexList)
    fun getPoiPayload(poiList: List<NextomePoi>): String {
        val flutterPoiList: List<FlutterPoi> = poiList.map { it.asFlutterPoi() }
        return Gson().toJson(flutterPoiList)
    }
}

fun NextomePoi.asFlutterPoi(): FlutterPoi {
    return FlutterPoi(
        id = id, name = name, x = x, y = y, map = map,
        descriptions = this.descriptions?.map { Descriptions(it.id, it.name, it.description ?: "", it.language) } ?: listOf()
    )
}

fun FlutterPoi.asNextomePoi(): NextomePoi {
    val flutterPoi: FlutterPoi = this

    return NextomePoi().apply {
        id = flutterPoi.id ?: 0
        name = flutterPoi.name
        x = flutterPoi.x ?: 0.0
        y = flutterPoi.y ?: 0.0
        map = flutterPoi.map ?: 0
        descriptions = flutterPoi.descriptions.map { NextomePoiDescriptions().apply {
            id = it?.id ?: 0
            name = it?.name
            description = it?.description
            language = it?.language
        } }
    }
}
