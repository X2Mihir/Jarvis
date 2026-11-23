package com.example.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

data class VaultSecurityInfo(
    val keyAlias: String,
    val algorithm: String,
    val provider: String,
    val isHardwareBacked: Boolean,
    val isOperational: Boolean
)

/**
 * Hardware-backed Android Keystore AES-256-GCM encryption engine for all local Jarvis memories,
 * transcripts, contact shortcuts, and automation payloads.
 */
object CryptoVault {
    private const val TAG = "CryptoVault"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "jarvis_memory_master_key_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128
    private const val PAYLOAD_PREFIX = "ENC1:"

    @Volatile
    private var cachedFallbackKey: SecretKey? = null

    private fun getOrCreateKey(): Pair<SecretKey, Boolean> {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) {
                return existing to true
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey() to true
        } catch (e: Exception) {
            // Only falls back in non-device JVM test environments
            runCatching {
                Log.w(TAG, "AndroidKeyStore unavailable in current runtime, using ephemeral test key", e)
            }
            val key = cachedFallbackKey ?: synchronized(this) {
                cachedFallbackKey ?: KeyGenerator.getInstance("AES").apply { init(256) }
                    .generateKey()
                    .also { cachedFallbackKey = it }
            }
            key to false
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: RuntimeException) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    private fun decodeBase64(text: String): ByteArray {
        return try {
            Base64.decode(text, Base64.NO_WRAP)
        } catch (_: RuntimeException) {
            java.util.Base64.getDecoder().decode(text)
        }
    }

    @Synchronized
    fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        return try {
            val (key, _) = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val packed = ByteBuffer.allocate(1 + iv.size + ciphertext.size)
                .put(iv.size.toByte())
                .put(iv)
                .put(ciphertext)
                .array()
            PAYLOAD_PREFIX + encodeBase64(packed)
        } catch (e: Exception) {
            runCatching { Log.e(TAG, "Encryption error", e) }
            throw IllegalStateException("Failed to encrypt local vault record", e)
        }
    }

    @Synchronized
    fun decrypt(payload: String): String {
        if (payload.isEmpty()) return ""
        if (!payload.startsWith(PAYLOAD_PREFIX)) {
            return payload
        }
        return try {
            val raw = decodeBase64(payload.removePrefix(PAYLOAD_PREFIX))
            val buffer = ByteBuffer.wrap(raw)
            val ivLen = buffer.get().toInt().coerceIn(1, 32)
            val iv = ByteArray(ivLen)
            buffer.get(iv)
            val ciphertext = ByteArray(buffer.remaining())
            buffer.get(ciphertext)

            val (key, _) = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plaintextBytes = cipher.doFinal(ciphertext)
            String(plaintextBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            runCatching { Log.e(TAG, "Decryption error", e) }
            "[Encrypted record unavailable — key mismatch]"
        }
    }

    fun inspectSecurity(): VaultSecurityInfo {
        return try {
            val (key, usingKeystore) = getOrCreateKey()
            var hardwareBacked = false
            if (usingKeystore) {
                val factory = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE)
                val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
                hardwareBacked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                        keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
                } else {
                    @Suppress("DEPRECATION")
                    keyInfo.isInsideSecureHardware
                }
            }
            VaultSecurityInfo(
                keyAlias = KEY_ALIAS,
                algorithm = "AES-256-GCM",
                provider = if (usingKeystore) ANDROID_KEYSTORE else "JVM-Ephemeral",
                isHardwareBacked = hardwareBacked,
                isOperational = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inspect keystore security", e)
            VaultSecurityInfo(
                keyAlias = KEY_ALIAS,
                algorithm = "AES-256-GCM",
                provider = "Unavailable",
                isHardwareBacked = false,
                isOperational = false
            )
        }
    }
}
