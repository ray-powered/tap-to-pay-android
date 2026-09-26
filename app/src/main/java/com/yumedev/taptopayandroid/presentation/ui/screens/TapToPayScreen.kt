package com.yumedev.taptopayandroid.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    LaunchedEffect(Unit) {
        viewModel.startNewTransaction(amount)
    }

    // Handle terminal state transitions
    LaunchedEffect(nfcState) {
        when (nfcState) {
            is NfcState.Success -> {
                onSuccess((nfcState as NfcState.Success).emvCardData)
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
        // ─── 1. POS Terminal Header Bar & 4 Contactless LEDs ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Merchant & Terminal ID
                Column {
                    Text(
                        text = terminalConfig.merchantName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "TID: ${terminalConfig.ifdSerialNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Standard EMV 4-LED Indicator
                PosLedIndicator(
                    isWaiting = !isSeePhone && !isReading,
                    isReading = isReading,
                    isSeePhone = isSeePhone
                )
            }
        }

        // ─── 2. Sale Amount Card ───
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SALE AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ─── 3. Center Hero Zone: Tap / Reading / See Phone ───
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
                    ReadingCardContent()
                }
                else -> {
                    WaitingTapContent()
                }
            }
        }

        // ─── 4. Supported Schemes & POS Cancel Control ───
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Supported payment badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val schemes = listOf("VISA", "MASTERCARD", "AMEX", "DISCOVER", "PAY")
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

            // POS Cancel Button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CANCEL TRANSACTION",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ─── EMV Contactless 4-LED Indicator ───

@Composable
private fun PosLedIndicator(
    isWaiting: Boolean,
    isReading: Boolean,
    isSeePhone: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val activeGreen = Color(0xFF00E676)
        val amberColor = Color(0xFFFFB300)
        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

        when {
            isSeePhone -> {
                // All 4 pulse amber to signal user attention
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(amberColor)
                    )
                }
            }
            isReading -> {
                // All LEDs active green during read
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(activeGreen)
                    )
                }
            }
            else -> {
                // 1st LED active green (EMV standard: reader ready for card presentation), others standby
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(activeGreen)
                )
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(inactiveColor)
                    )
                }
            }
        }
    }
}

// ─── Hero Content: Normal Waiting for Card Tap ───

@Composable
private fun WaitingTapContent() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val baseSize = (screenWidth * 0.38f).coerceAtMost(160.dp)
    val iconSize = baseSize * 0.5f

    val infiniteTransition = rememberInfiniteTransition(label = "posWave")

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveScale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(baseSize * 2.2f),
            contentAlignment = Alignment.Center
        ) {
            // Expanding pulsating radar wave
            Box(
                modifier = Modifier
                    .size(baseSize)
                    .scale(waveScale)
                    .alpha(waveAlpha)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )

            // Inner elevated circle
            Surface(
                modifier = Modifier.size(baseSize),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.tap_to_pay),
                        contentDescription = "Contactless Tap",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PRESENT CARD OR DEVICE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Hold contactless card or mobile wallet near the reader",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Hero Content: Reading in Progress ───

@Composable
private fun ReadingCardContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "READING CARD...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "DO NOT REMOVE CARD OR DEVICE",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Hero Content: SEE PHONE (Two Tap CDCVM Prompt) ───

@Composable
private fun SeePhoneRetryContent(instructions: String) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val baseSize = (screenWidth * 0.36f).coerceAtMost(150.dp)

    // Pulsating amber wave for second tap
    val infiniteTransition = rememberInfiniteTransition(label = "amberWave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "amberScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "amberAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(baseSize * 2.0f),
            contentAlignment = Alignment.Center
        ) {
            // Expanding amber wave
            Box(
                modifier = Modifier
                    .size(baseSize)
                    .scale(waveScale)
                    .alpha(waveAlpha)
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFFB300),
                        shape = CircleShape
                    )
            )

            // Center amber hero circle
            Surface(
                modifier = Modifier.size(baseSize),
                shape = CircleShape,
                color = Color(0xFFFFF3E0),
                border = BorderStroke(2.dp, Color(0xFFE65100)),
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "See Phone",
                        modifier = Modifier.size(baseSize * 0.55f),
                        tint = Color(0xFFE65100)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big alert headline
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE65100)
        ) {
            Text(
                text = "PLEASE SEE PHONE",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Authenticate on phone screen with Face ID / Fingerprint / Passcode",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "Reader is waiting for second tap...",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
