package com.diversify.solana.rpc

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SolanaRpcClientEdgeTest {

    @Test
    fun getBalance_withValidMainnetAddress_returnsResult() = runBlocking {
        val client = SolanaRpcClient()
        val result = client.getBalance(SYSTEM_PROGRAM_ADDRESS)
        assertTrue("Expected success for valid address, got $result", result.isSuccess)
        assertTrue("Balance must be non-negative", result.getOrThrow() >= 0L)
    }

    @Test
    fun getBalance_withMalformedAddress_failsGracefully() = runBlocking {
        val client = SolanaRpcClient()
        val result = client.getBalance("not-a-base58-public-key")
        assertTrue("Expected failure for malformed public key", result.isFailure)
    }

    companion object {
        private const val SYSTEM_PROGRAM_ADDRESS = "11111111111111111111111111111111"
    }
}
