package org.mobile.growfy.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * `growfy://app/...` deep links (notifications later, adb testing now; see docs/testing.md).
 *
 * growfy://app/home | reviews | posts | more      → that tab
 * growfy://app/reply/<reviewId>                    → Reply
 * growfy://app/compose | growfy://app/post/<id>    → New post / edit post
 * growfy://app/templates | accounts | notifications
 */
object AppLinkParser {
    const val PREFIX = "growfy://app/"

    fun parse(url: String): Any? {
        if (!url.startsWith(PREFIX)) return null
        val parts = url.removePrefix(PREFIX).substringBefore('?').trim('/').split('/')
        return when (parts.firstOrNull()) {
            "home" -> Tab.Home
            "reviews" -> Tab.Reviews
            "posts" -> Tab.Posts
            "more" -> Tab.More
            "reply" -> parts.getOrNull(1)?.let(Route::Reply)
            "compose" -> Route.ComposePost()
            "post" -> parts.getOrNull(1)?.let { Route.ComposePost(postId = it) }
            "templates" -> Route.Templates
            "accounts" -> Route.GoogleAccounts
            "notifications" -> Route.Notifications
            else -> null
        }
    }
}

/** Hands a tab picked by a deep link to MainScreen, which owns the tab NavHost. */
class TabRequests {
    private val _tabs = MutableSharedFlow<Tab>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val tabs: SharedFlow<Tab> = _tabs.asSharedFlow()

    fun request(tab: Tab) {
        _tabs.tryEmit(tab)
    }

    fun consumed() {
        _tabs.resetReplayCache()
    }
}
