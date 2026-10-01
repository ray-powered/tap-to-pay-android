package com.yumedev.taptopayandroid.util

import com.yumedev.taptopayandroid.domain.model.*

/**
 * Comprehensive EMV transaction response decoder and analyzer.
 * Decodes card-returned data objects and interprets what the transaction outcome
 * means (e.g., verification required, device screen check / CDCVM, offline approval,
 * online authorization, or contact interface switch).
 */
object TransactionResponseDecoder {

    /**
     * Synthesizes tags, APDUs, and cryptograms to produce a comprehensive transaction analysis.
     */
    fun analyzeTransaction(
        tags: Map<String, EmvTag>,
        apduCommands: List<ApduCommand>,
        strictOnlineAuthDisplay: Boolean = false
    ): TransactionAnalysisResult {
        val cid = tags["9F27"]?.value?.let { decodeCid(it) }
        val ctq = tags["9F6C"]?.value?.let { decodeCtq(it) }
        val cvmResults = tags["9F34"]?.value?.let { decodeCvmResults(it) }
        val cvmList = tags["8E"]?.value?.let { decodeCvmList(it) }
        val auc = tags["9F07"]?.value?.let { decodeAuc(it) }
        val iad = tags["9F10"]?.value?.let { decodeIad(it) }
        val tvr = tags["95"]?.value?.let { decodeTvr(it) }
        val aip = tags["82"]?.value?.let { AipDecoder.decode(it) }

        // Check APDU status words
        val hasSeePhoneStatus = apduCommands.any { it.statusWord.replace(" ", "").equals("6986", ignoreCase = true) }
        val hasSwitchContactStatus = apduCommands.any { it.statusWord.replace(" ", "").equals("6984", ignoreCase = true) }
        val hasConditionsNotSatisfied = apduCommands.any { it.statusWord.replace(" ", "").equals("6985", ignoreCase = true) }

        val highlights = mutableListOf<AnalysisHighlight>()

        // 1. Determine CVM requirement and screen check
        val requiresScreenCheck: Boolean
        val screenCheckInstructions: String?
        val cvmRequirement: CvmRequirement
        val cvmTitle: String
        val cvmDescription: String

        when {
            hasSeePhoneStatus -> {
                requiresScreenCheck = true
                screenCheckInstructions = "The mobile device requested on-device verification. Please unlock your phone screen or complete biometric verification (Face ID / Fingerprint / Passcode), then tap the reader again."
                cvmRequirement = CvmRequirement.CONSUMER_DEVICE_CVM_REQUIRED
                cvmTitle = "See Phone (On-Device Verification Required)"
                cvmDescription = "Mobile device requires authentication on its screen before completing payment."
                highlights.add(
                    AnalysisHighlight(
                        title = "See Phone / CDCVM Required",
                        description = "Card returned status 69 86: User must authenticate on mobile device screen.",
                        type = HighlightType.WARNING
                    )
                )
            }
            ctq?.cdcvmPerformed == true -> {
                requiresScreenCheck = false
                screenCheckInstructions = null
                cvmRequirement = CvmRequirement.CONSUMER_DEVICE_CVM_PERFORMED
                cvmTitle = "Consumer Device CVM Completed"
                cvmDescription = "Authentication was successfully performed on the customer's mobile device (e.g., Apple Pay / Google Pay Face ID or fingerprint)."
                highlights.add(
                    AnalysisHighlight(
                        title = "On-Device Biometric Verified",
                        description = "Consumer Device CVM was performed on the smartphone/wearable.",
                        type = HighlightType.SUCCESS
                    )
                )
            }
            ctq?.onlinePinRequired == true -> {
                requiresScreenCheck = false
                screenCheckInstructions = null
                cvmRequirement = CvmRequirement.ONLINE_PIN_REQUIRED
                cvmTitle = "Online PIN Required"
                cvmDescription = "Card requires cardholder to enter PIN on the terminal keypad."
                highlights.add(
                    AnalysisHighlight(
                        title = "Online PIN Required",
                        description = "Card Transaction Qualifiers (CTQ) specify Online PIN entry.",
                        type = HighlightType.WARNING
                    )
                )
            }
            ctq?.signatureRequired == true -> {
                requiresScreenCheck = false
                screenCheckInstructions = null
                cvmRequirement = CvmRequirement.SIGNATURE_REQUIRED
                cvmTitle = "Signature Required"
                cvmDescription = "Cardholder must sign paper receipt or terminal screen."
                highlights.add(
                    AnalysisHighlight(
                        title = "Signature Required",
                        description = "Card Transaction Qualifiers (CTQ) specify cardholder signature.",
                        type = HighlightType.INFO
                    )
                )
            }
            cvmResults != null -> {
                requiresScreenCheck = false
                screenCheckInstructions = null
                when {
                    cvmResults.methodPerformed.contains("No CVM", ignoreCase = true) -> {
                        cvmRequirement = CvmRequirement.NO_CVM_REQUIRED
                        cvmTitle = "No Verification Required"
                        cvmDescription = "Small amount contactless payment processed without cardholder verification."
                    }
                    cvmResults.methodPerformed.contains("Online PIN", ignoreCase = true) -> {
                        cvmRequirement = CvmRequirement.ONLINE_PIN_REQUIRED
                        cvmTitle = "Online PIN"
                        cvmDescription = "Online PIN verification performed."
                    }
                    cvmResults.methodPerformed.contains("Signature", ignoreCase = true) -> {
                        cvmRequirement = CvmRequirement.SIGNATURE_REQUIRED
                        cvmTitle = "Signature"
                        cvmDescription = "Signature verification performed."
                    }
                    else -> {
                        cvmRequirement = CvmRequirement.CVM_LIST_FALLBACK
                        cvmTitle = cvmResults.methodPerformed
                        cvmDescription = "CVM result: ${cvmResults.result.label}"
                    }
                }
            }
            aip?.onDeviceCardholderVerificationSupported == true && hasConditionsNotSatisfied -> {
                requiresScreenCheck = true
                screenCheckInstructions = "Device conditions not met. If using a mobile wallet, please unlock your phone screen and tap again."
                cvmRequirement = CvmRequirement.CONSUMER_DEVICE_CVM_REQUIRED
                cvmTitle = "Unlock Phone Screen"
                cvmDescription = "Mobile device returned status 69 85. Device may be locked."
            }
            else -> {
                requiresScreenCheck = false
                screenCheckInstructions = null
                cvmRequirement = CvmRequirement.NO_CVM_REQUIRED
                cvmTitle = "No CVM Required"
                cvmDescription = "Transaction processed under no-verification contactless limit."
            }
        }

        // 2. Determine Transaction Decision
        val decision: TransactionDecision
        val decisionTitle: String
        val decisionDescription: String
        val isApproved: Boolean
        val isDeclined: Boolean
        val isOnlineRequired: Boolean
        val isSwitchInterfaceRequired: Boolean

        when {
            hasSeePhoneStatus -> {
                decision = TransactionDecision.SEE_PHONE_CDCVM
                decisionTitle = "Action Required: Check Phone Screen"
                decisionDescription = "The mobile device requested biometric or passcode verification on its screen."
                isApproved = false
                isDeclined = false
                isOnlineRequired = false
                isSwitchInterfaceRequired = false
            }
            hasSwitchContactStatus || ctq?.switchToContactIfOdaFailed == true -> {
                decision = TransactionDecision.SWITCH_INTERFACE_CONTACT
                decisionTitle = "Please Insert Chip Card"
                decisionDescription = "Contactless transaction is not supported for this card or operation. Please insert the chip card into the terminal reader."
                isApproved = false
                isDeclined = false
                isOnlineRequired = false
                isSwitchInterfaceRequired = true
                highlights.add(
                    AnalysisHighlight(
                        title = "Switch to Chip Interface",
                        description = "Card instructs terminal to switch to contact chip slot.",
                        type = HighlightType.WARNING
                    )
                )
            }
            cid?.cryptogramType == CryptogramType.TC -> {
                decision = TransactionDecision.APPROVED_OFFLINE
                decisionTitle = "Approved Offline (TC)"
                decisionDescription = "Card issued a Transaction Certificate (TC). Transaction is authorized offline by the card chip without needing a host connection."
                isApproved = true
                isDeclined = false
                isOnlineRequired = false
                isSwitchInterfaceRequired = false
                highlights.add(
                    AnalysisHighlight(
                        title = "Offline Approved (TC)",
                        description = "Cryptogram TC (${cid.rawValue}) returned by card. Offline transaction authorized.",
                        type = HighlightType.SUCCESS
                    )
                )
            }
            cid?.cryptogramType == CryptogramType.ARQC || (cid == null && tags.containsKey("9F26")) -> {
                val cryptoVal = cid?.rawValue ?: tags["9F26"]?.value ?: ""
                decision = TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED
                if (strictOnlineAuthDisplay) {
                    decisionTitle = "Online Authorization Required (ARQC)"
                    decisionDescription = "Card generated an Authorisation Request Cryptogram (ARQC). Transaction must be sent online to the issuer host for authorization."
                    isApproved = false
                    isDeclined = false
                    isOnlineRequired = true
                    isSwitchInterfaceRequired = false
                    highlights.add(
                        AnalysisHighlight(
                            title = "Online Authorization Required",
                            description = "Cryptogram ARQC ($cryptoVal) generated. Awaiting issuer host authorization.",
                            type = HighlightType.INFO
                        )
                    )
                } else {
                    decisionTitle = "Approved Online (ARQC)"
                    decisionDescription = "Card generated an Authorisation Request Cryptogram (ARQC). Transaction was routed online and approved by the issuer host."
                    isApproved = true
                    isDeclined = false
                    isOnlineRequired = false
                    isSwitchInterfaceRequired = false
                    highlights.add(
                        AnalysisHighlight(
                            title = "Online Approved (ARQC)",
                            description = "Cryptogram ARQC ($cryptoVal) generated and authorized online by host.",
                            type = HighlightType.SUCCESS
                        )
                    )
                }
            }
            cid?.cryptogramType == CryptogramType.AAC -> {
                decision = TransactionDecision.DECLINED_BY_CARD
                decisionTitle = "Declined by Card (AAC)"
                decisionDescription = "Card generated an Application Authentication Cryptogram (AAC) and declined the transaction: ${cid.reason}."
                isApproved = false
                isDeclined = true
                isOnlineRequired = false
                isSwitchInterfaceRequired = false
                highlights.add(
                    AnalysisHighlight(
                        title = "Declined by Card (AAC)",
                        description = cid.reason,
                        type = HighlightType.ERROR
                    )
                )
            }
            tags.containsKey("5A") || tags.containsKey("57") -> {
                decision = TransactionDecision.DATA_READ_ONLY
                decisionTitle = "Card Read Successful"
                decisionDescription = "Card data and EMV tags successfully retrieved from card."
                isApproved = true
                isDeclined = false
                isOnlineRequired = false
                isSwitchInterfaceRequired = false
            }
            else -> {
                decision = TransactionDecision.TERMINATED_WITH_ERROR
                decisionTitle = "Incomplete Transaction"
                decisionDescription = "Card communication ended before transaction decision was reached."
                isApproved = false
                isDeclined = false
                isOnlineRequired = false
                isSwitchInterfaceRequired = false
            }
        }

        // 3. Determine Action Required
        val actionRequired: ActionRequired
        val actionTitle: String
        val actionDescription: String

        when {
            requiresScreenCheck -> {
                actionRequired = ActionRequired.SEE_PHONE_AND_RETAP
                actionTitle = "Check Phone & Tap Again"
                actionDescription = "Prompt customer to unlock their phone screen or authenticate with Face ID / Fingerprint, then tap again."
            }
            isSwitchInterfaceRequired -> {
                actionRequired = ActionRequired.INSERT_CHIP_CARD
                actionTitle = "Insert Chip Card"
                actionDescription = "Instruct customer to insert the card into the chip reader slot."
            }
            cvmRequirement == CvmRequirement.ONLINE_PIN_REQUIRED -> {
                actionRequired = ActionRequired.ENTER_ONLINE_PIN
                actionTitle = "Customer PIN Required"
                actionDescription = "Customer must enter their PIN on the terminal PIN pad."
            }
            cvmRequirement == CvmRequirement.SIGNATURE_REQUIRED -> {
                actionRequired = ActionRequired.OBTAIN_SIGNATURE
                actionTitle = "Obtain Signature"
                actionDescription = "Print merchant copy receipt and obtain customer's signature."
            }
            isOnlineRequired -> {
                actionRequired = ActionRequired.SEND_ONLINE_AUTHORIZATION
                actionTitle = "Send Online Authorization"
                actionDescription = "Terminal forwards ARQC cryptogram and transaction data to payment gateway."
            }
            isApproved -> {
                actionRequired = ActionRequired.NONE_TRANSACTION_APPROVED
                actionTitle = "Payment Complete"
                actionDescription = "No further customer action required. Receipt can be issued."
            }
            isDeclined -> {
                actionRequired = ActionRequired.TRANSACTION_DECLINED
                actionTitle = "Request Another Payment Method"
                actionDescription = "Card declined transaction. Ask customer for an alternative payment card."
            }
            else -> {
                actionRequired = ActionRequired.NONE_CARD_READ
                actionTitle = "Card Read Complete"
                actionDescription = "EMV tag inspection completed."
            }
        }

        // 4. Add security & capabilities highlights
        if (aip != null) {
            val oda = when {
                aip.cdaSupported -> "CDA (Combined DDA/AC Generation)"
                aip.ddaSupported -> "DDA (Dynamic Data Authentication)"
                aip.sdaSupported -> "SDA (Static Data Authentication)"
                else -> "None"
            }
            highlights.add(
                AnalysisHighlight(
                    title = "Offline Authentication",
                    description = "Supported method: $oda",
                    type = HighlightType.INFO
                )
            )
        }

        if (tvr != null) {
            val setFlags = tvr.flags.filter { it.isSet }
            if (setFlags.isNotEmpty()) {
                highlights.add(
                    AnalysisHighlight(
                        title = "Terminal Risk Flags (${setFlags.size})",
                        description = setFlags.joinToString("; ") { it.label },
                        type = HighlightType.WARNING
                    )
                )
            }
        }

        return TransactionAnalysisResult(
            decision = decision,
            decisionTitle = decisionTitle,
            decisionDescription = decisionDescription,
            cvmRequirement = cvmRequirement,
            cvmTitle = cvmTitle,
            cvmDescription = cvmDescription,
            actionRequired = actionRequired,
            actionTitle = actionTitle,
            actionDescription = actionDescription,
            requiresScreenCheck = requiresScreenCheck,
            screenCheckInstructions = screenCheckInstructions,
            isApproved = isApproved,
            isDeclined = isDeclined,
            isOnlineRequired = isOnlineRequired,
            isSwitchInterfaceRequired = isSwitchInterfaceRequired,
            highlights = highlights,
            cid = cid,
            ctq = ctq,
            cvmResults = cvmResults,
            cvmList = cvmList,
            auc = auc,
            iad = iad,
            tvr = tvr,
            aip = aip
        )
    }

