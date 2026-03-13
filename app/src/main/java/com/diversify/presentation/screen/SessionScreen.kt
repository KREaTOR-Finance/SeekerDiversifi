package com.diversify.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diversify.presentation.component.MatrixRain
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta
import com.diversify.presentation.screen.LeaderboardScreen
import com.diversify.presentation.viewmodel.SessionViewModel

@Composable
fun SessionScreen(
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        MatrixRain(intensity = 0.7f)
        
        if (uiState.leaderboard != null) {
            LeaderboardScreen(entries = uiState.leaderboard, onClose = { viewModel.clearLeaderboard() })
        } else {
            when {
                !uiState.isVaultReady -> {
                    ConnectVaultScreen(
                        onConnect = { viewModel.connectToVault() },
                        isLoading = uiState.isLoading,
                        error = uiState.error
                    )
                }
                
                !uiState.sessionActive && !uiState.sessionComplete -> {
                    SessionConfigScreen(
                        defaultConfig = uiState.defaultConfig ?: SessionViewModel.SessionConfig(
                            transactionCount = 1,
                            bundlerRatio = 0.5f,
                            curatedTokens = emptyList(),
                            fundingAsset = "SOL"
                        ),
                        onStartSession = viewModel::startSession,
                        onViewLeaderboard = viewModel::loadLeaderboard
                    )
                }
                
                uiState.sessionActive -> {
                    ActiveSessionScreen(
                        currentTransaction = uiState.currentTransaction,
                        progress = uiState.progress,
                        total = uiState.totalTransactions,
                        isSigning = uiState.isSigning,
                        onAnswerSubmit = viewModel::approveTransaction,
                        onSkipTransaction = viewModel::skipTransaction
                    )
                }
                
                uiState.sessionComplete -> {
                    NeutralZoneScreen(
                        onPlayAgain = { viewModel.startNewSession() }
                    )
                }
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
