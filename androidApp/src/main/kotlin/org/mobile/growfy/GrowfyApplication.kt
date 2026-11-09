package org.mobile.growfy

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.mobile.growfy.core.config.AppConfig
import org.mobile.growfy.core.di.initKoin

class GrowfyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(AppConfig(baseUrl = BuildConfig.API_BASE_URL, isDebug = BuildConfig.DEBUG)) {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@GrowfyApplication)
        }
    }
}
