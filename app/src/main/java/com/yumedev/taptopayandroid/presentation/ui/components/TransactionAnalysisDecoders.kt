package com.yumedev.taptopayandroid.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yumedev.taptopayandroid.domain.model.*

// ─── Transaction Overview & Analysis Card ───

@Composable
fun TransactionOverviewCard(
    analysis: TransactionAnalysisResult,
    modifier: Modifier = Modifier
) {
    val borderColor = when (analysis.decision) {
        TransactionDecision.APPROVED_OFFLINE,
        TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        TransactionDecision.DECLINED_BY_CARD -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        TransactionDecision.SEE_PHONE_CDCVM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        TransactionDecision.SWITCH_INTERFACE_CONTACT -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Decision Badge & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val icon = when (analysis.decision) {
                    TransactionDecision.APPROVED_OFFLINE,
                    TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> Icons.Default.CheckCircle
                    TransactionDecision.DECLINED_BY_CARD -> Icons.Default.Close
                    TransactionDecision.SEE_PHONE_CDCVM -> Icons.Default.Warning
                    TransactionDecision.SWITCH_INTERFACE_CONTACT -> Icons.Default.CreditCard
                    else -> Icons.Default.Info
                }

                val iconTint = when (analysis.decision) {
                    TransactionDecision.APPROVED_OFFLINE,
                    TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> MaterialTheme.colorScheme.primary
                    TransactionDecision.DECLINED_BY_CARD -> MaterialTheme.colorScheme.error
                    TransactionDecision.SEE_PHONE_CDCVM -> MaterialTheme.colorScheme.tertiary
                    TransactionDecision.SWITCH_INTERFACE_CONTACT -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = iconTint
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transaction Decision",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = analysis.decisionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = analysis.decisionDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // See Phone Screen Alert Banner (if screen check required)
            if (analysis.requiresScreenCheck && analysis.screenCheckInstructions != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Action Required: Check Phone Screen",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = analysis.screenCheckInstructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Verification & Action grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Verification Method
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Verification Method",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysis.cvmTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = analysis.cvmDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Action Required
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Next Action",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = analysis.actionTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = analysis.actionDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Highlights list
            if (analysis.highlights.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Text(
                    text = "Key Analysis Highlights",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                analysis.highlights.forEach { highlight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val hlTint = when (highlight.type) {
                            HighlightType.SUCCESS -> MaterialTheme.colorScheme.primary
                            HighlightType.WARNING -> MaterialTheme.colorScheme.error
                            HighlightType.INFO -> MaterialTheme.colorScheme.tertiary
                            HighlightType.ERROR -> MaterialTheme.colorScheme.error
                        }
                        Icon(
                            imageVector = when (highlight.type) {
                                HighlightType.SUCCESS -> Icons.Default.Check
                                HighlightType.WARNING -> Icons.Default.Warning
                                HighlightType.INFO -> Icons.Default.Info
                                HighlightType.ERROR -> Icons.Default.Close
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp),
                            tint = hlTint
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = highlight.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = highlight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── CID (Cryptogram Information Data, Tag 9F27) ───

@Composable
fun CidBitDecoder(cid: CidDecoded) {
    DecoderContainer(title = "Cryptogram Decision") {
        val (badgeColor, badgeTextColor) = when (cid.cryptogramType) {
            CryptogramType.TC -> Pair(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer
            )
            CryptogramType.ARQC -> Pair(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer
            )
            CryptogramType.AAC -> Pair(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
            CryptogramType.RFU -> Pair(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${cid.cryptogramType.label} — ${cid.cryptogramType.description}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cid.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = badgeTextColor.copy(alpha = 0.8f)
                )
            }
        }

        if (cid.adviceRequired) {
            Spacer(modifier = Modifier.height(8.dp))
            FlagItem(
                label = "Advice required",
                description = "Card requests issuer be advised of this transaction",
                isSet = true,
                isWarning = true
            )
        }
    }
}

// ─── CTQ (Card Transaction Qualifiers, Tag 9F6C) ───

@Composable
fun CtqBitDecoder(ctq: CtqDecoded) {
    DecoderContainer(title = "Card Transaction Qualifiers") {
        FlagItem(
            label = "Consumer Device CVM Performed",
            description = "Authentication completed on mobile device screen (Face ID / Fingerprint / Passcode)",
            isSet = ctq.cdcvmPerformed
        )
        FlagItem(
            label = "Online PIN Required",
            description = "Card requires online PIN entry on terminal",
            isSet = ctq.onlinePinRequired,
            isWarning = ctq.onlinePinRequired
        )
        FlagItem(
            label = "Signature Required",
            description = "Cardholder signature is required",
            isSet = ctq.signatureRequired,
            isWarning = ctq.signatureRequired
        )
        FlagItem(
            label = "Go Online if ODA Failed",
            description = "Force online authorization if offline data authentication failed",
            isSet = ctq.onlineIfOdaFailed
        )
        FlagItem(
            label = "Switch to Contact Interface",
            description = "Card requests switching to chip contact interface",
            isSet = ctq.switchToContactIfOdaFailed,
            isWarning = ctq.switchToContactIfOdaFailed
        )
        FlagItem(
            label = "Go Online if App Expired",
            description = "Force online if application is expired",
            isSet = ctq.goOnlineIfApplicationExpired
        )
        FlagItem(
            label = "Switch Interface for Cash",
            description = "Switch to contact for cash transactions",
            isSet = ctq.switchInterfaceForCash
        )
        FlagItem(
            label = "Switch Interface for Cashback",
            description = "Switch to contact for cashback transactions",
            isSet = ctq.switchInterfaceForCashback
        )
        FlagItem(
            label = "Issuer Update Processing",
            description = "Issuer script update processing supported at POS",
            isSet = ctq.issuerUpdateProcessingSupported
        )
    }
}

// ─── CVM Results (Tag 9F34) ───

@Composable
fun CvmResultsBitDecoder(cvmResults: CvmResultsDecoded) {
    DecoderContainer(title = "CVM Verification Outcome") {
        val resultColor = when (cvmResults.result) {
            CvmResultCode.SUCCESSFUL -> MaterialTheme.colorScheme.primary
            CvmResultCode.FAILED -> MaterialTheme.colorScheme.error
            CvmResultCode.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = resultColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (cvmResults.result) {
                            CvmResultCode.SUCCESSFUL -> Icons.Default.Check
                            CvmResultCode.FAILED -> Icons.Default.Close
                            CvmResultCode.UNKNOWN -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = resultColor
                    )
                    Text(
                        text = "Result: ${cvmResults.result.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LabelValueRow("Method", cvmResults.methodPerformed)
                LabelValueRow("Condition", cvmResults.conditionCode)
            }
        }
    }
}

// ─── CVM List (Tag 8E) ───

@Composable
fun CvmListDecoder(cvmList: CvmListDecoded) {
    DecoderContainer(title = "CVM Rules (Priority Order)") {
        if (cvmList.amountX > 0) {
            LabelValueRow("Amount X threshold", formatMinorUnits(cvmList.amountX))
        }
        if (cvmList.amountY > 0) {
            LabelValueRow("Amount Y threshold", formatMinorUnits(cvmList.amountY))
        }

        if (cvmList.rules.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        cvmList.rules.forEachIndexed { index, rule ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = rule.methodName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "If: ${rule.conditionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp)
                    )
                    if (rule.failIfUnsuccessful) {
                        Text(
                            text = "⚠ Fail if unsuccessful",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── AUC (Application Usage Control, Tag 9F07) ───

@Composable
fun AucBitDecoder(auc: AucDecoded) {
    DecoderContainer(title = "Application Usage Control") {
        FlagItem(label = "Domestic Cash", isSet = auc.domesticCashAllowed)
        FlagItem(label = "International Cash", isSet = auc.internationalCashAllowed)
        FlagItem(label = "Domestic Goods", isSet = auc.domesticGoodsAllowed)
        FlagItem(label = "International Goods", isSet = auc.internationalGoodsAllowed)
        FlagItem(label = "Domestic Services", isSet = auc.domesticServicesAllowed)
        FlagItem(label = "International Services", isSet = auc.internationalServicesAllowed)
        FlagItem(label = "ATM Allowed", isSet = auc.atmAllowed)
        FlagItem(label = "Non-ATM Terminal", isSet = auc.nonAtmTerminalAllowed)
        FlagItem(label = "Domestic Cashback", isSet = auc.domesticCashbackAllowed)
        FlagItem(label = "International Cashback", isSet = auc.internationalCashbackAllowed)
    }
}

// ─── IAD (Issuer Application Data, Tag 9F10) ───

@Composable
fun IadDecoder(iad: IadDecoded) {
    DecoderContainer(title = "Issuer Application Data") {
        LabelValueRow("Cryptogram Version", iad.cvnDescription)
        iad.derivationKeyIndex?.let { dki ->
            LabelValueRow("Derivation Key Index", "%02X".format(dki))
        }
        iad.cvrBytes?.let { cvr ->
            LabelValueRow("CVR (raw)", cvr)
        }

        if (iad.cvrBits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Card Verification Results",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            iad.cvrBits.forEach { flag ->
                FlagItem(
                    label = flag,
                    isSet = true,
                    isWarning = flag.contains("fail", ignoreCase = true) ||
                        flag.contains("decline", ignoreCase = true) ||
                        flag.contains("zero", ignoreCase = true)
                )
            }
        }
    }
}

// ─── TVR (Terminal Verification Results, Tag 95) ───

@Composable
fun TvrBitDecoder(tvr: TvrDecoded) {
    val setFlags = tvr.flags.filter { it.isSet }
    val unsetFlags = tvr.flags.filter { !it.isSet }

    DecoderContainer(title = "Terminal Verification Results") {
        if (setFlags.isEmpty()) {
            Text(
                text = "✓ All checks passed — no issues detected",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "Issues Detected (${setFlags.size})",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            setFlags.forEach { flag ->
                FlagItem(
                    label = flag.label,
                    description = "Byte ${flag.byteIndex}, Bit ${flag.bitIndex}",
                    isSet = true,
                    isWarning = true
                )
            }
        }

        if (unsetFlags.isNotEmpty() && setFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Passed Checks (${unsetFlags.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        unsetFlags.forEach { flag ->
            FlagItem(label = flag.label, isSet = false)
        }
    }
}

// ─── Shared components ───

@Composable
private fun DecoderContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
            content()
        }
    }
}

@Composable
private fun FlagItem(
    label: String,
    description: String? = null,
    isSet: Boolean,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = when {
            isSet && isWarning -> MaterialTheme.colorScheme.error
            isSet -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }

        Icon(
            imageVector = when {
                isSet && isWarning -> Icons.Default.Warning
                isSet -> Icons.Default.Check
                else -> Icons.Default.Close
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tint
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSet) FontWeight.Medium else FontWeight.Normal,
                color = if (isSet)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatMinorUnits(cents: Long): String {
    val dollars = cents / 100
    val remainder = cents % 100
    return "$${dollars}.%02d".format(remainder)
}
