package com.yumedev.taptopayandroid.domain.model

enum class PosLedColorMode {
    EMV_GREEN,     // Standard EMV 4-Green LEDs (LED 1 Solid, LED 2 Blinks on APDU, All 4 Solid on Success)
    UNIONPAY_COLOR // Standard 4-Color LEDs (Blue, Yellow, Green, Red)
}

/**
 * Configuration for POS Terminal and EMV transaction attributes.
 * Allows customizing currency, country, transaction type, TTQ, terminal capabilities,
 * terminal type, merchant details, and other EMV data objects.
 */
data class TerminalConfig(
    // Currency (Tag 5F2A & 9F3C)
    val currencyCode: String = "0840", // ISO 4217 Numeric BCD (e.g. 0840 = USD, 0156 = CNY)
    val currencySymbol: String = "$",   // Display symbol (e.g. "$", "¥", "€", "£")
    val currencyExponent: Int = 2,     // Decimal places (usually 2, 0 for JPY/KRW)

    // Terminal Country Code (Tag 9F1A)
    val countryCode: String = "0840",  // ISO 3166-1 Numeric BCD (e.g. 0840 = USA, 0156 = China)

    // Transaction Type (Tag 9C)
    val transactionType: String = "00", // 1 byte hex (00 = Purchase, 01 = Cash, 09 = Cashback, 20 = Refund)

    // Terminal Transaction Qualifiers (TTQ, Tag 9F66 - 4 bytes hex)
    // 0x36, 0x20, 0x40, 0x00 indicates Contactless EMV (qVSDC) supported,
    // MSD supported, Online PIN, Signature, Mobile CVM supported, Online Cryptogram required.
    val ttqHex: String = "36204000",

    // Terminal Capabilities (Tag 9F33 - 3 bytes hex)
    // E0 F8 C8: IC, Magstripe, PIN, Signature, DDA, CDA
    val terminalCapabilitiesHex: String = "E0F8C8",

    // Terminal Type (Tag 9F35 - 1 byte hex)
    // 22 = Attended Online Merchant Terminal
    val terminalTypeHex: String = "22",

    // Merchant Name and Location (Tag 9F4E)
    val merchantName: String = "TAP TO PAY SHOP",

    // IFD Serial Number / Terminal Hardware Serial (Tag 9F1E - 8 bytes ASCII)
    val ifdSerialNumber: String = "12345678",

    // Merchant Category Code (Tag 9F15 - 2 bytes BCD)
    val merchantCategoryCode: String = "5411", // 5411 = Grocery Stores

    // Additional Terminal Capabilities (Tag 9F40 - 5 bytes hex)
    val additionalTerminalCapabilitiesHex: String = "6000F0A001",

    // Contactless 4-LED Color Scheme (EMV Classic 4-Green vs. UnionPay 4-Color)
    val ledColorMode: PosLedColorMode = PosLedColorMode.EMV_GREEN
) {

    // --- TTQ Bitwise Accessors ---
    // Byte 0: Bit 8 (0x80) MSD supported, Bit 7 (0x40) qVSDC/EMV supported, Bit 6 (0x20) Offline only,
    //         Bit 5 (0x10) Online PIN, Bit 4 (0x08) Signature, Bit 3 (0x04) ODA for online auth
    // Byte 1: Bit 8 (0x80) Online cryptogram required (ARQC), Bit 7 (0x40) CVM required, Bit 6 (0x20) Offline PIN
    // Byte 2: Bit 8 (0x80) Issuer update supported, Bit 7 (0x40) Mobile CVM supported
    // Byte 3: Reserved

    private fun getTtqBytes(): ByteArray {
        val clean = ttqHex.replace(" ", "")
        val bytes = ByteArray(4)
        for (i in 0 until 4) {
            val hexPair = if (i * 2 + 2 <= clean.length) clean.substring(i * 2, i * 2 + 2) else "00"
            bytes[i] = hexPair.toIntOrNull(16)?.toByte() ?: 0
        }
        return bytes
    }

    private fun isTtqBitSet(byteIndex: Int, bitMask: Int): Boolean {
        val bytes = getTtqBytes()
        if (byteIndex !in 0..3) return false
        return (bytes[byteIndex].toInt() and bitMask) != 0
    }

    val ttqMagStripeSupported: Boolean get() = isTtqBitSet(0, 0x80)
    val ttqEmvSupported: Boolean get() = isTtqBitSet(0, 0x40)
    val ttqEmvOfflineOnly: Boolean get() = isTtqBitSet(0, 0x20)
    val ttqOnlinePinSupported: Boolean get() = isTtqBitSet(0, 0x10)
    val ttqSignatureSupported: Boolean get() = isTtqBitSet(0, 0x08)
    val ttqOfflineDataAuthSupported: Boolean get() = isTtqBitSet(0, 0x04)

    val ttqOnlineCryptogramRequired: Boolean get() = isTtqBitSet(1, 0x80)
    val ttqCvmRequired: Boolean get() = isTtqBitSet(1, 0x40)
    val ttqContactOfflinePinSupported: Boolean get() = isTtqBitSet(1, 0x20)

    val ttqIssuerUpdateSupported: Boolean get() = isTtqBitSet(2, 0x80)
    val ttqMobileCvmSupported: Boolean get() = isTtqBitSet(2, 0x40)

    fun withTtqBit(byteIndex: Int, bitMask: Int, enabled: Boolean): TerminalConfig {
        val bytes = getTtqBytes()
        if (byteIndex in 0..3) {
            val current = bytes[byteIndex].toInt() and 0xFF
            val updated = if (enabled) current or bitMask else current and bitMask.inv()
            bytes[byteIndex] = updated.toByte()
        }
        val newTtqHex = bytes.joinToString("") { "%02X".format(it) }
        return copy(ttqHex = newTtqHex)
    }

    // --- Formatting Helpers ---
    val formattedTtq: String
        get() = ttqHex.replace(" ", "").chunked(2).joinToString(" ")

    val formattedTerminalCapabilities: String
        get() = terminalCapabilitiesHex.replace(" ", "").chunked(2).joinToString(" ")

    val currencyDisplayName: String
        get() {
            val matched = Currencies.find { it.code == currencyCode }
            return matched?.name ?: "$currencyCode ($currencySymbol)"
        }

    val countryDisplayName: String
        get() {
            val matched = Countries.find { it.code == countryCode }
            return matched?.name ?: "Country ($countryCode)"
        }

    val transactionTypeDisplayName: String
        get() {
            val matched = TransactionTypes.find { it.code.equals(transactionType, ignoreCase = true) }
            return matched?.name ?: "Type ($transactionType)"
        }

    companion object {
        // Preset Currencies
        val Currencies = listOf(
            CurrencyPreset("0840", "$", 2, "USD - US Dollar ($)"),
            CurrencyPreset("0156", "¥", 2, "CNY - Chinese Yuan (¥)"),
            CurrencyPreset("0978", "€", 2, "EUR - Euro (€)"),
            CurrencyPreset("0826", "£", 2, "GBP - British Pound (£)"),
            CurrencyPreset("0392", "¥", 0, "JPY - Japanese Yen (¥)"),
            CurrencyPreset("0344", "HK$", 2, "HKD - Hong Kong Dollar (HK$)"),
            CurrencyPreset("0124", "C$", 2, "CAD - Canadian Dollar (C$)"),
            CurrencyPreset("0036", "A$", 2, "AUD - Australian Dollar (A$)"),
            CurrencyPreset("0702", "S$", 2, "SGD - Singapore Dollar (S$)"),
            CurrencyPreset("0410", "₩", 0, "KRW - South Korean Won (₩)"),
            CurrencyPreset("0756", "CHF", 2, "CHF - Swiss Franc (CHF)"),
            CurrencyPreset("0784", "AED", 2, "AED - UAE Dirham (AED)")
        )

        // Preset Countries
        val Countries = listOf(
            CountryPreset("0840", "United States (0840)"),
            CountryPreset("0156", "China (0156)"),
            CountryPreset("0826", "United Kingdom (0826)"),
            CountryPreset("0276", "Germany (0276)"),
            CountryPreset("0250", "France (0250)"),
            CountryPreset("0392", "Japan (0392)"),
            CountryPreset("0344", "Hong Kong (0344)"),
            CountryPreset("0124", "Canada (0124)"),
            CountryPreset("0036", "Australia (0036)"),
            CountryPreset("0702", "Singapore (0702)"),
            CountryPreset("0410", "South Korea (0410)"),
            CountryPreset("0756", "Switzerland (0756)")
        )

        // Preset Transaction Types (Tag 9C)
        val TransactionTypes = listOf(
            TransactionTypePreset("00", "00 - Purchase", "Goods and services purchase"),
            TransactionTypePreset("01", "01 - Cash Advance", "Cash advance or ATM cash disbursement"),
            TransactionTypePreset("09", "09 - Cashback", "Purchase with cashback"),
            TransactionTypePreset("20", "20 - Refund", "Return or refund transaction"),
            TransactionTypePreset("30", "30 - Balance Inquiry", "Balance inquiry"),
            TransactionTypePreset("31", "31 - Transfer", "Account transfer")
        )

        // Preset TTQ configurations (Tag 9F66)
        val TtqPresets = listOf(
            TtqPreset("Standard Online POS", "36204000", "qVSDC + MSD + Online PIN + Signature + Mobile CVM + ARQC"),
            TtqPreset("Mobile CVM Preferred (Apple/Google Pay)", "36204000", "Contactless EMV + CDCVM required for mobile wallets"),
            TtqPreset("Contactless EMV Only (No MSD)", "26204000", "Strict EMV chip emulation, magstripe disabled"),
            TtqPreset("Offline Capable POS", "36004000", "Offline transaction permitted, no online cryptogram required"),
            TtqPreset("Signature Only POS", "32200000", "Contactless with signature CVM only")
        )

        // Preset Terminal Capabilities (Tag 9F33)
        val CapabilitiesPresets = listOf(
            CapabilitiesPreset("Standard All-in-One POS", "E0F8C8", "IC + Magstripe + PIN + Signature + DDA + CDA"),
            CapabilitiesPreset("Chip & Online PIN Only", "204008", "IC contact/contactless + Online PIN + CDA"),
            CapabilitiesPreset("Contactless Mobile Only", "60B8C8", "Magstripe + IC + Mobile CVM + CDA")
        )

        // Preset Terminal Types (Tag 9F35)
        val TerminalTypePresets = listOf(
            TerminalTypePreset("22", "22 - Attended Online Only (Standard POS)"),
            TerminalTypePreset("21", "21 - Attended Offline w/ Online"),
            TerminalTypePreset("23", "23 - Attended Offline Only"),
            TerminalTypePreset("14", "14 - Financial Institution Online"),
            TerminalTypePreset("35", "35 - Cardholder Mobile POS")
        )
    }
}

data class CurrencyPreset(val code: String, val symbol: String, val exponent: Int, val name: String)
data class CountryPreset(val code: String, val name: String)
data class TransactionTypePreset(val code: String, val name: String, val description: String)
data class TtqPreset(val name: String, val hex: String, val description: String)
data class CapabilitiesPreset(val name: String, val hex: String, val description: String)
data class TerminalTypePreset(val code: String, val name: String)
