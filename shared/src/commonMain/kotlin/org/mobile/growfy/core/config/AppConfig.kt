package org.mobile.growfy.core.config

/**
 * Environment-specific values, supplied by each platform at startup
 * (Android: BuildConfig per product flavor, iOS: Info.plist via xcconfig).
 */
data class AppConfig(
    val baseUrl: String,
    val isDebug: Boolean,
)
