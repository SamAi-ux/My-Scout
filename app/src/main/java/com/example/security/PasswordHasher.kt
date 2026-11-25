package com.example.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(password.toCharArray(), salt)
        return "${salt.toHex()}:${hash.toHex()}"
    }

    fun verifyPassword(password: String, storedValue: String): Boolean {
        val parts = storedValue.split(":")
        if (parts.size != 2) return false
        return try {
            val salt = parts[0].hexToBytes()
            val expectedHash = parts[1].hexToBytes()
            val actualHash = pbkdf2(password.toCharArray(), salt)
            if (expectedHash.size != actualHash.size) return false
            var diff = 0
            for (i in expectedHash.indices) {
                diff = diff or (expectedHash[i].toInt() xor actualHash[i].toInt())
            }
            diff == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { b -> "%02x".format(b) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Invalid hex length" }
        return ByteArray(length / 2) { idx ->
            substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }
}
