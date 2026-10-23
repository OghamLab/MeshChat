
package com.oghamlab.meshchat.nostr.identity

import android.content.Context
import com.oghamlab.meshchat.core.security.SecureKeyStorage



/**
 * IdentityRepository
 *
 * PURPOSE:
 *
 * Manages the lifecycle of a user's Nostr identity
 * in the MeshChat Android application.
 *
 * A Nostr identity is based on a cryptographic
 * private/public key pair.
 *
 * PRIVATE KEY:
 * - A secret 32-byte value.
 * - Used to sign Nostr events.
 * - Can be exported in Bech32 "nsec" format.
 * - Must be protected from unauthorized access.
 *
 * PUBLIC KEY:
 * - Derived from the private key.
 * - Used to identify the Nostr user.
 * - Can be represented as hexadecimal or "npub".
 * - Can safely be shared publicly.
 *
 * This repository coordinates three components:
 *
 * 1. SecureKeyStorage
 *    Encrypts and stores the private key locally.
 *
 * 2. NostrKeyManager
 *    Generates private keys and derives public keys.
 *
 * 3. NostrBech32
 *    Encodes and decodes Nostr keys.
 *
 * ARCHITECTURE:
 *
 * Compose UI
 *     |
 *     v
 * ViewModel
 *     |
 *     v
 * IdentityRepository
 *     |
 *     +---- SecureKeyStorage
 *     |
 *     +---- NostrKeyManager
 *     |
 *     +---- NostrBech32
 *
 * The repository hides the details of key management
 * from the user interface.
 *
 * IMPORTANT:
 *
 * This implementation supports one stored identity
 * per application installation.
 *
 * It does not currently support:
 * - Multiple Nostr accounts
 * - Account switching
 * - Deleting an identity
 * - Rotating encryption keys
 * - Biometric authentication
 *
 * @param context
 * Android Context used to access secure local storage.
 */
class IdentityRepository(context: Context) {

    /**
     * Create the secure storage component.
     *
     * SecureKeyStorage is responsible for:
     *
     * - Obtaining an AES key from Android Keystore.
     * - Encrypting the Nostr private key.
     * - Saving the encrypted key in SharedPreferences.
     * - Decrypting the stored key when requested.
     *
     * "private val" means:
     *
     * private:
     *   This property can only be accessed
     *   inside IdentityRepository.
     *
     * val:
     *   The property reference cannot be reassigned
     *   after initialization.
     *
     * The storage object itself can still perform
     * operations that modify persistent data.
     */
    private val storage = SecureKeyStorage(context)

    /**
     * Create the Nostr cryptographic key manager.
     *
     * NostrKeyManager provides:
     *
     * generatePrivateKey()
     *   Creates a random valid secp256k1 private key.
     *
     * isValidPrivateKey()
     *   Checks whether a private key is valid.
     *
     * publicKeyBytes()
     *   Derives the public key from a private key.
     *
     * createIdentity()
     *   Produces a NostrIdentity containing
     *   the public key in hex and npub formats.
     *
     * Notice that NostrKeyManager does not
     * handle persistent storage.
     *
     * This separation is intentional.
     */
    private val keyManager = NostrKeyManager()


    /**
     * hasIdentity()
     *
     * Checks whether the application contains
     * a previously saved identity payload.
     *
     * Returns:
     *
     * true:
     *   SecureKeyStorage contains an identity entry.
     *
     * false:
     *   No identity entry exists.
     *
     * This function uses an expression body.
     *
     * Kotlin syntax:
     *
     * fun functionName(): ReturnType = expression
     *
     * It is equivalent to:
     *
     * fun hasIdentity(): Boolean {
     *     return storage.exists()
     * }
     *
     * IMPORTANT:
     *
     * storage.exists() only checks whether
     * the encrypted payload exists.
     *
     * It does not prove that the payload
     * can be successfully decrypted.
     */
    fun hasIdentity(): Boolean = storage.exists()


