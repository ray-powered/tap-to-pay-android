package com.yumedev.taptopayandroid.data.datasource.nfc

import com.yumedev.taptopayandroid.data.parser.EmvTagParser
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

        // 9F66 TTQ: byte 0 should be 0x36
        assertThat(data[0]).isEqualTo(0x36.toByte())

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
}
