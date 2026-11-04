package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ConduitApp
import com.example.ui.ConduitViewModel
import com.example.ui.theme.ConduitTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LightBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ConduitViewModel = viewModel(
                factory = ConduitViewModel.provideFactory(applicationContext)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = uiState.settings.isDarkMode

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(DarkBackground.toArgb())
                    } else {
                        SystemBarStyle.light(
                            LightBackground.toArgb(),
                            DarkBackground.toArgb()
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(DarkBackground.toArgb())
                    } else {
                        SystemBarStyle.light(
                            LightBackground.toArgb(),
                            DarkBackground.toArgb()
                        )
                    }
                )
            }

            ConduitTheme(darkTheme = isDark) {
                ConduitApp(viewModel = viewModel)
            }
        }
    }
}
