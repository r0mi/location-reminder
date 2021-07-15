package com.udacity.project4.locationreminders.savereminder

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class PointOfInterest(
    val latLng: LatLng,
    val radius: Double,
    val name: String
) : Parcelable
