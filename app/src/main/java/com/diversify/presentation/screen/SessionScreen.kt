package com.diversify.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diversify.presentation.component.MatrixRain
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta
import com.diversify.presentation.viewmodel.SessionViewModel

@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        MatrixRain(intensity = 0.7f)

        when {
            !uiState.isVaultReady -> {
                ConnectVaultScreen(
                    onConnect = { viewModel.connectToVault() },
                    isLoading = uiState.isLoading,
                    error = uiState.error
                )
            }

            !uiState.sessionActive && !uiState.sessionComplete -> {
                if (uiState.showQuickStart) {
                    QuickStartTutorialScreen(
                        onContinue = { viewModel.dismissQuickStartTutorial() }
                    )
                } else {
                    SessionConfigScreen(
                        defaultConfig = uiState.defaultConfig,
                        allowlistTokens = SessionViewModel.cyclerAllowlist,
                        onStartSession = viewModel::startSession
                    )
                }
            }

            uiState.sessionActive -> {
                ActiveSessionScreen(
                    currentTransaction = uiState.currentTransaction,
                    progress = uiState.progress,
                    total = uiState.totalTransactions,
                    isSigning = uiState.isSigning,
                    onAnswerSubmit = viewModel::approveTransaction
                )
            }

            uiState.sessionComplete -> {
                SessionCompleteScreen(onStartAgain = { viewModel.startNewSession() })
            }
        }

        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("DISMISS", color = MatrixMagenta)
                    }
                }
            ) {
                Text("> $error", color = MatrixGreen)
            }
        }

        uiState.infoMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearInfoMessage() }) {
                        Text("OK", color = MatrixMagenta)
                    }
                }
            ) {
                Text(msg, color = MatrixGreen)
            }
        }
    }
}
