package org.mobile.growfy.navigation

import kotlinx.serialization.Serializable

/** Full-screen destinations (no bottom bar). */
sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object Onboarding : Route
    @Serializable data object Main : Route
    @Serializable data class Reply(val reviewId: String) : Route
    /** [postId] edits an existing post; [scheduleAt] (ISO instant) pre-fills the schedule for a new one. */
    @Serializable data class ComposePost(val postId: String? = null, val scheduleAt: String? = null) : Route
    @Serializable data object Templates : Route
    @Serializable data object GoogleAccounts : Route
    @Serializable data object Notifications : Route
}

/** Bottom-bar tabs inside [Route.Main]. */
sealed interface Tab {
    @Serializable data object Home : Tab
    @Serializable data object Reviews : Tab
    @Serializable data object Posts : Tab
    @Serializable data object More : Tab
}
