package com.yumedev.taptopayandroid.presentation.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.R
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
        // NFC Status Badge
        NfcStatusBadge(
            status = nfcStatus,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.4f))

            // Amount and Terminal Status Header (POS Display Panel)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SALE AMOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "TID: ${terminalConfig.ifdSerialNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val amountFontSize = when {
                        displayAmount.length <= 6 -> 52.sp
                        displayAmount.length <= 9 -> 40.sp
                        else -> 32.sp
                    }

                    Text(
                        text = "${terminalConfig.currencySymbol}$displayAmount",
                        style = MaterialTheme.typography.displayLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = amountFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Terminal Status Pill
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showQuickSwitchSheet = true },
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${terminalConfig.currencyCode}  ·  ${terminalConfig.transactionTypeDisplayName}  ·  TTQ: ${terminalConfig.formattedTtq}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Terminal Config",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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

            PrimaryButton(
                text = "ENTER / CONFIRM",
                onClick = { onGeneratePayment(displayAmount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                isEnable = displayAmount != "0.00",
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

@Composable
private fun NfcStatusBadge(
    status: NfcStatus,
    modifier: Modifier = Modifier
) {
    val text: String
    val icon: ImageVector
    val backgroundColor: Color
    val contentColor: Color

    when (status) {
        NfcStatus.READY -> {
            text = stringResource(id = R.string.nfc_ready)
            icon = Icons.Default.CheckCircle
            backgroundColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        }
        NfcStatus.DISABLED -> {
            text = stringResource(id = R.string.nfc_disabled)
            icon = Icons.Default.Error
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }
        NfcStatus.NOT_SUPPORTED -> {
            text = stringResource(id = R.string.nfc_not_supported)
            icon = Icons.Default.Error
            backgroundColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
