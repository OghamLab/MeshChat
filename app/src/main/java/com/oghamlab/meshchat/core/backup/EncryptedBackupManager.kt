package com.oghamlab.meshchat.core.backup
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptedBackupManager {

    private val random = SecureRandom()

    companion object {
        private val MAGIC = byteArrayOf(
            'M'.code.toByte(),
            'C'.code.toByte(),
            'B'.code.toByte(),
            'K'.code.toByte()
        )

        private const val VERSION: Byte = 1
        private const val SALT_SIZE = 16
        private const val NONCE_SIZE = 12
        private const val KEY_BITS = 256
        private const val ITERATIONS = 600_000
        private const val SECRET_SIZE = 32
        private const val TAG_SIZE = 16

        private const val HEADER_SIZE =
            4 + 1 + SALT_SIZE + NONCE_SIZE

        private const val TOTAL_SIZE =
            HEADER_SIZE + SECRET_SIZE + TAG_SIZE
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray
    ): SecretKeySpec {
        val spec = PBEKeySpec(
            password,
            salt,
            ITERATIONS,
            KEY_BITS
        )

        return try {
            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

            val encoded = factory.generateSecret(spec).encoded

            try {
                SecretKeySpec(encoded, "AES")
            } finally {
                encoded.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(
        privateKey: ByteArray,
        password: CharArray
    ): ByteArray {
        require(privateKey.size == SECRET_SIZE)
        require(password.size >= 12) {
            "Use a recovery password of at least 12 characters"
        }

        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val nonce = ByteArray(NONCE_SIZE).also(random::nextBytes)

        val header = MAGIC +
                byteArrayOf(VERSION) +
                salt +
                nonce

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            deriveKey(password, salt),
            GCMParameterSpec(128, nonce)
        )

        cipher.updateAAD(header)

        val ciphertext = cipher.doFinal(privateKey)

        return header + ciphertext
    }

    fun decrypt(
        backup: ByteArray,
        password: CharArray
    ): ByteArray {
        require(backup.size == TOTAL_SIZE) {
            "Invalid backup file size"
        }

        require(backup.copyOfRange(0, 4).contentEquals(MAGIC)) {
            "Unrecognized backup format"
        }

        require(backup[4] == VERSION) {
            "Unsupported backup version"
        }

        val salt = backup.copyOfRange(5, 21)
        val nonce = backup.copyOfRange(21, 33)
        val header = backup.copyOfRange(0, HEADER_SIZE)
        val ciphertext = backup.copyOfRange(
            HEADER_SIZE,
            backup.size
        )

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            deriveKey(password, salt),
            GCMParameterSpec(128, nonce)
        )

        cipher.updateAAD(header)

        val secret = cipher.doFinal(ciphertext)

        check(secret.size == SECRET_SIZE)

        return secret
    }
}
