package app.desperse.core.util

/**
 * Base58 encoding/decoding using the Bitcoin/Solana alphabet.
 * Used for Privy SIWS signatures and wallet deeplink protocol payloads.
 */
object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128).apply {
        fill(-1)
        ALPHABET.forEachIndexed { i, c -> this[c.code] = i }
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""

        // Count leading zeros
        var leadingZeros = 0
        for (b in input) {
            if (b.toInt() == 0) leadingZeros++ else break
        }

        // Convert to a big integer (unsigned)
        val size = input.size * 138 / 100 + 1 // log(256) / log(58) upper bound
        val digits = IntArray(size)
        for (b in input) {
            var carry = b.toInt() and 0xFF
            for (j in digits.indices.reversed()) {
                carry += 256 * digits[j]
                digits[j] = carry % 58
                carry /= 58
            }
        }

        // Skip leading zeros in digit array
        var firstNonZero = 0
        while (firstNonZero < digits.size && digits[firstNonZero] == 0) firstNonZero++

        val sb = StringBuilder(leadingZeros + digits.size - firstNonZero)
        repeat(leadingZeros) { sb.append('1') }
        for (i in firstNonZero until digits.size) {
            sb.append(ALPHABET[digits[i]])
        }
        return sb.toString()
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)

        // Count leading '1' characters (= leading zero bytes)
        var leadingOnes = 0
        for (c in input) {
            if (c == '1') leadingOnes++ else break
        }

        // Decode base58 string to byte array
        val size = input.length * 733 / 1000 + 1 // log(58) / log(256) upper bound
        val output = ByteArray(size)
        for (c in input) {
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            require(digit >= 0) { "Invalid Base58 character: $c" }
            var carry = digit
            for (j in output.indices.reversed()) {
                carry += 58 * (output[j].toInt() and 0xFF)
                output[j] = (carry % 256).toByte()
                carry /= 256
            }
        }

        // Skip leading zeros in output
        var firstNonZero = 0
        while (firstNonZero < output.size && output[firstNonZero].toInt() == 0) firstNonZero++

        // Build result: leading zero bytes + decoded bytes
        val result = ByteArray(leadingOnes + output.size - firstNonZero)
        output.copyInto(result, leadingOnes, firstNonZero)
        return result
    }
}
