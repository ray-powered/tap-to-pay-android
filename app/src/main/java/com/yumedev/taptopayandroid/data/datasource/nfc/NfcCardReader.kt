package com.yumedev.taptopayandroid.data.datasource.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.yumedev.taptopayandroid.data.parser.EmvTagParser
import com.yumedev.taptopayandroid.domain.model.*
import com.yumedev.taptopayandroid.util.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcCardReader @Inject constructor(
    private val emvTagParser: EmvTagParser
) {

    companion object {
        private const val TAG = "NfcCardReader"
    }

    data class DolItem(val tag: String, val length: Int)

    suspend fun readCard(tag: Tag, amountCents: Long? = null): Result<EmvCardData> = withContext(Dispatchers.IO) {
        var isoDep: IsoDep? = null
        val apduCommands = mutableListOf<ApduCommand>()
        val allRecords = mutableListOf<ByteArray>()
        var commandSequence = 1
        var aipBytes: ByteArray? = null

        try {
            isoDep = IsoDep.get(tag)
            if (isoDep == null) {
                return@withContext Result.failure(Exception("Card does not support ISO-DEP"))
            }

            isoDep.connect()
            isoDep.timeout = 5000 // 5 seconds timeout
            SecureLogger.d(TAG) { "Connected to card" }

            // Step 1: Select PPSE (Proximity Payment System Environment)
            val ppseCommand = byteArrayOf(
                0x00.toByte(), // CLA
                0xA4.toByte(), // INS (SELECT)
                0x04.toByte(), // P1
                0x00.toByte(), // P2
                0x0E.toByte(), // Lc (length of data)
                // PPSE AID (2PAY.SYS.DDF01)
                0x32.toByte(), 0x50.toByte(), 0x41.toByte(), 0x59.toByte(),
                0x2E.toByte(), 0x53.toByte(), 0x59.toByte(), 0x53.toByte(),
                0x2E.toByte(), 0x44.toByte(), 0x44.toByte(), 0x46.toByte(),
                0x30.toByte(), 0x31.toByte(),
                0x00.toByte()  // Le (expected length)
            )

            val ppseResponse = isoDep.transceive(ppseCommand)

            apduCommands.add(ApduCommand(
                sequence = commandSequence++,
                name = "SELECT PPSE",
                description = "Select Proximity Payment System Environment",
                commandApdu = ppseCommand.toHexString(),
                responseApdu = ppseResponse.toHexString(),
                statusWord = getStatusWord(ppseResponse),
                statusDescription = getStatusDescription(ppseResponse)
            ))

            SecureLogger.dSecure(TAG, "PPSE Response: ${ppseResponse.toHexString()}")

            if (!isSuccessResponse(ppseResponse)) {
                return@withContext Result.failure(Exception("Failed to select PPSE"))
            }

            // Step 2: Extract AID from PPSE response
            val aidBytes = extractAID(ppseResponse) ?: return@withContext Result.failure(Exception("No AID found in PPSE response"))
            SecureLogger.d(TAG) { "Found AID: ${aidBytes.toHexString()}" }

            // Step 3: Select the payment application using AID
            val selectAidCommand = buildSelectCommand(aidBytes)
            val aidResponse = isoDep.transceive(selectAidCommand)

            apduCommands.add(ApduCommand(
                sequence = commandSequence++,
                name = "SELECT AID",
                description = "Select payment application",
                commandApdu = selectAidCommand.toHexString(),
                responseApdu = aidResponse.toHexString(),
                statusWord = getStatusWord(aidResponse),
                statusDescription = getStatusDescription(aidResponse)
            ))

            SecureLogger.dSecure(TAG, "AID Response: ${aidResponse.toHexString()}")

            if (!isSuccessResponse(aidResponse)) {
                return@withContext Result.failure(Exception("Failed to select application"))
            }

            // Step 4: Get Processing Options (GPO)
            // Parse PDOL (Processing Options Data Object List) from AID response to build real EMV parameters
            val pdol = findTag(aidResponse, 0x9F.toByte(), 0x38.toByte())
            val gpoCommand = if (pdol != null && pdol.isNotEmpty()) {
                val dolItems = parseDol(pdol)
                SecureLogger.d(TAG) { "Found PDOL (${dolItems.size} items): ${dolItems.joinToString { "${it.tag}:${it.length}B" }}" }

                // Build real EMV terminal transaction data according to EMVCo specifications
                val pdolData = buildDolData(dolItems, amountCents)
                SecureLogger.d(TAG) { "Constructed PDOL data (${pdolData.size}B): ${pdolData.toHexString()}" }

                // Build GPO command: 80 A8 00 00 Lc 83 Ld [Data] Le
                val lc = 2 + pdolData.size
                byteArrayOf(
                    0x80.toByte(), // CLA
                    0xA8.toByte(), // INS (GET PROCESSING OPTIONS)
                    0x00.toByte(), // P1
                    0x00.toByte(), // P2
                    lc.toByte(),   // Lc
                    0x83.toByte(), // Tag 83
                    pdolData.size.toByte() // Ld
                ) + pdolData + byteArrayOf(0x00.toByte()) // Le
            } else {
                SecureLogger.d(TAG) { "No PDOL requested by card, using empty GPO" }
                byteArrayOf(
                    0x80.toByte(), 0xA8.toByte(), 0x00.toByte(), 0x00.toByte(),
                    0x02.toByte(), 0x83.toByte(), 0x00.toByte(), 0x00.toByte()
                )
            }

            SecureLogger.d(TAG) { "GPO Command: ${gpoCommand.toHexString()}" }

            val gpoResponse = isoDep.transceive(gpoCommand)

            apduCommands.add(ApduCommand(
                sequence = commandSequence++,
                name = "GET PROCESSING OPTIONS",
                description = "Request card processing options with terminal transaction data",
                commandApdu = gpoCommand.toHexString(),
                responseApdu = gpoResponse.toHexString(),
                statusWord = getStatusWord(gpoResponse),
                statusDescription = getStatusDescription(gpoResponse)
            ))

            SecureLogger.dSecure(TAG, "GPO Response: ${gpoResponse.toHexString()}")

            if (isSuccessResponse(gpoResponse)) {
                // Remove status word bytes (last 2 bytes) before adding to records
                val cleanGpo = removeStatusWord(gpoResponse)
                allRecords.add(cleanGpo)

                // Extract AIP (Application Interchange Profile) from GPO response
                aipBytes = findTag(cleanGpo, 0x82.toByte()) ?: if (cleanGpo.size >= 4 && cleanGpo[0] == 0x80.toByte()) {
                    cleanGpo.copyOfRange(2, 4)
                } else null

                // Parse AFL (Application File Locator) from GPO response
                val afl = findTag(cleanGpo, 0x94.toByte()) ?: if (cleanGpo.size > 4 && cleanGpo[0] == 0x80.toByte()) {
                    cleanGpo.copyOfRange(4, cleanGpo.size)
                } else null

                if (afl != null) {
                    // Read records from AFL
                    var i = 0
                    while (i + 3 < afl.size) {
                        val sfi = (afl[i].toInt() shr 3) and 0x1F
                        val firstRecord = afl[i + 1].toInt() and 0xFF
                        val lastRecord = afl[i + 2].toInt() and 0xFF

                        for (record in firstRecord..lastRecord) {
                            try {
                                val readRecordCommand = byteArrayOf(
                                    0x00.toByte(), 0xB2.toByte(),
                                    record.toByte(),
                                    ((sfi shl 3) or 0x04).toByte(),
                                    0x00.toByte()
                                )
                                val recordResponse = isoDep.transceive(readRecordCommand)

                                apduCommands.add(ApduCommand(
                                    sequence = commandSequence++,
                                    name = "READ RECORD - SFI $sfi #$record",
                                    description = "Read application data from SFI $sfi, record $record",
                                    commandApdu = readRecordCommand.toHexString(),
                                    responseApdu = recordResponse.toHexString(),
                                    statusWord = getStatusWord(recordResponse),
                                    statusDescription = getStatusDescription(recordResponse)
                                ))

                                SecureLogger.dSecure(TAG, "Record SFI=$sfi Rec=$record: ${recordResponse.toHexString()}")

                                if (isSuccessResponse(recordResponse)) {
                                    allRecords.add(removeStatusWord(recordResponse))
                                }
                            } catch (e: Exception) {
                                SecureLogger.d(TAG) { "Failed to read SFI=$sfi Rec=$record: ${e.message}" }
                            }
                        }
                        i += 4
                    }
                }
            } else {
                Log.w(TAG, "GPO failed with status: ${gpoResponse.toHexString()}")
            }

            // Fallback: Manual record reading if AFL was missing or returned no data
            if (allRecords.size <= 1) {
                SecureLogger.d(TAG) { "Attempting manual record read fallback..." }
                val sfiOrder = listOf(2, 1, 3, 4)

                for (sfi in sfiOrder) {
                    for (record in 1..3) {
                        try {
                            val readRecordCommand = byteArrayOf(
                                0x00.toByte(), 0xB2.toByte(),
                                record.toByte(),
                                ((sfi shl 3) or 0x04).toByte(),
                                0x00.toByte()
                            )
                            val recordResponse = isoDep.transceive(readRecordCommand)

                            if (isSuccessResponse(recordResponse)) {
                                apduCommands.add(ApduCommand(
                                    sequence = commandSequence++,
                                    name = "READ RECORD - SFI $sfi #$record",
                                    description = "Manual read from SFI $sfi, record $record",
                                    commandApdu = readRecordCommand.toHexString(),
                                    responseApdu = recordResponse.toHexString(),
                                    statusWord = getStatusWord(recordResponse),
                                    statusDescription = getStatusDescription(recordResponse)
                                ))

                                allRecords.add(removeStatusWord(recordResponse))
                                SecureLogger.logByteArraySize(TAG, "Record SFI=$sfi Rec=$record", recordResponse)
                            } else {
                                if (record == 1) {
                                    SecureLogger.d(TAG) { "SFI=$sfi not available" }
                                }
                                break
                            }
                        } catch (e: Exception) {
                            if (record == 1) {
                                SecureLogger.d(TAG) { "SFI=$sfi error: ${e.message}" }
                            }
                            break
                        }
                    }
                }
            }

            // Step 5: Transaction APDU - GENERATE AC (Application Cryptogram)
            try {
                val combinedRecords = allRecords.flatMap { it.toList() }.toByteArray()
                val cdol1 = findTag(combinedRecords, 0x8C.toByte()) ?: findTag(aidResponse, 0x8C.toByte())
                if (cdol1 != null && cdol1.size in 2..64) {
                    val cdolItems = parseDol(cdol1)
                    if (cdolItems.isNotEmpty()) {
                        SecureLogger.d(TAG) { "Found CDOL1 (${cdolItems.size} items): ${cdolItems.joinToString { "${it.tag}:${it.length}B" }}" }
                        val cdolData = buildDolData(cdolItems, amountCents, aipBytes)
                        if (cdolData.isNotEmpty() && cdolData.size <= 255) {
                            SecureLogger.d(TAG) { "Constructed CDOL1 data (${cdolData.size}B): ${cdolData.toHexString()}" }

                            // CLA: 80, INS: AE (GENERATE AC), P1: 80 (Request ARQC for online auth), P2: 00
                            val genAcCommand = byteArrayOf(
                                0x80.toByte(),
                                0xAE.toByte(),
                                0x80.toByte(), // Request ARQC
                                0x00.toByte(),
                                cdolData.size.toByte()
                            ) + cdolData + byteArrayOf(0x00.toByte())

                            val genAcResponse = isoDep.transceive(genAcCommand)

                            apduCommands.add(ApduCommand(
                                sequence = commandSequence++,
                                name = "GENERATE AC (ARQC)",
                                description = "Request Application Cryptogram for transaction authorization",
                                commandApdu = genAcCommand.toHexString(),
                                responseApdu = genAcResponse.toHexString(),
                                statusWord = getStatusWord(genAcResponse),
                                statusDescription = getStatusDescription(genAcResponse)
                            ))

                            if (isSuccessResponse(genAcResponse)) {
                                allRecords.add(removeStatusWord(genAcResponse))
                                SecureLogger.dSecure(TAG, "GENERATE AC Response: ${genAcResponse.toHexString()}")
                            } else {
                                SecureLogger.d(TAG) { "GENERATE AC status: ${getStatusWord(genAcResponse)}" }
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                SecureLogger.d(TAG) { "GENERATE AC step skipped or unsupported: ${t.message}" }
            }

            // Parse all data using EmvTagParser
            val applicationInfo = emvTagParser.parseApplicationInfo(aidBytes, aidResponse)
            val transactionData = emvTagParser.parseTransactionData(allRecords, amountCents)
            val cardholderData = emvTagParser.parseCardholderData(allRecords)

            // Extract all tags from all records
            val allData = allRecords.flatMap { it.toList() }.toByteArray()
            val additionalTags = emvTagParser.extractAllTags(allData)

            val emvCardData = EmvCardData(
                applicationInfo = applicationInfo,
                transactionData = transactionData,
                cardholderData = cardholderData,
                apduCommands = apduCommands,
                additionalTags = additionalTags
            )

            SecureLogger.d(TAG) { "Card read successfully with ${apduCommands.size} APDU exchanges" }
            Result.success(emvCardData)

        } catch (e: IOException) {
            Log.e(TAG, "IO Error reading card", e)
            Result.failure(Exception("Communication error with card: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading card", e)
            Result.failure(Exception("Error reading card: ${e.message}"))
        } finally {
            try {
                isoDep?.close()
                SecureLogger.d(TAG) { "Connection closed" }
            } catch (e: IOException) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
    }

    /**
     * Parses an EMV Data Object List (DOL) into Tag and Length items.
     * Handles single-byte, two-byte, and multi-byte tags as well as variable-length encodings.
     */
    internal fun parseDol(dolBytes: ByteArray): List<DolItem> {
        val items = mutableListOf<DolItem>()
    internal fun parseDol(dolBytes: ByteArray): List<DolItem> {
        val items = mutableListOf<DolItem>()
        var i = 0

        while (i < dolBytes.size) {
            val b1 = dolBytes[i].toInt() and 0xFF
            i++
            // Skip padding or invalid bytes
            if (b1 == 0x00 || b1 == 0xFF) continue

            val tagBytes = mutableListOf(b1)
            if ((b1 and 0x1F) == 0x1F) {
                while (i < dolBytes.size) {
                    val nextB = dolBytes[i].toInt() and 0xFF
                    i++
                    tagBytes.add(nextB)
                    if ((nextB and 0x80) == 0) break
                }
            }

            val tagHex = tagBytes.joinToString("") { "%02X".format(it) }

            if (i < dolBytes.size) {
                // In EMV specifications (Book 3 Sec 5.4), DOL item length is strictly 1 byte
                val len = dolBytes[i].toInt() and 0xFF
                i++
                // Validate reasonable EMV tag length (1 to 64 bytes) to avoid corrupt data
                if (len in 1..64) {
                    items.add(DolItem(tagHex, len))
                }
            }
        }

        return items
    }

    /**
     * Constructs standards-compliant EMV terminal transaction data for a given DOL.
     * Accurately provides TTQ, actual transaction amount in BCD, current date, random numbers,
     * country and currency codes according to EMVCo Contactless specifications.
     */
    internal fun buildDolData(
        dolItems: List<DolItem>,
        amountCents: Long?,
        aip: ByteArray? = null
    ): ByteArray {
        val output = mutableListOf<Byte>()

        for (item in dolItems) {
            // Guard against excessive allocation
            val safeLength = item.length.coerceIn(1, 64)

            val value = when (item.tag) {
                // 9F66: Terminal Transaction Qualifiers (TTQ)
                // 0x36, 0x20, 0x40, 0x00 indicates Contactless EMV (qVSDC) supported,
                // Contactless MSD supported, Online PIN & Signature supported,
                // Mobile CVM supported, Online Cryptogram required.
                "9F66" -> byteArrayOf(0x36.toByte(), 0x20.toByte(), 0x40.toByte(), 0x00.toByte())

                // 9F02: Amount, Authorised (Numeric - 6 bytes BCD)
                "9F02" -> formatBcdAmount(amountCents ?: 0L, safeLength)

                // 9F03: Amount, Other (Numeric - 6 bytes BCD)
                "9F03" -> ByteArray(safeLength)

                // 9F1A: Terminal Country Code (2 bytes BCD - 08 40 = USD/USA)
                "9F1A" -> byteArrayOf(0x08.toByte(), 0x40.toByte())

                // 5F2A: Transaction Currency Code (2 bytes BCD - 08 40 = USD)
                "5F2A" -> byteArrayOf(0x08.toByte(), 0x40.toByte())

                // 9A: Transaction Date (3 bytes BCD - YY MM DD)
                "9A" -> formatBcdDate()

                // 9C: Transaction Type (1 byte - 00 = Purchase)
                "9C" -> byteArrayOf(0x00.toByte())

                // 9F37: Unpredictable Number (4 bytes cryptographically random)
                "9F37" -> ByteArray(safeLength).also { SecureRandom().nextBytes(it) }

                // 9F35: Terminal Type (1 byte - 22 = Attended Online Merchant Terminal)
                "9F35" -> byteArrayOf(0x22.toByte())

                // 9F33: Terminal Capabilities (3 bytes - E0 F8 C8: IC, Magstripe, PIN, Signature, DDA, CDA)
                "9F33" -> byteArrayOf(0xE0.toByte(), 0xF8.toByte(), 0xC8.toByte())

                // 95: Terminal Verification Results (TVR - 5 bytes 0x00)
                "95" -> ByteArray(safeLength)

                // 82: Application Interchange Profile (AIP)
                "82" -> aip?.takeIf { it.size == safeLength } ?: ByteArray(safeLength)

                // 9F36: Application Transaction Counter (ATC)
                "9F36" -> ByteArray(safeLength)

                // 9F34: CVM Results (3 bytes - 1F 00 02: No CVM required/successful)
                "9F34" -> byteArrayOf(0x1F.toByte(), 0x00.toByte(), 0x02.toByte())

                // 9F1E: IFD Serial Number (8 bytes ASCII)
                "9F1E" -> "12345678".take(safeLength).padEnd(safeLength, '0').toByteArray()

                // 9F4E: Merchant Name and Location
                "9F4E" -> "TAP TO PAY          ".take(safeLength).padEnd(safeLength, ' ').toByteArray()

                // Default fallback: zero-padded array of safe length (never exceeds 64B)
                else -> ByteArray(safeLength)
            }

            val adjusted = when {
                value.size == safeLength -> value
                value.size < safeLength -> value + ByteArray(safeLength - value.size)
                else -> value.copyOfRange(0, safeLength)
            }
            output.addAll(adjusted.toList())
        }

        return output.toByteArray()
    }

    private fun formatBcdAmount(cents: Long, targetLength: Int = 6): ByteArray {
        val totalDigits = targetLength * 2
        val clampedCents = cents.coerceAtLeast(0L)
        val formattedStr = "%0${totalDigits}d".format(clampedCents).takeLast(totalDigits)
        val result = ByteArray(targetLength)
        for (i in 0 until targetLength) {
            val highNibble = formattedStr[i * 2].digitToInt(16)
            val lowNibble = formattedStr[i * 2 + 1].digitToInt(16)
            result[i] = ((highNibble shl 4) or lowNibble).toByte()
        }
        return result
    }

    private fun formatBcdDate(): ByteArray {
        val dateStr = SimpleDateFormat("yyMMdd", Locale.US).format(Date())
        val result = ByteArray(3)
        for (i in 0 until 3) {
            val highNibble = dateStr[i * 2].digitToInt(16)
            val lowNibble = dateStr[i * 2 + 1].digitToInt(16)
            result[i] = ((highNibble shl 4) or lowNibble).toByte()
        }
        return result
    }

    private fun extractAID(response: ByteArray): ByteArray? {
        val aidTag = 0x4F.toByte()
        var i = 0
        while (i < response.size - 1) {
            if (response[i] == aidTag) {
                val length = response[i + 1].toInt() and 0xFF
                if (i + 2 + length <= response.size) {
                    return response.copyOfRange(i + 2, i + 2 + length)
                }
            }
            i++
        }
        return null
    }

    private fun buildSelectCommand(aid: ByteArray): ByteArray {
        return byteArrayOf(
            0x00.toByte(), // CLA
            0xA4.toByte(), // INS (SELECT)
            0x04.toByte(), // P1
            0x00.toByte(), // P2
            aid.size.toByte() // Lc
        ) + aid + byteArrayOf(0x00.toByte()) // Le
    }

    private fun parsePdolLength(pdol: ByteArray): Int {
        var totalLength = 0
        var i = 0

        while (i < pdol.size) {
            val currentByte = pdol[i].toInt() and 0xFF
            val is2ByteTag = (currentByte == 0x9F || currentByte == 0x5F ||
                             currentByte == 0xBF || currentByte == 0xDF)

            if (is2ByteTag) {
                i += 2
            } else {
                i += 1
            }

            if (i < pdol.size) {
                val length = pdol[i].toInt() and 0xFF
                totalLength += length
                i++
            }
        }

        return totalLength
    }

    private fun isSuccessResponse(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private fun getStatusWord(response: ByteArray): String {
        if (response.size < 2) return "N/A"
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return String.format("%02X %02X", sw1, sw2)
    }

    private fun getStatusDescription(response: ByteArray): String {
        if (response.size < 2) return "Invalid response"
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF

        return when {
            sw1 == 0x90 && sw2 == 0x00 -> "OK"
            sw1 == 0x6A && sw2 == 0x82 -> "File not found"
            sw1 == 0x6A && sw2 == 0x81 -> "Function not supported"
            sw1 == 0x69 && sw2 == 0x85 -> "Conditions not satisfied"
            sw1 == 0x6D && sw2 == 0x00 -> "Instruction not supported"
            sw1 == 0x6E && sw2 == 0x00 -> "Class not supported"
            else -> String.format("Error: %02X %02X", sw1, sw2)
        }
    }

    private fun findTag(data: ByteArray, tag: Byte): ByteArray? {
        var i = 0
        while (i < data.size - 1) {
            if (data[i] == tag) {
                val length = data[i + 1].toInt() and 0xFF
                if (i + 2 + length <= data.size) {
                    return data.copyOfRange(i + 2, i + 2 + length)
                }
            }
            i++
        }
        return null
    }

    private fun findTag(data: ByteArray, tag1: Byte, tag2: Byte): ByteArray? {
        var i = 0
        while (i < data.size - 2) {
            if (data[i] == tag1 && data[i + 1] == tag2) {
                val length = data[i + 2].toInt() and 0xFF
                if (i + 3 + length <= data.size) {
                    return data.copyOfRange(i + 3, i + 3 + length)
                }
            }
            i++
        }
        return null
    }

    private fun removeStatusWord(response: ByteArray): ByteArray {
        return if (response.size >= 2) {
            response.copyOfRange(0, response.size - 2)
        } else {
            response
        }
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(" ") { byte -> "%02X".format(byte) }
    }
}