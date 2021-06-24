package com.udacity.project4.base

import androidx.annotation.IdRes
import androidx.navigation.NavDirections

/**
 * Sealed class used with the live data to navigate between the fragments
 */
sealed class NavigationCommand {
    /**
     * navigate to a resource
     */
    data class ToId(@IdRes val destinationId: Int) : NavigationCommand()

    /**
     * navigate to a direction
     */
    data class To(val directions: NavDirections) : NavigationCommand()

    /**
     * navigate back to the previous fragment
     */
    object Back : NavigationCommand()

    /**
     * navigate back to a destination in the back stack
     */
    data class BackTo(@IdRes val destinationId: Int) : NavigationCommand()
}