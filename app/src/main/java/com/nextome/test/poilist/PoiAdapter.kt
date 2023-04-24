package com.nextome.test.poilist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nextome.test.R
import net.nextome.phoenix.models.packages.NextomePoi

class PoiAdapter(
    var poiList: List<NextomePoi>,
    private val onPoiClicked: (poi: NextomePoi) -> Unit
): RecyclerView.Adapter<PoiAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poiContainer: View = view.findViewById(R.id.poiContainer)
        val poiNameText: TextView = view.findViewById(R.id.poiName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poi, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val poiInfo = poiList[position].descriptions.firstOrNull()
        poiInfo?.let {
            holder.poiNameText.text = poiInfo.name
            holder.poiContainer.setOnClickListener { onPoiClicked(poiList[position]) }
        }
    }

    override fun getItemCount() = poiList.size
}