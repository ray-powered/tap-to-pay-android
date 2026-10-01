package com.yumedev.taptopayandroid.domain.model

enum class PosLedColorMode {
    EMV_GREEN,     // Standard EMV 4-Green LEDs (LED 1 Solid, LED 2 Blinks on APDU, All 4 Solid on Success)
    UNIONPAY_COLOR // Standard 4-Color LEDs (Blue, Yellow, Green, Red)
}

enum class TvrMode(val displayName: String, val description: String) {
    AUTOMATIC(
        "Automatic (TRM Engine)",
        "Dynamically sets TVR based on Floor Limit, ODA status, and card state"
    ),
    MANUAL(
        "Manual (Custom TVR)",
        "Manually configure TVR 5-byte hex value and individual verification bits"
    )
}

enum class GenAcRequestMode(val displayName: String, val description: String) {
    AUTO_TAA(
        "Auto (TAA Decision)",
        "Dynamically requests TC or ARQC based on floor limit, TVR, and terminal profile"
    ),
    FORCE_ARQC(
        "Force ARQC (0x80 / 0x90)",
        "Always request Authorisation Request Cryptogram for online authorization"
    ),
    FORCE_TC(
        "Force TC (0x40 / 0x50)",
        "Always request Transaction Certificate for offline authorization"
    ),
    FORCE_AAC(
        "Force AAC (0x00 / 0x10)",
        "Always request Application Authentication Cryptogram (Decline)"
    )
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
    // 0x76, 0x20, 0x40, 0x00 indicates Contactless EMV & qVSDC supported,
    // Contact EMV chip, Online PIN, Signature, Mobile CDCVM supported, Offline PIN supported.
    val ttqHex: String = "76204000",

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
    val ledColorMode: PosLedColorMode = PosLedColorMode.EMV_GREEN,

    // Terminal Floor Limit (Tag 9F1B - in minor units / cents, default: 10000 = $100.00 / ¥100.00)
    val floorLimit: Long = 10000L,

    // TVR Operating Mode (Tag 95 - Automatic TRM vs Manual TVR)
    val tvrMode: TvrMode = TvrMode.AUTOMATIC,

    // Manual TVR (Tag 95 - 5 bytes hex, e.g. 0000000000)
    val manualTvrHex: String = "0000000000",

    // GENERATE AC Request Policy
    val genAcRequestMode: GenAcRequestMode = GenAcRequestMode.AUTO_TAA,

