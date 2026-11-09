package org.mobile.growfy.core.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Opens a URL outside the app (Custom Tab on Android, Safari on iOS). */
fun interface UrlLauncher {
    fun open(url: String)
}

/** Deep links handed to the app by the OS, e.g. `growfy://business/connect?connected=2`. */
class DeepLinks {
    private val _links = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val links: SharedFlow<String> = _links.asSharedFlow()

    fun handle(url: String) {
        _links.tryEmit(url)
    }

    fun consumed() {
        _links.resetReplayCache()
    }
}

class PickedPhoto(val bytes: ByteArray, val fileName: String, val mimeType: String)

/** Returns a launcher for the system photo picker, or null where picking isn't supported yet. */
@Composable
expect fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit): (() -> Unit)?
