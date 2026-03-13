package com.diversify.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diversify.core.util.Base58
import com.diversify.domain.model.Session
import com.diversify.domain.model.Transaction
import com.diversify.domain.usecase.GenerateTransactionBatchUseCase
import com.diversify.solana.seedvault.SeedVaultManager
import com.diversify.data.repository.SettingsRepository
import com.diversify.domain.usecase.StartSessionUseCase
import com.diversify.solana.rpc.SolanaRpcClient

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val seedVaultManager: SeedVaultManager,
    private val generateTransactionBatch: GenerateTransactionBatchUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: com.diversify.data.repository.SessionRepository,
    private val returnScheduler: com.diversify.solana.wallet.ReturnScheduler,
    private val rpcClient: SolanaRpcClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // store config defaults loaded from settings
    private var defaultConfig = SessionConfig(
        transactionCount = 50,
        bundlerRatio = 0.5f,
        curatedTokens = emptyList(),
        fundingAsset = "SOL"
    )
    
    private var currentSession: Session? = null
    private var currentTransactionIndex = 0
    private var currentTransactions: List<Transaction> = emptyList()
    
    init {
        // watch wallet connection
        viewModelScope.launch {
            seedVaultManager.connectionState.collect { state ->
                _uiState.update { it.copy(
                    vaultState = state,
                    isVaultReady = state is SeedVaultManager.ConnectionState.Connected
                ) }
            }
        }

        // load saved configuration and any active session
        viewModelScope.launch {
            loadSettings()
            resumeExistingSession()
        }
    }
    
    fun connectToVault() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val result = seedVaultManager.connect()
            result.onFailure { error ->
                _uiState.update { it.copy(
                    error = error.message,
                    isLoading = false
                ) }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
    
    fun startSession(config: SessionConfig) {
        viewModelScope.launch {
            // persist user choice for next time
            settingsRepository.setTransactionCount(config.transactionCount)
            settingsRepository.setBundlerRatio(config.bundlerRatio)
            settingsRepository.setCuratedTokens(config.curatedTokens)
            settingsRepository.setFundingAsset(config.fundingAsset)

            _uiState.update { it.copy(isLoading = true, infoMessage = null) }

            // generate a pseudo-unique user id for this device/app
            val userId = "local_${System.currentTimeMillis()}"
            // immediately continue to start the session without any backend payment
            continueStartSession(config, userId)
        }
    }


    private fun continueStartSession(config: SessionConfig, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (session, transactions) = startSessionUseCase(
                transactionCount = config.transactionCount,
                bundlerRatio = config.bundlerRatio,
                curatedTokens = config.curatedTokens,
                fundingAsset = config.fundingAsset,
                userId = userId
            )
            currentSession = session
            currentTransactions = transactions
            currentTransactionIndex = 0
            _uiState.update { it.copy(
                currentTransaction = transactions.firstOrNull(),
                sessionActive = true,
                progress = 0,
                totalTransactions = transactions.size,
                isLoading = false,
                infoMessage = null
            )}
        }
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            // offline leaderboard: aggregate completed sessions from local database
            val sessions = sessionRepository.getAllSessions().first()
            val counts = sessions.groupingBy { it.userId }.eachCount()
            val list = counts.map { LeaderboardEntry(it.key, it.value) }
            _uiState.update { it.copy(leaderboard = list) }
        }
    }
    
    fun approveTransaction(userAnswer: String) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            val transactions = currentTransactions
            val transaction = transactions.getOrNull(currentTransactionIndex) ?: return@launch
            
            _uiState.update { it.copy(isSigning = true) }
            
            if (!verifyPuzzle(transaction.puzzle, userAnswer)) {
                _uiState.update { it.copy(
                    error = "Incorrect solution",
                    isSigning = false
                )}
                return@launch
            }
            
            val account = seedVaultManager.currentAccount.value
            if (account == null) {
                _uiState.update {
                    it.copy(
                        error = "No authorized account. Reconnect Seed Vault.",
                        isSigning = false
                    )
                }
                return@launch
            }
            
            val result = seedVaultManager.signTransaction(
                transaction = transaction.rawData,
                account = account,
                metadata = mapOf(
                    "transaction_id" to transaction.id,
                    "session_id" to session.id
                )
            )
            
            result.onSuccess { signedTransaction ->
                // persist update: status signed + signature
                val sigString = Base58.encode(signedTransaction)
                var confirmedAtTimestamp: Long? = null

                // attempt to confirm via RPC (async, best effort)
                rpcClient.getTransactionStatus(sigString).onSuccess { status ->
                    if (status == SolanaRpcClient.TransactionStatus.CONFIRMED) {
                        confirmedAtTimestamp = System.currentTimeMillis()
                    }
                }

                val updatedEntity = com.diversify.data.local.entity.TransactionEntity(
                    id = transaction.id,
                    sessionId = transaction.sessionId,
                    type = transaction.type.name,
                    amount = transaction.amount,
                    token = transaction.token,
                    destinationWallet = transaction.destinationWallet,
                    rawData = transaction.rawData,
                    walletIndex = transaction.metadata?.get("walletIndex")?.toString()?.toIntOrNull(),
                    status = com.diversify.domain.model.TransactionStatus.SIGNED.name,
                    signature = sigString,
                    puzzleId = transaction.puzzle.id,
                    puzzleQuestion = transaction.puzzle.question,
                    puzzleAnswer = transaction.puzzle.answer,
                    puzzleType = transaction.puzzle.type.name,
                    puzzleDifficulty = transaction.puzzle.difficulty.name,
                    puzzleSolved = transaction.puzzleSolved,
                    puzzleTimeMs = transaction.puzzleTimeMs,
                    createdAt = transaction.createdAt,
                    confirmedAt = confirmedAtTimestamp
                )
                sessionRepository.updateTransaction(updatedEntity)

                // if this was a bundler transfer, schedule dummy wallet return
                if (transaction.type == com.diversify.domain.model.TransactionType.BUNDLER_TRANSFER) {
                    val idx = transaction.metadata?.get("walletIndex")?.toIntOrNull() ?: 0
                    returnScheduler.scheduleReturn(session.id, idx, transaction.amount)
                }

                currentTransactionIndex++
                
                val isComplete = currentTransactionIndex >= transactions.size
                
                if (isComplete) {
                    // update session entity to completed
                    currentSession?.let { sess ->
                        val updatedEntity2 = com.diversify.data.local.entity.SessionEntity(
                            id = sess.id,
                            userId = sess.userId,
                            transactionCount = sess.transactionCount,
                            bundlerRatio = sess.bundlerRatio,
                            curatedTokens = sess.curatedTokens.joinToString(","),
                            fundingAsset = sess.fundingAsset,
                            startedAt = sess.startedAt,
                            completedAt = System.currentTimeMillis(),
                            status = com.diversify.domain.model.SessionStatus.COMPLETED.name,
                            feePaid = sess.feePaid,
                            skrRewardEarned = sess.skrRewardEarned,
                            governanceTokensEarned = sess.governanceTokensEarned
                        )
                        sessionRepository.updateSession(updatedEntity2)
                    }
                    _uiState.update { it.copy(
                        sessionActive = false,
                        sessionComplete = true,
                        progress = transactions.size,
                        isSigning = false
                    )}
                } else {
                    _uiState.update { it.copy(
                        currentTransaction = transactions[currentTransactionIndex],
                        progress = currentTransactionIndex,
                        isSigning = false
                    )}
                }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    error = error.message,
                    isSigning = false
                )}
            }
        }
    }
    
    fun startNewSession() {
        _uiState.update { SessionUiState() }
        currentSession = null
        currentTransactionIndex = 0
        currentTransactions = emptyList()
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearLeaderboard() {
        _uiState.update { it.copy(leaderboard = null) }
    }

    fun skipTransaction() {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            val transactions = currentTransactions
            val transaction = transactions.getOrNull(currentTransactionIndex) ?: return@launch

            // mark skipped in database
            val updatedEntity = com.diversify.data.local.entity.TransactionEntity(
                id = transaction.id,
                sessionId = transaction.sessionId,
                type = transaction.type.name,
                amount = transaction.amount,
                token = transaction.token,
                destinationWallet = transaction.destinationWallet,
                rawData = transaction.rawData,
                walletIndex = transaction.metadata?.get("walletIndex")?.toIntOrNull(),
                status = com.diversify.domain.model.TransactionStatus.SKIPPED.name,
                signature = transaction.signature,
                puzzleId = transaction.puzzle.id,
                puzzleQuestion = transaction.puzzle.question,
                puzzleAnswer = transaction.puzzle.answer,
                puzzleType = transaction.puzzle.type.name,
                puzzleDifficulty = transaction.puzzle.difficulty.name,
                puzzleSolved = transaction.puzzleSolved,
                puzzleTimeMs = transaction.puzzleTimeMs,
                createdAt = transaction.createdAt,
                confirmedAt = transaction.confirmedAt
            )
            sessionRepository.updateTransaction(updatedEntity)

            currentTransactionIndex++
            val isComplete = currentTransactionIndex >= transactions.size
            if (isComplete) {
                // finalize session in database
                currentSession?.let { sess ->
                    val updatedEntity = com.diversify.data.local.entity.SessionEntity(
                        id = sess.id,
                        userId = sess.userId,
                        transactionCount = sess.transactionCount,
                        bundlerRatio = sess.bundlerRatio,
                        curatedTokens = sess.curatedTokens.joinToString(","),
                        fundingAsset = sess.fundingAsset,
                        startedAt = sess.startedAt,
                        completedAt = System.currentTimeMillis(),
                        status = com.diversify.domain.model.SessionStatus.COMPLETED.name,
                        feePaid = sess.feePaid,
                        skrRewardEarned = sess.skrRewardEarned,
                        governanceTokensEarned = sess.governanceTokensEarned
                    )
                    sessionRepository.updateSession(updatedEntity)
                }
                _uiState.update { it.copy(
                    sessionActive = false,
                    sessionComplete = true,
                    progress = transactions.size,
                    isSigning = false
                )}
            } else {
                _uiState.update { it.copy(
                    currentTransaction = transactions[currentTransactionIndex],
                    progress = currentTransactionIndex,
                    isSigning = false
                )}
            }
        }
    }

    private suspend fun loadSettings() {
        val count = settingsRepository.getTransactionCount().takeIf { it > 0 } ?: 50
        val ratio = settingsRepository.getBundlerRatio().takeIf { it > 0f } ?: 0.5f
        val tokens = settingsRepository.getCuratedTokens().takeIf { it.isNotEmpty() }
            ?: listOf("USDC","USDT","JUP","RAY","JITO","BONK","PUMP","PENGU")
        val asset = settingsRepository.getFundingAsset().takeIf { it.isNotBlank() } ?: "SOL"
        defaultConfig = SessionConfig(count, ratio, tokens, fundingAsset = asset)
        _uiState.update { it.copy(defaultConfig = defaultConfig) }
    }

    private suspend fun resumeExistingSession() {
        val active = sessionRepository.getActiveSession()
        if (active != null) {
            currentSession = // convert to domain model
                Session(
                    id = active.id,
                    userId = active.userId,
                    transactionCount = active.transactionCount,
                    bundlerRatio = active.bundlerRatio,
                    curatedTokens = active.curatedTokens.split(","),
                    fundingAsset = active.fundingAsset,
                    startedAt = active.startedAt,
                    completedAt = active.completedAt,
                    status = com.diversify.domain.model.SessionStatus.valueOf(active.status),
                    feePaid = active.feePaid,
                    skrRewardEarned = active.skrRewardEarned,
                    governanceTokensEarned = active.governanceTokensEarned
                )
            // maybe track funding asset if needed
            // load transactions from repo
            val txEntities = sessionRepository.getTransactionsForSession(active.id).first()
            currentTransactions = txEntities.map { /* convert entity -> domain */
                Transaction(
                    id = it.id,
                    sessionId = it.sessionId,
                    type = com.diversify.domain.model.TransactionType.valueOf(it.type),
                    amount = it.amount,
                    token = it.token,
                    destinationWallet = it.destinationWallet,
                    puzzle = com.diversify.domain.model.MathPuzzle(
                        id = it.puzzleId ?: "",
                        question = it.puzzleQuestion ?: "",
                        answer = it.puzzleAnswer ?: "",
                        type = com.diversify.domain.model.PuzzleType.valueOf(
                            it.puzzleType ?: com.diversify.domain.model.PuzzleType.ADDITION.name
                        ),
                        difficulty = com.diversify.domain.model.PuzzleDifficulty.valueOf(
                            it.puzzleDifficulty ?: com.diversify.domain.model.PuzzleDifficulty.EASY.name
                        )
                    ),
                    rawData = it.rawData ?: byteArrayOf(),
                    status = com.diversify.domain.model.TransactionStatus.valueOf(it.status),
                    signature = it.signature,
                    puzzleSolved = it.puzzleSolved,
                    puzzleTimeMs = it.puzzleTimeMs,
                    createdAt = it.createdAt,
                    confirmedAt = it.confirmedAt
                ).also { tx ->
                    it.walletIndex?.let { idx ->
                        tx.metadata = mapOf("walletIndex" to idx.toString())
                    }
                }
            }
            val confirmed = sessionRepository.getCompletedTransactionsCount(active.id)
            currentTransactionIndex = confirmed
            _uiState.update {
                it.copy(
                    sessionActive = true,
                    currentTransaction = currentTransactions.getOrNull(currentTransactionIndex),
                    progress = confirmed,
                    totalTransactions = currentTransactions.size
                )
            }
        }
    }
    
    private fun verifyPuzzle(puzzle: com.diversify.domain.model.MathPuzzle, userAnswer: String): Boolean {
        return puzzle.answer == userAnswer.trim()
    }
    
    private fun generateSessionId(): String {
        return "sess_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    data class SessionUiState(
        val vaultState: SeedVaultManager.ConnectionState = SeedVaultManager.ConnectionState.Disconnected,
        val isVaultReady: Boolean = false,
        val sessionActive: Boolean = false,
        val sessionComplete: Boolean = false,
        val currentTransaction: Transaction? = null,
        val progress: Int = 0,
        val totalTransactions: Int = 0,
        val isLoading: Boolean = false,
        val isSigning: Boolean = false,
        val error: String? = null,
        val infoMessage: String? = null,
        val leaderboard: List<LeaderboardEntry>? = null,
        val defaultConfig: SessionConfig? = null
    )


    data class LeaderboardEntry(val userId: String, val completions: Int)
    
    data class SessionConfig(
        val transactionCount: Int,
        val bundlerRatio: Float,
        val curatedTokens: List<String>,
        val fundingAsset: String = "SOL" // "SOL" or "SKR"
    )
}
