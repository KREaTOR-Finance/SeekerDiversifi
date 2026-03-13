package com.diversify.solana.wallet

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.diversify.core.util.Base58
import com.diversify.domain.model.CyclerWallet
import com.diversify.solana.rpc.SolanaRpcClient
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

class CyclerWalletManager @Inject constructor(
    private val context: Context,
    private val rpcClient: SolanaRpcClient
) {

    companion object {
        private const val WALLET_COUNT = 50
        private const val PREFS_NAME = "cycler_wallets"
        private const val MASTER_SEED_KEY = "cycler_master_seed_v1"
    }

    private val secureRandom = SecureRandom()
    private val walletCache = mutableMapOf<Int, CyclerWallet>()

    private val encryptedPrefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun getOrCreateMasterSeed(): ByteArray {
        val stored = encryptedPrefs.getString(MASTER_SEED_KEY, null)
        if (!stored.isNullOrBlank()) {
            return android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        }

        val seed = ByteArray(32)
        secureRandom.nextBytes(seed)
        encryptedPrefs.edit()
            .putString(MASTER_SEED_KEY, android.util.Base64.encodeToString(seed, android.util.Base64.NO_WRAP))
            .apply()
        return seed
    }

    private fun derivePrivateSeed(index: Int): ByteArray {
        val master = getOrCreateMasterSeed()
        val indexBytes = ByteBuffer.allocate(4).putInt(index).array()

        val digest = SHA256Digest()
        digest.update(master, 0, master.size)
        digest.update(indexBytes, 0, indexBytes.size)

        val out = ByteArray(32)
        digest.doFinal(out, 0)
        return out
    }

    private fun deriveKeyPair(index: Int): Pair<ByteArray, ByteArray> {
        val privateSeed = derivePrivateSeed(index)
        val privateKey = Ed25519PrivateKeyParameters(privateSeed, 0)
        val publicKey = privateKey.generatePublicKey().encoded
        return privateSeed to publicKey
    }

    suspend fun getOrCreateWallet(index: Int): CyclerWallet = withContext(Dispatchers.IO) {
        require(index in 0 until WALLET_COUNT) { "Index must be 0-${WALLET_COUNT - 1}" }

        walletCache[index]?.let { return@withContext it }

        val (privateSeed, publicKeyBytes) = deriveKeyPair(index)
        val wallet = CyclerWallet(
            index = index,
            publicKey = Base58.encode(publicKeyBytes),
            balance = 0,
            isActive = true,
            privateSeed = Base58.encode(privateSeed)
        )

        walletCache[index] = wallet
        return@withContext wallet
    }

    suspend fun getAllWallets(): List<CyclerWallet> = withContext(Dispatchers.IO) {
        val wallets = mutableListOf<CyclerWallet>()
        for (i in 0 until WALLET_COUNT) {
            wallets.add(getOrCreateWallet(i))
        }
        wallets
    }

    suspend fun getWalletBalance(index: Int): Result<Long> {
        return try {
            val wallet = getOrCreateWallet(index)
            rpcClient.getBalance(wallet.publicKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createReturnTransaction(
        fromWallet: CyclerWallet,
        toAddress: String,
        amount: Long
    ): ByteArray {
        // TODO: replace with real Solana transfer transaction serialization.
        return "return_tx_${fromWallet.publicKey}_${toAddress}_${amount}".toByteArray()
    }

    fun signReturnPayload(walletIndex: Int, payload: ByteArray): ByteArray {
        val privateSeed = derivePrivateSeed(walletIndex)
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(privateSeed, 0))
        signer.update(payload, 0, payload.size)
        return signer.generateSignature()
    }
}