    /**
     * getIdentity()
     *
     * Retrieves the currently stored Nostr identity.
     *
     * This method:
     *
     * 1. Loads the encrypted private key from storage.
     * 2. Decrypts it.
     * 3. Derives the public identity.
     * 4. Clears the temporary private-key byte array.
     * 5. Returns the public identity.
     *
     * Return type:
     *
     * NostrIdentity?
     *
     * The question mark (?) indicates a nullable type.
     *
     * Possible results:
     *
     * NostrIdentity:
     *   An identity was successfully retrieved.
     *
     * null:
     *   No stored identity payload exists.
     *
     * Decryption errors may throw exceptions
     * rather than returning null.
     */
    fun getIdentity(): NostrIdentity? {

        /**
         * Attempt to load the private key.
         *
         * storage.load() returns ByteArray?
         *
         * This means it may return:
         *
         * - A ByteArray containing the secret.
         * - null when no identity exists.
         *
         * ?: is Kotlin's Elvis operator.
         *
         * Syntax:
         *
         * expression ?: alternative
         *
         * Here:
         *
         * If storage.load() returns null,
         * return null immediately from getIdentity().
         *
         * Otherwise, assign the non-null
         * ByteArray to the variable secret.
         */
        val secret = storage.load() ?: return null

        /**
         * Use try/finally to ensure the temporary
         * secret array is cleared.
         *
         * The try block performs the operation.
         *
         * The finally block executes whether:
         *
         * - The operation succeeds.
         * - The operation throws an exception.
         * - The try block returns a value.
         *
         * This is useful for cleanup operations.
         */
        return try {

            /**
             * Derive the public identity
             * from the private key.
             *
             * keyManager.createIdentity(secret)
             *
             * internally derives the public key
             * using secp256k1 scalar multiplication.
             *
             * It then produces:
             *
             * publicKeyHex:
             *   64-character hexadecimal public key.
             *
             * npub:
             *   Bech32-encoded public key.
             *
             * The returned NostrIdentity should
             * contain only public information.
             */
            keyManager.createIdentity(secret)

        } finally {

            /**
             * Overwrite the private-key byte array
             * with zeros.
             *
             * fill(0) replaces every byte
             * in the array with zero.
             *
             * Example:
             *
             * Before:
             * [42, 17, -93, 11, ...]
             *
             * After:
             * [0, 0, 0, 0, ...]
             *
             * This reduces how long the private key
             * remains in this particular memory buffer.
             *
             * IMPORTANT:
             *
             * It does not guarantee complete erasure
             * of every copy from JVM memory.
             */
            secret.fill(0)
        }
    }


    /**
     * createIdentity()
     *
     * Generates and saves a new Nostr identity.
     *
     * WORKFLOW:
     *
     * 1. Check that no identity is already stored.
     * 2. Generate a secure random private key.
     * 3. Derive the public identity.
     * 4. Encrypt and save the private key.
     * 5. Clear the temporary private-key buffer.
     * 6. Return the public identity.
     *
     * Returns:
     *
     * NostrIdentity
     *
     * Throws IllegalStateException if an identity
     * payload already exists or saving fails.
     */
    fun createIdentity(): NostrIdentity {

        /**
         * Prevent creating another identity
         * when one already exists.
         *
         * hasIdentity() returns a Boolean.
         *
         * ! is Kotlin's logical NOT operator.
         *
         * !hasIdentity()
         *
         * means:
         *
         * "An identity must NOT already exist."
         *
         * check() verifies the condition.
         *
         * If the condition is false,
         * check() throws IllegalStateException.
         *
         * The trailing lambda:
         *
         * { "An identity already exists" }
         *
         * supplies the exception message.
         *
         * The message is evaluated only
         * when the check fails.
         */
        check(!hasIdentity()) {
            "An identity already exists"
        }

        /**
         * Generate a new private key.
         *
         * The key manager uses SecureRandom
         * to generate 32 random bytes.
         *
         * It validates that the resulting
         * integer satisfies:
         *
         *     1 <= d < n
         *
         * where n is the secp256k1 curve order.
         *
         * The result is stored temporarily
         * in a ByteArray named secret.
         */
        val secret = keyManager.generatePrivateKey()

        /**
         * try/finally ensures that the temporary
         * private key is cleared after use.
         */
        return try {

            /**
             * Derive the public identity.
             *
             * The resulting NostrIdentity contains:
             *
             * - Public key in hexadecimal.
             * - Public key in npub format.
             *
             * The private key is not included
             * in the returned identity.
             */
            val identity = keyManager.createIdentity(secret)

            /**
             * Save the private key securely.
             *
             * SecureKeyStorage.save() performs:
             *
             * - AES-GCM encryption.
             * - Android Keystore key retrieval.
             * - Base64 encoding.
             * - SharedPreferences persistence.
             *
             * If saving fails, an exception
             * prevents this method from returning
             * a successfully created identity.
             */
            storage.save(secret)

            /**
             * In Kotlin, a try block can be
             * used as an expression.
             *
             * The last expression inside the
             * try block becomes its result.
             *
             * Here, the result is identity.
             *
             * That result is returned by
             * createIdentity().
             */
            identity

        } finally {

            /**
             * Clear the temporary private key
             * regardless of success or failure.
             */
            secret.fill(0)
        }
    }


