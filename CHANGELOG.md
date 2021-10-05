# Phoenix SDK Changelog
## 0.2.8
 * Add method to send locations to server: 
 ```kotlin
NextomePhoenixSdk().Builder(applicationContext)
    .withSendPositionToServer(true)
 ```

## 0.2.7
 * Fix issue in Event's radius parser;
 * Add debug tools to log on file: `startLoggingOnFile()` and `stopAndShareLog(context)`

## 0.2.2
 * Expose current map POIs in observer;
 
## 0.2.1
 * Add new `setForcedMap` and `setLiveMap` methods.
 * Improvements to Flutter map;

## 0.1.2
 * Fix issue with floor and map Id;
 * Fix issue with outdoor state;

## 0.1.1
 * Added mapId and floorId in NextomePosition.
 
 You can now listen for map and floor changes using the localizationLiveData.
 ```kotlin
         nextomeSdk.localizationLiveData.observe(this, Observer {
            val floor = it.floorId
            val map = it.mapId
            log( "User Position is ${it.x}, ${it.y}")
        })
 ```
## 0.1.0
 * Initial release;
