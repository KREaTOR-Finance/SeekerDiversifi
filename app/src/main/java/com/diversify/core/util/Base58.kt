package com.diversify.core.util

import java.math.BigInteger

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(58L)
    private val INDEXES = IntArray(128) { -1 }.apply {
        ALPHABET.forEachIndexed { index, c ->
            this[c.code] = index
        }
    }

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

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return byteArrayOf()

        val decoded = BigInteger.ZERO
        var value = decoded
        input.forEach { char ->
            val charCode = char.code
            if (charCode >= INDEXES.size || INDEXES[charCode] < 0) {
                throw IllegalArgumentException("Invalid Base58 character: $char")
            }
            value = value.multiply(BASE).add(BigInteger.valueOf(INDEXES[charCode].toLong()))
        }

        val bytes = value.toByteArray().let {
            if (it.size > 1 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it
        }
        val leadingZeros = input.takeWhile { it == ALPHABET[0] }.length
        return ByteArray(leadingZeros) + bytes
    }
}
