
package com.oghamlab.meshchat.nostr.identity


/**
 * NostrBech32
 *
 * This object converts Nostr public and private keys between:
 *
 * 1. Raw 32-byte binary representation
 * 2. Human-readable Bech32 representation
 *
 * Examples:
 *
 * npub1... = Nostr public key
 * nsec1... = Nostr private key
 *
 * Nostr uses the Bech32 encoding format described in BIP-173.
 * NIP-19 specifies how Nostr identifiers use Bech32.
 *
 * IMPORTANT:
 * Bech32 is an ENCODING format, not encryption.
 *
 * An nsec contains a private key. Anyone who obtains that
 * private key can control the corresponding Nostr identity.
 *
 * This implementation supports only:
 * - npub (public keys)
 * - nsec (private keys)
 *
 * It does not support note, nprofile, nevent, or naddr.
 */
object NostrBech32 {

    /**
     * The Bech32 character alphabet.
     *
     * Bech32 converts binary data into groups of 5 bits.
     *
     * Five bits can represent:
     *
     * 2^5 = 32 possible values (0 through 31).
     *
     * Each value maps to one character in this string.
     *
     * Example:
     *
     * 0  -> q
     * 1  -> p
     * 2  -> z
     * 3  -> r
     *
     * The alphabet excludes visually confusing characters
     * and is defined by the Bech32 specification.
     */
    private const val CHARS =
        "qpzry9x8gf2tvdw0s3jn54khce6mua7l"


    /**
     * polymod()
     *
     * Calculates the Bech32 checksum polynomial.
     *
     * PURPOSE:
     *
     * Bech32 includes a checksum that helps detect mistakes
     * when someone copies or types a Nostr key.
     *
     * For example, if a character in an npub is changed,
     * checksum validation will usually detect the error.
     *
     * Parameter:
     *
     * values:
     *   An array of integers representing the expanded
     *   prefix and the encoded data.
     *
     * Returns:
     *
     * An integer containing the checksum state.
     *
     * IMPORTANT:
     * A checksum detects accidental corruption.
     * It is not a cryptographic signature or authentication.
     */
    private fun polymod(values: IntArray): Int {

        /**
         * These five constants are the generator values
         * defined by the Bech32 checksum algorithm.
         *
         * They are hexadecimal numbers.
         *
         * 0x indicates hexadecimal notation.
         *
         * These constants must not be changed because
         * other Bech32 implementations use the same values.
         */
        val generators = intArrayOf(
            0x3b6a57b2,
            0x26508e6d,
            0x1ea119fa,
            0x3d4233dd,
            0x2a1462b3
        )

        /**
         * chk is the running checksum state.
         *
         * It begins with the value 1 as specified by Bech32.
         */
        var chk = 1

        /**
         * Process every integer in the input array.
         *
         * Each value contributes to the checksum.
         */
        for (v in values) {

            /**
             * ushr = unsigned shift right.
             *
             * Move the checksum 25 bits to the right.
             *
             * This extracts the five highest bits
             * of the 30-bit checksum state.
             *
             * These bits determine which generator
             * constants will be applied.
             */
            val top = chk ushr 25

            /**
             * Update the checksum state.
             *
             * and 0x1ffffff:
             *   Keep only the lowest 25 bits.
             *
             * shl 5:
             *   Shift those bits five positions left,
             *   making room for the next 5-bit value.
             *
             * xor v:
             *   Combine the next input value using XOR.
             *
             * XOR means exclusive OR.
             *
             * XOR outputs 1 when the two compared bits differ.
             */
            chk = ((chk and 0x1ffffff) shl 5) xor v

            /**
             * Examine the five bits extracted into top.
             *
             * 0..4 means:
             * 0, 1, 2, 3, 4
             */
            for (i in 0..4) {

                /**
                 * top shr i:
                 *   Shift bit i into the lowest position.
                 *
                 * and 1:
                 *   Isolate that bit.
                 *
                 * != 0:
                 *   Check whether that bit is set.
                 */
                if ((top shr i) and 1 != 0) {

                    /**
                     * If the bit is set, XOR the corresponding
                     * generator constant into the checksum.
                     */
                    chk = chk xor generators[i]
                }
            }
        }

        /**
         * Return the final checksum state.
         */
        return chk
    }


