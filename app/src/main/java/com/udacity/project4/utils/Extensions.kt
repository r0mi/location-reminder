package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.udacity.project4.base.BaseRecyclerViewAdapter
import com.udacity.project4.base.SwipeController
import kotlin.math.sqrt


/**
 * Extension function to setup the RecyclerView
 */
fun <T> RecyclerView.setup(
    adapter: BaseRecyclerViewAdapter<T>,
    swipeRefreshLayout: SwipeRefreshLayout? = null,
    callBack: ((selectedReminder: T, direction: Int) -> Unit)? = null
) {
    this.apply {
        layoutManager = LinearLayoutManager(this.context)
        this.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(SwipeController(swipeRefreshLayout, callBack))
        itemTouchHelper.attachToRecyclerView(this)
    }
}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
            bool
        )
    }
}

//animate changing the view visibility
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

//animate changing the view visibility
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}

//calculate distance between two LatLng points
fun LatLng.distanceTo(latLng: LatLng): Double {
    val pointA = Location("A")
    pointA.latitude = this.latitude
    pointA.longitude = this.longitude
    return pointA.distanceTo(Location("B").apply {
        latitude = latLng.latitude
        longitude = latLng.longitude
    }).toDouble()
}

operator fun <T> MutableLiveData<MutableList<T>>.plusAssign(v: T) {
    val value = this.value ?: mutableListOf()
    value.add(v)
    this.value = value
}

operator fun <T> MutableLiveData<MutableList<T>>.minusAssign(v: T) {
    val value = this.value ?: mutableListOf()
    value.remove(v)
    this.value = value
}

fun <T> MutableLiveData<MutableList<T>>.clear() {
    val value = this.value ?: mutableListOf()
    value.clear()
    this.value = value
}

fun <T> MutableLiveData<MutableList<T>>.addIfNotExists(v: T) {
    val value = this.value ?: mutableListOf()
    if (v !in value) {
        value.add(v)
        this.value = value
    }
}

fun LatLng.toBounds(radius: Double): LatLngBounds {
    val southwest = SphericalUtil.computeOffset(this, radius * sqrt(2.0), 225.0)
    val northeast = SphericalUtil.computeOffset(this, radius * sqrt(2.0), 45.0)
    return LatLngBounds(southwest, northeast)
}

fun Resources.getHtmlSpannedString(@StringRes id: Int, vararg formatArgs: Any): Spanned =
    getString(id, *formatArgs).toHtmlSpan()

fun String.toHtmlSpan(): Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
} else {
    Html.fromHtml(this)
}