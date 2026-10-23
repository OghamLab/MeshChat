
package com.oghamlab.meshchat.nostr.identity

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.math.ec.FixedPointCombMultiplier
import java.math.BigInteger
import java.security.SecureRandom



/**
 * NostrKeyManager
 *
 * Responsible for managing the cryptographic keys
 * used to create a Nostr identity.
 *
 * Nostr uses public-key cryptography based on
 * the secp256k1 elliptic curve.
 *
 * A Nostr identity consists of:
 *
 * 1. Private key:
 *    - A secret 256-bit integer.
 *    - Used to sign Nostr events.
 *    - Must never be shared publicly.
 *
 * 2. Public key:
 *    - Derived mathematically from the private key.
 *    - Used to identify a Nostr user.
 *    - Used to verify digital signatures.
 *
 * This class provides four main operations:
 *
 * generatePrivateKey()
 *     Generates a secure random private key.
 *
 * isValidPrivateKey()
 *     Checks whether a private key is valid.
 *
 * publicKeyBytes()
 *     Derives a Nostr public key from a private key.
 *
 * createIdentity()
 *     Creates a Nostr identity containing the public
 *     key in hexadecimal and Bech32 formats.
 *
 * IMPORTANT:
 * This class does not store private keys.
 * Secure key storage must be implemented separately.
 */
class NostrKeyManager {

    /**
     * Load the parameters of the secp256k1 elliptic curve.
     *
     * SECNamedCurves is provided by Bouncy Castle.
     *
     * getByName("secp256k1") retrieves the standard
     * cryptographic parameters for this curve.
     *
     * The curve object contains important values:
     *
     * curve.curve:
     *     The elliptic curve itself.
     *
     * curve.g:
     *     The generator point G.
     *
     * curve.n:
     *     The order of the generator point.
     *
     * The generator point and its order are used
     * when generating and deriving cryptographic keys.
     *
     * secp256k1 is also used by Bitcoin.
     *
     * Nostr uses secp256k1 with BIP-340 Schnorr
     * signatures for event authentication.
     */
    private val curve =
        SECNamedCurves.getByName("secp256k1")

    /**
     * Create a cryptographically secure
     * random number generator.
     *
     * SecureRandom is part of Java's
     * cryptography APIs.
     *
     * It is different from Kotlin's Random.
     *
     * Kotlin Random:
     *     Suitable for ordinary random operations.
     *
     * SecureRandom:
     *     Designed to generate unpredictable values
     *     suitable for cryptographic operations.
     *
     * Private keys must be unpredictable because
     * anyone who discovers the private key can
     * control the corresponding Nostr identity.
     */
    private val random = SecureRandom()


    /**
     * generatePrivateKey()
     *
     * Generates a valid random private key
     * for the secp256k1 elliptic curve.
     *
     * A private key is an integer d satisfying:
     *
     *     1 <= d < n
     *
     * Where:
     *
     * d = private key integer
     * n = order of the secp256k1 generator point
     *
     * The key is represented as 32 bytes.
     *
     * 32 bytes * 8 bits = 256 bits.
     *
     * Returns:
     *     ByteArray containing a valid private key.
     */
    fun generatePrivateKey(): ByteArray {

        /**
         * Create an array containing 32 bytes.
         *
         * Initially, all bytes are zero.
         *
         * Example:
         *
         * [0, 0, 0, 0, ...]
         *
         * The array will hold our generated key.
         */
        val bytes = ByteArray(32)

        /**
         * A do-while loop executes its body
         * at least once.
         *
         * Syntax:
         *
         * do {
         *     operation
         * } while (condition)
         *
         * Here, the loop continues until
         * a valid private key is generated.
         */
        do {

            /**
             * Fill the byte array with
             * cryptographically secure random bytes.
             *
             * The contents of 'bytes' are modified
             * directly by nextBytes().
             *
             * Example representation:
             *
             * [42, -17, 83, 11, ...]
             *
             * Kotlin Byte values range from
             * -128 to 127 because Byte is signed.
             *
             * This does not mean the private key
             * itself is a negative integer.
             */
            random.nextBytes(bytes)

            /**
             * Validate the generated key.
             *
             * The exclamation mark ! means NOT.
             *
             * !isValidPrivateKey(bytes)
             *
             * means:
             *
             * Continue looping while the key
             * is invalid.
             *
             * This approach is called
             * rejection sampling.
             *
             * It avoids bias that could arise
             * from reducing arbitrary random
             * integers modulo the curve order.
             */
        } while (!isValidPrivateKey(bytes))

        /**
         * Return the valid private key.
         *
         * SECURITY:
         * Never log this array or display
         * its contents in production.
         */
        return bytes
    }