    /**
     * hrpExpand()
     *
     * HRP stands for Human-Readable Part.
     *
     * Examples:
     *
     * npub = public key prefix
     * nsec = private key prefix
     *
     * Bech32 includes the prefix in checksum calculations.
     *
     * This prevents an encoded public key from simply having
     * its prefix changed to nsec while retaining a valid
     * checksum.
     *
     * Example input:
     *
     * "npub"
     *
     * Each character is converted into its Unicode code value.
     *
     * Then the code value is split into:
     *
     * 1. Higher bits
     * 2. Lower five bits
     *
     * A zero separates the two groups.
     */
    private fun hrpExpand(hrp: String): IntArray =

        (
                /**
                 * First group:
                 *
                 * it.code gets the numeric value of a character.
                 *
                 * shr 5 shifts that value right by five bits.
                 */
                hrp.map { it.code shr 5 } +

                        /**
                         * Insert the separator value 0.
                         */
                        listOf(0) +

                        /**
                         * Second group:
                         *
                         * and 31 extracts the lowest five bits.
                         *
                         * Decimal 31 = binary 11111.
                         */
                        hrp.map { it.code and 31 }

                ).toIntArray()


    /**
     * convertBits()
     *
     * Converts data between different bit-group sizes.
     *
     * WHY THIS IS NECESSARY:
     *
     * Computers normally represent binary data using
     * 8-bit bytes.
     *
     * Bech32 represents data using 5-bit values.
     *
     * Therefore:
     *
     * Encoding:
     *   8-bit groups -> 5-bit groups
     *
     * Decoding:
     *   5-bit groups -> 8-bit groups
     *
     * Parameters:
     *
     * data:
     *   Input integer values.
     *
     * from:
     *   Number of bits in each input value.
     *
     * to:
     *   Number of bits in each output value.
     *
     * pad:
     *   Whether zero padding is allowed at the end.
     *
     * Examples:
     *
     * Encoding:
     *   convertBits(data, 8, 5, true)
     *
     * Decoding:
     *   convertBits(data, 5, 8, false)
     */
    private fun convertBits(
        data: IntArray,
        from: Int,
        to: Int,
        pad: Boolean
    ): IntArray {

        /**
         * acc = accumulator.
         *
         * Temporarily stores incoming bits while
         * they are being reorganized.
         */
        var acc = 0

        /**
         * Number of bits currently available
         * inside the accumulator.
         */
        var bits = 0

        /**
         * A mutable list for the converted values.
         *
         * We use a mutable list because the number of
         * output groups may differ from the number
         * of input groups.
         */
        val result = mutableListOf<Int>()

        /**
         * Maximum value that fits in one output group.
         *
         * shl means shift left.
         *
         * For to = 5:
         *
         * (1 shl 5) - 1
         * = 32 - 1
         * = 31
         *
         * Binary:
         * 11111
         *
         * For to = 8:
         *
         * (1 shl 8) - 1 = 255
         */
        val maxv = (1 shl to) - 1

        /**
         * Process each input value.
         */
        for (value in data) {

            /**
             * Validate the input.
             *
             * value >= 0:
             *   The value cannot be negative.
             *
             * value shr from == 0:
             *   The value must fit within the
             *   specified input bit width.
             *
             * Example:
             *
             * from = 5
             * valid range = 0..31
             *
             * require() throws IllegalArgumentException
             * if the condition is false.
             */
            require(value >= 0 && value shr from == 0)

            /**
             * Append the new bits to the accumulator.
             *
             * acc shl from:
             *   Shift existing bits left.
             *
             * or value:
             *   Insert the incoming value.
             *
             * and 0x7fffffff:
             *   Keep the accumulator within
             *   the lower 31 bits.
             */
            acc = (acc shl from or value) and 0x7fffffff

            /**
             * Increase the number of available bits.
             */
            bits += from

            /**
             * As long as enough bits are available
             * to form an output group, extract one.
             */
            while (bits >= to) {

                /**
                 * Account for the bits being extracted.
                 */
                bits -= to

                /**
                 * acc shr bits:
                 *   Move the next output group
                 *   into the lowest bit positions.
                 *
                 * and maxv:
                 *   Keep only the required number of bits.
                 *
                 * Add the resulting value to the list.
                 */
                result.add((acc shr bits) and maxv)
            }
        }

        /**
         * Handle any remaining bits.
         */
        if (pad) {

            /**
             * Encoding allows zero padding.
             *
             * If fewer than 'to' bits remain,
             * shift them left and fill the
             * remaining positions with zeros.
             */
            if (bits > 0) {
                result.add(
                    (acc shl (to - bits)) and maxv
                )
            }

        } else {

            /**
             * Decoding does not allow arbitrary padding.
             *
             * First check that there are not enough
             * leftover bits to represent another
             * complete input group.
             */
            require(bits < from)

            /**
             * Check that the leftover padding bits
             * are all zero.
             *
             * This prevents malformed encodings
             * from being silently accepted.
             */
            require(
                ((acc shl (to - bits)) and maxv) == 0
            )
        }

        /**
         * Convert the mutable list to an IntArray
         * and return it.
         */
        return result.toIntArray()
    }


