package com.udacity.project4.utils

import android.os.Build

fun ifRunningOreoOrLater(f: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        f()
    }
}

fun ifRunningQOrLater(f: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        f()
    }
}
