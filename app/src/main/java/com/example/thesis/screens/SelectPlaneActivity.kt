package com.example.thesis.screens

import android.app.Activity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import com.example.thesis.core.PlaneApp
import com.example.thesis.R

import com.example.thesis.model.Plane

import java.util.ArrayList

class SelectPlaneActivity : Activity() {

    private var planesAdapter: PlaneAdapter? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_plane)

        planesAdapter = PlaneAdapter()
        val gridview = findViewById(R.id.grid_view) as GridView
        gridview.adapter = planesAdapter
        gridview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedPlane = planesAdapter!!.getItem(position) as Plane
            startActivity(ArPlaneActivity.getIntent(selectedPlane, this@SelectPlaneActivity))
        }

        val app = applicationContext as PlaneApp
        val savedPlanes = app.savedPlanes
        val planes = savedPlanes.planes
        planesAdapter!!.add(planes)
    }

    private inner class PlaneAdapter : BaseAdapter() {
        private val list: MutableList<Plane>

        init {
            list = ArrayList()
        }

        fun add(item: Plane) {
            list.add(item)
            notifyDataSetChanged()
        }

        fun add(items: List<Plane>) {
            list.addAll(items)
            notifyDataSetChanged()
        }

        fun replace(items: List<Plane>) {
            list.clear()
            add(items)
            notifyDataSetChanged()
        }

        fun clear() {
            val size = list.size
            list.clear()
            notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Any {
            return list[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(
                        parent.context).inflate(
                        R.layout.item_plane, parent, false)

            }

            val plane = list[position]
            val image = convertView!!.findViewById(R.id.loading_image) as ImageView
            image.setImageDrawable(ContextCompat.getDrawable(convertView.context, plane.icon))

            return convertView
        }
    }
}
