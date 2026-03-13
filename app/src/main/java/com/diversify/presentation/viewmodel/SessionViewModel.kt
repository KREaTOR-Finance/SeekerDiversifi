package com.diversify.presentation.viewmodel

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diversify.core.util.Base58
import com.diversify.domain.model.Session
import com.diversify.domain.model.SessionStatus
import com.diversify.domain.model.Transaction
import com.diversify.domain.model.TransactionStatus
import com.diversify.domain.model.TransactionType
import com.diversify.domain.usecase.StartSessionUseCase
import com.diversify.solana.rpc.JupiterClient
import com.diversify.solana.rpc.SolanaRpcClient
import com.diversify.solana.seedvault.SeedVaultManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val seedVaultManager: SeedVaultManager,
    private val startSessionUseCase: StartSessionUseCase,
    private val jupiterClient: JupiterClient,
    private val rpcClient: SolanaRpcClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState(defaultConfig = SessionConfig()))
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var currentSession: Session? = null
    private var currentTransactionIndex = 0
    private var currentTransactions: List<Transaction> = emptyList()
    private var baselineTokenBalances: Map<String, Long> = emptyMap()

    init {
        viewModelScope.launch {
            seedVaultManager.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        vaultState = state,
                        isVaultReady = state is SeedVaultManager.ConnectionState.Connected
                    )
                }
            }
        }
    }

    fun connectToVault() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = seedVaultManager.connect()
            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message, isLoading = false) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun bindActivity(activity: ComponentActivity) {
        seedVaultManager.attachActivity(activity)
    }

    fun startSession(config: SessionConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, infoMessage = null, error = null) }

            val account = seedVaultManager.currentAccount.value
            if (account == null) {
                _uiState.update {
                    it.copy(
                        error = "Wallet connection is required before starting a session.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val totalTransactions = config.totalTransactions
            if (totalTransactions < 2 || totalTransactions % 2 != 0) {
                _uiState.update {
                    it.copy(
                        error = "Total transactions must be an even number of at least 2.",
                        isLoading = false
                    )
                }
                return@launch
            }

            if (config.sessionAmount <= 0.0) {
                _uiState.update {
                    it.copy(
                        error = "Session amount must be greater than 0.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val fundingAsset = config.fundingAsset.uppercase()
            if (!isSupportedFundingAsset(fundingAsset)) {
                _uiState.update {
                    it.copy(
                        error = "Unsupported funding asset. Choose SOL or USDC.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val allowlistTokens = cyclerAllowlist
                .filter { it != fundingAsset }
                .filter { resolveMint(it) != null }
            if (allowlistTokens.isEmpty()) {
                _uiState.update {
                    it.copy(
                        error = "No tradable allowlist tokens are available for the selected funding asset.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val reserveCheck = validateBalances(
                publicKey = account.publicKey,
                fundingAsset = fundingAsset,
                sessionAmount = config.sessionAmount
            )
            if (reserveCheck != null) {
                _uiState.update { it.copy(error = reserveCheck, isLoading = false) }
                return@launch
            }

            val sessionStart = runCatching {
                startSessionUseCase(
                    totalTransactions = totalTransactions,
                    sessionAmount = config.sessionAmount,
                    allowlistTokens = allowlistTokens,
                    fundingAsset = fundingAsset,
                    userId = account.publicKey
                )
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        error = error.message ?: "Unable to start session.",
                        isLoading = false
                    )
                }
                return@launch
            }

            val (session, transactions) = sessionStart
            baselineTokenBalances = captureBaselineBalances(
                publicKey = account.publicKey,
                tokens = allowlistTokens
            )

            currentSession = session
            currentTransactions = transactions
            currentTransactionIndex = 0

            _uiState.update {
                it.copy(
                    currentTransaction = transactions.firstOrNull(),
                    sessionActive = true,
                    sessionComplete = false,
                    progress = 0,
                    totalTransactions = transactions.size,
                    isLoading = false,
                    infoMessage = null
                )
            }
        }
    }

    fun approveTransaction(userAnswer: String) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            val transaction = currentTransactions.getOrNull(currentTransactionIndex) ?: return@launch
            val account = seedVaultManager.currentAccount.value

            if (account == null) {
                _uiState.update { it.copy(error = "No authorized account. Reconnect Seed Vault.", isSigning = false) }
                return@launch
            }

            if (!verifyPuzzle(transaction.puzzle.answer, userAnswer)) {
                _uiState.update { it.copy(error = "Incorrect solution", isSigning = false) }
                return@launch
            }

            _uiState.update { it.copy(isSigning = true) }

            val payloadResult = resolveTransactionPayload(
                session = session,
                transaction = transaction,
                userPublicKey = account.publicKey
            )

            if (payloadResult.isFailure) {
                _uiState.update {
                    it.copy(
                        error = payloadResult.exceptionOrNull()?.message ?: "Unable to create mainnet transaction payload.",
                        isSigning = false
                    )
                }
                return@launch
            }

            val payload = payloadResult.getOrThrow()
            val signResult = seedVaultManager.signTransaction(
                transaction = payload,
                account = account,
                metadata = mapOf(
                    "transaction_id" to transaction.id,
                    "session_id" to session.id
                )
            )

            if (signResult.isFailure) {
                _uiState.update { it.copy(error = signResult.exceptionOrNull()?.message, isSigning = false) }
                return@launch
            }

            val signature = Base58.encode(signResult.getOrThrow())
            val confirmed = awaitTransactionConfirmation(signature)
            if (!confirmed) {
                _uiState.update {
                    it.copy(
                        error = "Transaction was submitted but not confirmed yet. Retry this step.",
                        isSigning = false
                    )
                }
                return@launch
            }

            val updated = transaction.copy(
                rawData = payload,
                status = TransactionStatus.CONFIRMED,
                signature = signature,
                confirmedAt = System.currentTimeMillis()
            )
            currentTransactions = currentTransactions.toMutableList().also { list ->
                list[currentTransactionIndex] = updated
            }

            currentTransactionIndex++
            val complete = currentTransactionIndex >= currentTransactions.size
            if (complete) {
                currentSession = currentSession?.copy(
                    completedAt = System.currentTimeMillis(),
                    status = SessionStatus.COMPLETED
                )
                _uiState.update {
                    it.copy(
                        sessionActive = false,
                        sessionComplete = true,
                        currentTransaction = null,
                        progress = currentTransactions.size,
                        isSigning = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentTransaction = currentTransactions[currentTransactionIndex],
                        progress = currentTransactionIndex,
                        isSigning = false
                    )
                }
            }
        }
    }

    fun startNewSession() {
        currentSession = null
        currentTransactionIndex = 0
        currentTransactions = emptyList()
        baselineTokenBalances = emptyMap()
        _uiState.update {
            SessionUiState(
                vaultState = it.vaultState,
                isVaultReady = it.isVaultReady,
                showQuickStart = false,
                defaultConfig = SessionConfig()
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun dismissQuickStartTutorial() {
        _uiState.update { it.copy(showQuickStart = false) }
    }

    private suspend fun validateBalances(
        publicKey: String,
        fundingAsset: String,
        sessionAmount: Double
    ): String? {
        val solBalance = rpcClient.getBalance(publicKey).getOrElse { error ->
            val reason = error.message ?: "network error"
            return "Unable to read SOL balance from mainnet RPC. $reason"
        }

        if (solBalance < SOL_FEE_RESERVE_LAMPORTS) {
            return "Insufficient SOL for fees. Keep at least 0.02 SOL reserved."
        }

        if (fundingAsset == "SOL") {
            val required = toBaseUnits("SOL", sessionAmount)
                ?: return "Invalid SOL session amount."
            if (solBalance < required + SOL_FEE_RESERVE_LAMPORTS) {
                return "Not enough SOL for session amount plus 0.02 SOL fee reserve."
            }
            return null
        }

        val fundingMint = resolveMint(fundingAsset)
            ?: return "Unsupported funding mint."
        val requiredFundingUnits = toBaseUnits(fundingAsset, sessionAmount)
            ?: return "Invalid funding amount for $fundingAsset."
        val fundingBalance = rpcClient.getTokenBalanceBaseUnits(publicKey, fundingMint).getOrElse { error ->
            val reason = error.message ?: "network error"
            return "Unable to read $fundingAsset balance from mainnet RPC. $reason"
        }
        if (fundingBalance < requiredFundingUnits) {
            return "Insufficient $fundingAsset balance for selected session amount."
        }

        return null
    }

    private suspend fun captureBaselineBalances(
        publicKey: String,
        tokens: List<String>
    ): Map<String, Long> {
        val snapshot = mutableMapOf<String, Long>()
        tokens.forEach { token ->
            val mint = resolveMint(token) ?: return@forEach
            val balance = rpcClient.getTokenBalanceBaseUnits(publicKey, mint).getOrDefault(0L)
            snapshot[token] = balance
        }
        return snapshot
    }

    private suspend fun awaitTransactionConfirmation(signature: String): Boolean {
        repeat(CONFIRMATION_MAX_ATTEMPTS) {
            val status = rpcClient.getTransactionStatus(signature).getOrNull()
            when (status) {
                SolanaRpcClient.TransactionStatus.CONFIRMED -> return true
                SolanaRpcClient.TransactionStatus.FAILED -> return false
                else -> delay(CONFIRMATION_POLL_MS)
            }
        }
        return false
    }

    private fun verifyPuzzle(expectedAnswer: String, userAnswer: String): Boolean {
        return expectedAnswer == userAnswer.trim()
    }

    private suspend fun resolveTransactionPayload(
        session: Session,
        transaction: Transaction,
        userPublicKey: String
    ): Result<ByteArray> {
        val fundingAsset = session.fundingAsset.uppercase()
        return when (transaction.type) {
            TransactionType.BUY -> {
                val outputToken = transaction.token.uppercase()
                val amountBaseUnits = toBaseUnits(fundingAsset, transaction.amount)
                    ?: return Result.failure(IllegalArgumentException("Invalid buy amount."))
                createMainnetSwapPayload(
                    inputToken = fundingAsset,
                    outputToken = outputToken,
                    amountBaseUnits = amountBaseUnits,
                    userPublicKey = userPublicKey
                )
            }

            TransactionType.SELL -> {
                val inputToken = transaction.token.uppercase()
                val inputMint = resolveMint(inputToken)
                    ?: return Result.failure(IllegalArgumentException("Unsupported sell token: $inputToken"))
                val currentBalance = rpcClient.getTokenBalanceBaseUnits(userPublicKey, inputMint).getOrElse {
                    return Result.failure(IllegalStateException("Unable to read token balance for $inputToken"))
                }
                val baseline = baselineTokenBalances[inputToken] ?: 0L
                val amountBaseUnits = (currentBalance - baseline).coerceAtLeast(0L)
                if (amountBaseUnits <= 0L) {
                    return Result.failure(IllegalStateException("No session-acquired $inputToken balance available to sell."))
                }
                createMainnetSwapPayload(
                    inputToken = inputToken,
                    outputToken = fundingAsset,
                    amountBaseUnits = amountBaseUnits,
                    userPublicKey = userPublicKey
                )
            }
        }
    }

    private suspend fun createMainnetSwapPayload(
        inputToken: String,
        outputToken: String,
        amountBaseUnits: Long,
        userPublicKey: String
    ): Result<ByteArray> {
        if (amountBaseUnits <= 0L) {
            return Result.failure(IllegalArgumentException("Swap amount must be greater than zero."))
        }

        val inputMint = resolveMint(inputToken)
            ?: return Result.failure(IllegalArgumentException("Unsupported input token: $inputToken"))
        val outputMint = resolveMint(outputToken)
            ?: return Result.failure(IllegalArgumentException("Unsupported output token: $outputToken"))

        if (inputMint == outputMint) {
            return Result.failure(IllegalArgumentException("Input and output tokens must differ."))
        }

        return jupiterClient.getSwapTransaction(
            inputMint = inputMint,
            outputMint = outputMint,
            amountLamports = amountBaseUnits,
            userPubkey = userPublicKey
        )
    }

    private fun isSupportedFundingAsset(token: String): Boolean {
        return token == "SOL" || token == "USDC"
    }

    private fun resolveMint(token: String): String? {
        return when (token.uppercase()) {
            "SOL" -> "So11111111111111111111111111111111111111112"
            "USDC" -> "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
            "JUP" -> "JUPyiwrYJFskUPiHa7hkeR8VUtAeFoSYbKedZNsDvCN"
            "BONK" -> "DezXAZ8z7PnrnRJjz3wXBoRgixCa6gYm4C7YaB1pPB263"
            else -> null
        }
    }

    private fun toBaseUnits(inputToken: String, amount: Double): Long? {
        if (amount <= 0.0) return null
        val decimals = when (inputToken.uppercase()) {
            "USDC" -> 6
            else -> 9
        }
        return try {
            BigDecimal.valueOf(amount)
                .movePointRight(decimals)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    data class SessionUiState(
        val vaultState: SeedVaultManager.ConnectionState = SeedVaultManager.ConnectionState.Disconnected,
        val isVaultReady: Boolean = false,
        val showQuickStart: Boolean = true,
        val sessionActive: Boolean = false,
        val sessionComplete: Boolean = false,
        val currentTransaction: Transaction? = null,
        val progress: Int = 0,
        val totalTransactions: Int = 0,
        val isLoading: Boolean = false,
        val isSigning: Boolean = false,
        val error: String? = null,
        val infoMessage: String? = null,
        val defaultConfig: SessionConfig = SessionConfig()
    )

    data class SessionConfig(
        val totalTransactions: Int = 10,
        val sessionAmount: Double = 0.1,
        val fundingAsset: String = "SOL"
    )

    companion object {
        val cyclerAllowlist = listOf("USDC", "JUP", "BONK")
        private const val SOL_FEE_RESERVE_LAMPORTS = 20_000_000L
        private const val CONFIRMATION_MAX_ATTEMPTS = 15
        private const val CONFIRMATION_POLL_MS = 1500L
    }
}
