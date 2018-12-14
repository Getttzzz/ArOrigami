package com.example.thesis.model

import android.os.Parcel
import android.os.Parcelable

data class Plane(val name: String, val icon: Int, val uri: String) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(icon)
        parcel.writeString(uri)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Plane> {
        override fun createFromParcel(parcel: Parcel): Plane {
            return Plane(parcel)
        }

        override fun newArray(size: Int): Array<Plane?> {
            return arrayOfNulls(size)
        }
    }
}
