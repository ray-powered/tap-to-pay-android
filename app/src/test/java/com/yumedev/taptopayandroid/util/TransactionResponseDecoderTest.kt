package com.yumedev.taptopayandroid.util

import com.google.common.truth.Truth.assertThat
import com.yumedev.taptopayandroid.domain.model.CryptogramType
import com.yumedev.taptopayandroid.domain.model.EmvTag
import com.yumedev.taptopayandroid.domain.model.TransactionDecision
import org.junit.Test

class TransactionResponseDecoderTest {

    @Test
    fun `analyzeTransaction with ARQC returns Approved Online when strictOnlineAuthDisplay is false`() {
        val tags = mapOf(
            "9F27" to EmvTag("9F27", "Cryptogram Information Data", "80", "ARQC", "Control"),
            "9F26" to EmvTag("9F26", "Application Cryptogram", "0102030405060708", "0102030405060708", "Control")
        )

        val result = TransactionResponseDecoder.analyzeTransaction(
            tags = tags,
            apduCommands = emptyList(),
            strictOnlineAuthDisplay = false
        )

        assertThat(result.decision).isEqualTo(TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED)
        assertThat(result.isApproved).isTrue()
        assertThat(result.isOnlineRequired).isFalse()
        assertThat(result.decisionTitle).isEqualTo("Approved Online (ARQC)")
    }

    @Test
    fun `analyzeTransaction with ARQC returns Online Authorization Required when strictOnlineAuthDisplay is true`() {
        val tags = mapOf(
            "9F27" to EmvTag("9F27", "Cryptogram Information Data", "80", "ARQC", "Control"),
            "9F26" to EmvTag("9F26", "Application Cryptogram", "0102030405060708", "0102030405060708", "Control")
        )

        val result = TransactionResponseDecoder.analyzeTransaction(
            tags = tags,
            apduCommands = emptyList(),
            strictOnlineAuthDisplay = true
        )

        assertThat(result.decision).isEqualTo(TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED)
        assertThat(result.isApproved).isFalse()
        assertThat(result.isOnlineRequired).isTrue()
        assertThat(result.decisionTitle).isEqualTo("Online Authorization Required (ARQC)")
    }

    @Test
    fun `analyzeTransaction with TC returns Approved Offline`() {
        val tags = mapOf(
            "9F27" to EmvTag("9F27", "Cryptogram Information Data", "40", "TC", "Control"),
            "9F26" to EmvTag("9F26", "Application Cryptogram", "0102030405060708", "0102030405060708", "Control")
        )

        val result = TransactionResponseDecoder.analyzeTransaction(
            tags = tags,
            apduCommands = emptyList()
        )

        assertThat(result.decision).isEqualTo(TransactionDecision.APPROVED_OFFLINE)
        assertThat(result.isApproved).isTrue()
        assertThat(result.isDeclined).isFalse()
        assertThat(result.decisionTitle).isEqualTo("Approved Offline (TC)")
    }

    @Test
    fun `analyzeTransaction with AAC returns Declined by Card`() {
        val tags = mapOf(
            "9F27" to EmvTag("9F27", "Cryptogram Information Data", "00", "AAC", "Control")
        )

        val result = TransactionResponseDecoder.analyzeTransaction(
            tags = tags,
            apduCommands = emptyList()
        )

        assertThat(result.decision).isEqualTo(TransactionDecision.DECLINED_BY_CARD)
        assertThat(result.isApproved).isFalse()
        assertThat(result.isDeclined).isTrue()
        assertThat(result.decisionTitle).isEqualTo("Declined by Card (AAC)")
    }

    @Test
    fun `decodeTvr decodes floor limit exceeded and offline auth status`() {
        // TVR: 80 00 00 80 00 -> Byte 1 Bit 8 (0x80) and Byte 4 Bit 8 (0x80)
        val tvr = TransactionResponseDecoder.decodeTvr("8000008000")

        assertThat(tvr).isNotNull()
        assertThat(tvr?.offlineDataAuthNotPerformed).isTrue()
        assertThat(tvr?.transactionExceedsFloorLimit).isTrue()
        assertThat(tvr?.sdaFailed).isFalse()
        assertThat(tvr?.expiredApplication).isFalse()
    }
}
