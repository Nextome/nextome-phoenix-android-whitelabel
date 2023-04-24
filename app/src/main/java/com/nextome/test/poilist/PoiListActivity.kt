package com.nextome.test.poilist

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.SearchView.OnQueryTextListener
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.nextome.test.R
import com.nextome.test.helper.NmSerialization.asJson
import com.nextome.test.helper.NmSerialization.fromJson

class PoiListActivity : AppCompatActivity() {

    private val viewModel: PoiListViewModel by viewModels()
    lateinit var searchView: SearchView
    lateinit var poiAdapter: PoiAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poi_list)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel.setPoiList(intent.getStringExtra("poiList")!!.fromJson())

        poiAdapter = PoiAdapter(viewModel.filteredList) { selectedPoi ->
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("selectedPoi", selectedPoi.asJson())
            })

            finish()
        }
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        findViewById<RecyclerView>(R.id.poiRecycler).layoutManager = LinearLayoutManager(this)
        findViewById<RecyclerView>(R.id.poiRecycler).adapter = poiAdapter

        findViewById<SearchView>(R.id.poiSearchView).setOnQueryTextListener(object: OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.updateFilter(query ?: "")
                poiAdapter.poiList = viewModel.filteredList
                poiAdapter.notifyDataSetChanged()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if(newText.isNullOrEmpty()){
                    onQueryTextSubmit("")
                }
                return true
            }

        })


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