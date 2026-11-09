package org.mobile.growfy.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidUrlLauncher(private val context: Context) : UrlLauncher {
    override fun open(url: String) {
        val intent = CustomTabsIntent.Builder().build().intent.apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
actual fun rememberPhotoPicker(onPicked: (PickedPhoto) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val photo = withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                val mime = resolver.getType(uri) ?: "image/jpeg"
                PickedPhoto(bytes, uri.lastPathSegment ?: "photo", mime)
            }
            photo?.let(onPicked)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