    /**
     * encode()
     *
     * Converts a raw 32-byte Nostr key into
     * a human-readable Bech32 string.
     *
     * Parameters:
     *
     * prefix:
     *   "npub" for public keys
     *   "nsec" for private keys
     *
     * bytes:
     *   The raw 32-byte key.
     *
     * Returns:
     *   A Bech32 string containing the prefix,
     *   separator, encoded data, and checksum.
     *
     * Example structure:
     *
     * npub1 + encoded-data + checksum
     */
    fun encode(
        prefix: String,
        bytes: ByteArray
    ): String {

        /**
         * Only allow supported Nostr key prefixes.
         */
        require(prefix == "npub" || prefix == "nsec")

        /**
         * Nostr public and private keys are 32 bytes.
         *
         * 32 bytes x 8 bits = 256 bits.
         */
        require(bytes.size == 32)

        /**
         * Convert the 8-bit bytes into 5-bit values.
         *
         * Kotlin Byte is signed:
         * -128 to 127.
         *
         * and 0xff converts each Byte into an unsigned
         * integer representation from 0 to 255.
         *
         * Example:
         *
         * (-1).toByte().toInt() and 0xff = 255
         *
         * pad = true because Bech32 encoding permits
         * zero padding for the final incomplete group.
         */
        val data = convertBits(
            bytes.map {
                it.toInt() and 0xff
            }.toIntArray(),
            8,
            5,
            true
        )

        /**
         * Expand the prefix for checksum calculation.
         */
        val expanded = hrpExpand(prefix)

        /**
         * Combine:
         *
         * 1. Expanded prefix
         * 2. Encoded key data
         * 3. Six placeholder zero values
         *
         * Bech32 uses six 5-bit checksum characters.
         */
        val values = expanded + data + IntArray(6)

        /**
         * Calculate the checksum.
         *
         * xor 1 is required by the original
         * Bech32 checksum specification.
         *
         * This implementation uses Bech32,
         * not Bech32m.
         */
        val checksum = polymod(values) xor 1

        /**
         * Build the encoded suffix.
         *
         * First:
         *   Convert the encoded data into a List.
         *
         * Next:
         *   Extract six 5-bit groups from the checksum.
         *
         * (0..5).map:
         *   Generate six checksum values.
         *
         * checksum shr (5 * (5 - it)):
         *   Shift the required checksum group
         *   into the lowest five bits.
         *
         * and 31:
         *   Keep only those five bits.
         *
         * Finally:
         *   Map each integer to a character
         *   in the Bech32 alphabet.
         *
         * joinToString(""):
         *   Concatenate the characters without
         *   any separator between them.
         */
        val suffix = (
                data.toList() +
                        (0..5).map {
                            (checksum shr (5 * (5 - it))) and 31
                        }
                ).joinToString("") {
                CHARS[it].toString()
            }

        /**
         * Build the final Nostr key.
         *
         * Example:
         *
         * prefix = "npub"
         *
         * Result structure:
         *
         * npub1...
         *
         * The character '1' separates the prefix
         * from the encoded data and checksum.
         */
        return prefix + "1" + suffix
    }


