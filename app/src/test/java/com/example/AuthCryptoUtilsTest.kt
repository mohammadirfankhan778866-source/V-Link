package com.example

import com.example.util.AuthCryptoUtils
import org.junit.Assert.*
import org.junit.Test

class AuthCryptoUtilsTest {

    @Test
    fun testGenerateSaltIsNonEmptyAndUnique() {
        val salt1 = AuthCryptoUtils.generateSalt()
        val salt2 = AuthCryptoUtils.generateSalt()
        assertTrue(salt1.isNotEmpty())
        assertTrue(salt2.isNotEmpty())
        assertNotEquals(salt1, salt2)
    }

    @Test
    fun testHashPasswordAndVerify() {
        val password = "StrongPassword@123"
        val salt = AuthCryptoUtils.generateSalt()
        val hash = AuthCryptoUtils.hashPassword(password, salt)

        assertTrue(AuthCryptoUtils.verifyPassword(password, salt, hash))
        assertFalse(AuthCryptoUtils.verifyPassword("WrongPassword", salt, hash))
        assertFalse(AuthCryptoUtils.verifyPassword(password, "differentsalt", hash))
    }
}