    // ---- CID (Cryptogram Information Data) - Tag 9F27, 1 byte ----

    fun decodeCid(hexValue: String): CidDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.isEmpty()) return null

        val b = bytes[0].toInt() and 0xFF
        val typeBits = b and 0xC0
        val adviceBit = (b and 0x08) != 0
        val reasonBits = b and 0x07

        val cryptogramType = when (typeBits) {
            0x00 -> CryptogramType.AAC
            0x40 -> CryptogramType.TC
            0x80 -> CryptogramType.ARQC
            else -> CryptogramType.RFU
        }

        val reason = when {
            cryptogramType == CryptogramType.AAC -> when (reasonBits) {
                0 -> "Card declined — no specific reason"
                1 -> "Card declined — PIN try limit exceeded"
                2 -> "Card declined — application expired"
                else -> "Card declined — reason code $reasonBits"
            }
            cryptogramType == CryptogramType.ARQC -> "Card requests online authorization"
            cryptogramType == CryptogramType.TC -> "Card approves transaction offline"
            else -> "Unknown cryptogram type"
        }

        return CidDecoded(
            rawValue = hexValue,
            cryptogramType = cryptogramType,
            adviceRequired = adviceBit,
            reason = reason
        )
    }

    // ---- CTQ (Card Transaction Qualifiers) - Tag 9F6C, 2 bytes (EMV Contactless / VCPS) ----

    fun decodeCtq(hexValue: String): CtqDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 2) return null

        val b1 = bytes[0].toInt() and 0xFF
        val b2 = bytes[1].toInt() and 0xFF

        return CtqDecoded(
            rawValue = hexValue,
            onlinePinRequired = (b1 and 0x80) != 0,
            signatureRequired = (b1 and 0x40) != 0,
            onlineIfOdaFailed = (b1 and 0x20) != 0,
            switchToContactIfOdaFailed = (b1 and 0x10) != 0,
            goOnlineIfApplicationExpired = (b1 and 0x08) != 0,
            switchInterfaceForCash = (b1 and 0x04) != 0,
            switchInterfaceForCashback = (b1 and 0x02) != 0,
            cdcvmPerformed = (b2 and 0x80) != 0,
            issuerUpdateProcessingSupported = (b2 and 0x40) != 0
        )
    }

    // ---- CVM Results (Tag 9F34) - 3 bytes ----

    fun decodeCvmResults(hexValue: String): CvmResultsDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 3) return null

        val methodByte = bytes[0].toInt() and 0xFF
        val condByte = bytes[1].toInt() and 0xFF
        val resultByte = bytes[2].toInt() and 0xFF

        val method = decodeCvmMethod(methodByte and 0x3F)
        val condition = decodeCvmCondition(condByte)
        val result = when (resultByte) {
            0x00 -> CvmResultCode.UNKNOWN
            0x01 -> CvmResultCode.FAILED
            0x02 -> CvmResultCode.SUCCESSFUL
            else -> CvmResultCode.UNKNOWN
        }

        return CvmResultsDecoded(
            rawValue = hexValue,
            methodPerformed = method,
            conditionCode = condition,
            result = result
        )
    }

    // ---- CVM List (Tag 8E) - variable length ----

    fun decodeCvmList(hexValue: String): CvmListDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 10) return null

        val amountX = bytesToLong(bytes, 0, 4)
        val amountY = bytesToLong(bytes, 4, 4)

        val rules = mutableListOf<CvmRule>()
        var i = 8
        while (i + 1 < bytes.size) {
            val methodByte = bytes[i].toInt() and 0xFF
            val condByte = bytes[i + 1].toInt() and 0xFF
            val failIfUnsuccessful = (methodByte and 0x40) == 0
            val methodCode = methodByte and 0x3F

            rules.add(
                CvmRule(
                    methodCode = methodCode,
                    conditionCode = condByte,
                    methodName = decodeCvmMethod(methodCode),
                    conditionName = decodeCvmCondition(condByte),
                    failIfUnsuccessful = failIfUnsuccessful
                )
            )
            i += 2
        }

        return CvmListDecoded(
            rawValue = hexValue,
            amountX = amountX,
            amountY = amountY,
            rules = rules
        )
    }

    // ---- AUC (Application Usage Control) - Tag 9F07, 2 bytes ----

    fun decodeAuc(hexValue: String): AucDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 2) return null

        val b1 = bytes[0].toInt() and 0xFF
        val b2 = bytes[1].toInt() and 0xFF

        return AucDecoded(
            rawValue = hexValue,
            domesticCashAllowed = (b1 and 0x80) != 0,
            internationalCashAllowed = (b1 and 0x40) != 0,
            domesticGoodsAllowed = (b1 and 0x20) != 0,
            internationalGoodsAllowed = (b1 and 0x10) != 0,
            domesticServicesAllowed = (b1 and 0x08) != 0,
            internationalServicesAllowed = (b1 and 0x04) != 0,
            atmAllowed = (b1 and 0x02) != 0,
            nonAtmTerminalAllowed = (b1 and 0x01) != 0,
            domesticCashbackAllowed = (b2 and 0x80) != 0,
            internationalCashbackAllowed = (b2 and 0x40) != 0
        )
    }

    // ---- IAD (Issuer Application Data) - Tag 9F10, variable ----

    fun decodeIad(hexValue: String): IadDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 3) return null

        val firstByte = bytes[0].toInt() and 0xFF
        val (dki, cvn, cvrStart) = if (firstByte <= bytes.size - 1 && firstByte > 0 && firstByte < bytes.size) {
            Triple(null, bytes[1].toInt() and 0xFF, 2)
        } else {
            Triple(firstByte, bytes[1].toInt() and 0xFF, 2)
        }

        val cvnDesc = when (cvn) {
            0x0A, 10 -> "CVN 10 — Visa (legacy)"
            0x11, 17 -> "CVN 17 — Visa (IAD format 1/3)"
            0x12, 18 -> "CVN 18 — Visa (IAD format 0/1/3)"
            0x01, 1 -> "CVN 01 — MC CSK"
            0x02, 2 -> "CVN 02 — MC CPA, session key"
            0x04, 4 -> "CVN 04 — MC M/Chip, session key"
            0x14, 20 -> "CVN 20 — MC CPA proprietary"
            else -> "CVN %02X".format(cvn)
        }

        val cvrBytes = if (cvrStart < bytes.size) {
            bytes.sliceArray(cvrStart until bytes.size)
                .joinToString("") { "%02X".format(it) }
        } else null

        val cvrBits = decodeCvrBits(bytes, cvrStart, cvn)

        return IadDecoded(
            rawValue = hexValue,
            derivationKeyIndex = dki,
            cryptogramVersionNumber = cvn,
            cvnDescription = cvnDesc,
            cvrBytes = cvrBytes,
            cvrBits = cvrBits
        )
    }

    // ---- TVR (Terminal Verification Results) - Tag 95, 5 bytes ----

    fun decodeTvr(hexValue: String): TvrDecoded? {
        val bytes = parseHex(hexValue) ?: return null
        if (bytes.size < 5) return null

        val flags = mutableListOf<TvrFlag>()

        val definitions = arrayOf(
            arrayOf(
                "Offline data authentication not performed",
                "SDA failed",
                "ICC data missing",
                "Card on terminal exception file",
                "DDA failed",
                "CDA failed",
                "SDA selected",
                "RFU"
            ),
            arrayOf(
                "ICC and terminal have different app versions",
                "Expired application",
                "Application not yet effective",
                "Requested service not allowed",
                "New card",
                "RFU", "RFU", "RFU"
            ),
            arrayOf(
                "Cardholder verification was not successful",
                "Unrecognised CVM",
                "PIN Try Limit exceeded",
                "PIN entry required, keypad present, but PIN not entered",
                "PIN entry required, but no keypad",
                "Online PIN entered",
                "RFU", "RFU"
            ),
            arrayOf(
                "Transaction exceeds floor limit",
                "Lower consecutive offline limit exceeded",
                "Upper consecutive offline limit exceeded",
                "Transaction selected randomly for online",
                "Merchant forced transaction online",
                "RFU", "RFU", "RFU"
            ),
            arrayOf(
                "Default TDOL used",
                "Issuer authentication failed",
                "Script processing failed before final GENERATE AC",
                "Script processing failed after final GENERATE AC",
                "RFU", "RFU", "RFU", "RFU"
            )
        )

        for (byteIdx in 0 until 5) {
            val b = bytes[byteIdx].toInt() and 0xFF
            for (bitIdx in 0 until 8) {
                val label = definitions[byteIdx][bitIdx]
                if (label != "RFU") {
                    val isSet = (b and (0x80 shr bitIdx)) != 0
                    flags.add(
                        TvrFlag(
                            byteIndex = byteIdx + 1,
                            bitIndex = bitIdx + 1,
                            label = label,
                            isSet = isSet
                        )
                    )
                }
            }
        }

        return TvrDecoded(rawValue = hexValue, flags = flags)
    }

    // ---- Helpers ----

    private fun decodeCvmMethod(code: Int): String = when (code) {
        0x00 -> "Fail CVM processing"
        0x01 -> "Plaintext PIN verification by ICC"
        0x02 -> "Enciphered PIN verified online"
        0x03 -> "Plaintext PIN verification by ICC + signature"
        0x04 -> "Enciphered PIN verification by ICC"
        0x05 -> "Enciphered PIN verification by ICC + signature"
        0x1E -> "Signature (paper)"
        0x1F -> "No CVM required"
        0x20 -> "No CVM required (mobile)"
        else -> if (code in 0x06..0x1D) "RFU (${"0x%02X".format(code)})"
        else "Unknown (${"0x%02X".format(code)})"
    }

    private fun decodeCvmCondition(code: Int): String = when (code) {
        0x00 -> "Always"
        0x01 -> "If unattended cash"
        0x02 -> "If not (unattended cash or manual cash or cashback)"
        0x03 -> "If terminal supports the CVM"
        0x04 -> "If manual cash"
        0x05 -> "If purchase with cashback"
        0x06 -> "If transaction in application currency and under X"
        0x07 -> "If transaction in application currency and over X"
        0x08 -> "If transaction in application currency and under Y"
        0x09 -> "If transaction in application currency and over Y"
        else -> "RFU (${"0x%02X".format(code)})"
    }

    private fun decodeCvrBits(bytes: ByteArray, cvrStart: Int, cvn: Int): List<String> {
        if (cvrStart >= bytes.size) return emptyList()

        val cvr = bytes.sliceArray(cvrStart until bytes.size)
        val bits = mutableListOf<String>()

        if (cvr.isNotEmpty()) {
            val b1 = cvr[0].toInt() and 0xFF
            val acType = (b1 shr 6) and 0x03
            when (acType) {
                0 -> bits.add("2nd GENERATE AC returned AAC")
                1 -> bits.add("2nd GENERATE AC returned TC")
                2 -> bits.add("2nd GENERATE AC returned ARQC (error)")
                3 -> bits.add("2nd GENERATE AC not requested")
            }
            val ac1Type = (b1 shr 4) and 0x03
            when (ac1Type) {
                0 -> bits.add("1st GENERATE AC returned AAC (decline)")
                1 -> bits.add("1st GENERATE AC returned TC (approve)")
                2 -> bits.add("1st GENERATE AC returned ARQC (go online)")
                3 -> bits.add("1st GENERATE AC — RFU")
            }
            if ((b1 and 0x08) != 0) bits.add("PIN try counter is zero")
            if ((b1 and 0x04) != 0) bits.add("Last online transaction not completed")
            if ((b1 and 0x02) != 0) bits.add("Lower offline transaction count exceeded")
            if ((b1 and 0x01) != 0) bits.add("Upper offline transaction count exceeded")
        }

        if (cvr.size > 1) {
            val b2 = cvr[1].toInt() and 0xFF
            if ((b2 and 0x80) != 0) bits.add("New card (first transaction)")
            if ((b2 and 0x40) != 0) bits.add("Unable to go online")
            if ((b2 and 0x10) != 0) bits.add("Issuer authentication failed")
            if ((b2 and 0x08) != 0) bits.add("Script processing failed")
        }

        return bits
    }

    private fun bytesToLong(bytes: ByteArray, offset: Int, length: Int): Long {
        var value = 0L
        for (i in 0 until length) {
            if (offset + i >= bytes.size) break
            value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
        }
        return value
    }

    internal fun parseHex(hexValue: String): ByteArray? {
        val clean = hexValue.replace(" ", "").replace(":", "").trim()
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        return try {
            ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
