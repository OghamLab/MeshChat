
package com.oghamlab.meshchat.core.security



import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64

import java.security.KeyStore

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecureKeyStorage
 *
 * PURPOSE:
 *
 * Securely stores and retrieves a Nostr private key
 * on an Android device.
 *
 * A Nostr private key is extremely sensitive.
 *
 * Anyone who obtains the private key can sign events
 * and impersonate the corresponding Nostr identity.
 *
 * This class uses three Android/Java technologies:
 *
 * 1. Android Keystore
 *    - Creates and protects an AES encryption key.
 *    - The AES key is not exported to application code.
 *
 * 2. AES-256-GCM
 *    - Encrypts the Nostr private key.
 *    - Authenticates the encrypted data.
 *    - Detects tampering or incorrect decryption keys.
 *
 * 3. SharedPreferences
 *    - Stores the encrypted private key.
 *    - Stores Base64 text rather than raw binary data.
 *
 * IMPORTANT DISTINCTION:
 *
 * Android Keystore stores the AES wrapping key.
 *
 * SharedPreferences stores the encrypted Nostr key.
 *
 * The Nostr private key is NOT itself stored
 * directly inside Android Keystore.
 *
 * This protects the key at rest but does not guarantee
 * protection against a compromised application process
 * or a fully compromised Android device.
 *
 * @param context
 * Android Context used to access application storage.
 */
class SecureKeyStorage(context: Context) {

    /**
     * Obtain a SharedPreferences storage instance.
     *
     * getSharedPreferences() creates or opens a
     * preferences file belonging to this application.
     *
     * First argument:
     *
     * "meshchat_identity"
     *
     * This is the logical name of the preferences file.
     *
     * Android normally stores the file in the
     * application's private data directory.
     *
     * Second argument:
     *
     * Context.MODE_PRIVATE
     *
     * This restricts normal file access to
     * the application that owns the preferences.
     *
     * SECURITY:
     *
     * MODE_PRIVATE does not encrypt the data.
     *
     * That is why this class encrypts the private key
     * before saving anything to SharedPreferences.
     */
    private val prefs = context.getSharedPreferences(
        "meshchat_identity",
        Context.MODE_PRIVATE
    )

    /**
     * Unique name assigned to our AES encryption key.
     *
     * Android Keystore identifies stored keys
     * using aliases.
     *
     * Think of an alias as a key's identifier.
     *
     * It is not a password or encryption key.
     *
     * When we later call:
     *
     * keyStore.getKey(alias, null)
     *
     * Android looks for the key associated
     * with this alias.
     */
    private val alias = "meshchat_identity_aes"

