package com.yumedev.taptopayandroid.presentation.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import com.yumedev.taptopayandroid.domain.model.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.presentation.ui.components.Keypad
import com.yumedev.taptopayandroid.presentation.ui.components.PrimaryButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.yumedev.taptopayandroid.presentation.ui.components.TerminalQuickSwitchBottomSheet
import com.yumedev.taptopayandroid.presentation.viewmodel.SettingsViewModel

private fun formatCurrency(digits: String): String {
    if (digits.isEmpty()) return "0.00"
    val padded = digits.padStart(3, '0')
    val intPart = padded.dropLast(2).trimStart('0').ifEmpty { "0" }
    val decPart = padded.takeLast(2)
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return "$formattedInt.$decPart"
}

enum class NfcStatus {
    READY,
    DISABLED,
    NOT_SUPPORTED
}

@Composable
fun HomeScreen(
    onGeneratePayment: (String) -> Unit,
    innerPadding: PaddingValues = PaddingValues(),
    onOpenSettings: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val terminalConfig by settingsViewModel.terminalConfig.collectAsState()
    var showQuickSwitchSheet by remember { mutableStateOf(false) }

    var rawDigits by remember { mutableStateOf("") }
    val displayAmount = formatCurrency(rawDigits)

    // Check NFC availability with reactive updates
    val context = LocalContext.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }

    var nfcStatus by remember {
        mutableStateOf(
            when {
                nfcAdapter == null -> NfcStatus.NOT_SUPPORTED
                nfcAdapter.isEnabled -> NfcStatus.READY
                else -> NfcStatus.DISABLED
            }
        )
    }

    // Listen for NFC state changes
    DisposableEffect(context) {
        if (nfcAdapter == null) {
            return@DisposableEffect onDispose {}
        }

        val nfcStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF
                    )
                    nfcStatus = when (state) {
                        NfcAdapter.STATE_ON -> NfcStatus.READY
                        NfcAdapter.STATE_OFF, NfcAdapter.STATE_TURNING_OFF -> NfcStatus.DISABLED
                        else -> nfcStatus
                    }
                }
            }
        }

        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        ContextCompat.registerReceiver(
            context,
            nfcStateReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(nfcStateReceiver)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 1. POS Terminal Top Bar: Identity & Quick Switch ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Merchant Name with NFC Status Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (nfcStatus) {
                                    NfcStatus.READY -> Color(0xFF00E676)
                                    NfcStatus.DISABLED -> Color(0xFFFFB300)
                                    NfcStatus.NOT_SUPPORTED -> Color(0xFFE53935)
                                }
                            )
                    )
                    Text(
                        text = terminalConfig.merchantName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Currency & Mode Switch Pill
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { showQuickSwitchSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${terminalConfig.currencyCode} · ${terminalConfig.transactionTypeDisplayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Quick Config",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // NFC Warning Banner if disabled
            if (nfcStatus != NfcStatus.READY) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (nfcStatus == NfcStatus.DISABLED) "NFC is turned off. Please enable NFC in Android Settings." else "NFC is not supported on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // ─── 2. POS Crisp Amount Display ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TOTAL SALE AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val amountFontSize = when {
                    displayAmount.length <= 6 -> 52.sp
                    displayAmount.length <= 9 -> 40.sp
                    else -> 32.sp
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = terminalConfig.currencySymbol,
                        style = MaterialTheme.typography.displayMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = (amountFontSize.value * 0.7f).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayAmount,
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = amountFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "TID: ${terminalConfig.ifdSerialNumber}  ·  TTQ: ${terminalConfig.formattedTtq}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.weight(0.8f))

            // ─── 3. POS Tactile Number Keypad ───
            Keypad(
                onNumberClick = { number ->
                    if (rawDigits.isEmpty() && number == 0) return@Keypad
                    if (rawDigits.length < 9) rawDigits += number.toString()
                },
                onClear = {
                    rawDigits = ""
                },
                onBackspace = {
                    rawDigits = rawDigits.dropLast(1)
                }
            )

            Spacer(modifier = Modifier.weight(0.4f))

            // ─── 4. POS Primary Action Button ───
            PrimaryButton(
                text = if (displayAmount == "0.00") "ENTER AMOUNT" else "CHARGE  ${terminalConfig.currencySymbol}$displayAmount",
                onClick = { onGeneratePayment(displayAmount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                isEnable = displayAmount != "0.00" && nfcStatus == NfcStatus.READY,
                leadingIcon = Icons.Default.Nfc
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (showQuickSwitchSheet) {
            TerminalQuickSwitchBottomSheet(
                terminalConfig = terminalConfig,
                onDismiss = { showQuickSwitchSheet = false },
                onCurrencySelected = { preset ->
                    settingsViewModel.updateCurrency(preset.code, preset.symbol, preset.exponent)
                },
                onTransactionTypeSelected = { typeCode ->
                    settingsViewModel.updateTransactionType(typeCode)
                },
                onOpenFullSettings = {
                    showQuickSwitchSheet = false
                    onOpenSettings()
                }
            )
        }
    }
}

