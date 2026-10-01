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

    @Test
    fun `TRM and TVR configuration default values are correct`() {
        val config = TerminalConfig()
        assertThat(config.floorLimit).isEqualTo(10000L)
        assertThat(config.formattedFloorLimit).isEqualTo("$100.00")
        assertThat(config.tvrMode).isEqualTo(TvrMode.AUTOMATIC)
        assertThat(config.manualTvrHex).isEqualTo("0000000000")
        assertThat(config.formattedManualTvr).isEqualTo("00 00 00 00 00")
        assertThat(config.genAcRequestMode).isEqualTo(GenAcRequestMode.AUTO_TAA)
        assertThat(config.strictOnlineAuthDisplay).isFalse()
        assertThat(config.capCdaSupported).isTrue()
        assertThat(config.capOfflineOnly).isFalse()
    }

    @Test
    fun `manual TVR bit manipulation works correctly`() {
        var config = TerminalConfig(manualTvrHex = "0000000000")

        // Byte 0, Bit 8 (0x80): Offline data auth not performed
        assertThat(config.isManualTvrBitSet(0, 0x80)).isFalse()
        config = config.withManualTvrBit(0, 0x80, true)
        assertThat(config.isManualTvrBitSet(0, 0x80)).isTrue()
        assertThat(config.manualTvrHex).isEqualTo("8000000000")

        // Byte 3, Bit 8 (0x80): Transaction exceeds floor limit
        assertThat(config.isManualTvrBitSet(3, 0x80)).isFalse()
        config = config.withManualTvrBit(3, 0x80, true)
        assertThat(config.isManualTvrBitSet(3, 0x80)).isTrue()
        assertThat(config.manualTvrHex).isEqualTo("8000008000")

        // Clear Byte 0, Bit 8
        config = config.withManualTvrBit(0, 0x80, false)
        assertThat(config.isManualTvrBitSet(0, 0x80)).isFalse()
        assertThat(config.manualTvrHex).isEqualTo("0000008000")

        val bytes = config.getManualTvrBytes()
        assertThat(bytes).isEqualTo(byteArrayOf(0x00, 0x00, 0x00, 0x80.toByte(), 0x00))
    }

    @Test
    fun `effectiveTtqHex and effectiveTerminalTypeHex dynamically adapt for FORCE_TC and FORCE_ARQC`() {
        val defaultConfig = TerminalConfig(ttqHex = "76204000", terminalTypeHex = "22")

        // 1. In AUTO_TAA mode: effective matches configured
        assertThat(defaultConfig.effectiveTtqHex).isEqualTo("76204000")
        assertThat(defaultConfig.effectiveTerminalTypeHex).isEqualTo("22")
        assertThat(defaultConfig.capOfflineOnly).isFalse()

        // 2. In FORCE_TC mode: Byte 0 Bit 4 (0x08, Reader offline only) is set to 1,
        // Byte 1 Bit 8 (0x80, Online cryptogram required) is cleared to 0.
        // 76 | 08 = 7E. 20 & ~80 = 20. Result: 7E204000
        val forceTcConfig = defaultConfig.copy(genAcRequestMode = GenAcRequestMode.FORCE_TC)
        assertThat(forceTcConfig.effectiveTtqHex).isEqualTo("7E204000")
        assertThat(forceTcConfig.effectiveTerminalTypeHex).isEqualTo("23")
        assertThat(forceTcConfig.capOfflineOnly).isTrue()

        // 3. In FORCE_ARQC mode: Byte 0 Bit 4 is 0, Byte 1 Bit 8 is set to 1.
        // 76 & ~08 = 76. 20 | 80 = A0. Result: 76A04000
        val forceArqcConfig = defaultConfig.copy(genAcRequestMode = GenAcRequestMode.FORCE_ARQC)
        assertThat(forceArqcConfig.effectiveTtqHex).isEqualTo("76A04000")
        assertThat(forceArqcConfig.effectiveTerminalTypeHex).isEqualTo("22")
    }
}
