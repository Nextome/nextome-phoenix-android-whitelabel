package com.nextome.test.poilist

import androidx.lifecycle.ViewModel
import net.nextome.phoenix_sdk.models.packages.NextomePoi

class PoiListViewModel: ViewModel() {
    var poiList = listOf<NextomePoi>()
}