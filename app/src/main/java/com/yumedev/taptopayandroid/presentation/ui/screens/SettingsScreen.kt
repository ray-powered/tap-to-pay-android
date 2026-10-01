package com.yumedev.taptopayandroid.presentation.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.yumedev.taptopayandroid.domain.model.PosLedColorMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import com.yumedev.taptopayandroid.presentation.ui.components.*
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Splitscreen
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yumedev.taptopayandroid.R
import com.yumedev.taptopayandroid.data.preferences.PreferencesManager
import com.yumedev.taptopayandroid.domain.model.DetailLevel
import com.yumedev.taptopayandroid.presentation.ui.components.DetailLevelSelector
import com.yumedev.taptopayandroid.presentation.viewmodel.SettingsViewModel
import java.util.Calendar
import androidx.core.net.toUri

enum class ThemeOption {
    LIGHT, DARK, SYSTEM
}

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues = PaddingValues(),
    onThemeChanged: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionName = packageInfo.versionName
    val versionText = "$versionName"

    val themeMode by viewModel.themeMode.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val detailLevel by viewModel.detailLevel.collectAsState()
    val terminalConfig by viewModel.terminalConfig.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showTransactionTypeDialog by remember { mutableStateOf(false) }
    var showTtqDialog by remember { mutableStateOf(false) }
    var showCapabilitiesDialog by remember { mutableStateOf(false) }
    var showTerminalTypeDialog by remember { mutableStateOf(false) }
    var showMerchantDialog by remember { mutableStateOf(false) }
    var showTvrDialog by remember { mutableStateOf(false) }
    var showGenAcDialog by remember { mutableStateOf(false) }

    val selectedTheme = when (themeMode) {
        PreferencesManager.THEME_LIGHT -> ThemeOption.LIGHT
        PreferencesManager.THEME_DARK -> ThemeOption.DARK
        PreferencesManager.THEME_SYSTEM -> ThemeOption.SYSTEM
        else -> ThemeOption.SYSTEM
    }

    val selectedDetailLevel = detailLevel

    var rawLogsEnabled by remember { mutableStateOf(true) }

    // Dialogs for Terminal Configuration
    if (showCurrencyDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.CurrencySelectionDialog(
            currentConfig = terminalConfig,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = { code, symbol, exponent ->
                viewModel.updateCurrency(code, symbol, exponent)
                showCurrencyDialog = false
            }
        )
    }

    if (showCountryDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.CountrySelectionDialog(
            currentCountryCode = terminalConfig.countryCode,
            onDismiss = { showCountryDialog = false },
            onConfirm = { code ->
                viewModel.updateCountry(code)
                showCountryDialog = false
            }
        )
    }

    if (showTransactionTypeDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.TransactionTypeSelectionDialog(
            currentType = terminalConfig.transactionType,
            onDismiss = { showTransactionTypeDialog = false },
            onConfirm = { type ->
                viewModel.updateTransactionType(type)
                showTransactionTypeDialog = false
            }
        )
    }

    if (showTtqDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.TtqEditorDialog(
            currentConfig = terminalConfig,
            onDismiss = { showTtqDialog = false },
            onConfirm = { newTtq ->
                viewModel.updateTtq(newTtq)
                showTtqDialog = false
            }
        )
    }

    if (showCapabilitiesDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.TerminalCapabilitiesDialog(
            currentCapabilitiesHex = terminalConfig.terminalCapabilitiesHex,
            onDismiss = { showCapabilitiesDialog = false },
            onConfirm = { newHex ->
                viewModel.updateTerminalCapabilities(newHex)
                showCapabilitiesDialog = false
            }
        )
    }

    if (showTerminalTypeDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.TerminalTypeDialog(
            currentTypeHex = terminalConfig.terminalTypeHex,
            onDismiss = { showTerminalTypeDialog = false },
            onConfirm = { newHex ->
                viewModel.updateTerminalType(newHex)
                showTerminalTypeDialog = false
            }
        )
    }

    if (showMerchantDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.MerchantDetailsDialog(
            merchantName = terminalConfig.merchantName,
            ifdSerial = terminalConfig.ifdSerialNumber,
            mcc = terminalConfig.merchantCategoryCode,
            onDismiss = { showMerchantDialog = false },
            onConfirm = { name, ifd, mcc ->
                viewModel.updateMerchantDetails(name, ifd, mcc)
                showMerchantDialog = false
            }
        )
    }

    if (showTvrDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.TvrConfigurationDialog(
            currentConfig = terminalConfig,
            onDismiss = { showTvrDialog = false },
            onConfirm = { mode, manualTvr, floorLimit ->
                viewModel.updateTvrMode(mode)
                viewModel.updateManualTvr(manualTvr)
                viewModel.updateFloorLimit(floorLimit)
                showTvrDialog = false
            }
        )
    }

    if (showGenAcDialog) {
        com.yumedev.taptopayandroid.presentation.ui.components.GenAcPolicyDialog(
            currentMode = terminalConfig.genAcRequestMode,
            onDismiss = { showGenAcDialog = false },
            onConfirm = { newMode ->
                viewModel.updateGenAcMode(newMode)
                showGenAcDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // --- POS Terminal & EMV Transaction Settings Section ---
        item {
            SettingsSection(
                title = "EMV Terminal & Transaction Configuration"
            ) {
                SettingsItemNavigable(
                    icon = Icons.Outlined.Info,
                    title = "Transaction Currency",
                    subtitle = "Tag 5F2A: ${terminalConfig.currencyDisplayName}",
                    endText = "${terminalConfig.currencySymbol} (${terminalConfig.currencyCode})",
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Info,
                    title = "Terminal Country Code",
                    subtitle = "Tag 9F1A: ${terminalConfig.countryDisplayName}",
                    endText = terminalConfig.countryCode,
                    onClick = { showCountryDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Info,
                    title = "Transaction Type",
                    subtitle = "Tag 9C: ${terminalConfig.transactionTypeDisplayName}",
                    endText = terminalConfig.transactionType,
                    onClick = { showTransactionTypeDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Code,
                    title = "Terminal Transaction Qualifiers (TTQ)",
                    subtitle = "Tag 9F66: ${terminalConfig.formattedTtq} (Tap to configure)",
                    endText = terminalConfig.formattedTtq,
                    onClick = { showTtqDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Splitscreen,
                    title = "Terminal Capabilities",
                    subtitle = "Tag 9F33: ${terminalConfig.formattedTerminalCapabilities} · Chip / Magstripe / CVM",
                    endText = terminalConfig.formattedTerminalCapabilities,
                    onClick = { showCapabilitiesDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Terminal Type",
                    subtitle = "Tag 9F35: ${terminalConfig.terminalTypeHex} · Merchant terminal profile",
                    endText = terminalConfig.terminalTypeHex,
                    onClick = { showTerminalTypeDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Code,
                    title = "Terminal Verification Results (TVR)",
                    subtitle = if (terminalConfig.tvrMode == com.yumedev.taptopayandroid.domain.model.TvrMode.AUTOMATIC)
                        "Auto TRM · Floor Limit: ${terminalConfig.formattedFloorLimit}"
                    else
                        "Manual · Tag 95: ${terminalConfig.formattedManualTvr}",
                    endText = if (terminalConfig.tvrMode == com.yumedev.taptopayandroid.domain.model.TvrMode.AUTOMATIC) "Auto" else "Manual",
                    onClick = { showTvrDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Splitscreen,
                    title = "GENERATE AC Policy",
                    subtitle = "Request ${terminalConfig.genAcRequestMode.displayName}",
                    endText = terminalConfig.genAcRequestMode.name.substringAfter("FORCE_").ifEmpty { "Auto" },
                    onClick = { showGenAcDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Strict Online Auth Display",
                    subtitle = "Show 'Online Authorization Required' instead of 'Approved Online' when ARQC is returned"
                ) {
                    Switch(
                        checked = terminalConfig.strictOnlineAuthDisplay,
                        onCheckedChange = { viewModel.updateStrictOnlineAuthDisplay(it) }
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Info,
                    title = "Merchant & Hardware IFD",
                    subtitle = "${terminalConfig.merchantName} · MCC: ${terminalConfig.merchantCategoryCode}",
                    endText = "Edit",
                    onClick = { showMerchantDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Default.Refresh,
                    title = "Reset Terminal Defaults",
                    subtitle = "Reset to standard EMV contactless terminal (TTQ: 36204000, USD, Purchase)",
                    showChevron = false,
                    endText = "Reset",
                    onClick = { viewModel.resetTerminalConfig() }
                )
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.appearance_section)
            ) {
                SettingsItem(
                    icon = Icons.Outlined.BrightnessAuto,
                    title = stringResource(R.string.theme_title),
                    subtitle = stringResource(R.string.theme_subtitle)
                ) {
                    ThemeSelector(
                        selectedTheme = selectedTheme,
                        onThemeSelected = { newTheme ->
                            val newThemeMode = when (newTheme) {
                                ThemeOption.LIGHT -> PreferencesManager.THEME_LIGHT
                                ThemeOption.DARK -> PreferencesManager.THEME_DARK
                                ThemeOption.SYSTEM -> PreferencesManager.THEME_SYSTEM
                            }
                            viewModel.updateThemeMode(newThemeMode)
                            onThemeChanged(newThemeMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Contactless LED Indicator",
                    subtitle = "POS physical LED color and sequence standard"
                ) {
                    PosLedModeSelector(
                        selectedMode = terminalConfig.ledColorMode,
                        onModeSelected = { newMode ->
                            viewModel.updateLedColorMode(newMode)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.sound_vibration_section)
            ) {
                SettingsItemSwitch(
                    icon = Icons.Outlined.VolumeUp,
                    title = stringResource(R.string.sound_on_detect_title),
                    subtitle = stringResource(R.string.sound_on_detect_subtitle),
                    checked = soundEnabled,
                    onCheckedChange = {
                        viewModel.updateSoundEnabled(it)
                    }
                )
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.developer_mode_section)
            ) {
                SettingsItemSwitch(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.raw_nfc_logs_title),
                    subtitle = stringResource(R.string.raw_nfc_logs_subtitle),
                    checked = rawLogsEnabled,
                    onCheckedChange = { rawLogsEnabled = it }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItem(
                    icon = Icons.Outlined.Splitscreen,
                    title = stringResource(R.string.detail_level_title),
                    subtitle = stringResource(R.string.detail_level_subtitle)
                ) {
                    DetailLevelSelector(
                        selectedLevel = selectedDetailLevel,
                        onLevelSelected = { newLevel ->
                            viewModel.updateDetailLevel(newLevel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.about_section)
            ) {
                SettingsItemInfo(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.version_title),
                    subtitle = versionText
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.developer_title),
                    subtitle = stringResource(R.string.developer_subtitle),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://${context.getString(R.string.developer_github)}".toUri())
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SettingsItemNavigable(
                    icon = Icons.Outlined.OpenInNew,
                    title = stringResource(R.string.repository_title),
                    subtitle = stringResource(R.string.repository_url),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            "https://${context.getString(R.string.repository_url)}".toUri())
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 1.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        content()
    }
}

@Composable
private fun SettingsItemSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsItemNavigable(
    icon: ImageVector,
    title: String,
    subtitle: String,
    endText: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        if (endText != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    text = endText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = if (endText.any { it.isDigit() }) FontFamily.Monospace else null,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItemInfo(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeSelector(
    selectedTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            ThemeOption(
                text = stringResource(R.string.theme_light),
                icon = Icons.Outlined.BrightnessAuto,
                isSelected = selectedTheme == ThemeOption.LIGHT,
                onClick = { onThemeSelected(ThemeOption.LIGHT) },
                modifier = Modifier.weight(1f),
                cornerRadius = RoundedCornerShape(
                    topStart = 21.dp,
                    bottomStart = 21.dp,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp
                )
            )

            if (selectedTheme != ThemeOption.LIGHT) {
                androidx.compose.material3.VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            ThemeOption(
                text = stringResource(R.string.theme_dark),
                icon = Icons.Outlined.DarkMode,
                isSelected = selectedTheme == ThemeOption.DARK,
                onClick = { onThemeSelected(ThemeOption.DARK) },
                modifier = Modifier.weight(1f),
                cornerRadius = RoundedCornerShape(0.dp)
            )

            if (selectedTheme != ThemeOption.DARK) {
                androidx.compose.material3.VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            ThemeOption(
                text = stringResource(R.string.theme_system),
                icon = Icons.Outlined.PhoneAndroid,
                isSelected = selectedTheme == ThemeOption.SYSTEM,
                onClick = { onThemeSelected(ThemeOption.SYSTEM) },
                modifier = Modifier.weight(1f),
                cornerRadius = RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = 21.dp,
                    bottomEnd = 21.dp
                )
            )
        }
    }
}

@Composable
private fun ThemeOption(
    text: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: RoundedCornerShape
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = cornerRadius,
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null && isSelected) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PosLedModeSelector(
    selectedMode: PosLedColorMode,
    onModeSelected: (PosLedColorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val isEmv = selectedMode == PosLedColorMode.EMV_GREEN
        val isUpay = selectedMode == PosLedColorMode.UNIONPAY_COLOR

        // Option 1: EMV 4-Green
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onModeSelected(PosLedColorMode.EMV_GREEN) },
            shape = RoundedCornerShape(12.dp),
            color = if (isEmv) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                width = if (isEmv) 1.5.dp else 1.dp,
                color = if (isEmv) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4 Green Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val green = Color(0xFF00E676)
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(green)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "EMV 4-Green",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isEmv) FontWeight.Bold else FontWeight.Medium,
                        color = if (isEmv) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "EMV Book B Classic",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Option 2: UnionPay 4-Color
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onModeSelected(PosLedColorMode.UNIONPAY_COLOR) },
            shape = RoundedCornerShape(12.dp),
            color = if (isUpay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                width = if (isUpay) 1.5.dp else 1.dp,
                color = if (isUpay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4 Color Dots (Blue, Yellow, Green, Red)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors = listOf(Color(0xFF2979FF), Color(0xFFFFB300), Color(0xFF00E676), Color(0xFFE53935))
                    colors.forEach { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "UnionPay 4-Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isUpay) FontWeight.Bold else FontWeight.Medium,
                        color = if (isUpay) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PBOC / China UnionPay",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
