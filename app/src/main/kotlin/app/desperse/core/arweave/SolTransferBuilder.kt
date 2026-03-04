package app.desperse.core.arweave

import app.desperse.core.util.Base58
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds a raw unsigned Solana transaction with a single SystemProgram.Transfer instruction.
 * Used for Turbo credit top-up (SOL → Turbo wallet).
 *
 * Transaction format: legacy (v0 is not needed for simple transfers).
 */
object SolTransferBuilder {

    private val SYSTEM_PROGRAM_ID = ByteArray(32) // All zeros

    /**
     * Build an unsigned SOL transfer transaction.
     *
     * @param fromPubkey The sender's public key (32 bytes, base58)
     * @param toPubkey The recipient's public key (32 bytes, base58)
     * @param lamports Amount of lamports to transfer
     * @param recentBlockhash Recent blockhash (base58)
     * @return The unsigned transaction bytes, ready for signing
     */
    fun buildTransferTransaction(
        fromPubkey: String,
        toPubkey: String,
        lamports: Long,
        recentBlockhash: String
    ): ByteArray {
        val fromKey = Base58.decode(fromPubkey)
        val toKey = Base58.decode(toPubkey)
        val blockhashBytes = Base58.decode(recentBlockhash)

        // Transaction layout (legacy):
        // 1 byte: num_signatures (1)
        // 64 bytes: signature placeholder (zeros — to be filled by wallet)
        // Message:
        //   1 byte: num_required_signatures (1)
        //   1 byte: num_readonly_signed_accounts (0)
        //   1 byte: num_readonly_unsigned_accounts (1) — SystemProgram
        //   compact-u16: num_account_keys (3)
        //   32*3 bytes: account keys [from, to, system_program]
        //   32 bytes: recent_blockhash
        //   compact-u16: num_instructions (1)
        //   Instruction:
        //     1 byte: program_id_index (2 — SystemProgram)
        //     compact-u16: num_accounts (2)
        //     1 byte: account_index[0] (0 — from, signer+writable)
        //     1 byte: account_index[1] (1 — to, writable)
        //     compact-u16: data_length (12)
        //     4 bytes: instruction_index (2 = Transfer)
        //     8 bytes: lamports (little-endian u64)

        val buf = ByteBuffer.allocate(1 + 64 + 3 + 1 + 32 * 3 + 32 + 1 + 1 + 1 + 2 + 1 + 1 + 12)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        // Signature section
        buf.put(1.toByte()) // 1 signature required
        buf.put(ByteArray(64)) // Placeholder for signature

        // Message header
        buf.put(1.toByte()) // num_required_signatures
        buf.put(0.toByte()) // num_readonly_signed_accounts
        buf.put(1.toByte()) // num_readonly_unsigned_accounts (SystemProgram)

        // Account keys (compact-u16 count = 3)
        buf.put(3.toByte())
        buf.put(fromKey) // index 0: signer + writable
        buf.put(toKey) // index 1: writable
        buf.put(SYSTEM_PROGRAM_ID) // index 2: readonly (SystemProgram)

        // Recent blockhash
        buf.put(blockhashBytes)

        // Instructions (compact-u16 count = 1)
        buf.put(1.toByte())

        // Instruction: SystemProgram.Transfer
        buf.put(2.toByte()) // program_id_index = 2 (SystemProgram)

        // Account indices (compact-u16 count = 2)
        buf.put(2.toByte())
        buf.put(0.toByte()) // from account
        buf.put(1.toByte()) // to account

        // Instruction data (compact-u16 length = 12)
        buf.put(12.toByte())
        buf.putInt(2) // SystemProgram instruction index: Transfer = 2
        buf.putLong(lamports) // Amount in lamports (u64 LE)

        buf.flip()
        val result = ByteArray(buf.remaining())
        buf.get(result)
        return result
    }

    /** Convert SOL amount to lamports (1 SOL = 10^9 lamports) */
    fun solToLamports(sol: Double): Long = (sol * 1_000_000_000L).toLong()
}