    // Strict Online Authorization Display (If false, ARQC displays friendly "Approved Online"; if true, displays "Online Authorization Required")
    val strictOnlineAuthDisplay: Boolean = false
) {

    // --- TTQ Bitwise Accessors (EMV Book B / VCPS Tag 9F66) ---
    // Byte 1 (Index 0):
    //   Bit 8 (0x80) Mag-stripe mode supported (MSD)
    //   Bit 7 (0x40) Contactless VSDC (qVSDC) supported
    //   Bit 6 (0x20) Contactless EMV mode supported
    //   Bit 5 (0x10) EMV contact chip supported
    //   Bit 4 (0x08) Reader is offline-only
    //   Bit 3 (0x04) Online PIN supported
    //   Bit 2 (0x02) Signature supported
    //   Bit 1 (0x01) Offline Data Authentication (ODA) for Online Authorizations supported
    // Byte 2 (Index 1):
    //   Bit 8 (0x80) Online cryptogram required (ARQC)
    //   Bit 7 (0x40) CVM required
    //   Bit 6 (0x20) Contact chip offline PIN supported
    // Byte 3 (Index 2):
    //   Bit 8 (0x80) Issuer update processing supported
    //   Bit 7 (0x40) Mobile functionality / Consumer Device CVM (CDCVM) supported
    // Byte 4 (Index 3): Reserved (00)

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

    // Byte 1
    val ttqMagStripeSupported: Boolean get() = isTtqBitSet(0, 0x80)
    val ttqQvsdcSupported: Boolean get() = isTtqBitSet(0, 0x40)
    val ttqEmvSupported: Boolean get() = isTtqBitSet(0, 0x40) // Alias for qVSDC
    val ttqEmvModeSupported: Boolean get() = isTtqBitSet(0, 0x20)
    val ttqContactChipSupported: Boolean get() = isTtqBitSet(0, 0x10)
    val ttqReaderOfflineOnly: Boolean get() = isTtqBitSet(0, 0x08)
    val ttqEmvOfflineOnly: Boolean get() = isTtqBitSet(0, 0x08) // Backward compat alias
    val ttqOnlinePinSupported: Boolean get() = isTtqBitSet(0, 0x04)
    val ttqSignatureSupported: Boolean get() = isTtqBitSet(0, 0x02)
    val ttqOfflineDataAuthSupported: Boolean get() = isTtqBitSet(0, 0x01)

    // Byte 2
    val ttqOnlineCryptogramRequired: Boolean get() = isTtqBitSet(1, 0x80)
    val ttqCvmRequired: Boolean get() = isTtqBitSet(1, 0x40)
    val ttqContactOfflinePinSupported: Boolean get() = isTtqBitSet(1, 0x20)

    // Byte 3
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

    // --- Terminal Capabilities (Tag 9F33 - 3 bytes hex) Bitwise Accessors (EMV Book 4) ---
    // Byte 1: Card Data Input Capability
    //   Bit 8 (0x80) Manual Key Entry
    //   Bit 7 (0x40) Magnetic Stripe
    //   Bit 6 (0x20) IC with Contacts
    // Byte 2: Cardholder Verification Method (CVM) Capability
    //   Bit 8 (0x80) Plaintext PIN for Offline Verification
    //   Bit 7 (0x40) Enciphered PIN for Online Verification
    //   Bit 6 (0x20) Signature (paper)
    //   Bit 5 (0x10) Enciphered PIN for Offline Verification
    //   Bit 4 (0x08) No CVM Required
    // Byte 3: Security Capability
    //   Bit 8 (0x80) SDA (Static Data Authentication)
    //   Bit 7 (0x40) DDA (Dynamic Data Authentication)
    //   Bit 6 (0x20) Card Capture
    //   Bit 4 (0x08) CDA (Combined DDA/AC Generation)

    private fun getTerminalCapabilitiesBytes(): ByteArray {
        val clean = terminalCapabilitiesHex.replace(" ", "")
        val bytes = ByteArray(3)
        for (i in 0 until 3) {
            val hexPair = if (i * 2 + 2 <= clean.length) clean.substring(i * 2, i * 2 + 2) else "00"
            bytes[i] = hexPair.toIntOrNull(16)?.toByte() ?: 0
        }
        return bytes
    }

    private fun isTerminalCapabilityBitSet(byteIndex: Int, bitMask: Int): Boolean {
        val bytes = getTerminalCapabilitiesBytes()
        if (byteIndex !in 0..2) return false
        return (bytes[byteIndex].toInt() and bitMask) != 0
    }

    fun withTerminalCapabilityBit(byteIndex: Int, bitMask: Int, enabled: Boolean): TerminalConfig {
        val bytes = getTerminalCapabilitiesBytes()
        if (byteIndex in 0..2) {
            val current = bytes[byteIndex].toInt() and 0xFF
            val updated = if (enabled) current or bitMask else current and bitMask.inv()
            bytes[byteIndex] = updated.toByte()
        }
        val newHex = bytes.joinToString("") { "%02X".format(it) }
        return copy(terminalCapabilitiesHex = newHex)
    }

    // Byte 1: Card Data Input Capability
    val capManualKeyEntry: Boolean get() = isTerminalCapabilityBitSet(0, 0x80)
    val capMagneticStripe: Boolean get() = isTerminalCapabilityBitSet(0, 0x40)
    val capContactIC: Boolean get() = isTerminalCapabilityBitSet(0, 0x20)

    // Byte 2: CVM Capability
    val capPlaintextOfflinePin: Boolean get() = isTerminalCapabilityBitSet(1, 0x80)
    val capOnlinePin: Boolean get() = isTerminalCapabilityBitSet(1, 0x40)
    val capSignature: Boolean get() = isTerminalCapabilityBitSet(1, 0x20)
    val capEncipheredOfflinePin: Boolean get() = isTerminalCapabilityBitSet(1, 0x10)
    val capNoCvm: Boolean get() = isTerminalCapabilityBitSet(1, 0x08)

    // Byte 3: Security Capability
    val capSda: Boolean get() = isTerminalCapabilityBitSet(2, 0x80)
    val capDda: Boolean get() = isTerminalCapabilityBitSet(2, 0x40)
    val capCardCapture: Boolean get() = isTerminalCapabilityBitSet(2, 0x20)
    val capCda: Boolean get() = isTerminalCapabilityBitSet(2, 0x08)
    val capCdaSupported: Boolean get() = capCda

    val capOfflineOnly: Boolean
        get() = terminalTypeHex in listOf("21", "23", "11", "24") || ttqReaderOfflineOnly

    // --- TVR Accessors and Mutators (Tag 95 - 5 bytes) ---
    fun getManualTvrBytes(): ByteArray {
        val clean = manualTvrHex.replace(" ", "")
        val bytes = ByteArray(5)
        for (i in 0 until 5) {
            val hexPair = if (i * 2 + 2 <= clean.length) clean.substring(i * 2, i * 2 + 2) else "00"
            bytes[i] = hexPair.toIntOrNull(16)?.toByte() ?: 0
        }
        return bytes
    }

    fun isManualTvrBitSet(byteIndex: Int, bitMask: Int): Boolean {
        val bytes = getManualTvrBytes()
        if (byteIndex !in 0..4) return false
        return (bytes[byteIndex].toInt() and bitMask) != 0
    }

    fun withManualTvrBit(byteIndex: Int, bitMask: Int, enabled: Boolean): TerminalConfig {
        val bytes = getManualTvrBytes()
        if (byteIndex in 0..4) {
            val current = bytes[byteIndex].toInt() and 0xFF
            val updated = if (enabled) current or bitMask else current and bitMask.inv()
            bytes[byteIndex] = updated.toByte()
        }
        val newHex = bytes.joinToString("") { "%02X".format(it) }
        return copy(manualTvrHex = newHex)
    }

    // --- Formatting Helpers ---
    val formattedTtq: String
        get() = ttqHex.replace(" ", "").chunked(2).joinToString(" ")

    val formattedTerminalCapabilities: String
        get() = terminalCapabilitiesHex.replace(" ", "").chunked(2).joinToString(" ")

    val formattedManualTvr: String
        get() = getManualTvrBytes().joinToString(" ") { "%02X".format(it) }

    val formattedFloorLimit: String
        get() = "$currencySymbol${"%.2f".format(floorLimit / 100.0)}"

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
            TtqPreset("Standard Online POS", "76204000", "qVSDC + Contactless EMV + Contact Chip + Online PIN + Signature + Mobile CVM"),
            TtqPreset("Online POS w/ MSD", "F6204000", "MSD + qVSDC + Contactless EMV + Contact Chip + Online PIN + Signature + Mobile CVM"),
            TtqPreset("Strict Online (ARQC Required)", "76A04000", "Standard Online POS + Online Cryptogram Required + CVM Required"),
            TtqPreset("Mobile CVM Preferred", "76204000", "Contactless EMV + CDCVM (Apple Pay / Google Pay) supported"),
            TtqPreset("Offline Capable POS", "76004000", "Offline transaction permitted, no online cryptogram required"),
            TtqPreset("Signature Only POS", "72200000", "Contactless with signature CVM only (no Online PIN)")
        )

        // Preset Terminal Capabilities (Tag 9F33)
        val CapabilitiesPresets = listOf(
            CapabilitiesPreset("Standard All-in-One POS", "E0F8C8", "Key + Magstripe + IC + PINs + Signature + No CVM + SDA + DDA + CDA"),
            CapabilitiesPreset("Chip & Online PIN Only", "204008", "Contact IC + Online PIN + CDA"),
            CapabilitiesPreset("Chip & PIN / Signature", "206008", "Contact IC + Online PIN + Signature + CDA"),
            CapabilitiesPreset("Contactless Mobile & Chip", "60B8C8", "Magstripe + IC + Online PIN + Signature + No CVM + SDA + DDA + CDA")
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
