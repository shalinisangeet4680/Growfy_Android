package org.mobile.growfy.core.config

/** Values the backend must accept for the mobile app (docs/backend-gaps.md). */
object MobileContract {
    /** Deep link the backend redirects to when Google Business OAuth finishes (§1). */
    const val OAUTH_REDIRECT_URI = "growfy://business/connect"
}
