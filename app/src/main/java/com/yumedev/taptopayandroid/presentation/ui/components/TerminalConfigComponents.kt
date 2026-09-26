package com.yumedev.taptopayandroid.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumedev.taptopayandroid.domain.model.CurrencyPreset
import com.yumedev.taptopayandroid.domain.model.TerminalConfig

/**
 * Currency Selection Dialog
 */
@Composable
fun CurrencySelectionDialog(
    currentConfig: TerminalConfig,
    onDismiss: () -> Unit,
    onConfirm: (code: String, symbol: String, exponent: Int) -> Unit
) {
    var selectedCode by remember { mutableStateOf(currentConfig.currencyCode) }
    var selectedSymbol by remember { mutableStateOf(currentConfig.currencySymbol) }
    var selectedExponent by remember { mutableIntStateOf(currentConfig.currencyExponent) }
    var isCustom by remember {
        mutableStateOf(TerminalConfig.Currencies.none { it.code == currentConfig.currencyCode })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择交易币种 / Currency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.Currencies.forEach { preset ->
                    val isSelected = !isCustom && selectedCode == preset.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustom = false
                                selectedCode = preset.code
                                selectedSymbol = preset.symbol
                                selectedExponent = preset.exponent
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    isCustom = false
                                    selectedCode = preset.code
                                    selectedSymbol = preset.symbol
                                    selectedExponent = preset.exponent
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Tag 5F2A: ${preset.code} · 符号: ${preset.symbol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom currency option
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isCustom,
                                onClick = { isCustom = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "自定义币种代码 / Custom",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isCustom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = selectedCode,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) selectedCode = it },
                                label = { Text("ISO 4217 代码 (如 0840)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = selectedSymbol,
                                onValueChange = { selectedSymbol = it },
                                label = { Text("显示符号 (如 $, ¥, €)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCode.padStart(4, '0'), selectedSymbol, selectedExponent)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Country Selection Dialog
 */
@Composable
fun CountrySelectionDialog(
    currentCountryCode: String,
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var selectedCode by remember { mutableStateOf(currentCountryCode) }
    var isCustom by remember {
        mutableStateOf(TerminalConfig.Countries.none { it.code == currentCountryCode })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择终端国家代码 / Country (Tag 9F1A)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.Countries.forEach { preset ->
                    val isSelected = !isCustom && selectedCode == preset.code
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustom = false
                                selectedCode = preset.code
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    isCustom = false
                                    selectedCode = preset.code
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "Tag 9F1A: ${preset.code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom country
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isCustom,
                                onClick = { isCustom = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "自定义国家代码 / Custom Country",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isCustom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = selectedCode,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) selectedCode = it },
                                label = { Text("ISO 3166-1 数值代码 (如 0840, 0156)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCode.padStart(4, '0'))
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Transaction Type Selection Dialog (Tag 9C)
 */
@Composable
fun TransactionTypeSelectionDialog(
    currentType: String,
    onDismiss: () -> Unit,
    onConfirm: (code: String) -> Unit
) {
    var selectedCode by remember { mutableStateOf(currentType) }
    var isCustom by remember {
        mutableStateOf(TerminalConfig.TransactionTypes.none { it.code.equals(currentType, ignoreCase = true) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择交易类型 / Transaction Type (Tag 9C)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.TransactionTypes.forEach { preset ->
                    val isSelected = !isCustom && selectedCode.equals(preset.code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isCustom = false
                                selectedCode = preset.code
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    isCustom = false
                                    selectedCode = preset.code
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom type
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustom = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isCustom,
                                onClick = { isCustom = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "自定义类型代码 / Custom Hex",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        if (isCustom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = selectedCode,
                                onValueChange = { if (it.length <= 2) selectedCode = it.uppercase() },
                                label = { Text("1 字节 HEX 码 (如 00, 01, 20)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCode.padStart(2, '0').uppercase())
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Terminal Transaction Qualifiers (TTQ - Tag 9F66) Editor Dialog
 */
@Composable
fun TtqEditorDialog(
    currentConfig: TerminalConfig,
    onDismiss: () -> Unit,
    onConfirm: (newTtqHex: String) -> Unit
) {
    var tempConfig by remember { mutableStateOf(currentConfig) }
    var directHexInput by remember { mutableStateOf(currentConfig.ttqHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "终端交易限定符 (TTQ - 9F66)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "当前值: ${tempConfig.formattedTtq}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Presets
                Text(
                    text = "快速预设 (Presets)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TerminalConfig.TtqPresets.forEach { preset ->
                    val isSelected = tempConfig.ttqHex.equals(preset.hex, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempConfig = tempConfig.copy(ttqHex = preset.hex)
                                directHexInput = preset.hex
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = preset.hex.chunked(2).joinToString(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Direct Hex Input
                OutlinedTextField(
                    value = directHexInput,
                    onValueChange = { input ->
                        val clean = input.filter { it.isLetterOrDigit() }.take(8).uppercase()
                        directHexInput = clean
                        if (clean.length == 8) {
                            tempConfig = tempConfig.copy(ttqHex = clean)
                        }
                    },
                    label = { Text("直接编辑 HEX (4 字节 / 8 字符)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Bitwise switch toggles
                Text(
                    text = "独立 Bit 位开关控制 (Bitwise Flags)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TtqSwitchItem(
                    title = "非接触式 EMV 模式 (qVSDC)",
                    subtitle = "Byte 1 Bit 7 (0x40) · 标准芯片非接交易",
                    checked = tempConfig.ttqEmvSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(0, 0x40, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "非接触式磁条模式 (MSD)",
                    subtitle = "Byte 1 Bit 8 (0x80) · 向后兼容磁条仿真",
                    checked = tempConfig.ttqMagStripeSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(0, 0x80, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "要求联机密文 (ARQC Required)",
                    subtitle = "Byte 2 Bit 8 (0x80) · 必须生成联机交易授权请求",
                    checked = tempConfig.ttqOnlineCryptogramRequired,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(1, 0x80, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "支持移动设备持卡人验证 (Mobile CVM)",
                    subtitle = "Byte 3 Bit 7 (0x40) · Apple Pay / Google Pay / 指纹面容",
                    checked = tempConfig.ttqMobileCvmSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(2, 0x40, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "支持联机 PIN 验证 (Online PIN)",
                    subtitle = "Byte 1 Bit 5 (0x10) · 密码键盘联机核密",
                    checked = tempConfig.ttqOnlinePinSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(0, 0x10, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "支持纸质签名验证 (Signature)",
                    subtitle = "Byte 1 Bit 4 (0x08) · 传统凭单小票签名",
                    checked = tempConfig.ttqSignatureSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(0, 0x08, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "要求持卡人验证 (CVM Required)",
                    subtitle = "Byte 2 Bit 7 (0x40) · 要求 PIN 或签名",
                    checked = tempConfig.ttqCvmRequired,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(1, 0x40, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )

                TtqSwitchItem(
                    title = "支持发卡行脚本更新 (Issuer Update)",
                    subtitle = "Byte 3 Bit 8 (0x80) · 联机响应写卡处理",
                    checked = tempConfig.ttqIssuerUpdateSupported,
                    onCheckedChange = {
                        tempConfig = tempConfig.withTtqBit(2, 0x80, it)
                        directHexInput = tempConfig.ttqHex
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = directHexInput.padEnd(8, '0').take(8).uppercase()
                    onConfirm(finalHex)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun TtqSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Terminal Capabilities Dialog (Tag 9F33)
 */
@Composable
fun TerminalCapabilitiesDialog(
    currentCapabilitiesHex: String,
    onDismiss: () -> Unit,
    onConfirm: (newHex: String) -> Unit
) {
    var selectedHex by remember { mutableStateOf(currentCapabilitiesHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "终端能力 (Tag 9F33 - 3 字节)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "快速预设 (Presets)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                TerminalConfig.CapabilitiesPresets.forEach { preset ->
                    val isSelected = selectedHex.equals(preset.hex, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHex = preset.hex },
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = preset.hex.chunked(2).joinToString(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = selectedHex,
                    onValueChange = { if (it.length <= 6) selectedHex = it.uppercase() },
                    label = { Text("自定义 HEX (3 字节 / 6 字符，如 E0F8C8)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedHex.padEnd(6, '0').take(6).uppercase()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Terminal Type Dialog (Tag 9F35)
 */
@Composable
fun TerminalTypeDialog(
    currentTypeHex: String,
    onDismiss: () -> Unit,
    onConfirm: (newHex: String) -> Unit
) {
    var selectedHex by remember { mutableStateOf(currentTypeHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "终端类型 (Tag 9F35 - 1 字节)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.TerminalTypePresets.forEach { preset ->
                    val isSelected = selectedHex.equals(preset.code, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedHex = preset.code },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedHex = preset.code }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = selectedHex,
                    onValueChange = { if (it.length <= 2) selectedHex = it.uppercase() },
                    label = { Text("自定义 1 字节 HEX (如 22, 21, 35)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedHex.padStart(2, '0').take(2).uppercase()) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Merchant & Terminal Details Dialog
 */
@Composable
fun MerchantDetailsDialog(
    merchantName: String,
    ifdSerial: String,
    mcc: String,
    onDismiss: () -> Unit,
    onConfirm: (merchantName: String, ifdSerial: String, mcc: String) -> Unit
) {
    var tempMerchantName by remember { mutableStateOf(merchantName) }
    var tempIfdSerial by remember { mutableStateOf(ifdSerial) }
    var tempMcc by remember { mutableStateOf(mcc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "商户与硬件终端信息",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tempMerchantName,
                    onValueChange = { tempMerchantName = it.take(30) },
                    label = { Text("商户名称与地址 (Tag 9F4E)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tempIfdSerial,
                    onValueChange = { tempIfdSerial = it.take(8) },
                    label = { Text("终端硬件序列号 (Tag 9F1E - 8位)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tempMcc,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) tempMcc = it },
                    label = { Text("商户类别码 MCC (Tag 9F15 - 4位，如 5411)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempMerchantName, tempIfdSerial, tempMcc) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * Quick Switch Bottom Sheet for Home Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalQuickSwitchBottomSheet(
    terminalConfig: TerminalConfig,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyPreset) -> Unit,
    onTransactionTypeSelected: (String) -> Unit,
    onOpenFullSettings: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "快速切换 POS 交易参数",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    onDismiss()
                    onOpenFullSettings()
                }) {
                    Text("更多设置")
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Quick Currency Selection
            Text(
                text = "交易币种 (Currency)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.Currencies.take(4).forEach { currency ->
                    val isSelected = terminalConfig.currencyCode == currency.code
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCurrencySelected(currency) },
                        label = { Text("${currency.symbol} ${currency.code}") }
                    )
                }
            }

            // Quick Transaction Type Selection
            Text(
                text = "交易类型 (Transaction Type)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TerminalConfig.TransactionTypes.take(4).forEach { type ->
                    val isSelected = terminalConfig.transactionType.equals(type.code, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTransactionTypeSelected(type.code) },
                        label = { Text(type.name.split(" - ").getOrNull(1)?.split(" / ")?.getOrNull(0) ?: type.code) }
                    )
                }
            }

            // Status Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前 POS 终端属性状态",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TTQ (9F66): ${terminalConfig.formattedTtq}  |  终端能力 (9F33): ${terminalConfig.formattedTerminalCapabilities}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "国家代码: ${terminalConfig.countryCode}  |  终端类型: ${terminalConfig.terminalTypeHex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
