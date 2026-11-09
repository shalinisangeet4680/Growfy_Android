package org.mobile.growfy.core.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mobile.growfy.core.platform.AndroidUrlLauncher
import org.mobile.growfy.core.platform.UrlLauncher
import org.mobile.growfy.core.storage.DATA_STORE_FILE_NAME
import org.mobile.growfy.core.storage.createDataStore

actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }
    single {
        val context = androidContext()
        createDataStore { context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath }
    }
}
