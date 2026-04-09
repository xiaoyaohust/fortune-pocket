package com.fortunepocket.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.fortunepocket.app.navigation.AppNavigation
import com.fortunepocket.core.ui.theme.FortunePocketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FortunePocketTheme {
                AppNavigation(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
