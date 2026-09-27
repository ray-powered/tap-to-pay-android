package com.yumedev.taptopayandroid.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yumedev.taptopayandroid.R
import com.yumedev.taptopayandroid.domain.model.EmvCardData
import com.yumedev.taptopayandroid.domain.model.NfcState
import com.yumedev.taptopayandroid.domain.model.PosLedColorMode
import com.yumedev.taptopayandroid.presentation.util.HapticHelper
import com.yumedev.taptopayandroid.presentation.viewmodel.TapToPayViewModel

@Composable
fun TapToPayScreen(
    amount: String,
    onCancel: () -> Unit,
    onSuccess: (EmvCardData) -> Unit,
    onError: (String) -> Unit,
    innerPadding: PaddingValues,
    viewModel: TapToPayViewModel = viewModel()
) {
    val nfcState by viewModel.nfcState.collectAsState()
    val terminalConfig by viewModel.terminalConfig.collectAsState()
    val currentApduCommand by viewModel.currentApduCommand.collectAsState()
    val apduPulseCount by viewModel.apduPulseCount.collectAsState()
    val liveApduLogs by viewModel.liveApduLogs.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.startNewTransaction(amount)
    }

    // Handle terminal state transitions
    LaunchedEffect(nfcState) {
        when (nfcState) {
            is NfcState.Success -> {
                HapticHelper.playSuccessVibration(context, hapticFeedback)
                onSuccess((nfcState as NfcState.Success).emvCardData)
            }
            is NfcState.SeePhone -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            is NfcState.Error -> {
                onError((nfcState as NfcState.Error).message)
            }
            // NfcState.SeePhone intentionally stays on this screen to let user retry 2nd tap!
            else -> {}
        }
    }

    val formattedAmount = if (amount.any { !it.isDigit() && it != '.' && it != ',' }) {
        amount
    } else {
        "${terminalConfig.currencySymbol}$amount"
    }

    val isSeePhone = nfcState is NfcState.SeePhone
    val isReading = nfcState is NfcState.Reading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ─── 1. POS Terminal Hardware Bezel & 4 Contactless LEDs ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Hardware Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Text(
                            text = terminalConfig.merchantName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "TID:${terminalConfig.ifdSerialNumber} · ONLINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Physical Contactless 4-LED Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contactless Wave Mark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.tap_to_pay),
                            contentDescription = "Contactless",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "CONTACTLESS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 4 Physical POS LEDs
                    PosPhysicalLedModule(
                        isWaiting = !isSeePhone && !isReading,
                        isReading = isReading,
                        isSeePhone = isSeePhone,
                        apduPulseCount = apduPulseCount,
                        ledColorMode = terminalConfig.ledColorMode,
                        onToggleMode = { viewModel.toggleLedColorMode() }
                    )
                }
            }
        }

        // ─── 2. POS High-Contrast Financial Display ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SALE TRANSACTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "BATCH: 0001 · TRACE: 083921",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ─── 3. Center Hero Zone: POS Landing Pad / APDU HUD / See Phone ───
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSeePhone -> {
                    val instructions = (nfcState as NfcState.SeePhone).instructions
                    SeePhoneRetryContent(instructions = instructions)
                }
                isReading -> {
                    ReadingCardPosHud(
                        currentApdu = currentApduCommand,
                        liveLogs = liveApduLogs
                    )
                }
                else -> {
                    PosContactlessLandingPad()
                }
            }
        }

        // ─── 4. Supported Schemes & POS Cancel Control ───
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Supported payment badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val schemes = listOf("VISA", "MASTERCARD", "UNIONPAY", "AMEX", "DISCOVER")
                schemes.forEachIndexed { index, scheme ->
                    Text(
                        text = scheme,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (index < schemes.size - 1) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // POS Cancel Button (Emergency red action)
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CANCEL TRANSACTION",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── Contactless 4-LED Physical Module (EMV 4-Green & UnionPay 4-Color) ───

@Composable
fun PosPhysicalLedModule(
    isWaiting: Boolean,
    isReading: Boolean,
    isSeePhone: Boolean,
    apduPulseCount: Int,
    ledColorMode: PosLedColorMode,
    onToggleMode: () -> Unit
) {
    val greenColor = Color(0xFF00E676)
    val blueColor = Color(0xFF2979FF)
    val yellowColor = Color(0xFFFFB300)
    val redColor = Color(0xFFE53935)
    val inactiveAlpha = 0.15f

    // Fast blink animation for LED 2 during APDU transmission / reading (120ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "posLedTransition")
    val led2BlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led2Blink"
    )

    // Pulsing attention wave for See Phone (CDCVM)
    val seePhonePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seePhonePulse"
    )

    // Dynamic pulse scale whenever a new APDU command is transmitted
    val pulseScale = remember { Animatable(1f) }
    LaunchedEffect(apduPulseCount) {
        if (isReading && apduPulseCount > 0) {
            pulseScale.snapTo(1.35f)
            pulseScale.animateTo(1f, animationSpec = tween(110, easing = FastOutSlowInEasing))
        }
    }

    // Determine colors and alphas for the 4 physical lenses
    val (c1, a1, s1) = when (ledColorMode) {
        PosLedColorMode.EMV_GREEN -> Triple(greenColor, 1f, 1f)
        PosLedColorMode.UNIONPAY_COLOR -> Triple(blueColor, 1f, 1f)
    }

    val (c2, a2, s2) = when {
        isReading -> {
            val color = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else yellowColor
            Triple(color, led2BlinkAlpha, pulseScale.value)
        }
        isSeePhone -> {
            Triple(yellowColor, seePhonePulseAlpha, 1f)
        }
        else -> {
            val color = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else yellowColor
            Triple(color, inactiveAlpha, 1f)
        }
    }

    val (c3, a3, s3) = when (ledColorMode) {
        PosLedColorMode.EMV_GREEN -> Triple(greenColor, inactiveAlpha, 1f)
        PosLedColorMode.UNIONPAY_COLOR -> Triple(greenColor, inactiveAlpha, 1f)
    }

    val (c4, a4, s4) = when (ledColorMode) {
        PosLedColorMode.EMV_GREEN -> Triple(greenColor, inactiveAlpha, 1f)
        PosLedColorMode.UNIONPAY_COLOR -> Triple(redColor, inactiveAlpha, 1f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Clickable LED Standard Toggle Badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1E232A),
            border = BorderStroke(1.dp, Color(0xFF384352)),
            modifier = Modifier.clickable { onToggleMode() }
        ) {
            Text(
                text = if (ledColorMode == PosLedColorMode.EMV_GREEN) "EMV 4-GREEN" else "UPAY 4-COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = if (ledColorMode == PosLedColorMode.EMV_GREEN) Color(0xFF00E676) else Color(0xFF2979FF),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        // Hardware Bezel housing the 4 physical lenses
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF101418),
            border = BorderStroke(1.2.dp, Color(0xFF2E3844)),
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PosSingleLedLens(number = "1", color = c1, alpha = a1, scale = s1)
                PosSingleLedLens(number = "2", color = c2, alpha = a2, scale = s2)
                PosSingleLedLens(number = "3", color = c3, alpha = a3, scale = s3)
                PosSingleLedLens(number = "4", color = c4, alpha = a4, scale = s4)
            }
        }
    }
}

