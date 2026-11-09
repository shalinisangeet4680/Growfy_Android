package org.mobile.growfy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import org.koin.android.ext.android.inject
import org.mobile.growfy.core.platform.DeepLinks

@OptIn(ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    private val deepLinks: DeepLinks by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            // Exposes Compose testTags as resource-ids for uiautomator / scripts/ui.sh (docs/testing.md).
            Box(Modifier.semantics { testTagsAsResourceId = true }) { App() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.data?.toString()?.let(deepLinks::handle)
    }
}