    /**
     * importIdentity()
     *
     * Imports an existing Nostr identity
     * using its nsec private key.
     *
     * Example format:
     *
     * nsec1...
     *
     * IMPORTANT:
     *
     * nsec is not encrypted.
     *
     * It is a Bech32 representation
     * of the private key.
     *
     * Anyone who possesses a valid nsec
     * can control the corresponding identity.
     *
     * WORKFLOW:
     *
     * 1. Check that no identity already exists.
     * 2. Remove surrounding whitespace.
     * 3. Decode the nsec into raw bytes.
     * 4. Validate the private key.
     * 5. Derive the public identity.
     * 6. Encrypt and save the private key.
     * 7. Clear the temporary secret buffer.
     * 8. Return the public identity.
     *
     * Parameter:
     *
     * nsec:
     *   Bech32-encoded Nostr private key.
     */
    fun importIdentity(nsec: String): NostrIdentity {

        /**
         * Prevent overwriting an existing
         * stored identity.
         *
         * This implementation supports
         * only one identity at a time.
         */
        check(!hasIdentity()) {
            "An identity already exists"
        }

        /**
         * Decode the provided nsec.
         *
         * trim():
         *   Removes leading and trailing whitespace.
         *
         * Example:
         *
         * "  nsec1abc...  "
         *
         * becomes:
         *
         * "nsec1abc..."
         *
         * NostrBech32.decode():
         *   Converts the encoded string into
         *   the original binary private key.
         *
         * "nsec":
         *   Specifies the expected prefix.
         *
         * The decoder also validates:
         *
         * - Bech32 characters.
         * - Case formatting.
         * - Checksum.
         * - Decoded key length.
         *
         * The resulting secret is a ByteArray.
         */
        val secret = NostrBech32.decode(
            nsec.trim(),
            "nsec"
        )

        /**
         * Ensure the temporary secret buffer
         * is cleared after import processing.
         */
        return try {

            /**
             * Validate the secp256k1 private key.
             *
             * A valid private key must:
             *
             * - Be exactly 32 bytes.
             * - Represent an integer greater than zero.
             * - Be smaller than the curve order n.
             *
             * Bech32 checksum validation alone
             * does not guarantee that the decoded
             * bytes form a valid private key.
             */
            require(
                keyManager.isValidPrivateKey(secret)
            ) {
                "Invalid Nostr private key"
            }

            /**
             * Derive the public identity
             * from the imported private key.
             *
             * The resulting identity is the
             * same Nostr identity associated
             * with the imported secret.
             *
             * Importing does not create
             * a new cryptographic identity.
             */
            val identity = keyManager.createIdentity(secret)

            /**
             * Encrypt and persist the imported key.
             *
             * This allows the user to access
             * the same Nostr identity after
             * restarting MeshChat.
             */
            storage.save(secret)

            /**
             * Return the public identity.
             */
            identity

        } finally {

            /**
             * Overwrite the decoded private-key
             * byte array with zeros.
             *
             * Note that the original nsec String
             * may remain in JVM memory.
             *
             * Kotlin/Java Strings are immutable
             * and cannot be reliably zeroed.
             */
            secret.fill(0)
        }
    }


    /**
     * exportNsec()
     *
     * Exports the stored Nostr private key
     * into human-readable nsec format.
     *
     * This is a sensitive operation.
     *
     * Anyone who obtains the exported nsec
     * can take control of the user's identity.
     *
     * Recommended UI precautions:
     *
     * - Require explicit user confirmation.
     * - Consider biometric authentication.
     * - Avoid logging the exported value.
     * - Avoid automatic clipboard copying.
     * - Prevent screenshots on the export screen
     *   when appropriate.
     *
     * Returns:
     *
     * String:
     *   Bech32-encoded private key.
     *
     * Throws IllegalStateException if
     * no identity payload exists.
     */
    fun exportNsec(): String {

        /**
         * Load and decrypt the stored private key.
         *
         * The Elvis operator ?: provides
         * an alternative when the result is null.
         *
         * error("No identity found")
         *
         * throws IllegalStateException.
         *
         * Therefore:
         *
         * If storage.load() returns null,
         * this method fails explicitly.
         *
         * If storage.load() returns a ByteArray,
         * it is assigned to secret.
         */
        val secret = storage.load()
            ?: error("No identity found")

        /**
         * Convert the raw private key
         * into Nostr's nsec format.
         */
        return try {

            /**
             * NostrBech32.encode() converts
             * the 32-byte private key into
             * a Bech32 string.
             *
             * The "nsec" prefix identifies
             * the encoded data as a Nostr
             * private key.
             *
             * Bech32 encoding includes:
             *
             * - Human-readable prefix.
             * - Data converted to 5-bit groups.
             * - Six-character checksum.
             *
             * IMPORTANT:
             *
             * Bech32 does not encrypt the key.
             *
             * The resulting String is sensitive.
             */
            NostrBech32.encode(
                "nsec",
                secret
            )

        } finally {

            /**
             * Clear the temporary raw
             * private-key byte array.
             *
             * This does not clear the
             * returned nsec String.
             *
             * The application must carefully
             * manage where the String is
             * displayed, copied, or retained.
             */
            secret.fill(0)
        }
    }
}
