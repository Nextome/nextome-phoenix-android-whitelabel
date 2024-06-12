package com.nextome.test.poilist

import androidx.lifecycle.ViewModel
import com.nextome.nxt_data.data.NextomePoi

class PoiListViewModel: ViewModel() {
    private var poiList = listOf<NextomePoi>()
    var filteredList = listOf<NextomePoi>()

    fun setPoiList(poiList: List<NextomePoi>){
        this.poiList = poiList
        this.filteredList = poiList
    }

    fun updateFilter(filterValue: String){
        filteredList = if(filterValue.isNotEmpty()){
            poiList
                .filter {
                    (it.descriptions.firstOrNull()?.name ?: "")
                        .lowercase()
                        .contains(filterValue.lowercase())
                }
        }else{
            poiList
        }

    }
}