    /**
     * decode()
     *
     * Converts a Bech32-encoded Nostr key
     * back into its original 32-byte representation.
     *
     * Parameters:
     *
     * value:
     *   The encoded Nostr key.
     *
     * prefix:
     *   The expected key type ("npub" or "nsec").
     *
     * Returns:
     *   The original 32-byte key.
     *
     * Invalid inputs throw IllegalArgumentException.
     */
    fun decode(
        value: String,
        prefix: String
    ): ByteArray {

        /**
         * Validate the expected prefix.
         */
        require(prefix == "npub" || prefix == "nsec")

        /**
         * Standard Bech32 strings have a
         * maximum length of 90 characters.
         */
        require(value.length <= 90)

        /**
         * Bech32 accepts either:
         *
         * - All lowercase
         * - All uppercase
         *
         * Mixed-case strings are invalid.
         *
         * Example:
         *
         * npub1... = valid case format
         * NPUB1... = valid case format
         * nPuB1... = invalid case format
         */
        require(
            value == value.lowercase() ||
                    value == value.uppercase()
        )

        /**
         * Normalize the input to lowercase.
         *
         * This makes subsequent comparisons easier.
         */
        val normalized = value.lowercase()

        /**
         * Verify that the string starts with
         * the expected prefix followed by '1'.
         *
         * Example:
         *
         * prefix = "npub"
         *
         * Expected beginning:
         *
         * npub1
         */
        require(normalized.startsWith(prefix + "1"))

        /**
         * Find the last occurrence of '1'.
         *
         * In Bech32, this character separates
         * the human-readable prefix from the payload.
         */
        val separator = normalized.lastIndexOf('1')

        /**
         * Extract everything after the separator.
         *
         * This includes:
         *
         * - Encoded key data
         * - Six checksum characters
         */
        val payload = normalized.substring(separator + 1)

        /**
         * The payload must contain at least
         * six characters for the checksum.
         *
         * The 32-byte length is validated later.
         */
        require(payload.length >= 6)

        /**
         * Convert each Bech32 character
         * back into its numeric value.
         *
         * Example:
         *
         * q -> 0
         * p -> 1
         * z -> 2
         *
         * indexOf() searches the CHARS alphabet.
         *
         * If the character is not found,
         * indexOf() returns -1.
         */
        val data = payload.map {

            /**
             * Find the character's position
             * in the Bech32 alphabet.
             */
            val index = CHARS.indexOf(it)

            /**
             * Reject characters that do not
             * belong to the Bech32 alphabet.
             */
            require(index >= 0) {
                "Invalid Bech32 character"
            }

            /**
             * Return the numeric value
             * for this character.
             */
            index

        }.toIntArray()

        /**
         * Validate the checksum.
         *
         * Combine:
         *
         * - Expanded prefix
         * - Encoded data including checksum
         *
         * For original Bech32, a valid
         * checksum produces polymod == 1.
         *
         * If this check fails, the input
         * may contain an incorrect character,
         * an incorrect prefix, or other corruption.
         */
        require(
            polymod(hrpExpand(prefix) + data) == 1
        ) {
            "Invalid Bech32 checksum"
        }

        /**
         * Remove the last six checksum values.
         *
         * Those values are not part of
         * the original Nostr key.
         *
         * Convert the remaining 5-bit values
         * back into 8-bit bytes.
         *
         * pad = false:
         *   Reject nonzero or excessive padding.
         */
        val decoded = convertBits(
            data.dropLast(6).toIntArray(),
            5,
            8,
            false
        )

        /**
         * Verify that decoding produced
         * exactly 32 bytes.
         */
        require(decoded.size == 32)

        /**
         * Convert the integer values into Bytes.
         *
         * toByte():
         *   Converts each integer into Kotlin Byte.
         *
         * toByteArray():
         *   Creates the final ByteArray.
         */
        return decoded.map {
            it.toByte()
        }.toByteArray()
    }
}
