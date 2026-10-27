
package com.oghamlab.meshchat.core.security

import android.util.Base64
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object RecoveryBackupCrypto {

    private const val FORMAT = "meshchat-nostr-recovery"
    private const val VERSION = 1
    private const val ITERATIONS = 600_000
    private const val MAX_FILE_BYTES = 16_384

    private val random = SecureRandom()

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        val spec = PBEKeySpec(
            password,
            salt,
            iterations,
            256
        )

        return try {
            SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            ).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun authenticatedHeader(): ByteArray =
        "$FORMAT:$VERSION:$ITERATIONS".toByteArray(
            Charsets.UTF_8
        )

    fun encrypt(
        privateKey: ByteArray,
        password: CharArray
    ): ByteArray {
        require(privateKey.size == 32)
        require(password.size >= 16) {
            "Use a recovery passphrase of at least 16 characters"
        }

        val salt = ByteArray(16).also {
            random.nextBytes(it)
        }

        val nonce = ByteArray(12).also {
            random.nextBytes(it)
        }

        val keyBytes = deriveKey(
            password,
            salt,
            ITERATIONS
        )

        val ciphertext = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(128, nonce)
            )

            cipher.updateAAD(authenticatedHeader())
            cipher.doFinal(privateKey)
        } finally {
            keyBytes.fill(0)
        }

        val json = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("kdf", "PBKDF2-HMAC-SHA256")
            put("iterations", ITERATIONS)
            put("cipher", "AES-256-GCM")
            put("salt", encode(salt))
            put("nonce", encode(nonce))
            put("ciphertext", encode(ciphertext))
        }

        return json.toString(2).toByteArray(Charsets.UTF_8)
    }

    fun decrypt(
        fileBytes: ByteArray,
        password: CharArray
    ): ByteArray {
        require(fileBytes.size in 1..MAX_FILE_BYTES) {
            "Invalid recovery file size"
        }

        val json = JSONObject(
            fileBytes.toString(Charsets.UTF_8)
        )

        require(json.getString("format") == FORMAT)
        require(json.getInt("version") == VERSION)
        require(json.getString("kdf") == "PBKDF2-HMAC-SHA256")
        require(json.getString("cipher") == "AES-256-GCM")

        val iterations = json.getInt("iterations")
        require(iterations == ITERATIONS)

        val salt = decode(json.getString("salt"))
        val nonce = decode(json.getString("nonce"))
        val ciphertext = decode(json.getString("ciphertext"))

        require(salt.size == 16)
        require(nonce.size == 12)
        require(ciphertext.size == 48)

        val keyBytes = deriveKey(
            password,
            salt,
            iterations
        )

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(128, nonce)
            )

            cipher.updateAAD(authenticatedHeader())
            val privateKey = cipher.doFinal(ciphertext)

            require(privateKey.size == 32)
            privateKey
        } finally {
            keyBytes.fill(0)
        }
    }
}