    /**
     * isValidPrivateKey()
     *
     * Checks whether a ByteArray represents
     * a valid secp256k1 private key.
     *
     * A valid private key must:
     *
     * 1. Contain exactly 32 bytes.
     * 2. Represent an integer greater than zero.
     * 3. Be smaller than the curve order n.
     *
     * Parameter:
     *     bytes = private key to validate.
     *
     * Returns:
     *     true  -> valid private key
     *     false -> invalid private key
     */
    fun isValidPrivateKey(
        bytes: ByteArray
    ): Boolean {

        /**
         * Validate the key length.
         *
         * bytes.size returns the number
         * of elements in the ByteArray.
         *
         * != means "not equal".
         *
         * If the array does not contain
         * exactly 32 bytes, reject it.
         *
         * This is an early return.
         */
        if (bytes.size != 32) {
            return false
        }

        /**
         * Convert the byte array into
         * a BigInteger.
         *
         * BigInteger represents integers
         * that can be much larger than
         * Kotlin's Int or Long.
         *
         * BigInteger(1, bytes)
         *
         * The first argument, 1, specifies
         * that the integer is positive.
         *
         * This is important because Kotlin
         * Byte values are signed.
         *
         * Without the positive signum,
         * a byte sequence whose highest
         * bit is set could be interpreted
         * as a negative integer.
         */
        val value = BigInteger(1, bytes)

        /**
         * Check the mathematical range:
         *
         *     0 < value < n
         *
         * BigInteger.ZERO represents zero.
         *
         * curve.n is the order of the
         * secp256k1 generator point.
         *
         * Both conditions must be true.
         *
         * && is Kotlin's logical AND operator.
         *
         * A private key equal to zero is invalid.
         *
         * A private key greater than or equal
         * to n is also invalid.
         */
        return value > BigInteger.ZERO &&
                value < curve.n
    }


    /**
     * publicKeyBytes()
     *
     * Derives a Nostr public key from
     * a valid private key.
     *
     * Elliptic curve public-key derivation:
     *
     *     P = d * G
     *
     * Where:
     *
     * P = public key point
     * d = private key scalar
     * G = secp256k1 generator point
     *
     * The * symbol here represents
     * elliptic curve scalar multiplication,
     * not ordinary integer multiplication.
     *
     * It is computationally practical
     * to derive P from d.
     *
     * However, deriving d from P is
     * computationally infeasible with
     * currently known classical algorithms.
     *
     * Nostr uses x-only public keys.
     *
     * This means we keep only the
     * 32-byte X coordinate of P.
     *
     * Parameter:
     *     privateKey = 32-byte private key.
     *
     * Returns:
     *     ByteArray containing the
     *     32-byte x-only public key.
     */
    fun publicKeyBytes(
        privateKey: ByteArray
    ): ByteArray {

        /**
         * Verify that the private key is valid.
         *
         * require() throws an
         * IllegalArgumentException if
         * the condition is false.
         *
         * This prevents invalid scalars
         * from reaching the multiplication.
         */
        require(isValidPrivateKey(privateKey))

        /**
         * Convert the private key bytes
         * into a positive BigInteger.
         *
         * This integer becomes the scalar d
         * used in elliptic curve multiplication.
         */
        val scalar = BigInteger(1, privateKey)

        /**
         * Calculate:
         *
         *     P = d * G
         *
         * FixedPointCombMultiplier is a
         * Bouncy Castle implementation
         * optimized for scalar multiplication
         * involving a fixed elliptic curve point.
         *
         * curve.g:
         *     The secp256k1 generator point G.
         *
         * scalar:
         *     The private key integer d.
         *
         * multiply(curve.g, scalar):
         *     Derives the public key point.
         *
         * normalize():
         *     Converts the result into a form
         *     where affine coordinates can
         *     be accessed.
         *
         * SECURITY NOTE:
         * This code preserves your original
         * multiplier. For production secret-key
         * operations, use an audited library/API
         * that explicitly supports safe handling
         * of secret scalars and BIP-340 signing.
         */
        val point = FixedPointCombMultiplier()
            .multiply(curve.g, scalar)
            .normalize()

        /**
         * Extract the X coordinate
         * from the elliptic curve point.
         *
         * An elliptic curve point has
         * coordinates:
         *
         *     P = (x, y)
         *
         * Nostr uses only x.
         *
         * affineXCoord:
         *     Retrieves the affine X coordinate.
         *
         * toBigInteger():
         *     Converts the coordinate into
         *     a BigInteger.
         *
         * toByteArray():
         *     Converts the integer into bytes.
         *
         * IMPORTANT:
         * BigInteger.toByteArray() uses
         * signed two's-complement encoding.
         *
         * Therefore, the resulting array
         * is not guaranteed to be 32 bytes.
         *
         * We must normalize its length.
         */
        return point.affineXCoord
            .toBigInteger()
            .toByteArray()

            /**
             * let is a Kotlin scope function.
             *
             * It passes the previous result
             * into a lambda expression.
             *
             * raw is the lambda parameter
             * containing the byte array.
             *
             * Syntax:
             *
             * value.let { raw ->
             *     operation(raw)
             * }
             *
             * The final expression inside
             * the lambda becomes its result.
             */
            .let { raw ->

                /**
                 * when is Kotlin's multi-branch
                 * conditional expression.
                 *
                 * Similar to switch in Java,
                 * but more flexible.
                 *
                 * Here, we normalize the
                 * public key to exactly 32 bytes.
                 */
                when {

                    /**
                     * CASE 1:
                     *
                     * The result already
                     * contains 32 bytes.
                     *
                     * No changes are needed.
                     */
                    raw.size == 32 -> raw

                    /**
                     * CASE 2:
                     *
                     * BigInteger may add a leading
                     * zero byte to preserve a
                     * positive sign.
                     *
                     * Example:
                     *
                     * [0, byte1, byte2, ..., byte32]
                     *
                     * This produces 33 bytes.
                     *
                     * raw[0] checks the first byte.
                     *
                     * If the first byte is zero,
                     * remove it.
                     *
                     * copyOfRange(1, 33)
                     * copies indexes 1 through 32.
                     *
                     * The end index is exclusive.
                     */
                    raw.size == 33 &&
                            raw[0] == 0.toByte() ->

                        raw.copyOfRange(1, 33)

                    /**
                     * CASE 3:
                     *
                     * The coordinate uses fewer
                     * than 32 bytes.
                     *
                     * This can happen when the
                     * integer begins with zero bits.
                     *
                     * For example, a coordinate
                     * might require only 31 bytes.
                     *
                     * We add leading zero bytes
                     * until the result reaches
                     * exactly 32 bytes.
                     *
                     * ByteArray(32 - raw.size):
                     *     Creates the missing
                     *     leading zero bytes.
                     *
                     * + raw:
                     *     Appends the original bytes.
                     */
                    raw.size < 32 ->

                        ByteArray(32 - raw.size) + raw

                    /**
                     * CASE 4:
                     *
                     * Any unexpected byte length
                     * indicates a problem.
                     *
                     * error() throws an
                     * IllegalStateException.
                     */
                    else ->
                        error("Unexpected public key length")
                }
            }
    }


