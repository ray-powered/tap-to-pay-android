package com.yumedev.taptopayandroid.presentation.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.domain.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SuccessScreen(
    amount: String,
    emvCardData: EmvCardData,
    terminalConfig: TerminalConfig? = null,
    innerPadding: PaddingValues,
    onNavigateToDetails: () -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val analysis = emvCardData.transactionAnalysis
    val config = terminalConfig ?: TerminalConfig()
    val scrollState = rememberScrollState()

    // Determine POS theme colors based on transaction decision
    val (statusBgColor, statusContentColor, statusTitle, statusSubtitle, statusIcon) = when (analysis.decision) {
        TransactionDecision.APPROVED_OFFLINE -> PosStatusConfig(
            bgColor = Color(0xFF1B5E20), // Rich POS Emerald
            contentColor = Color(0xFFE8F5E9),
            title = "TRANSACTION APPROVED",
            subtitle = "OFFLINE AUTHORIZED • TC GENERATED",
            icon = Icons.Default.CheckCircle
        )
        TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> PosStatusConfig(
            bgColor = Color(0xFF0D47A1), // POS Host Blue
            contentColor = Color(0xFFE3F2FD),
            title = "ONLINE AUTH REQUESTED",
            subtitle = "ARQC GENERATED • HOST ROUTING REQUIRED",
            icon = Icons.Default.Lock
        )
        TransactionDecision.SEE_PHONE_CDCVM -> PosStatusConfig(
            bgColor = Color(0xFFE65100), // POS Warning Amber
            contentColor = Color(0xFFFFF3E0),
            title = "PLEASE SEE PHONE",
            subtitle = "AUTHENTICATE ON DEVICE SCREEN & TAP AGAIN",
            icon = Icons.Default.Fingerprint
        )
        TransactionDecision.DECLINED_BY_CARD -> PosStatusConfig(
            bgColor = Color(0xFFB71C1C), // POS Decline Red
            contentColor = Color(0xFFFFEBEE),
            title = "TRANSACTION DECLINED",
            subtitle = "DECLINED BY CARD (AAC)",
            icon = Icons.Default.Close
        )
        TransactionDecision.SWITCH_INTERFACE_CONTACT -> PosStatusConfig(
            bgColor = Color(0xFFF57F17), // POS Interface Amber
            contentColor = Color(0xFFFFFDE7),
            title = "PLEASE INSERT CARD",
            subtitle = "CONTACTLESS NOT PERMITTED • USE CHIP SLOT",
            icon = Icons.Default.CreditCard
        )
        else -> PosStatusConfig(
            bgColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            title = "TRANSACTION COMPLETE",
            subtitle = "EMV CONTACTLESS DATA CAPTURED",
            icon = Icons.Default.Done
        )
    }

    val formattedAmount = if (amount.any { !it.isDigit() && it != '.' && it != ',' }) {
        amount
    } else {
        "${config.currencySymbol}$amount"
    }

    val currentTimestamp = SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.US).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable POS Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── 1. POS Terminal Top Bar with 4 Contactless LEDs ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contactless 4-LED Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ledColor = if (analysis.isDeclined) Color(0xFFE53935) else Color(0xFF00E676)
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ledColor)
                            )
                        }
                    }

                    Text(
                        text = "EMV CONTACTLESS POS",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ─── 2. POS Hero Status Banner ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = statusBgColor,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = statusContentColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = statusContentColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusContentColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── 3. Amount Display ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL SALE AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ─── 4. POS Terminal Transaction Slip (Receipt Style) ───
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Merchant Header
                    Text(
                        text = config.merchantName.uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TID: ${config.ifdSerialNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "MID: ${config.merchantCategoryCode}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = currentTimestamp,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dashed separator
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Card & Account Details
                    PosReceiptRow(
                        label = "CARD TYPE",
                        value = emvCardData.cardType.name
                    )

                    PosReceiptRow(
                        label = "ACCOUNT NUM",
                        value = "•••• •••• •••• ${emvCardData.cardholderData.panLastFour}",
                        isBold = true
                    )

                    emvCardData.cardholderData.cardholderName?.let { name ->
                        if (name.isNotBlank()) {
                            PosReceiptRow(label = "CARDHOLDER", value = name.uppercase())
                        }
                    }

                    PosReceiptRow(
                        label = "TRANSACTION",
                        value = emvCardData.transactionData.transactionTypeDescription ?: "PURCHASE (00)"
                    )

                    PosReceiptRow(
                        label = "ENTRY METHOD",
                        value = "NFC CONTACTLESS (CLSS)"
                    )

                    PosReceiptRow(
                        label = "AID",
                        value = emvCardData.applicationInfo.aid.chunked(4).joinToString(" ")
                    )

                    emvCardData.applicationInfo.applicationLabel?.let { label ->
                        PosReceiptRow(label = "APP LABEL", value = label)
                    }

                    emvCardData.transactionData.atc?.let { atc ->
                        PosReceiptRow(label = "ATC", value = String.format("#%04d", atc))
                    }

                    // Dashed separator
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Verification & Cryptogram Details
                    PosReceiptRow(
                        label = "CVM VERIFICATION",
                        value = analysis.cvmTitle.uppercase(),
                        isBold = true,
                        valueColor = if (analysis.requiresScreenCheck) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )

                    val cryptogramTag = emvCardData.additionalTags["9F26"]
                    val cidTag = emvCardData.additionalTags["9F27"]
                    if (cryptogramTag != null || cidTag != null || analysis.cid != null) {
                        val cryptType = analysis.cid?.cryptogramType?.label ?: "AC"
                        val cryptVal = cryptogramTag?.value?.take(16) ?: ""
                        PosReceiptRow(
                            label = "CRYPTOGRAM ($cryptType)",
                            value = if (cryptVal.isNotEmpty()) "$cryptVal..." else cryptType,
                            isMonospace = true
                        )
                    }

                    PosReceiptRow(
                        label = "RESPONSE CODE",
                        value = when (analysis.decision) {
                            TransactionDecision.APPROVED_OFFLINE -> "00 (APPROVED)"
                            TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> "00 (ARQC GENERATED)"
                            TransactionDecision.SEE_PHONE_CDCVM -> "69 86 (SEE PHONE)"
                            TransactionDecision.DECLINED_BY_CARD -> "05 (DECLINED)"
                            TransactionDecision.SWITCH_INTERFACE_CONTACT -> "69 84 (USE CHIP)"
                            else -> "00 (DATA OK)"
                        },
                        isBold = true
                    )
                }
            }

            // Screen Check Banner if applicable
            if (analysis.requiresScreenCheck) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Customer Action Required: Unlock mobile device or complete biometric authentication (Face ID / Fingerprint / Passcode), then tap the reader again.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // ─── 5. POS Bottom Controls ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary POS Button: NEW TRANSACTION
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEW SALE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary POS Button: VIEW FULL EMV AUDIT
                OutlinedButton(
                    onClick = onNavigateToDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIEW FULL EMV AUDIT & APDUS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PosReceiptRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isMonospace: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

private data class PosStatusConfig(
    val bgColor: Color,
    val contentColor: Color,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)
