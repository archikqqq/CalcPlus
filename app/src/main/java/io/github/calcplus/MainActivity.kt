package io.github.calcplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.calcplus.data.GeminiClient
import io.github.calcplus.data.PreferencesRepository
import io.github.calcplus.model.CalcMode
import io.github.calcplus.ui.CalcUiState
import io.github.calcplus.ui.CalcViewModel
import io.github.calcplus.ui.screens.AiSolverScreen
import io.github.calcplus.ui.screens.CalculatorScreen
import io.github.calcplus.ui.screens.ConverterScreen
import io.github.calcplus.ui.screens.FractionsScreen
import io.github.calcplus.ui.screens.SettingsBottomSheet
import io.github.calcplus.ui.screens.MotionTasksScreen
import io.github.calcplus.ui.theme.CalcPlusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = PreferencesRepository(applicationContext)
        val geminiClient = GeminiClient()

        setContent {
            val factory = remember { CalcViewModel.Factory(preferencesRepository, geminiClient) }
            val viewModel: CalcViewModel = viewModel(factory = factory)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CalcPlusTheme(
                themeMode = uiState.themeMode,
                useDynamicColor = uiState.useDynamicColor
            ) {
                CalcPlusMainContent(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun CalcPlusMainContent(
    uiState: CalcUiState,
    viewModel: CalcViewModel
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CalcMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.currentMode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(mode.title) }
                    )
                }
            }

            AnimatedContent(
                targetState = uiState.currentMode,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(
                                initialScale = 0.95f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )).togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "modeTransition"
            ) { mode ->
                when (mode) {
                    CalcMode.BASIC, CalcMode.SCIENTIFIC -> CalculatorScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    CalcMode.FRACTIONS -> FractionsScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    CalcMode.CONVERTER -> ConverterScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                    CalcMode.SPEED -> MotionTasksScreen()

                    CalcMode.AI -> AiSolverScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenSettings = { viewModel.toggleSettings(true) }
                    )
                }
            }
        }

        if (uiState.isSettingsOpen) {
            SettingsBottomSheet(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { viewModel.toggleSettings(false) }
            )
        }
    }
}