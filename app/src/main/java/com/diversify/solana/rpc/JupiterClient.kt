package com.diversify.solana.rpc

import com.diversify.app.BuildConfig
import com.diversify.core.util.Base58
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JupiterClient @Inject constructor() {
    private val client = OkHttpClient()
    private val quoteUrl = "https://api.jup.ag/swap/v1/quote"
    private val swapUrl = "https://api.jup.ag/swap/v1/swap"

    suspend fun getSwapTransaction(
        inputMint: String,
        outputMint: String,
        amountLamports: Long,
        userPubkey: String
    ): Result<ByteArray> {
        return try {
            val quoteRequest = buildQuoteRequest(inputMint, outputMint, amountLamports)
            val quoteJson = client.newCall(quoteRequest).execute().use { quoteResponse ->
                if (!quoteResponse.isSuccessful) {
                    return Result.failure(IOException("Jupiter quote error ${quoteResponse.code}"))
                }
                val quoteBody = quoteResponse.body?.string() ?: ""
                val parsed = JSONObject(quoteBody)
                if (parsed.has("error")) {
                    return Result.failure(IOException(parsed.optString("error", "Jupiter quote failure")))
                }
                validateQuoteInvariant(
                    quoteJson = parsed,
                    expectedInputMint = inputMint,
                    expectedOutputMint = outputMint,
                    expectedAmountLamports = amountLamports
                ).onFailure { return Result.failure(it) }
                parsed
            }

            val swapBodyJson = JSONObject()
                .put("quoteResponse", quoteJson)
                .put("userPublicKey", userPubkey)
                .put("dynamicComputeUnitLimit", true)
                .put("dynamicSlippage", true)
                .put("asLegacyTransaction", true)

            val swapBody = swapBodyJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())
            val swapRequest = withAuthHeaders(Request.Builder())
                .url(swapUrl)
                .post(swapBody)
                .build()

            val swapTx = client.newCall(swapRequest).execute().use { swapResponse ->
                if (!swapResponse.isSuccessful) {
                    return Result.failure(IOException("Jupiter swap error ${swapResponse.code}"))
                }

                val swapResponseBody = swapResponse.body?.string() ?: ""
                val swapJson = JSONObject(swapResponseBody)
                if (swapJson.has("error")) {
                    return Result.failure(IOException(swapJson.optString("error", "Jupiter swap failure")))
                }
                validateSwapInvariant(
                    swapJson = swapJson,
                    expectedUserPubkey = userPubkey,
                    expectedInputMint = inputMint,
                    expectedAmountLamports = amountLamports
                ).onFailure { return Result.failure(it) }
                swapJson.getString("swapTransaction")
            }

            val bytes = android.util.Base64.decode(swapTx, android.util.Base64.DEFAULT)
            if (bytes.isEmpty()) {
                return Result.failure(IOException("Jupiter swap payload was empty."))
            }
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildQuoteRequest(inputMint: String, outputMint: String, amountLamports: Long): Request {
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("api.jup.ag")
            .addPathSegment("swap")
            .addPathSegment("v1")
            .addPathSegment("quote")
            .addQueryParameter("inputMint", inputMint)
            .addQueryParameter("outputMint", outputMint)
            .addQueryParameter("amount", amountLamports.toString())
            .addQueryParameter("slippageBps", "50")
            .addQueryParameter("restrictIntermediateTokens", "true")
            .build()

        return withAuthHeaders(Request.Builder())
            .url(url)
            .get()
            .build()
    }

    private fun withAuthHeaders(builder: Request.Builder): Request.Builder {
        val apiKey = BuildConfig.JUP_API_KEY
        return if (apiKey.isBlank()) {
            builder
        } else {
            builder.addHeader("x-api-key", apiKey)
        }
    }

    private fun validateQuoteInvariant(
        quoteJson: JSONObject,
        expectedInputMint: String,
        expectedOutputMint: String,
        expectedAmountLamports: Long
    ): Result<Unit> {
        val quoteInputMint = quoteJson.optString("inputMint")
        val quoteOutputMint = quoteJson.optString("outputMint")
        val quoteInAmount = quoteJson.optString("inAmount").toLongOrNull()
        val routePlan = quoteJson.optJSONArray("routePlan")
        val swapMode = quoteJson.optString("swapMode")

        if (quoteInputMint != expectedInputMint) {
            return Result.failure(IOException("Quote invariant failed: unexpected input mint."))
        }
        if (quoteOutputMint != expectedOutputMint) {
            return Result.failure(IOException("Quote invariant failed: unexpected output mint."))
        }
        if (quoteInAmount == null || quoteInAmount != expectedAmountLamports) {
            return Result.failure(IOException("Quote invariant failed: unexpected inAmount."))
        }
        if (routePlan == null || routePlan.length() == 0) {
            return Result.failure(IOException("Quote invariant failed: no routePlan available."))
        }
        if (swapMode.isBlank()) {
            return Result.failure(IOException("Quote invariant failed: missing swap mode."))
        }

        return Result.success(Unit)
    }

    private fun validateSwapInvariant(
        swapJson: JSONObject,
        expectedUserPubkey: String,
        expectedInputMint: String,
        expectedAmountLamports: Long
    ): Result<Unit> {
        val swapTx = swapJson.optString("swapTransaction")
        if (swapTx.isBlank()) {
            return Result.failure(IOException("Swap invariant failed: missing swapTransaction."))
        }

        if (swapJson.has("lastValidBlockHeight")) {
            val lastValidBlockHeight = swapJson.optLong("lastValidBlockHeight", -1L)
            if (lastValidBlockHeight <= 0L) {
                return Result.failure(IOException("Swap invariant failed: invalid lastValidBlockHeight."))
            }
        }

        val txBytes = try {
            android.util.Base64.decode(swapTx, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            return Result.failure(IOException("Swap invariant failed: invalid base64 transaction payload.", e))
        }

        val parsed = parseLegacyTransaction(txBytes)
            .getOrElse { return Result.failure(it) }

        if (parsed.header.numRequiredSignatures != 1) {
            return Result.failure(IOException("Swap invariant failed: unexpected signer count."))
        }
        if (parsed.header.numReadonlySignedAccounts != 0) {
            return Result.failure(IOException("Swap invariant failed: fee payer must be writable."))
        }
        if (parsed.signatureCount != parsed.header.numRequiredSignatures) {
            return Result.failure(IOException("Swap invariant failed: signature header mismatch."))
        }
        if (parsed.accountKeys.isEmpty()) {
            return Result.failure(IOException("Swap invariant failed: no account keys found."))
        }

        val feePayer = Base58.encode(parsed.accountKeys.first())
        if (feePayer != expectedUserPubkey) {
            return Result.failure(IOException("Swap invariant failed: fee payer does not match connected wallet."))
        }
        if (parsed.instructions.isEmpty()) {
            return Result.failure(IOException("Swap invariant failed: no instructions present."))
        }

        val accountKeyStrings = parsed.accountKeys.map { Base58.encode(it) }
        val maxAllowedSystemDebit = if (expectedInputMint == SOL_MINT) {
            expectedAmountLamports + SOL_WRAP_OVERHEAD_LAMPORTS
        } else {
            NON_SOL_SYSTEM_DEBIT_MAX_LAMPORTS
        }
        var debitsFromFeePayer = 0L

        parsed.instructions.forEach { ix ->
            if (ix.programIdIndex !in accountKeyStrings.indices) {
                return Result.failure(IOException("Swap invariant failed: invalid instruction program index."))
            }
            if (ix.accountIndices.any { it !in accountKeyStrings.indices }) {
                return Result.failure(IOException("Swap invariant failed: invalid instruction account index."))
            }

            val programId = accountKeyStrings[ix.programIdIndex]
            if (programId == SYSTEM_PROGRAM_ID) {
                val lamportDebit = extractSystemLamportDebitForFeePayer(
                    instruction = ix,
                    accountKeys = accountKeyStrings,
                    feePayer = feePayer
                ).getOrElse { return Result.failure(it) }
                debitsFromFeePayer += lamportDebit
            }
        }

        if (debitsFromFeePayer > maxAllowedSystemDebit) {
            return Result.failure(
                IOException(
                    "Swap invariant failed: SOL debit exceeds allowed bound. " +
                        "Observed=$debitsFromFeePayer Allowed=$maxAllowedSystemDebit"
                )
            )
        }

        return Result.success(Unit)
    }

    private fun parseLegacyTransaction(transactionBytes: ByteArray): Result<ParsedLegacyTransaction> {
        return try {
            val cursor = ByteCursor(transactionBytes)
            val signatureCount = cursor.readShortVec()
            if (signatureCount <= 0) {
                return Result.failure(IOException("Swap invariant failed: no signatures declared."))
            }
            repeat(signatureCount) {
                cursor.readBytes(64)
            }

            val firstMessageByte = cursor.peekU8()
            if ((firstMessageByte and 0x80) != 0) {
                return Result.failure(IOException("Swap invariant failed: versioned transactions are not allowed."))
            }

            val header = MessageHeader(
                numRequiredSignatures = cursor.readU8(),
                numReadonlySignedAccounts = cursor.readU8(),
                numReadonlyUnsignedAccounts = cursor.readU8()
            )

            val accountCount = cursor.readShortVec()
            if (accountCount <= 0) {
                return Result.failure(IOException("Swap invariant failed: account list is empty."))
            }
            val accountKeys = MutableList(accountCount) { cursor.readBytes(32) }
            cursor.readBytes(32) // recent blockhash

            val instructionCount = cursor.readShortVec()
            val instructions = MutableList(instructionCount) {
                val programIdIndex = cursor.readU8()
                val accountIndexCount = cursor.readShortVec()
                val accountIndices = MutableList(accountIndexCount) { cursor.readU8() }
                val dataLength = cursor.readShortVec()
                val data = cursor.readBytes(dataLength)
                CompiledInstruction(
                    programIdIndex = programIdIndex,
                    accountIndices = accountIndices,
                    data = data
                )
            }

            if (cursor.remaining() != 0) {
                return Result.failure(IOException("Swap invariant failed: trailing bytes in transaction payload."))
            }

            Result.success(
                ParsedLegacyTransaction(
                    signatureCount = signatureCount,
                    header = header,
                    accountKeys = accountKeys,
                    instructions = instructions
                )
            )
        } catch (e: Exception) {
            Result.failure(IOException("Swap invariant failed: cannot parse legacy transaction.", e))
        }
    }

    private fun extractSystemLamportDebitForFeePayer(
        instruction: CompiledInstruction,
        accountKeys: List<String>,
        feePayer: String
    ): Result<Long> {
        if (instruction.accountIndices.isEmpty()) {
            return Result.failure(IOException("Swap invariant failed: malformed system instruction."))
        }
        if (instruction.data.size < 4) {
            return Result.failure(IOException("Swap invariant failed: malformed system instruction data."))
        }

        val fromAccountIndex = instruction.accountIndices.first()
        val fromAccount = accountKeys.getOrNull(fromAccountIndex)
            ?: return Result.failure(IOException("Swap invariant failed: system instruction account out of range."))
        if (fromAccount != feePayer) {
            return Result.success(0L)
        }

        val instructionType = readLeU32(instruction.data, 0)
        val lamports = when (instructionType) {
            0 -> { // CreateAccount
                if (instruction.data.size < 12) {
                    return Result.failure(IOException("Swap invariant failed: invalid CreateAccount payload."))
                }
                readLeU64(instruction.data, 4)
            }

            2 -> { // Transfer
                if (instruction.data.size < 12) {
                    return Result.failure(IOException("Swap invariant failed: invalid Transfer payload."))
                }
                readLeU64(instruction.data, 4)
            }

            3 -> { // CreateAccountWithSeed
                if (instruction.data.size < 44) {
                    return Result.failure(IOException("Swap invariant failed: invalid CreateAccountWithSeed payload."))
                }
                val seedLen = readLeU64(instruction.data, 36)
                val lamportsOffset = 44L + seedLen
                if (lamportsOffset < 0 || lamportsOffset > Int.MAX_VALUE.toLong()) {
                    return Result.failure(IOException("Swap invariant failed: invalid seed length in system instruction."))
                }
                if (instruction.data.size < lamportsOffset.toInt() + 8) {
                    return Result.failure(IOException("Swap invariant failed: truncated CreateAccountWithSeed payload."))
                }
                readLeU64(instruction.data, lamportsOffset.toInt())
            }

            11 -> { // TransferWithSeed
                if (instruction.data.size < 12) {
                    return Result.failure(IOException("Swap invariant failed: invalid TransferWithSeed payload."))
                }
                readLeU64(instruction.data, 4)
            }

            1, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15 -> 0L
            else -> {
                return Result.failure(IOException("Swap invariant failed: unsupported system instruction type."))
            }
        }

        return Result.success(lamports)
    }

    private fun readLeU32(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readLeU64(data: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 8) {
            value = value or ((data[offset + i].toLong() and 0xFFL) shl (8 * i))
        }
        return value
    }

    private data class ParsedLegacyTransaction(
        val signatureCount: Int,
        val header: MessageHeader,
        val accountKeys: List<ByteArray>,
        val instructions: List<CompiledInstruction>
    )

    private data class MessageHeader(
        val numRequiredSignatures: Int,
        val numReadonlySignedAccounts: Int,
        val numReadonlyUnsignedAccounts: Int
    )

    private data class CompiledInstruction(
        val programIdIndex: Int,
        val accountIndices: List<Int>,
        val data: ByteArray
    )

    private class ByteCursor(private val bytes: ByteArray) {
        private var position: Int = 0

        fun remaining(): Int = bytes.size - position

        fun peekU8(): Int {
            if (remaining() < 1) {
                throw IOException("Unexpected end of transaction bytes.")
            }
            return bytes[position].toInt() and 0xFF
        }

        fun readU8(): Int {
            val v = peekU8()
            position += 1
            return v
        }

        fun readBytes(count: Int): ByteArray {
            if (count < 0 || remaining() < count) {
                throw IOException("Unexpected end of transaction bytes.")
            }
            val out = bytes.copyOfRange(position, position + count)
            position += count
            return out
        }

        fun readShortVec(): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = readU8()
                result = result or ((b and 0x7F) shl shift)
                if ((b and 0x80) == 0) {
                    break
                }
                shift += 7
                if (shift > 28) {
                    throw IOException("Invalid shortvec length.")
                }
            }
            return result
        }
    }

    companion object {
        private const val SYSTEM_PROGRAM_ID = "11111111111111111111111111111111"
        private const val SOL_MINT = "So11111111111111111111111111111111111111112"
        private const val SOL_WRAP_OVERHEAD_LAMPORTS = 30_000_000L
        private const val NON_SOL_SYSTEM_DEBIT_MAX_LAMPORTS = 30_000_000L
    }
}
