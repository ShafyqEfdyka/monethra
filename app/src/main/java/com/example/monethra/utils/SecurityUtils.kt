package com.example.monethra.utils

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Hashes a raw password string using SHA-256 and returns a hex-encoded string.
     */
    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val bytes = password.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies if a raw password matches a stored password hash (or plaintext for legacy users).
     */
    fun verifyPassword(password: String, storedHash: String?): Boolean {
        if (password.isBlank() || storedHash.isNullOrBlank()) return false
        val hashedPassword = hashPassword(password)
        return hashedPassword.equals(storedHash, ignoreCase = true) || password == storedHash
    }
}
