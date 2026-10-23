package app.errand

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.errand.data.Settings
import app.errand.ui.ErrandRoot
import app.errand.ui.theme.ErrandTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(t, t), navigationBarStyle = SystemBarStyle.light(t, t))
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome screen doesn't flash for someone already here.
            settings?.let { s -> ErrandTheme { ErrandRoot(s) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        opened(intent)
    }

    /** "Share → Errand" from a messenger: a shopping list becomes orders. */
    private fun opened(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        if (text.isNotBlank()) container.incoming.value = text
    }
}
