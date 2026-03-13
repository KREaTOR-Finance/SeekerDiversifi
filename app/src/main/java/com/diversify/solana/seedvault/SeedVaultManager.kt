package com.diversify.solana.seedvault

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import com.diversify.core.util.Base58
import com.diversify.app.BuildConfig
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionParams
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class SeedVaultManager(private val context: Context) {

    private var activitySender: ActivityResultSender? = null

    private val walletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://diversify.app"),
            iconUri = Uri.parse("icon.png"),
            identityName = "Diversify"
        )
    ).apply {
        authToken = null
        blockchain = when (BuildConfig.SOLANA_CLUSTER.lowercase()) {
            "mainnet", "mainnet-beta" -> Solana.Mainnet
            "testnet" -> Solana.Testnet
            else -> Solana.Devnet
        }
    }

    val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val currentAccount = MutableStateFlow<Account?>(null)

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Authenticated : ConnectionState()
        data class Connected(val accounts: List<Account>) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    data class Account(
        val publicKey: String,
        val label: String? = null,
        val isGenesisToken: Boolean = false
    )

    fun attachActivity(activity: ComponentActivity) {
        activitySender = ActivityResultSender(activity)
    }

    suspend fun connect(): Result<List<Account>> {
        connectionState.value = ConnectionState.Connecting
        val sender = activitySender
            ?: return reportFailure("Wallet connect requires an active screen. Keep the app open and try again.")

        return withContext(Dispatchers.Main.immediate) {
            when (val result = walletAdapter.transact(sender) { authResult ->
                authResult.accounts.map { account ->
                    Account(
                        publicKey = Base58.encode(account.publicKey),
                        label = account.accountLabel
                    )
                }
            }) {
                is TransactionResult.Success -> {
                    val accounts = result.payload
                    if (accounts.isEmpty()) {
                        reportFailure("Wallet returned no accounts.")
                    } else {
                        currentAccount.value = accounts.first()
                        connectionState.value = ConnectionState.Connected(accounts)
                        Result.success(accounts)
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    reportFailure(result.message)
                }

                is TransactionResult.Failure -> {
                    handleWalletFailure(result.message, result.e)
                }
            }
        }
    }

    suspend fun signTransaction(
        transaction: ByteArray,
        account: Account,
        metadata: Map<String, String> = emptyMap()
    ): Result<ByteArray> {
        val sender = activitySender
            ?: return reportFailure("Wallet signing requires an active screen. Keep the app open and try again.")

        @Suppress("UNUSED_VARIABLE")
        val ignoredInputs = account.publicKey to metadata.size

        connectionState.value = ConnectionState.Authenticated

        return withContext(Dispatchers.Main.immediate) {
            when (
                val result = walletAdapter.transact(sender) {
                    val txResult = signAndSendTransactions(
                        transactions = arrayOf(transaction),
                        params = TransactionParams(
                            minContextSlot = null,
                            commitment = "confirmed",
                            skipPreflight = false,
                            maxRetries = 3,
                            waitForCommitmentToSendNextTransaction = false
                        )
                    )
                    txResult.signatures.firstOrNull()
                        ?: throw IllegalStateException("Wallet did not return a transaction signature")
                }
            ) {
                is TransactionResult.Success -> {
                    connectionState.value = ConnectionState.Connected(listOf(account))
                    Result.success(result.payload)
                }

                is TransactionResult.NoWalletFound -> {
                    reportFailure(result.message)
                }

                is TransactionResult.Failure -> {
                    handleWalletFailure(result.message, result.e)
                }
            }
        }
    }

    fun disconnect() {
        clearAuthToken()
        connectionState.value = ConnectionState.Disconnected
        currentAccount.value = null
    }

    private fun clearAuthToken() {
        walletAdapter.authToken = null
    }

    private fun <T> handleWalletFailure(message: String, error: Exception): Result<T> {
        if (message.contains("Auth token invalid", ignoreCase = true)) {
            clearAuthToken()
            currentAccount.value = null
            connectionState.value = ConnectionState.Disconnected
            return Result.failure(IllegalStateException("Wallet session expired. Reconnect your wallet.", error))
        }

        return reportFailure(mapWalletMessage(message), error)
    }

    private fun <T> reportFailure(message: String, error: Exception? = null): Result<T> {
        connectionState.value = ConnectionState.Error(message)
        return Result.failure(error ?: IllegalStateException(message))
    }

    private fun mapWalletMessage(rawMessage: String): String {
        return when {
            rawMessage.contains("No compatible wallet found", ignoreCase = true) ->
                "No compatible Solana wallet found on this device."

            rawMessage.contains("did not authorize signing", ignoreCase = true) ->
                "Transaction approval was cancelled in wallet."

            rawMessage.contains("Request was interrupted", ignoreCase = true) ->
                "Wallet request cancelled before completion."

            rawMessage.contains("Timed out", ignoreCase = true) ->
                "Wallet request timed out. Reopen wallet and try again."

            else -> rawMessage
        }
    }
}
