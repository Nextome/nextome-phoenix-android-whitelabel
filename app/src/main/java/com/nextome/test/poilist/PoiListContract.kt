package com.nextome.test.poilist

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import net.nextome.phoenix.models.packages.NextomePoi
import com.nextome.test.helper.NmSerialization.asJson
import com.nextome.test.helper.NmSerialization.fromJson

class PoiListContract: ActivityResultContract<List<NextomePoi>, NextomePoi?>() {

    override fun createIntent(context: Context, input: List<NextomePoi>): Intent {
        return Intent(context, PoiListActivity::class.java).apply {
            putExtra("poiList", input.asJson())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): NextomePoi? {
        val selectedPoiSerialized = intent?.getStringExtra("selectedPoi")
        return if (selectedPoiSerialized == null) {
            null
        } else {
            selectedPoiSerialized.fromJson()
        }
    }
}