package com.yumedev.taptopayandroid.domain.model

/**
 * High-level transaction outcome analyzed from card response tags,
 * APDU status words, and cryptogram decisions.
 */
data class TransactionAnalysisResult(
    val decision: TransactionDecision,
    val decisionTitle: String,
    val decisionDescription: String,
    val cvmRequirement: CvmRequirement,
    val cvmTitle: String,
    val cvmDescription: String,
    val actionRequired: ActionRequired,
    val actionTitle: String,
    val actionDescription: String,
    val requiresScreenCheck: Boolean = false,
    val screenCheckInstructions: String? = null,
    val isApproved: Boolean = false,
    val isDeclined: Boolean = false,
    val isOnlineRequired: Boolean = false,
    val isSwitchInterfaceRequired: Boolean = false,
    val highlights: List<AnalysisHighlight> = emptyList(),
    val cid: CidDecoded? = null,
    val ctq: CtqDecoded? = null,
    val cvmResults: CvmResultsDecoded? = null,
    val cvmList: CvmListDecoded? = null,
    val auc: AucDecoded? = null,
    val iad: IadDecoded? = null,
    val tvr: TvrDecoded? = null,
    val aip: AipDecoded? = null
)

enum class TransactionDecision {
    APPROVED_OFFLINE,
    ONLINE_AUTHORIZATION_REQUIRED,
    DECLINED_BY_CARD,
    SEE_PHONE_CDCVM,
    SWITCH_INTERFACE_CONTACT,
    DATA_READ_ONLY,
    TERMINATED_WITH_ERROR
}

enum class CvmRequirement {
    CONSUMER_DEVICE_CVM_REQUIRED,
    CONSUMER_DEVICE_CVM_PERFORMED,
    ONLINE_PIN_REQUIRED,
    SIGNATURE_REQUIRED,
    OFFLINE_PIN_REQUIRED,
    NO_CVM_REQUIRED,
    CVM_LIST_FALLBACK,
    UNKNOWN
}

enum class ActionRequired {
    SEE_PHONE_AND_RETAP,
    ENTER_ONLINE_PIN,
    OBTAIN_SIGNATURE,
    INSERT_CHIP_CARD,
    SEND_ONLINE_AUTHORIZATION,
    NONE_TRANSACTION_APPROVED,
    TRANSACTION_DECLINED,
    NONE_CARD_READ
}

data class AnalysisHighlight(
    val title: String,
    val description: String,
    val type: HighlightType
)

enum class HighlightType {
    SUCCESS,
    WARNING,
    INFO,
    ERROR
}

/**
 * Cryptogram Information Data (Tag 9F27) — 1 byte
 * Tells the terminal what cryptogram type the card returned and why.
 */
data class CidDecoded(
    val rawValue: String,
    val cryptogramType: CryptogramType,
    val adviceRequired: Boolean,
    val reason: String
)

enum class CryptogramType(val code: Int, val label: String, val description: String) {
    AAC(0x00, "AAC", "Application Authentication Cryptogram — Transaction Declined"),
    TC(0x40, "TC", "Transaction Certificate — Approved Offline"),
    ARQC(0x80, "ARQC", "Authorisation Request Cryptogram — Go Online"),
    RFU(0xFF, "RFU", "Reserved for Future Use")
}

/**
 * Card Transaction Qualifiers (Tag 9F6C) — 2 bytes (EMV Contactless / VCPS)
 * Tells the terminal what the contactless card or mobile wallet requires.
 */
data class CtqDecoded(
    val rawValue: String,
    // Byte 1
    val onlinePinRequired: Boolean,
    val signatureRequired: Boolean,
    val onlineIfOdaFailed: Boolean,
    val switchToContactIfOdaFailed: Boolean,
    val goOnlineIfApplicationExpired: Boolean,
    val switchInterfaceForCash: Boolean,
    val switchInterfaceForCashback: Boolean,
    // Byte 2
    val cdcvmPerformed: Boolean, // Consumer Device CVM Performed (e.g. Apple Pay / Google Pay / Biometric screen verified)
    val issuerUpdateProcessingSupported: Boolean
)

/**
 * CVM Results (Tag 9F34) — 3 bytes
 * Records which CVM was performed and the outcome.
 */
data class CvmResultsDecoded(
    val rawValue: String,
    val methodPerformed: String,
    val conditionCode: String,
    val result: CvmResultCode
)

enum class CvmResultCode(val code: Int, val label: String) {
    UNKNOWN(0x00, "Unknown"),
    FAILED(0x01, "Failed"),
    SUCCESSFUL(0x02, "Successful")
}

/**
 * CVM List (Tag 8E) — variable length
 * Ordered list of cardholder verification methods the card supports.
 */
data class CvmListDecoded(
    val rawValue: String,
    val amountX: Long, // amount field X (4 bytes)
    val amountY: Long, // amount field Y (4 bytes)
    val rules: List<CvmRule>
)

data class CvmRule(
    val methodCode: Int,
    val conditionCode: Int,
    val methodName: String,
    val conditionName: String,
    val failIfUnsuccessful: Boolean
)

/**
 * Application Usage Control (Tag 9F07) — 2 bytes
 * Issuer restrictions on card usage.
 */
data class AucDecoded(
    val rawValue: String,
    // Byte 1
    val domesticCashAllowed: Boolean,
    val internationalCashAllowed: Boolean,
    val domesticGoodsAllowed: Boolean,
    val internationalGoodsAllowed: Boolean,
    val domesticServicesAllowed: Boolean,
    val internationalServicesAllowed: Boolean,
    val atmAllowed: Boolean,
    val nonAtmTerminalAllowed: Boolean,
    // Byte 2
    val domesticCashbackAllowed: Boolean,
    val internationalCashbackAllowed: Boolean
)

/**
 * Issuer Application Data (Tag 9F10) — variable length
 * Contains CVN (Cryptogram Version Number) and CVR (Card Verification Results).
 */
data class IadDecoded(
    val rawValue: String,
    val derivationKeyIndex: Int?,
    val cryptogramVersionNumber: Int?,
    val cvnDescription: String,
    val cvrBytes: String?,
    val cvrBits: List<String>
)

/**
 * Terminal Verification Results (Tag 95) — 5 bytes
 * Records the outcome of terminal risk checks.
 */
data class TvrDecoded(
    val rawValue: String,
    val flags: List<TvrFlag>
)

data class TvrFlag(
    val byteIndex: Int,
    val bitIndex: Int,
    val label: String,
    val isSet: Boolean
)
