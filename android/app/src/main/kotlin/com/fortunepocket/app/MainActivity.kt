package com.fortunepocket.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fortunepocket.app.navigation.AppNavigation
import com.fortunepocket.core.ui.theme.FortunePocketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var pendingDestinationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDestinationRoute = intent?.getStringExtra(EXTRA_DESTINATION)
        enableEdgeToEdge()
        setContent {
            FortunePocketTheme {
                AppNavigation(
                    pendingDestinationRoute = pendingDestinationRoute,
                    onPendingDestinationConsumed = { pendingDestinationRoute = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestinationRoute = intent.getStringExtra(EXTRA_DESTINATION)
    }

    companion object {
        const val EXTRA_DESTINATION = "destination"
    }
}
