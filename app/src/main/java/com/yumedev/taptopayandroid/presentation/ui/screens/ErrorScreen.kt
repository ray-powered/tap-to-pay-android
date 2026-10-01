package com.yumedev.taptopayandroid.presentation.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.domain.model.*
import com.yumedev.taptopayandroid.presentation.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ErrorScreen(
    amount: String,
    errorMessage: String,
    innerPadding: PaddingValues,
    emvCardData: EmvCardData? = null,
    terminalConfig: TerminalConfig? = null,
    onTryAgain: () -> Unit = {},
    onNavigateToDetails: (() -> Unit)? = null,
    onNavigateToHome: () -> Unit
) {
    BackHandler(onBack = onNavigateToHome)

    val config = terminalConfig ?: TerminalConfig()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val analysis = emvCardData?.transactionAnalysis
    val cid = analysis?.cid

    // --- Intelligent Failure Diagnosis ---
    val failureConfig = remember(errorMessage, analysis, config) {
        classifyFailure(errorMessage, analysis, config)
    }

    // --- Animations ---
    val iconScale = remember { Animatable(0.25f) }
    val iconAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0.7f) }
    val rippleAlpha = remember { Animatable(0.75f) }

    val bannerAlpha = remember { Animatable(0f) }
    val bannerSlide = remember { Animatable(-18f) }

    val receiptAlpha = remember { Animatable(0f) }
    val receiptSlide = remember { Animatable(32f) }

    val adviceAlpha = remember { Animatable(0f) }
    val adviceSlide = remember { Animatable(25f) }

    val actionsAlpha = remember { Animatable(0f) }
    val actionsSlide = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Trigger distinct failure vibration (triple reject buzz)
        HapticHelper.playFailureVibration(context, hapticFeedback)

        // Banner and Icon pop
        launch { bannerAlpha.animateTo(1f, tween(200)) }
        launch {
            bannerSlide.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        launch { iconAlpha.animateTo(1f, tween(150)) }
        launch {
            iconScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        // Bloom ripple wave behind icon
        launch {
            delay(100)
            launch { rippleScale.animateTo(1.7f, tween(650, easing = FastOutSlowInEasing)) }
            launch { rippleAlpha.animateTo(0f, tween(650, easing = LinearEasing)) }
        }

        // Receipt slip entrance
        launch {
            delay(180)
            launch { receiptAlpha.animateTo(1f, tween(300)) }
            launch {
                receiptSlide.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // Diagnostic advice card entrance
        launch {
            delay(260)
            launch { adviceAlpha.animateTo(1f, tween(300)) }
            launch {
                adviceSlide.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

        // Bottom actions entrance
        launch {
            delay(340)
            launch { actionsAlpha.animateTo(1f, tween(250)) }
            launch {
                actionsSlide.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    val formattedAmount = if (amount.any { !it.isDigit() && it != '.' && it != ',' }) {
        amount
    } else {
        "${config.currencySymbol}$amount"
    }

    val currentTimestamp = remember {
        SimpleDateFormat("yyyy-MM-dd  HH:mm:ss", Locale.US).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Scrollable POS Failure View
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── 1. POS Terminal Header with 4 Contactless LEDs (Decline Pattern) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252))
                    )
                    Text(
                        text = config.merchantName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PosAnimatedLedIndicator(
                    isDeclined = true,
                    ledColorMode = config.ledColorMode
                )
            }

            // ─── 2. POS Hero Decline/Failure Banner (with Sale Amount) ───
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = bannerAlpha.value
                        translationY = bannerSlide.value
                    },
                shape = RoundedCornerShape(16.dp),
                color = failureConfig.headerBgColor,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp)
                    ) {
                        // Expanding ripple halo
                        if (rippleAlpha.value > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .graphicsLayer {
                                        scaleX = rippleScale.value
                                        scaleY = rippleScale.value
                                        alpha = rippleAlpha.value
                                    }
                                    .clip(CircleShape)
                                    .background(failureConfig.headerContentColor.copy(alpha = 0.35f))
                            )
                        }

                        // Main Status Icon with spring bounce
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(failureConfig.headerContentColor.copy(alpha = 0.18f))
                                .graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                    alpha = iconAlpha.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = failureConfig.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = failureConfig.headerContentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Sale Amount
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = failureConfig.headerContentColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = failureConfig.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = failureConfig.headerContentColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )

                    if (failureConfig.badgeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = failureConfig.badgeBgColor,
                            border = BorderStroke(1.dp, failureConfig.headerContentColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = failureConfig.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = failureConfig.badgeTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = failureConfig.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = failureConfig.headerContentColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── 3. POS Terminal Failure Slip (Receipt Style) ───
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = receiptAlpha.value
                        translationY = receiptSlide.value
                    },
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

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    if (emvCardData != null) {
                        PosReceiptRow(
                            label = "CARD TYPE",
                            value = emvCardData.cardType.name
                        )

                        PosReceiptRow(
                            label = "ACCOUNT NUM",
                            value = "•••• •••• •••• ${emvCardData.cardholderData.panLastFour.ifEmpty { "----" }}",
                            isBold = true
                        )

                        emvCardData.cardholderData.cardholderName?.let { name ->
                            if (name.isNotBlank()) {
                                PosReceiptRow(label = "CARDHOLDER", value = name.uppercase())
                            }
                        }

                        PosReceiptRow(
                            label = "ENTRY METHOD",
                            value = "NFC CONTACTLESS"
                        )

                        if (emvCardData.applicationInfo.aid.isNotEmpty()) {
                            PosReceiptRow(
                                label = "AID",
                                value = emvCardData.applicationInfo.aid.chunked(4).joinToString(" ")
                            )
                        }

                        emvCardData.applicationInfo.applicationLabel?.let { label ->
                            if (label.isNotBlank()) {
                                PosReceiptRow(label = "APP LABEL", value = label)
                            }
                        }

                        PosReceiptRow(
                            label = "DECISION",
                            value = analysis?.decisionTitle ?: "DECLINED",
                            isBold = true,
                            valueColor = MaterialTheme.colorScheme.error
                        )

                        cid?.let {
                            PosReceiptRow(
                                label = "CRYPTOGRAM",
                                value = "${it.cryptogramType.name} (${it.rawValue})"
                            )
                            if (it.reason.isNotBlank()) {
                                PosReceiptRow(
                                    label = "REASON",
                                    value = it.reason
                                )
                            }
                        }
                    } else {
                        PosReceiptRow(
                            label = "TRANSACTION",
                            value = "PURCHASE"
                        )
                        PosReceiptRow(
                            label = "ENTRY METHOD",
                            value = "NFC CONTACTLESS"
                        )
                        PosReceiptRow(
                            label = "RESULT",
                            value = "COMMUNICATION / RF FAILURE",
                            isBold = true,
                            valueColor = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    PosReceiptRow(
                        label = "TERMINAL STATUS",
                        value = "DECLINED / NOT COMPLETED",
                        isBold = true,
                        valueColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ─── 4. Intelligent Diagnostic & Troubleshooting Card ───
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = adviceAlpha.value
                        translationY = adviceSlide.value
                    },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DIAGNOSIS & SUGGESTIONS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Primary Cause
                    Text(
                        text = "• Cause: ${failureConfig.primaryReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    // Actionable Suggestions
                    Text(
                        text = "• Suggestion: ${failureConfig.troubleshootingAdvice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Technical Details Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Log: $errorMessage",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ─── 5. Fixed POS Action Buttons Bar ───
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = actionsAlpha.value
                    translationY = actionsSlide.value
                },
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary POS Button: TRY AGAIN
                Button(
                    onClick = onTryAgain,
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
                        text = "TRY AGAIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary POS Button: VIEW FULL EMV AUDIT & APDUS
                if (emvCardData != null && onNavigateToDetails != null) {
                    OutlinedButton(
                        onClick = onNavigateToDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VIEW APDU & CARD LOGS",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Tertiary POS Button: RETURN TO HOME
                TextButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CANCEL & RETURN HOME",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Classification Helper ───

private data class PosFailureConfig(
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val headerBgColor: Color,
    val headerContentColor: Color,
    val icon: ImageVector,
    val primaryReason: String,
    val troubleshootingAdvice: String
)

private fun classifyFailure(
    errorMessage: String,
    analysis: TransactionAnalysisResult?,
    config: TerminalConfig
): PosFailureConfig {
    val isDeclined = analysis?.decision == TransactionDecision.DECLINED_BY_CARD ||
        errorMessage.contains("AAC", ignoreCase = true) ||
        errorMessage.contains("declined", ignoreCase = true) ||
        errorMessage.contains("拒绝", ignoreCase = true)

    val isSwitchContact = analysis?.decision == TransactionDecision.SWITCH_INTERFACE_CONTACT ||
        analysis?.isSwitchInterfaceRequired == true ||
        errorMessage.contains("69 84") ||
        errorMessage.contains("insert chip", ignoreCase = true) ||
        errorMessage.contains("Contact chip required", ignoreCase = true) ||
        errorMessage.contains("插入", ignoreCase = true)

    val isSeePhone = analysis?.decision == TransactionDecision.SEE_PHONE_CDCVM ||
        errorMessage.contains("69 86") ||
        errorMessage.contains("See Phone", ignoreCase = true) ||
        errorMessage.contains("CDCVM", ignoreCase = true)

    val isCommsLost = errorMessage.contains("Card removed", ignoreCase = true) ||
        errorMessage.contains("connection lost", ignoreCase = true) ||
        errorMessage.contains("Communication error", ignoreCase = true) ||
        errorMessage.contains("Tag was lost", ignoreCase = true) ||
        errorMessage.contains("timeout", ignoreCase = true)

    val isConditionsNotMet = errorMessage.contains("6A 86") ||
        errorMessage.contains("69 85") ||
        errorMessage.contains("6D 00") ||
        errorMessage.contains("Conditions of use", ignoreCase = true)

    val isUnsupportedAid = errorMessage.contains("Failed to select", ignoreCase = true) ||
        errorMessage.contains("AID", ignoreCase = true) ||
        errorMessage.contains("application", ignoreCase = true)

    return when {
        isDeclined -> {
            val cidReason = analysis?.cid?.reason ?: "Card issued AAC (Application Authentication Cryptogram) and declined the transaction."
            PosFailureConfig(
                title = "TRANSACTION DECLINED",
                subtitle = "DECLINED BY CARD (AAC)",
                badgeText = "DECLINED · AAC",
                badgeBgColor = Color(0xFF880E4F),
                badgeTextColor = Color(0xFFFFCDD2),
                headerBgColor = Color(0xFFB71C1C),
                headerContentColor = Color(0xFFFFEBEE),
                icon = Icons.Default.Cancel,
                primaryReason = cidReason,
                troubleshootingAdvice = if (config.genAcRequestMode == GenAcRequestMode.FORCE_TC) {
                    "You selected 'Force TC (0x40)' mode. Most standard consumer credit/debit cards forbid offline approval (offline floor limit is $0 or online auth is required by issuer). In Settings, change 'GENERATE AC Policy' to 'Auto (TAA)' or 'Force ARQC' to allow online authorization."
                } else {
                    "The card chip's internal risk management declined this transaction. Please verify card expiration date and credit limit, or ask cardholder for an alternative card."
                }
            )
        }
        isSwitchContact -> {
            PosFailureConfig(
                title = "PLEASE INSERT CARD",
                subtitle = "CONTACTLESS NOT PERMITTED (69 84)",
                badgeText = "USE CHIP SLOT · 69 84",
                badgeBgColor = Color(0xFFBF360C),
                badgeTextColor = Color(0xFFFFCC80),
                headerBgColor = Color(0xFFE65100),
                headerContentColor = Color(0xFFFFF3E0),
                icon = Icons.Default.CreditCard,
                primaryReason = "Card returned status 69 84: Contactless payment is not permitted for this transaction.",
                troubleshootingAdvice = "The card issuer requires this card to be inserted into a physical contact chip slot (e.g. cumulative contactless spending limit reached, or card profile restriction)."
            )
        }
        isSeePhone -> {
            PosFailureConfig(
                title = "CHECK PHONE SCREEN",
                subtitle = "ON-DEVICE AUTHENTICATION REQUIRED (69 86)",
                badgeText = "CDCVM REQUIRED · 69 86",
                badgeBgColor = Color(0xFF1A237E),
                badgeTextColor = Color(0xFFC5CAE9),
                headerBgColor = Color(0xFF283593),
                headerContentColor = Color(0xFFE8EAF6),
                icon = Icons.Default.Fingerprint,
                primaryReason = "Mobile device (Apple Pay / Google Pay) returned status 69 86 requesting consumer authentication on phone screen.",
                troubleshootingAdvice = "Unlock your phone or wearable screen, complete biometric (Face ID / Fingerprint) or passcode verification, then tap again."
            )
        }
        isCommsLost -> {
            PosFailureConfig(
                title = "COMMUNICATION LOST",
                subtitle = "CARD REMOVED BEFORE COMPLETION",
                badgeText = "RF LINK LOST",
                badgeBgColor = Color(0xFFB71C1C),
                badgeTextColor = Color(0xFFFFCDD2),
                headerBgColor = Color(0xFFC62828),
                headerContentColor = Color(0xFFFFEBEE),
                icon = Icons.Default.SensorsOff,
                primaryReason = "The NFC radio frequency connection was interrupted before APDU exchanges finished.",
                troubleshootingAdvice = "Hold the card firmly against the center of the phone's NFC antenna for at least 1-2 seconds until the transaction completes."
            )
        }
        isConditionsNotMet -> {
            val sw = when {
                errorMessage.contains("6A 86") -> "6A 86 (Parameter Mismatch)"
                errorMessage.contains("69 85") -> "69 85 (Conditions Not Satisfied)"
                errorMessage.contains("6D 00") -> "6D 00 (Instruction Not Supported)"
                errorMessage.contains("6A 82") -> "6A 82 (File Not Found)"
                else -> "ERROR"
            }
            PosFailureConfig(
                title = "CARD CONDITIONS NOT MET",
                subtitle = "COMMAND REJECTED BY CARD",
                badgeText = "APDU $sw",
                badgeBgColor = Color(0xFF4A148C),
                badgeTextColor = Color(0xFFE1BEE7),
                headerBgColor = Color(0xFF880E4F),
                headerContentColor = Color(0xFFF3E5F5),
                icon = Icons.Default.ErrorOutline,
                primaryReason = "The card chip returned status $sw rejecting the command parameter or transaction conditions.",
                troubleshootingAdvice = "Check the POS terminal settings (TTQ, TVR, GENERATE AC policy) or ensure the card is not temporarily locked."
            )
        }
        isUnsupportedAid -> {
            PosFailureConfig(
                title = "CARD NOT SUPPORTED",
                subtitle = "NO MATCHING PAYMENT APPLICATION",
                badgeText = "UNSUPPORTED CARD",
                badgeBgColor = Color(0xFF263238),
                badgeTextColor = Color(0xFFCFD8DC),
                headerBgColor = Color(0xFF37474F),
                headerContentColor = Color(0xFFECEFF1),
                icon = Icons.Default.CreditCardOff,
                primaryReason = "The terminal was unable to select any supported payment application on this card.",
                troubleshootingAdvice = "Ensure the card is a contactless EMV card (Visa, Mastercard, UnionPay, Amex, Discover). Transit/loyalty/access cards are not supported."
            )
        }
        else -> {
            PosFailureConfig(
                title = "TRANSACTION FAILED",
                subtitle = "PAYMENT PROCESSING ERROR",
                badgeText = "FAILED",
                badgeBgColor = Color(0xFFB71C1C),
                badgeTextColor = Color(0xFFFFCDD2),
                headerBgColor = Color(0xFFC62828),
                headerContentColor = Color(0xFFFFEBEE),
                icon = Icons.Default.Error,
                primaryReason = errorMessage,
                troubleshootingAdvice = "Please check the terminal settings or tap again to retry."
            )
        }
    }
}
