package com.diversify.solana.seedvault

import android.content.Context
import com.diversify.core.util.Base58
import com.solanamobile.mobilewalletadapter.clientlib.AdapterOperations
import com.solanamobile.mobilewalletadapter.clientlib.AdapterOperations.AppIdentity
import com.solanamobile.mobilewalletadapter.clientlib.MobileWalletAdapterClient
import com.solanamobile.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterConfig
import com.solanamobile.mobilewalletadapter.clientlib.protocol.authorize.AuthorizeResult
import com.solanamobile.mobilewalletadapter.clientlib.protocol.signandsendtransactions.SignAndSendTransactionsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around the Solana Mobile Wallet Adapter client.
 * Exposes a simple connect/sign API used by the viewmodel; under the hood
 * authorization and signing are delegated to the user's wallet app.
 */
class SeedVaultManager(private val context: Context) {

    private val authPrefs = context.getSharedPreferences("mwa_auth", Context.MODE_PRIVATE)
    private val client = MobileWalletAdapterClient()
    private var authToken: String? = authPrefs.getString("auth_token", null)

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

    suspend fun connect(): Result<List<Account>> {
        connectionState.value = ConnectionState.Connecting
        return try {
            // app identity can be arbitrary; wallet will display it
            val identity = AdapterOperations.AppIdentity(
                name = "Diversify",
                uri = "https://diversify.app"
            )
            val authorizeResult: AuthorizeResult = client.authorize(
                appIdentity = identity,
                cluster = "mainnet-beta",
                config = MobileWalletAdapterConfig()
            )

            authToken = authorizeResult.authToken
            authPrefs.edit().putString("auth_token", authToken).apply()
            val keyBytes = authorizeResult.publicKey
            val pubkey = Base58.encode(keyBytes)

            val account = Account(publicKey = pubkey)
            currentAccount.value = account
            connectionState.value = ConnectionState.Connected(listOf(account))
            Result.success(listOf(account))
        } catch (e: Exception) {
            connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    suspend fun signTransaction(
        transaction: ByteArray,
        _account: Account,
        _metadata: Map<String, String> = emptyMap()
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            connectionState.value = ConnectionState.Authenticated
            val signResult: SignAndSendTransactionsResult = client.signAndSendTransactions(
                transactions = arrayOf(transaction),
                minContextSlot = null,
                commitment = null,
                skipPreflight = true,
                maxRetries = null,
                waitForCommitmentToSendNextTransaction = false
            )
            val sig = signResult.signatures.firstOrNull()
                ?: throw IllegalStateException("no signature returned")
            Result.success(sig)
        } catch (e: Exception) {
            connectionState.value = ConnectionState.Error(e.message ?: "Sign failed")
            Result.failure(e)
        }
    }

    fun disconnect() {
        authToken?.let { client.deauthorize(it) }
        authToken = null
        authPrefs.edit().remove("auth_token").apply()
        connectionState.value = ConnectionState.Disconnected
        currentAccount.value = null
    }
}
