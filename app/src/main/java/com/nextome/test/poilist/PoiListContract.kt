package com.nextome.test.poilist

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.google.gson.Gson
import net.nextome.phoenix_sdk.models.packages.NextomePoi

class PoiListContract: ActivityResultContract<List<NextomePoi>, NextomePoi?>() {

    override fun createIntent(context: Context, input: List<NextomePoi>?): Intent {
        return Intent(context, PoiListActivity::class.java).apply {
            putExtra("poiList", Gson().toJson(input))
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): NextomePoi? {
        val selectedPoiSerialized = intent?.getStringExtra("selectedPoi")
        return if (selectedPoiSerialized == null) {
            null
        } else {
            Gson().fromJson(selectedPoiSerialized, NextomePoi::class.java)
        }
    }

}