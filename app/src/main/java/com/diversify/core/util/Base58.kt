package com.diversify.core.util

import java.math.BigInteger

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58L)

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        var zeros = 0
        while (zeros < input.size && input[zeros] == 0.toByte()) {
            zeros++
        }

        var value = BigInteger(1, input)
        val encoded = StringBuilder()
        while (value > BigInteger.ZERO) {
            val divRem = value.divideAndRemainder(BASE)
            value = divRem[0]
            encoded.append(ALPHABET[divRem[1].toInt()])
        }

        repeat(zeros) {
            encoded.append(ALPHABET[0])
        }

        return encoded.reverse().toString()
    }
}
