package com.example.thesis.core

import com.example.thesis.R
import com.example.thesis.model.Plane

class SavedPlanes {
    val planes by lazy {
        arrayListOf(Plane("Simple plane", R.drawable.plane_1, "file:///android_asset/paperplane_1.vrx"))
    }
}