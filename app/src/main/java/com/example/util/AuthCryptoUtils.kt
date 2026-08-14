package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom

object AuthCryptoUtils {
    fun generateSalt(): String {
        val random = ByteArray(16)
        SecureRandom().nextBytes(random)
        return random.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val combined = "$password:$salt:vlink_secure_pepper_2026"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculated = hashPassword(password, salt)
        return calculated.equals(expectedHash, ignoreCase = true)
    }
}
