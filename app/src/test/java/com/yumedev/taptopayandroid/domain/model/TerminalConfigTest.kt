package com.yumedev.taptopayandroid.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TerminalConfigTest {

    @Test
    fun `default TerminalConfig has expected defaults`() {
        val config = TerminalConfig()

        assertThat(config.currencyCode).isEqualTo("0840")
        assertThat(config.currencySymbol).isEqualTo("$")
        assertThat(config.countryCode).isEqualTo("0840")
        assertThat(config.transactionType).isEqualTo("00")
        assertThat(config.ttqHex).isEqualTo("76204000")
        assertThat(config.terminalCapabilitiesHex).isEqualTo("E0F8C8")
        assertThat(config.terminalTypeHex).isEqualTo("22")
    }

    @Test
    fun `TTQ bit flags decode correctly`() {
        val config = TerminalConfig(ttqHex = "76204000")

        // Byte 1 (76 = 0111 0110):
        assertThat(config.ttqMagStripeSupported).isFalse()       // Bit 8 (0x80)
        assertThat(config.ttqQvsdcSupported).isTrue()             // Bit 7 (0x40)
        assertThat(config.ttqEmvModeSupported).isTrue()           // Bit 6 (0x20)
        assertThat(config.ttqContactChipSupported).isTrue()       // Bit 5 (0x10)
        assertThat(config.ttqReaderOfflineOnly).isFalse()         // Bit 4 (0x08)
        assertThat(config.ttqOnlinePinSupported).isTrue()         // Bit 3 (0x04)
        assertThat(config.ttqSignatureSupported).isTrue()         // Bit 2 (0x02)
        assertThat(config.ttqOfflineDataAuthSupported).isFalse()  // Bit 1 (0x01)

        // Byte 2 (20 = 0010 0000):
        assertThat(config.ttqOnlineCryptogramRequired).isFalse()  // Bit 8 (0x80)
        assertThat(config.ttqCvmRequired).isFalse()               // Bit 7 (0x40)
        assertThat(config.ttqContactOfflinePinSupported).isTrue() // Bit 6 (0x20)

        // Byte 3 (40 = 0100 0000):
        assertThat(config.ttqIssuerUpdateSupported).isFalse()     // Bit 8 (0x80)
        assertThat(config.ttqMobileCvmSupported).isTrue()         // Bit 7 (0x40)
    }

    @Test
    fun `withTtqBit modifies specific bit correctly`() {
        var config = TerminalConfig(ttqHex = "00000000")

        // Enable Mobile CVM (Byte 2, 0x40)
        config = config.withTtqBit(2, 0x40, true)
        assertThat(config.ttqMobileCvmSupported).isTrue()
        assertThat(config.ttqHex).isEqualTo("00004000")

        // Enable qVSDC (Byte 0, 0x40)
        config = config.withTtqBit(0, 0x40, true)
        assertThat(config.ttqQvsdcSupported).isTrue()
        assertThat(config.ttqHex).isEqualTo("40004000")

        // Disable Mobile CVM
        config = config.withTtqBit(2, 0x40, false)
        assertThat(config.ttqMobileCvmSupported).isFalse()
        assertThat(config.ttqHex).isEqualTo("40000000")
    }

    @Test
    fun `Terminal Capabilities bit flags decode and modify correctly`() {
        val config = TerminalConfig(terminalCapabilitiesHex = "E0F8C8")

        // Byte 1: E0 = 1110 0000
        assertThat(config.capManualKeyEntry).isTrue()
        assertThat(config.capMagneticStripe).isTrue()
        assertThat(config.capContactIC).isTrue()

        // Byte 2: F8 = 1111 1000
        assertThat(config.capPlaintextOfflinePin).isTrue()
        assertThat(config.capOnlinePin).isTrue()
        assertThat(config.capSignature).isTrue()
        assertThat(config.capEncipheredOfflinePin).isTrue()
        assertThat(config.capNoCvm).isTrue()

        // Byte 3: C8 = 1100 1000
        assertThat(config.capSda).isTrue()
        assertThat(config.capDda).isTrue()
        assertThat(config.capCardCapture).isFalse()
        assertThat(config.capCda).isTrue()

        // Modify a capability bit
        val modified = config.withTerminalCapabilityBit(2, 0x20, true) // Enable Card Capture
        assertThat(modified.capCardCapture).isTrue()
        assertThat(modified.terminalCapabilitiesHex).isEqualTo("E0F8E8")
    }

    @Test
    fun `formattedTtq inserts spaces between bytes`() {
        val config = TerminalConfig(ttqHex = "76204000")
        assertThat(config.formattedTtq).isEqualTo("76 20 40 00")
    }
}
