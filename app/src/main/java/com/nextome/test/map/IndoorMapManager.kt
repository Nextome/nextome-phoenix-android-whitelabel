package com.nextome.test.map

import android.content.Context
import com.nextome.nextomemapview.NextomeMapViewHandler
import com.nextome.nextomemapview.models.NMColor
import com.nextome.nextomemapview.models.NMLineStyle
import com.nextome.nextomemapview.models.NMMarker
import com.nextome.nextomemapview.models.NMPath
import com.nextome.nextomemapview.models.NMPoint
import com.nextome.nextomemapview.models.NMSourceType
import com.nextome.nextomemapview.models.NMTile
import com.nextome.nxt_data.data.NextomePoi
import com.nextome.nxt_data.data.NextomePosition
import com.nextome.nxt_data.data.Vertex
import java.io.File

class IndoorMapManager {

    final var FLUTTERMAP_INDOOR_LAYER_PATHS = "PathsLayer"
    final var FLUTTERMAP_INDOOR_LAYER_MARKERS = "MarkersLayer"

    var flutterMap : NextomeMapViewHandler = NextomeMapViewHandler()
    var flutterMapReady = false

    var indoorBluedotMarker : NMMarker = NMMarker()
    var indoorPath: NMPath = NMPath()
    var indoorMarkerList: MutableList<NMMarker> = mutableListOf()

    var application: Context? = null

    fun loadIndoorMapTiles(mapId: Int, mapTilesUrl: String, mapHeight: Int, mapWidth: Int) {
        // Devo aspettare qualche istante dopo l'inizializzazione di flutter, altrimenti carica flutter ma non vedo niente.
        val tile = NMTile()
        tile.show = true
        tile.name = mapId.toString()
        tile.id = mapId.toString()
        tile.source = mapTilesUrl
        flutterMap.setResources(tiles = listOf(tile), zoom = 1, width = mapWidth, height = mapHeight, useMockView = false)
    }

    /**
     * Set the indoor items like marker, paths etc..
     * */
    fun setIndoorMapItems(application: Context) {

        this.application = application

        val blueDotFile = application.getFileFromAssets("current_position.png")
        indoorBluedotMarker.id = "indoorBluedotMarker"
        indoorBluedotMarker.x = 200.0
        indoorBluedotMarker.y = 200.0
        indoorBluedotMarker.height = 30.0
        indoorBluedotMarker.width = 30.0
        indoorBluedotMarker.source = blueDotFile.absolutePath
        indoorBluedotMarker.sourceType = NMSourceType.FILESYSTEM

        indoorPath.id = "indoorPath"
        indoorPath.color = NMColor(50, 50, 255)
        indoorPath.width = 4.0;
        indoorPath.style = NMLineStyle.NORMAL
        indoorPath.points = listOf(NMPoint(0.0, 0.0), NMPoint(1.0, 1.0))

        flutterMap.addLayer(FLUTTERMAP_INDOOR_LAYER_PATHS)
        flutterMap.addLayer(FLUTTERMAP_INDOOR_LAYER_MARKERS)

        flutterMap.setLayerVisibility(FLUTTERMAP_INDOOR_LAYER_PATHS, false)
        flutterMap.setLayerVisibility(FLUTTERMAP_INDOOR_LAYER_MARKERS, true)

        flutterMap.addMarker(FLUTTERMAP_INDOOR_LAYER_MARKERS, indoorBluedotMarker)
        flutterMap.addPath(FLUTTERMAP_INDOOR_LAYER_PATHS, indoorPath)

        flutterMap.apply()

        flutterMapReady = true
    }

    fun isFlutterMapReady(): Boolean {
        return flutterMapReady
    }


    fun updateBlueDotMarker(position: NextomePosition, callApply: Boolean = false){

        if(!flutterMapReady){
            return
        }

        indoorBluedotMarker.x = position.x
        indoorBluedotMarker.y = position.y
        flutterMap.updateMarker(FLUTTERMAP_INDOOR_LAYER_MARKERS, indoorBluedotMarker)
        if(callApply){
            flutterMap.apply()
        }
    }

    fun clearPathAndMarker(callApply: Boolean = false){
        if(!flutterMapReady){
            return
        }

        indoorPath!!.points = listOf()
        flutterMap.updatePath(FLUTTERMAP_INDOOR_LAYER_PATHS, indoorPath)

        flutterMap.setLayerVisibility(FLUTTERMAP_INDOOR_LAYER_PATHS, false)

        if(callApply){
            flutterMap.apply()
        }
    }

    fun drawPath(path: List<Vertex>, callApply: Boolean = false){

        if(!flutterMapReady){
            return
        }

        var points: MutableList<NMPoint> = mutableListOf()
        path.forEach { vertex ->
            points.add(NMPoint(vertex.x, vertex.y))
        }

        indoorPath.points = points
        flutterMap.updatePath(FLUTTERMAP_INDOOR_LAYER_PATHS, indoorPath)

        flutterMap.setLayerVisibility(FLUTTERMAP_INDOOR_LAYER_PATHS, true)

        if(callApply){
            flutterMap.apply()
        }
    }

    fun apply(){
        flutterMap.apply()
    }

    fun buildPois(pois: List<NextomePoi>){
        application?.let {
            pois.forEach {
                val poiMarkerFile = application!!.getFileFromAssets("poi_colored.png")
                var poiMarker: NMMarker = NMMarker()
                poiMarker.id = "poi_" + it.id
                poiMarker.x = it.x
                poiMarker.y = it.y
                poiMarker.height = 30.0
                poiMarker.width = 30.0
                poiMarker.source = poiMarkerFile.absolutePath
                poiMarker.sourceType = NMSourceType.FILESYSTEM
                flutterMap.addMarker(FLUTTERMAP_INDOOR_LAYER_MARKERS, poiMarker)
            }

            flutterMap.apply()
        }
    }
}

fun Context.getFileFromAssets(fileName: String): File = File(cacheDir, fileName)
    .also {
        if (!it.exists()) {
            it.outputStream().use { cache ->
                assets.open(fileName).use { inputStream ->
                    inputStream.copyTo(cache)
                }
            }
        }
    }