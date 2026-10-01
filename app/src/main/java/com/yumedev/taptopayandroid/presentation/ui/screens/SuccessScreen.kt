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
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // ─── Animation States ───
    val iconScale = remember { Animatable(0.25f) }
    val iconAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0.7f) }
    val rippleAlpha = remember { Animatable(0.75f) }

    val bannerAlpha = remember { Animatable(0f) }
    val bannerSlide = remember { Animatable(-18f) }

    val receiptAlpha = remember { Animatable(0f) }
    val receiptSlide = remember { Animatable(32f) }

    val actionsAlpha = remember { Animatable(0f) }
    val actionsSlide = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Trigger subtle double-pulse tactile vibration feedback
        HapticHelper.playSuccessVibration(context, hapticFeedback)

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

        // Bloom ripple wave behind the icon
        launch {
            delay(100)
            launch { rippleScale.animateTo(1.7f, tween(650, easing = FastOutSlowInEasing)) }
            launch { rippleAlpha.animateTo(0f, tween(650, easing = LinearEasing)) }
        }

        // Receipt slip entrance (paper slide-out feel)
        launch {
            delay(200)
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

        // Bottom actions entrance
        launch {
            delay(300)
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

    // Determine POS theme colors and badges based on transaction decision
    val statusConfig = when (analysis.decision) {
        TransactionDecision.APPROVED_OFFLINE -> PosStatusConfig(
            bgColor = Color(0xFF1B5E20), // Rich POS Emerald
            contentColor = Color(0xFFE8F5E9),
            title = "OFFLINE APPROVED",
            subtitle = "OFFLINE AUTHORIZED • TC GENERATED BY CHIP",
            icon = Icons.Default.CheckCircle,
            badgeText = "OFFLINE · TC",
            badgeBgColor = Color(0xFF2E7D32),
            badgeTextColor = Color(0xFFA5D6A7)
        )
        TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> {
            val isStrict = analysis.isOnlineRequired
            PosStatusConfig(
                bgColor = Color(0xFF0D47A1), // POS Deep Host Royal Blue
                contentColor = Color(0xFFE3F2FD),
                title = if (isStrict) "ONLINE AUTH REQUIRED" else "ONLINE APPROVED",
                subtitle = if (isStrict) "ARQC GENERATED • FORWARD TO HOST GATEWAY" else "ONLINE AUTHORIZED • ARQC HOST APPROVED",
                icon = Icons.Default.CheckCircle,
                badgeText = if (isStrict) "ONLINE AUTH · ARQC" else "ONLINE · ARQC",
                badgeBgColor = Color(0xFF1565C0),
                badgeTextColor = Color(0xFF90CAF9)
            )
        }
        TransactionDecision.SEE_PHONE_CDCVM -> PosStatusConfig(
            bgColor = Color(0xFFE65100), // POS Warning Amber
            contentColor = Color(0xFFFFF3E0),
            title = "PLEASE SEE PHONE",
            subtitle = "AUTHENTICATE ON DEVICE SCREEN & TAP AGAIN",
            icon = Icons.Default.Fingerprint,
            badgeText = "CDCVM REQUIRED",
            badgeBgColor = Color(0xFFBF360C),
            badgeTextColor = Color(0xFFFFCC80)
        )
        TransactionDecision.DECLINED_BY_CARD -> PosStatusConfig(
            bgColor = Color(0xFFB71C1C), // POS Decline Red
            contentColor = Color(0xFFFFEBEE),
            title = "TRANSACTION DECLINED",
            subtitle = "DECLINED BY CARD (AAC)",
            icon = Icons.Default.Close,
            badgeText = "DECLINED · AAC",
            badgeBgColor = Color(0xFF880E4F),
            badgeTextColor = Color(0xFFFFCDD2)
        )
        TransactionDecision.SWITCH_INTERFACE_CONTACT -> PosStatusConfig(
            bgColor = Color(0xFFF57F17), // POS Interface Amber
            contentColor = Color(0xFFFFFDE7),
            title = "PLEASE INSERT CARD",
            subtitle = "CONTACTLESS NOT PERMITTED • USE CHIP SLOT",
            icon = Icons.Default.CreditCard,
            badgeText = "USE CHIP SLOT",
            badgeBgColor = Color(0xFFE65100),
            badgeTextColor = Color(0xFFFFF9C4)
        )
        else -> PosStatusConfig(
            bgColor = Color(0xFF1B5E20),
            contentColor = Color(0xFFE8F5E9),
            title = "TRANSACTION APPROVED",
            subtitle = "EMV CONTACTLESS DATA CAPTURED",
            icon = Icons.Default.CheckCircle,
            badgeText = "COMPLETE",
            badgeBgColor = Color(0xFF2E7D32),
            badgeTextColor = Color(0xFFA5D6A7)
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
            // ─── 1. POS Terminal Header with 4 Contactless LEDs ───
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
                            .background(Color(0xFF00E676))
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
                    isDeclined = analysis.isDeclined,
                    ledColorMode = config.ledColorMode
                )
            }

            // ─── 2. POS Hero Status Banner (with Amount Included) ───
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = bannerAlpha.value
                        translationY = bannerSlide.value
                    },
                shape = RoundedCornerShape(16.dp),
                color = statusConfig.bgColor,
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
                        // Expanding celebratory ripple/halo wave
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
                                    .background(statusConfig.contentColor.copy(alpha = 0.35f))
                            )
                        }

                        // Main Status Icon with spring bounce
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(statusConfig.contentColor.copy(alpha = 0.18f))
                                .graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                    alpha = iconAlpha.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = statusConfig.icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = statusConfig.contentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent Sale Amount inside the Hero Banner
                    Text(
                        text = formattedAmount,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = statusConfig.contentColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusConfig.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusConfig.contentColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    )

                    if (statusConfig.badgeText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusConfig.badgeBgColor,
                            border = BorderStroke(1.dp, statusConfig.contentColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = statusConfig.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = statusConfig.badgeTextColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statusConfig.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = statusConfig.contentColor.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ─── 4. POS Terminal Transaction Slip (Receipt Style) ───
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

                    PosReceiptRow(
                        label = "AUTH MODE",
                        value = when (analysis.decision) {
                            TransactionDecision.APPROVED_OFFLINE -> "OFFLINE (LOCAL CHIP)"
                            TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> "ONLINE (ISSUER HOST)"
                            TransactionDecision.SEE_PHONE_CDCVM -> "DEVICE CVM PENDING"
                            TransactionDecision.DECLINED_BY_CARD -> "DECLINED (CARD CHIP)"
                            TransactionDecision.SWITCH_INTERFACE_CONTACT -> "CONTACT INTERFACE"
                            else -> "CONTACTLESS EMV"
                        },
                        isBold = true,
                        valueColor = when (analysis.decision) {
                            TransactionDecision.APPROVED_OFFLINE -> Color(0xFF2E7D32)
                            TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> Color(0xFF1565C0)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    val cryptogramTag = emvCardData.additionalTags["9F26"]
                    val cidTag = emvCardData.additionalTags["9F27"]
                    if (cryptogramTag != null || cidTag != null || analysis.cid != null) {
                        val cryptType = when (analysis.decision) {
                            TransactionDecision.APPROVED_OFFLINE -> "TC"
                            TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> "ARQC"
                            else -> analysis.cid?.cryptogramType?.label ?: "AC"
                        }
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
                            TransactionDecision.APPROVED_OFFLINE -> "Y1 (OFFLINE APPROVED)"
                            TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED -> "00 (ONLINE APPROVED)"
                            TransactionDecision.SEE_PHONE_CDCVM -> "69 86 (SEE PHONE)"
                            TransactionDecision.DECLINED_BY_CARD -> "05 (DECLINED)"
                            TransactionDecision.SWITCH_INTERFACE_CONTACT -> "69 84 (USE CHIP)"
                            else -> "00 (APPROVED)"
                        },
                        isBold = true
                    )

                    if (analysis.isApproved || analysis.decision == TransactionDecision.ONLINE_AUTHORIZATION_REQUIRED || analysis.decision == TransactionDecision.APPROVED_OFFLINE) {
                        PosReceiptRow(
                            label = "AUTH CODE",
                            value = if (analysis.decision == TransactionDecision.APPROVED_OFFLINE) "OFFLINE" else "083921",
                            isMonospace = true,
                            isBold = true
                        )
                        PosReceiptRow(
                            label = "HOST ROUTING",
                            value = if (analysis.decision == TransactionDecision.APPROVED_OFFLINE) "LOCAL OFFLINE (NO HOST)" else "ONLINE ISSUER HOST",
                            isMonospace = true
                        )
                    }
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
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = actionsAlpha.value
                    translationY = actionsSlide.value
                },
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

// ─── Contactless 4-LED Indicator: EMV Green & UnionPay 4-Color ───

@Composable
private fun PosAnimatedLedIndicator(
    isDeclined: Boolean,
    ledColorMode: PosLedColorMode = PosLedColorMode.EMV_GREEN
) {
    val greenColor = Color(0xFF00E676)
    val blueColor = Color(0xFF2979FF)
    val yellowColor = Color(0xFFFFB300)
    val redColor = Color(0xFFE53935)

    val inactiveAlpha = 0.15f

    // 4 sequential LED animations
    val led1Alpha = remember { Animatable(inactiveAlpha) }
    val led2Alpha = remember { Animatable(inactiveAlpha) }
    val led3Alpha = remember { Animatable(inactiveAlpha) }
    val led4Alpha = remember { Animatable(inactiveAlpha) }

    LaunchedEffect(isDeclined, ledColorMode) {
        delay(40)
        if (isDeclined) {
            led1Alpha.animateTo(1f, tween(60))
            led4Alpha.animateTo(1f, tween(120))
        } else {
            if (ledColorMode == PosLedColorMode.EMV_GREEN) {
                // EMV Level 1 Standard: All 4 Green LEDs illuminate sequentially to solid green on success!
                led1Alpha.animateTo(1f, tween(60))
                delay(30)
                led2Alpha.animateTo(1f, tween(60))
                delay(30)
                led3Alpha.animateTo(1f, tween(60))
                delay(30)
                led4Alpha.animateTo(1f, tween(60))
            } else {
                // UnionPay 4-Color Standard: Blue (Ready) -> Yellow (Read) -> Green (Approved)
                led1Alpha.animateTo(1f, tween(70))
                delay(40)
                led2Alpha.animateTo(1f, tween(70))
                delay(40)
                led3Alpha.animateTo(1f, tween(90))
            }
        }
    }

    // Determine colors for each lens
    val c1 = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else blueColor
    val c2 = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else yellowColor
    val c3 = greenColor
    val c4 = if (isDeclined) redColor else if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else redColor

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LED 1
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(c1.copy(alpha = led1Alpha.value))
                .graphicsLayer {
                    scaleX = if (led1Alpha.value > 0.5f) 1f else 0.85f
                    scaleY = if (led1Alpha.value > 0.5f) 1f else 0.85f
                }
        )

        // LED 2
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(c2.copy(alpha = led2Alpha.value))
                .graphicsLayer {
                    scaleX = if (led2Alpha.value > 0.5f) 1f else 0.85f
                    scaleY = if (led2Alpha.value > 0.5f) 1f else 0.85f
                }
        )

        // LED 3
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(c3.copy(alpha = led3Alpha.value))
                .graphicsLayer {
                    scaleX = if (led3Alpha.value > 0.5f) 1f else 0.85f
                    scaleY = if (led3Alpha.value > 0.5f) 1f else 0.85f
                }
        )

        // LED 4
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(c4.copy(alpha = led4Alpha.value))
                .graphicsLayer {
                    scaleX = if (led4Alpha.value > 0.5f) 1f else 0.85f
                    scaleY = if (led4Alpha.value > 0.5f) 1f else 0.85f
                }
        )
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
    val icon: ImageVector,
    val badgeText: String = "",
    val badgeBgColor: Color = Color.Transparent,
    val badgeTextColor: Color = Color.White
)

