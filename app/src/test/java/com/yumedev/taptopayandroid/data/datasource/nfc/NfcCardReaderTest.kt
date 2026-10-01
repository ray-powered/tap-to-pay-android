package com.yumedev.taptopayandroid.data.datasource.nfc

import com.yumedev.taptopayandroid.data.parser.EmvTagParser
import com.yumedev.taptopayandroid.domain.model.TerminalConfig
import com.yumedev.taptopayandroid.domain.model.TvrMode
import com.yumedev.taptopayandroid.domain.usecase.ValidatePanUseCase
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class NfcCardReaderTest {

    private val validatePanUseCase = ValidatePanUseCase()
    private val emvTagParser = EmvTagParser(validatePanUseCase)
    private val reader = NfcCardReader(emvTagParser)

    @Test
    fun `isSuccessResponse returns true for valid 90 00 status`() {
        val response = byteArrayOf(0x6F.toByte(), 0x10.toByte(), 0x90.toByte(), 0x00.toByte())

        val isSuccess = reader.javaClass.getDeclaredMethod("isSuccessResponse", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as Boolean

        assertThat(isSuccess).isTrue()
    }

    @Test
    fun `isSuccessResponse returns false for error status 6A 82`() {
        val response = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        val isSuccess = reader.javaClass.getDeclaredMethod("isSuccessResponse", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as Boolean

        assertThat(isSuccess).isFalse()
    }

    @Test
    fun `getStatusDescription returns OK for 90 00`() {
        val response = byteArrayOf(0x90.toByte(), 0x00.toByte())

        val description = reader.javaClass.getDeclaredMethod("getStatusDescription", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as String

        assertThat(description).isEqualTo("OK")
    }

    @Test
    fun `getStatusDescription returns File not found for 6A 82`() {
        val response = byteArrayOf(0x6A.toByte(), 0x82.toByte())

        val description = reader.javaClass.getDeclaredMethod("getStatusDescription", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as String

        assertThat(description).isEqualTo("File not found")
    }

    @Test
    fun `extractAID finds valid AID tag 4F in response`() {
        val aidBytes = byteArrayOf(0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte())
        val response = byteArrayOf(
            0x6F.toByte(), 0x10.toByte(),
            0x4F.toByte(), 0x05.toByte()
        ) + aidBytes + byteArrayOf(0x90.toByte(), 0x00.toByte())

        val extractedAid = reader.javaClass.getDeclaredMethod("extractAID", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as ByteArray?

        assertThat(extractedAid).isNotNull()
        assertThat(extractedAid).isEqualTo(aidBytes)
    }

    @Test
    fun `extractAID returns null when no AID tag present`() {
        val response = byteArrayOf(0x6F.toByte(), 0x05.toByte(), 0x50.toByte(), 0x03.toByte(), 0x56.toByte(), 0x49.toByte(), 0x53.toByte())

        val extractedAid = reader.javaClass.getDeclaredMethod("extractAID", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as ByteArray?

        assertThat(extractedAid).isNull()
    }

    @Test
    fun `removeStatusWord removes last 2 bytes correctly`() {
        val response = byteArrayOf(0x6F.toByte(), 0x10.toByte(), 0x84.toByte(), 0x05.toByte(), 0x90.toByte(), 0x00.toByte())
        val expected = byteArrayOf(0x6F.toByte(), 0x10.toByte(), 0x84.toByte(), 0x05.toByte())

        val cleaned = reader.javaClass.getDeclaredMethod("removeStatusWord", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, response) as ByteArray

        assertThat(cleaned).isEqualTo(expected)
    }

    @Test
    fun `buildSelectCommand creates valid APDU for AID`() {
        val aid = byteArrayOf(0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte())

        val command = reader.javaClass.getDeclaredMethod("buildSelectCommand", ByteArray::class.java).apply {
            isAccessible = true
        }.invoke(reader, aid) as ByteArray

        assertThat(command[0]).isEqualTo(0x00.toByte())
        assertThat(command[1]).isEqualTo(0xA4.toByte())
        assertThat(command[4]).isEqualTo(aid.size.toByte())
    }

    @Test
    fun `parseDol correctly parses standard Visa PDOL`() {
        val visaPdol = byteArrayOf(
            0x9F.toByte(), 0x66.toByte(), 0x04.toByte(),
            0x9F.toByte(), 0x02.toByte(), 0x06.toByte(),
            0x9F.toByte(), 0x03.toByte(), 0x06.toByte(),
            0x9F.toByte(), 0x1A.toByte(), 0x02.toByte(),
            0x95.toByte(), 0x05.toByte(),
            0x5F.toByte(), 0x2A.toByte(), 0x02.toByte(),
            0x9A.toByte(), 0x03.toByte(),
            0x9C.toByte(), 0x01.toByte(),
            0x9F.toByte(), 0x37.toByte(), 0x04.toByte()
        )

        val items = reader.parseDol(visaPdol)

        assertThat(items).hasSize(9)
        assertThat(items.map { it.tag }).containsExactly(
            "9F66", "9F02", "9F03", "9F1A", "95", "5F2A", "9A", "9C", "9F37"
        ).inOrder()
        assertThat(items.sumOf { it.length }).isEqualTo(33)
    }

    @Test
    fun `buildDolData correctly encodes amount and transaction parameters`() {
        val dolItems = listOf(
            NfcCardReader.DolItem("9F66", 4),
            NfcCardReader.DolItem("9F02", 6),
            NfcCardReader.DolItem("9F1A", 2),
            NfcCardReader.DolItem("9C", 1)
        )

        // Amount: $25.50 -> 2550 cents
        val data = reader.buildDolData(dolItems, amountCents = 2550L)

        // Total length: 4 + 6 + 2 + 1 = 13 bytes
        assertThat(data).hasLength(13)

        // 9F66 TTQ: byte 0 should be 0x76 (qVSDC + Contactless EMV + Contact Chip + Online PIN + Signature)
        assertThat(data[0]).isEqualTo(0x76.toByte())

        // 9F02 Amount: 00 00 00 00 25 50
        assertThat(data[4]).isEqualTo(0x00.toByte())
        assertThat(data[5]).isEqualTo(0x00.toByte())
        assertThat(data[6]).isEqualTo(0x00.toByte())
        assertThat(data[7]).isEqualTo(0x00.toByte())
        assertThat(data[8]).isEqualTo(0x25.toByte())
        assertThat(data[9]).isEqualTo(0x50.toByte())

        // 9F1A Country: 08 40
        assertThat(data[10]).isEqualTo(0x08.toByte())
        assertThat(data[11]).isEqualTo(0x40.toByte())

        // 9C Transaction Type: 00
        assertThat(data[12]).isEqualTo(0x00.toByte())
    }

    @Test
    fun `buildDolData uses custom TerminalConfig values`() {
        val dolItems = listOf(
            NfcCardReader.DolItem("5F2A", 2), // Currency: CNY (0156)
            NfcCardReader.DolItem("9F1A", 2), // Country: China (0156)
            NfcCardReader.DolItem("9C", 1),   // Type: Refund (20)
            NfcCardReader.DolItem("9F66", 4)  // TTQ: 26 20 40 00
        )

        val customConfig = com.yumedev.taptopayandroid.domain.model.TerminalConfig(
            currencyCode = "0156",
            countryCode = "0156",
            transactionType = "20",
            ttqHex = "26204000"
        )

        val data = reader.buildDolData(dolItems, amountCents = 1000L, terminalConfig = customConfig)

        assertThat(data).hasLength(9)
        // 5F2A: 01 56
        assertThat(data[0]).isEqualTo(0x01.toByte())
        assertThat(data[1]).isEqualTo(0x56.toByte())
        // 9F1A: 01 56
        assertThat(data[2]).isEqualTo(0x01.toByte())
        assertThat(data[3]).isEqualTo(0x56.toByte())
        // 9C: 20
        assertThat(data[4]).isEqualTo(0x20.toByte())
        // 9F66: 26 20 40 00
        assertThat(data[5]).isEqualTo(0x26.toByte())
    }

    @Test
    fun `calculateTvr in automatic mode handles floor limit and ODA status`() {
        val config = TerminalConfig(
            tvrMode = TvrMode.AUTOMATIC,
            floorLimit = 5000L // $50.00
        )
        // AIP with SDA (0x40)
        val aipWithOda = byteArrayOf(0x40.toByte(), 0x00.toByte())
        // AIP without ODA (e.g. 0x00)
        val aipNoOda = byteArrayOf(0x00.toByte(), 0x00.toByte())

        // 1. Transaction under floor limit with ODA: TVR all zeros
        val tvrUnderLimit = reader.calculateTvr(amountCents = 2500L, aip = aipWithOda, terminalConfig = config)
        assertThat(tvrUnderLimit).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00))

        // 2. Transaction over floor limit ($60.00 > $50.00): Byte 3 Bit 8 (0x80) set
        val tvrOverLimit = reader.calculateTvr(amountCents = 6000L, aip = aipWithOda, terminalConfig = config)
        assertThat(tvrOverLimit).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte(), 0x00))

        // 3. Card without ODA: Byte 0 Bit 8 (0x80) set
        val tvrNoOda = reader.calculateTvr(amountCents = 2500L, aip = aipNoOda, terminalConfig = config)
        assertThat(tvrNoOda).isEqualTo(byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00, 0x00))

        // 4. Over limit AND no ODA
        val tvrBoth = reader.calculateTvr(amountCents = 6000L, aip = aipNoOda, terminalConfig = config)
        assertThat(tvrBoth).isEqualTo(byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x80.toByte(), 0x00))
    }

    @Test
    fun `calculateTvr in manual mode returns configured manual TVR`() {
        val manualConfig = TerminalConfig(
            tvrMode = TvrMode.MANUAL,
            manualTvrHex = "8000408000"
        )
        val tvr = reader.calculateTvr(amountCents = 1000L, aip = null, terminalConfig = manualConfig)
        assertThat(tvr).isEqualTo(byteArrayOf(0x80.toByte(), 0x00, 0x40.toByte(), 0x80.toByte(), 0x00))
    }

    @Test
    fun `calculateTsi sets TRM and ODA performed flags`() {
        val config = TerminalConfig()
        val aipWithOda = byteArrayOf(0x20.toByte(), 0x00.toByte()) // DDA supported
        val aipNoOda = byteArrayOf(0x00.toByte(), 0x00.toByte())

        // With ODA: TRM (0x08) + ODA (0x80) = 0x88
        val tsiWithOda = reader.calculateTsi(aip = aipWithOda, terminalConfig = config)
        assertThat(tsiWithOda).isEqualTo(byteArrayOf(0x88.toByte(), 0x00))

        // Without ODA: TRM only = 0x08
        val tsiNoOda = reader.calculateTsi(aip = aipNoOda, terminalConfig = config)
        assertThat(tsiNoOda).isEqualTo(byteArrayOf(0x08.toByte(), 0x00))
    }

    @Test
    fun `buildDolData encodes Tag 95 TVR, Tag 9B TSI, and Tag 9F1B Floor Limit`() {
        val dolItems = listOf(
            NfcCardReader.DolItem("95", 5),
            NfcCardReader.DolItem("9B", 2),
            NfcCardReader.DolItem("9F1B", 4)
        )

        val config = TerminalConfig(
            floorLimit = 10000L, // 0x00002710
            tvrMode = TvrMode.MANUAL,
            manualTvrHex = "0000008000"
        )

        val data = reader.buildDolData(dolItems, amountCents = 15000L, terminalConfig = config)
        assertThat(data).hasLength(11)

        // Tag 95 (5 bytes)
        assertThat(data.copyOfRange(0, 5)).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte(), 0x00))
        // Tag 9B (2 bytes) - TRM performed (0x08)
        assertThat(data.copyOfRange(5, 7)).isEqualTo(byteArrayOf(0x08.toByte(), 0x00))
        // Tag 9F1B (4 bytes binary): 10000 = 0x00002710
        assertThat(data.copyOfRange(7, 11)).isEqualTo(byteArrayOf(0x00, 0x00, 0x27, 0x10))
    }

    @Test
    fun `calculateTvr under FORCE_TC suppresses floor limit exceeded and missing ODA to allow offline TC`() {
        val forceTcConfig = TerminalConfig(
            tvrMode = TvrMode.AUTOMATIC,
            floorLimit = 5000L, // $50.00
            genAcRequestMode = com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.FORCE_TC
        )
        val aipNoOda = byteArrayOf(0x00.toByte(), 0x00.toByte())

        // Even with amount > floorLimit and no ODA supported, FORCE_TC ensures TVR is clean (0000000000)
        // so that Card Action Analysis (CAA) does not trigger IAC-Online
        val tvr = reader.calculateTvr(amountCents = 15000L, aip = aipNoOda, terminalConfig = forceTcConfig)
        assertThat(tvr).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00))
    }

    @Test
    fun `calculateTvr under FORCE_ARQC sets Merchant forced transaction online bit`() {
        val forceArqcConfig = TerminalConfig(
            tvrMode = TvrMode.AUTOMATIC,
            floorLimit = 10000L,
            genAcRequestMode = com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.FORCE_ARQC
        )
        val aipWithOda = byteArrayOf(0x40.toByte(), 0x00.toByte())

        // Byte 4 Bit 4 (0x08) is set for Merchant forced transaction online
        val tvr = reader.calculateTvr(amountCents = 1000L, aip = aipWithOda, terminalConfig = forceArqcConfig)
        assertThat(tvr).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x08.toByte(), 0x00))
    }

    @Test
    fun `buildDolData under FORCE_TC encodes offline TTQ and terminal type 23`() {
        val dolItems = listOf(
            NfcCardReader.DolItem("9F66", 4), // TTQ
            NfcCardReader.DolItem("9F35", 1)  // Terminal Type
        )
        val forceTcConfig = TerminalConfig(
            ttqHex = "76204000",
            terminalTypeHex = "22",
            genAcRequestMode = com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.FORCE_TC
        )

        val data = reader.buildDolData(dolItems, amountCents = 1000L, terminalConfig = forceTcConfig)
        assertThat(data).hasLength(5)

        // 9F66 TTQ: 76 | 08 = 7E (Reader offline only bit set), 20 (Online cryptogram cleared)
        assertThat(data[0]).isEqualTo(0x7E.toByte())
        assertThat(data[1]).isEqualTo(0x20.toByte())
        assertThat(data[2]).isEqualTo(0x40.toByte())
        assertThat(data[3]).isEqualTo(0x00.toByte())

        // 9F35 Terminal Type: 23 (Attended Offline Only)
        assertThat(data[4]).isEqualTo(0x23.toByte())
    }
}
