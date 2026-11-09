package org.mobile.growfy.core.platform

import androidx.compose.runtime.Composable
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosUrlLauncher : UrlLauncher {
    override fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

// PHPickerViewController bridge still to do; the New post screen hides "Add photo" while this is null.
@Composable
actual fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit): (() -> Unit)? = null

/** Called from Swift's `.onOpenURL`. */
fun handleDeepLink(url: String) {
    KoinPlatform.getKoin().get<DeepLinks>().handle(url)
}
