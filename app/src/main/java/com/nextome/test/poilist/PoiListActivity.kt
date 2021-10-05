package com.nextome.test.poilist

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextome.test.R
import kotlinx.android.synthetic.main.activity_poi_list.*
import net.nextome.phoenix_sdk.models.packages.NextomePoi

class PoiListActivity : AppCompatActivity() {

    val viewModel: PoiListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val type = object : TypeToken<List<NextomePoi>>() {}.type
        viewModel.poiList = Gson().fromJson(intent.getStringExtra("poiList"), type)

        poiRecycler.layoutManager = LinearLayoutManager(this)
        poiRecycler.adapter = PoiAdapter(viewModel.poiList) { selectedPoi ->
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("selectedPoi", Gson().toJson(selectedPoi))
            })

            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if( id == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}