    /**
     * Obtain the Android Keystore instance.
     *
     * KeyStore.getInstance("AndroidKeyStore")
     *
     * requests Android's system-managed
     * cryptographic key storage provider.
     *
     * Unlike a normal Java KeyStore stored in a file,
     * AndroidKeyStore can protect keys using
     * Android's security infrastructure.
     *
     * Depending on the device, keys may be protected
     * by hardware-backed security.
     *
     * Hardware backing is not guaranteed by this code.
     *
     * apply { ... } is a Kotlin scope function.
     *
     * It executes the lambda using the object
     * as its receiver and returns that same object.
     *
     * load(null)
     *
     * initializes the Android Keystore instance.
     *
     * AndroidKeyStore does not require a file
     * to be supplied to load().
     */
    private val keyStore: KeyStore? =
        try {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
            }
        } catch (_: Exception) {
            null
        }


    /**
     * getOrCreateKey()
     *
     * Retrieves an existing AES encryption key
     * or creates a new one if necessary.
     *
     * Returns:
     *
     * SecretKey
     *
     * SecretKey represents a symmetric
     * cryptographic key.
     *
     * Symmetric means the same key is used
     * for encryption and decryption.
     *
     * This method is private because
     * only SecureKeyStorage should manage
     * the wrapping key.
     */
    private fun getOrCreateKey(): SecretKey {

        val ks = keyStore ?: throw IllegalStateException("AndroidKeyStore not available")

        /**
         * Attempt to retrieve an existing key
         * from Android Keystore.
         *
         * alias:
         *   Identifies the key.
         *
         * null:
         *   No password is supplied.
         *
         * AndroidKeyStore manages access to
         * its key entries through the platform.
         */
        val existing = ks.getKey(alias, null)

        /**
         * Kotlin's "is" operator checks
         * an object's runtime type.
         *
         * If the existing key is a SecretKey,
         * return it immediately.
         *
         * This is called an early return.
         *
         * It avoids generating a new key
         * every time the application starts.
         *
         * IMPORTANT:
         *
         * Previously encrypted data must normally
         * be decrypted using the same AES key.
         */
        if (existing is SecretKey) {
            return existing
        }

        /**
         * Create a KeyGenerator configured
         * to generate AES keys.
         *
         * KeyGenerator.getInstance() accepts:
         *
         * 1. Algorithm name
         * 2. Cryptographic provider name
         *
         * KEY_ALGORITHM_AES:
         *   Specifies AES encryption.
         *
         * "AndroidKeyStore":
         *   Specifies Android's Keystore provider.
         *
         * The resulting key will be generated
         * and managed through Android Keystore.
         */
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        /**
         * Configure the new AES key.
         *
         * KeyGenParameterSpec.Builder creates
         * a configuration for an Android Keystore key.
         *
         * The configuration controls:
         *
         * - Key alias
         * - Allowed operations
         * - Block mode
         * - Padding
         * - Key size
         */
        generator.init(

            KeyGenParameterSpec.Builder(

                /**
                 * Name assigned to the key.
                 */
                alias,

                /**
                 * Define the operations
                 * permitted for this key.
                 *
                 * PURPOSE_ENCRYPT:
                 *   Allow encryption.
                 *
                 * PURPOSE_DECRYPT:
                 *   Allow decryption.
                 *
                 * "or" is Kotlin's bitwise OR
                 * operation for integers.
                 *
                 * It combines the two flags
                 * into one configuration value.
                 *
                 * This is different from ||,
                 * which is logical OR for Booleans.
                 */
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )

                /**
                 * Select GCM block mode.
                 *
                 * GCM stands for
                 * Galois/Counter Mode.
                 *
                 * AES-GCM provides:
                 *
                 * 1. Confidentiality
                 *    The private key is encrypted.
                 *
                 * 2. Integrity/authentication
                 *    Tampering can be detected.
                 *
                 * GCM is an authenticated
                 * encryption mode.
                 */
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )

                /**
                 * Specify no block padding.
                 *
                 * GCM works as a streaming-like
                 * authenticated encryption mode
                 * and does not require conventional
                 * block padding such as PKCS7.
                 *
                 * Therefore:
                 *
                 * ENCRYPTION_PADDING_NONE
                 */
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )

                /**
                 * Configure a 256-bit AES key.
                 *
                 * 256 bits = 32 bytes.
                 *
                 * This AES key is separate from
                 * the 32-byte Nostr private key.
                 *
                 * Although both are 32 bytes,
                 * they serve different purposes.
                 */
                .setKeySize(256)

                /**
                 * Finish building the
                 * key generation specification.
                 */
                .build()
        )

        /**
         * Generate the AES key.
         *
         * Android Keystore manages the
         * generated key material.
         *
         * The returned SecretKey acts as
         * a handle to the protected key.
         *
         * This key is reused for future
         * encryption and decryption.
         */
        return generator.generateKey()
    }


    /**
     * save()
     *
     * Encrypts and stores a Nostr private key.
     *
     * Parameter:
     *
     * privateKey:
     *   The raw 32-byte Nostr private key.
     *
     * Encryption process:
     *
     * Private key
     *     |
     *     v
     * AES-256-GCM encryption
     *     |
     *     v
     * IV + Ciphertext + Authentication Tag
     *     |
     *     v
     * Base64 encoding
     *     |
     *     v
     * SharedPreferences
     *
     * This function overwrites the previously
     * saved identity payload, if one exists.
     */
    fun save(privateKey: ByteArray) {

        /**
         * Verify the key length.
         *
         * Nostr private keys are 32 bytes.
         *
         * require() throws
         * IllegalArgumentException if false.
         *
         * NOTE:
         *
         * This only checks the byte length.
         *
         * It does not check that the key
         * represents a valid secp256k1 scalar.
         *
         * A separate NostrKeyManager validator
         * should check the scalar range.
         */
        require(privateKey.size == 32)

        /**
         * Create a Cipher instance.
         *
         * Cipher is the Java cryptography API
         * used to perform encryption/decryption.
         *
         * "AES/GCM/NoPadding" specifies:
         *
         * AES:
         *   Symmetric encryption algorithm.
         *
         * GCM:
         *   Authenticated encryption mode.
         *
         * NoPadding:
         *   No conventional block padding.
         */
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding")

        /**
         * Initialize the cipher for encryption.
         *
         * Cipher.ENCRYPT_MODE:
         *   Select encryption.
         *
         * getOrCreateKey():
         *   Obtain the AES key from Keystore.
         *
         * The provider generates a fresh IV
         * when encryption is initialized.
         *
         * IV means Initialization Vector.
         *
         * For AES-GCM, the IV must not be reused
         * with the same AES key.
         *
         * Android Keystore normally enforces
         * randomized encryption for such keys.
         */
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        /**
         * Encrypt the private key.
         *
         * doFinal() processes the plaintext
         * and finishes the encryption operation.
         *
         * In GCM mode, the output normally contains:
         *
         * Ciphertext + Authentication Tag
         *
         * The authentication tag is used later
         * to verify that the encrypted data
         * has not been modified.
         */
        val encrypted = cipher.doFinal(privateKey)

        /**
         * Create a single binary payload.
         *
         * cipher.iv:
         *   Retrieves the IV used for encryption.
         *
         * encrypted:
         *   Contains ciphertext and GCM tag.
         *
         * The + operator concatenates ByteArrays.
         *
         * Result:
         *
         * [IV][Ciphertext][Authentication Tag]
         *
         * For this implementation:
         *
         * IV is expected to be 12 bytes.
         *
         * Authentication tag is normally 16 bytes.
         *
         * The IV does not need to be secret.
         * It must be preserved for decryption.
         *
         * IMPORTANT:
         *
         * The load() function assumes a 12-byte IV.
         *
         * A production storage format should
         * explicitly define or record the IV length
         * rather than relying on an implicit assumption.
         */
        val payload = cipher.iv + encrypted

        /**
         * Store the encrypted payload.
         *
         * SharedPreferences stores simple
         * values such as Strings and integers.
         *
         * Base64 converts binary data
         * into printable text.
         *
         * Base64 is NOT encryption.
         *
         * The data is already encrypted
         * before Base64 encoding occurs.
         */
        val saved = prefs.edit()

            /**
             * Store a String under the
             * preference key "key_payload".
             *
             * This is the name used later
             * when retrieving the data.
             */
            .putString(
                "key_payload",

                /**
                 * Convert the encrypted binary
                 * payload into Base64 text.
                 *
                 * Base64.NO_WRAP prevents
                 * automatic line breaks.
                 */
                Base64.encodeToString(
                    payload,
                    Base64.NO_WRAP
                )
            )

            /**
             * commit() writes the changes
             * synchronously.
             *
             * It returns:
             *
             * true:
             *   Write reported success.
             *
             * false:
             *   Write reported failure.
             *
             * Unlike apply(), commit()
             * provides a Boolean result.
             *
             * Since this is synchronous,
             * it should not be called on
             * the UI thread for potentially
             * slow storage operations.
             */
            .commit()

        /**
         * Verify that saving succeeded.
         *
         * check() throws IllegalStateException
         * when the condition is false.
         *
         * The trailing lambda supplies
         * the exception message lazily.
         *
         * Syntax:
         *
         * check(condition) {
         *     "Error message"
         * }
         */
        check(saved) {
            "Failed to save identity"
        }
    }


    /**
     * load()
     *
     * Retrieves and decrypts the previously
     * saved Nostr private key.
     *
     * Return type:
     *
     * ByteArray?
     *
     * The ? means the result is nullable.
     *
     * Possible results:
     *
     * ByteArray:
     *   A decrypted private key.
     *
     * null:
     *   No stored identity payload exists.
     *
     * IMPORTANT:
     *
     * A corrupted payload, missing Keystore key,
     * invalid authentication tag, or other failure
     * may throw an exception instead of returning null.
     */
    fun load(): ByteArray? {

        /**
         * Read the saved Base64 String.
         *
         * getString() accepts:
         *
         * 1. Preference key
         * 2. Default value
         *
         * null means no default String.
         */
        val encoded =
            prefs.getString("key_payload", null)

            /**
             * ?: is Kotlin's Elvis operator.
             *
             * If getString() returns null,
             * immediately return null
             * from load().
             *
             * Otherwise, continue with
             * the non-null String.
             */
                ?: return null

        /**
         * Decode Base64 text back
         * into the original binary payload.
         *
         * This does not decrypt the key.
         *
         * It only reverses the Base64
         * representation.
         */
        val payload = Base64.decode(
            encoded,
            Base64.NO_WRAP
        )

        /**
         * Validate the minimum payload length.
         *
         * Expected structure:
         *
         * [12-byte IV]
         * [Ciphertext]
         * [16-byte authentication tag]
         *
         * 12 + 16 = 28 bytes minimum.
         *
         * require() rejects payloads
         * shorter than 28 bytes.
         *
         * NOTE:
         *
         * Because this class always encrypts
         * a 32-byte private key, its normal
         * payload length is:
         *
         * 12 + 32 + 16 = 60 bytes.
         *
         * This minimum-length check does not
         * verify the full expected structure.
         */
        require(payload.size >= 12 + 16)

        /**
         * Extract the IV.
         *
         * copyOfRange(start, end)
         *
         * start is inclusive.
         * end is exclusive.
         *
         * Therefore:
         *
         * copyOfRange(0, 12)
         *
         * copies byte indexes 0 through 11.
         */
        val iv = payload.copyOfRange(0, 12)

        /**
         * Extract the encrypted data.
         *
         * Starting at index 12 skips the IV.
         *
         * The remaining bytes contain:
         *
         * Ciphertext + Authentication Tag.
         */
        val encrypted = payload.copyOfRange(
            12,
            payload.size
        )

        /**
         * Create a cipher using the same
         * algorithm, mode, and padding
         * used during encryption.
         *
         * Encryption and decryption must
         * use compatible parameters.
         */
        val cipher =
            Cipher.getInstance("AES/GCM/NoPadding")

        /**
         * Initialize the cipher for decryption.
         *
         * Cipher.DECRYPT_MODE:
         *   Select decryption.
         *
         * getOrCreateKey():
         *   Retrieve the AES key from Keystore.
         *
         * GCMParameterSpec(128, iv):
         *
         * 128:
         *   Authentication tag length in bits.
         *
         * iv:
         *   Initialization Vector used
         *   during the original encryption.
         *
         * The same AES key and IV are required
         * to decrypt the stored ciphertext.
         *
         * SECURITY NOTE:
         *
         * If the original Keystore key is lost,
         * getOrCreateKey() may create a new key.
         *
         * That new key cannot decrypt ciphertext
         * encrypted under the previous key.
         *
         * A production implementation should
         * detect this situation and provide
         * an explicit recovery or reset workflow.
         */
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(
                128,
                iv
            )
        )

        /**
         * Decrypt and authenticate the payload.
         *
         * doFinal() performs GCM decryption
         * and verifies the authentication tag.
         *
         * If authentication fails, an exception
         * such as AEADBadTagException is thrown.
         *
         * A successful operation returns
         * the original plaintext bytes.
         *
         * In this case:
         *
         * The Nostr private key.
         *
         * SECURITY:
         *
         * The private key is now in application
         * memory as a ByteArray.
         *
         * Avoid logging it, displaying it,
         * or retaining unnecessary copies.
         */
        return cipher.doFinal(encrypted)
    }


    /**
     * exists()
     *
     * Checks whether SharedPreferences
     * contains an encrypted identity payload.
     *
     * Returns:
     *
     * true:
     *   The preference entry exists.
     *
     * false:
     *   The preference entry does not exist.
     *
     * This does not decrypt the payload.
     *
     * It also does not verify that the
     * Android Keystore key is available.
     *
     * Therefore:
     *
     * exists() == true
     *
     * does not guarantee that load()
     * will succeed.
     *
     * Kotlin syntax:
     *
     * fun exists(): Boolean = expression
     *
     * This is an expression-bodied function.
     *
     * It is equivalent to:
     *
     * fun exists(): Boolean {
     *     return prefs.contains("key_payload")
     * }
     */
    fun exists(): Boolean =
        prefs.contains("key_payload")
}