@Composable
private fun PosSingleLedLens(
    number: String,
    color: Color,
    alpha: Float,
    scale: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Metallic Bezel Ring + Acrylic Diode Lens
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFF222933))
                .border(1.2.dp, Color(0xFF3F4B5A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Emissive Glowing Core
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha))
            )
        }
        // Silkscreen micro-label (1, 2, 3, 4)
        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 7.sp,
            color = Color(0xFF6B7A8D)
        )
    }
}

// ─── Center Zone: POS Contactless Landing Pad ───

@Composable
fun PosContactlessLandingPad() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Placement Target Zone with Industrial Corner Marks
            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Corner Alignment Registration Brackets
                Text(
                    text = "┌",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 2.dp)
                )
                Text(
                    text = "┐",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 2.dp)
                )
                Text(
                    text = "└",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 2.dp)
                )
                Text(
                    text = "┘",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 2.dp)
                )

                // Large EMV Contactless Symbol
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.tap_to_pay),
                        contentDescription = "Contactless Tap Zone",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "TOUCH CARD HERE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // High-Contrast Terminal Guidance
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "PRESENT CARD OR MOBILE WALLET",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Hold card flat against reader until beep sounds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Hardware Status Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Text(
                        text = "RF FIELD ACTIVE · 13.56 MHz · ISO/IEC 14443",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Center Zone: Reading HUD with Live POS APDU Console ───

@Composable
fun ReadingCardPosHud(
    currentApdu: String?,
    liveLogs: List<String>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with pulsing read indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulseYellow")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB300).copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "EMV L2 KERNEL ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB300),
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "PROCESSING",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Current Action Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFFFB300)
                    )
                    Text(
                        text = currentApdu ?: "EXCHANGING APDU FRAMES...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            // POS APDU Terminal Console Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0C1014),
                border = BorderStroke(1.2.dp, Color(0xFF263238))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TERMINAL CONSOLE (NFC-A / ISO 14443-4)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF546E7A)
                        )
                        Text(
                            text = "BAUD: 106 kbps",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = Color(0xFF546E7A)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF263238), thickness = 0.8.dp)

                    // Stream last 4 log lines or fallback
                    val displayLogs = if (liveLogs.isNotEmpty()) {
                        liveLogs.takeLast(4)
                    } else {
                        listOf(
                            ">> CARRIER DETECTED · ATS RECEIVED",
                            ">> SELECT PPSE (2PAY.SYS.DDF01)...",
                            ">> WAITING FOR ICC RESPONSE..."
                        )
                    }

                    displayLogs.forEach { log ->
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (log.startsWith("<<")) Color(0xFF00E676) else Color(0xFF4FC3F7),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Warning Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFB300)
                )
                Text(
                    text = "HOLD CARD STILL · DO NOT REMOVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB300)
                )
            }
        }
    }
}

// ─── Hero Content: SEE PHONE (Two Tap CDCVM Prompt) ───

@Composable
fun SeePhoneRetryContent(instructions: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, Color(0xFFE65100).copy(alpha = 0.8f)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Amber Header Bar
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFE65100)
            ) {
                Text(
                    text = "CDCVM REQUIRED · PLEASE SEE PHONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Fingerprint / Phone Icon
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFFFFF3E0),
                border = BorderStroke(2.dp, Color(0xFFE65100))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "See Phone",
                        modifier = Modifier.size(46.dp),
                        tint = Color(0xFFE65100)
                    )
                }
            }

            // Action Instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "AUTHENTICATE ON PHONE SCREEN",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Unlock with Face ID, Fingerprint, or Device PIN, then tap the reader again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Reader Standby Indicator
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E232A),
                border = BorderStroke(1.dp, Color(0xFF384352))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Text(
                        text = "READER WAITING FOR 2ND TAP...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                }
            }
        }
    }
}