    /**
     * createIdentity()
     *
     * Creates a NostrIdentity object from
     * an existing private key.
     *
     * The resulting identity contains:
     *
     * 1. Public key in hexadecimal format.
     * 2. Public key in Bech32 npub format.
     *
     * Example:
     *
     * Hex:
     *   32 bytes represented by 64 hex characters.
     *
     * Bech32:
     *   npub1...
     *
     * This function does not return
     * the private key.
     *
     * It also does not save the identity
     * to a database or device storage.
     */
    fun createIdentity(
        privateKey: ByteArray
    ): NostrIdentity {

        /**
         * Derive the public key
         * from the supplied private key.
         *
         * publicKeyBytes() performs
         * private-key validation first.
         */
        val publicKey = publicKeyBytes(privateKey)

        /**
         * Convert the public key bytes
         * into hexadecimal text.
         *
         * Hexadecimal uses 16 symbols:
         *
         * 0-9 and a-f.
         *
         * Each byte is represented by
         * two hexadecimal characters.
         *
         * 32 bytes * 2 = 64 characters.
         *
         * joinToString(""):
         *     Combines all formatted bytes
         *     into one String with no separator.
         *
         * "%02x".format(...):
         *
         * %  = Start of format specification.
         * 0  = Pad with zeros if necessary.
         * 2  = Minimum width of two characters.
         * x  = Lowercase hexadecimal.
         *
         * Example:
         *
         * Decimal 15 -> "0f"
         * Decimal 255 -> "ff"
         *
         * it:
         *     Represents the current Byte.
         *
         * it.toInt() and 0xff:
         *     Converts the signed Byte into
         *     an unsigned value from 0 to 255.
         */
        val hex = publicKey.joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

        /**
         * Create and return a NostrIdentity.
         *
         * publicKeyHex:
         *     The 64-character hexadecimal
         *     representation of the public key.
         *
         * npub:
         *     The Bech32 representation
         *     of the same public key.
         *
         * NostrBech32.encode("npub", publicKey)
         * converts the raw 32-byte public key
         * into the standard npub format.
         *
         * Both representations identify
         * the same Nostr public key.
         */
        return NostrIdentity(
            publicKeyHex = hex,
            npub = NostrBech32.encode(
                "npub",
                publicKey
            )
        )
    }
}
