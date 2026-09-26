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
        assertThat(config.ttqHex).isEqualTo("36204000")
        assertThat(config.terminalCapabilitiesHex).isEqualTo("E0F8C8")
        assertThat(config.terminalTypeHex).isEqualTo("22")
    }

    @Test
    fun `TTQ bit flags decode correctly`() {
        val config = TerminalConfig(ttqHex = "36204000")

        // Byte 0 (36 = 0011 0110):
        // Bit 8 (0x80): false (MSD not set in 0x36)
        // Bit 7 (0x40): false (qVSDC not set in 0x36: wait, 0x36 = 0011 0110 -> 0x20 is online PIN, 0x10 is signature, 0x04 is ODA, 0x02 is CDA)
        // Byte 1 (20 = 0010 0000): Bit 6 (0x20) is Contact Chip Offline PIN
        // Byte 2 (40 = 0100 0000): Bit 7 (0x40) is Mobile CVM supported -> true
        assertThat(config.ttqMobileCvmSupported).isTrue()
    }

    @Test
    fun `withTtqBit modifies specific bit correctly`() {
        var config = TerminalConfig(ttqHex = "00000000")

        // Enable Mobile CVM (Byte 2, 0x40)
        config = config.withTtqBit(2, 0x40, true)
        assertThat(config.ttqMobileCvmSupported).isTrue()
        assertThat(config.ttqHex).isEqualTo("00004000")

        // Disable Mobile CVM
        config = config.withTtqBit(2, 0x40, false)
        assertThat(config.ttqMobileCvmSupported).isFalse()
        assertThat(config.ttqHex).isEqualTo("00000000")
    }

    @Test
    fun `formattedTtq inserts spaces between bytes`() {
        val config = TerminalConfig(ttqHex = "36204000")
        assertThat(config.formattedTtq).isEqualTo("36 20 40 00")
    }
}
