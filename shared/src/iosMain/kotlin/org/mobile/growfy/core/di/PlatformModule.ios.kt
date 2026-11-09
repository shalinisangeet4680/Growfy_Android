package org.mobile.growfy.core.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import org.mobile.growfy.core.config.AppConfig
import org.mobile.growfy.core.platform.IosUrlLauncher
import org.mobile.growfy.core.platform.UrlLauncher
import org.mobile.growfy.core.storage.DATA_STORE_FILE_NAME
import org.mobile.growfy.core.storage.createDataStore
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<UrlLauncher> { IosUrlLauncher() }
    single {
        createDataStore {
            val documents = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            requireNotNull(documents).path + "/$DATA_STORE_FILE_NAME"
        }
    }
}

/** Called from Swift (iOSApp.init) with values read from Info.plist. */
fun startKoinIos(baseUrl: String, isDebug: Boolean) {
    initKoin(AppConfig(baseUrl = baseUrl, isDebug = isDebug))
}
