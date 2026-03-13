package com.diversify.core.util

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureRandomProvider @Inject constructor() {
    
    private val secureRandom = SecureRandom()
    
    fun nextBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes
    }
    
    fun nextInt(min: Int, max: Int): Int {
        return secureRandom.nextInt(max - min) + min
    }
    
    fun nextLong(): Long {
        return secureRandom.nextLong()
    }
    
    fun nextDouble(min: Double, max: Double): Double {
        return min + (max - min) * secureRandom.nextDouble()
    }
    
    fun randomString(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[secureRandom.nextInt(chars.length)] }
            .joinToString("")
    }
}
