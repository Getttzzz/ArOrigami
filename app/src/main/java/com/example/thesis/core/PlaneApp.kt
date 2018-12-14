package com.example.thesis.core

import android.app.Application

class PlaneApp : Application() {
    val savedPlanes by lazy { SavedPlanes() }
}
