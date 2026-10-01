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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    viewModel: TapToPayViewModel = hiltViewModel()
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ─── 1. POS Clean Header: Merchant Name & 4 Contactless LEDs ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Text(
                    text = terminalConfig.merchantName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // 4 Contactless LEDs Module
            PosPhysicalLedModule(
                isWaiting = !isSeePhone && !isReading,
                isReading = isReading,
                isSeePhone = isSeePhone,
                apduPulseCount = apduPulseCount,
                ledColorMode = terminalConfig.ledColorMode
            )
        }

        // ─── 2. Prominent Sale Amount ───
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "TOTAL DUE",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // ─── 3. Center Hero Zone: Contactless Wave / Reading HUD / See Phone ───
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

        // ─── 4. Supported Schemes & Cancel Control ───
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val schemes = listOf("Visa", "Mastercard", "UnionPay", "Amex", "Discover")
                schemes.forEachIndexed { index, scheme ->
                    Text(
                        text = scheme,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (index < schemes.size - 1) {
                        Text(
                            text = "  ·  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Contactless 4-LED Physical Module (EMV 4-Green & UnionPay 4-Color) ───

// ─── Contactless 4-LED Physical Module (EMV 4-Green & UnionPay 4-Color) ───

@Composable
fun PosPhysicalLedModule(
    isWaiting: Boolean,
    isReading: Boolean,
    isSeePhone: Boolean,
    apduPulseCount: Int,
    ledColorMode: PosLedColorMode
) {
    val greenColor = Color(0xFF00E676)
    val blueColor = Color(0xFF2979FF)
    val yellowColor = Color(0xFFFFB300)
    val redColor = Color(0xFFE53935)
    val inactiveAlpha = 0.18f

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

    val seePhonePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "seePhonePulse"
    )

    val (c1, a1) = when (ledColorMode) {
        PosLedColorMode.EMV_GREEN -> Pair(greenColor, 1f)
        PosLedColorMode.UNIONPAY_COLOR -> Pair(blueColor, 1f)
    }

    val (c2, a2) = when {
        isReading -> {
            val color = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else yellowColor
            Pair(color, led2BlinkAlpha)
        }
        isSeePhone -> Pair(yellowColor, seePhonePulseAlpha)
        else -> {
            val color = if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else yellowColor
            Pair(color, inactiveAlpha)
        }
    }

    val (c3, a3) = Pair(greenColor, inactiveAlpha)
    val (c4, a4) = Pair(if (ledColorMode == PosLedColorMode.EMV_GREEN) greenColor else redColor, inactiveAlpha)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosSingleLedDot(color = c1, alpha = a1)
            PosSingleLedDot(color = c2, alpha = a2)
            PosSingleLedDot(color = c3, alpha = a3)
            PosSingleLedDot(color = c4, alpha = a4)
        }
    }
}

@Composable
private fun PosSingleLedDot(color: Color, alpha: Float) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
            .graphicsLayer {
                scaleX = if (alpha > 0.5f) 1f else 0.85f
                scaleY = if (alpha > 0.5f) 1f else 0.85f
            }
    )
}

// ─── Center Zone: POS Contactless Landing Pad ───

@Composable
fun PosContactlessLandingPad() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfcWaveTransition")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer {
                        scaleX = waveScale
                        scaleY = waveScale
                        alpha = waveAlpha
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )

            Surface(
                modifier = Modifier.size(130.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.tap_to_pay),
                        contentDescription = "Contactless Tap Zone",
                        modifier = Modifier.size(68.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Tap card or phone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Hold near reader to pay",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Center Zone: Reading HUD ───

@Composable
fun ReadingCardPosHud(
    currentApdu: String?,
    liveLogs: List<String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(140.dp),
                strokeWidth = 3.dp,
                color = Color(0xFFFFB300)
            )

            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = Color(0xFFFFF8E1),
                border = BorderStroke(1.5.dp, Color(0xFFFFB300).copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.tap_to_pay),
                        contentDescription = "Reading Card",
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFF57F17)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Reading card...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Please do not remove card",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!currentApdu.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = currentApdu,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─── Hero Content: SEE PHONE (Two Tap CDCVM Prompt) ───

@Composable
fun SeePhoneRetryContent(instructions: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Surface(
            modifier = Modifier.size(130.dp),
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
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFE65100)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Please see phone",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
                textAlign = TextAlign.Center
            )
            Text(
                text = instructions.ifEmpty { "Authenticate on phone screen, then tap again." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}
