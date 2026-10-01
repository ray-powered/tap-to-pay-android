package com.yumedev.taptopayandroid.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.yumedev.taptopayandroid.domain.model.DetailLevel
import com.yumedev.taptopayandroid.domain.usecase.GetDetailLevelUseCase
import com.yumedev.taptopayandroid.domain.usecase.GetSoundEnabledUseCase
import com.yumedev.taptopayandroid.domain.usecase.GetThemeModeUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateDetailLevelUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateSoundEnabledUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getThemeModeUseCase: GetThemeModeUseCase,
    private val updateThemeModeUseCase: UpdateThemeModeUseCase,
    private val getSoundEnabledUseCase: GetSoundEnabledUseCase,
    private val updateSoundEnabledUseCase: UpdateSoundEnabledUseCase,
    private val getDetailLevelUseCase: GetDetailLevelUseCase,
    private val updateDetailLevelUseCase: UpdateDetailLevelUseCase,
    private val getTerminalConfigUseCase: com.yumedev.taptopayandroid.domain.usecase.GetTerminalConfigUseCase? = null,
    private val updateTerminalConfigUseCase: com.yumedev.taptopayandroid.domain.usecase.UpdateTerminalConfigUseCase? = null,
    private val resetTerminalConfigUseCase: com.yumedev.taptopayandroid.domain.usecase.ResetTerminalConfigUseCase? = null
) : ViewModel() {

    private val _themeMode = MutableStateFlow(getThemeModeUseCase())
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(getSoundEnabledUseCase())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _detailLevel = MutableStateFlow(getDetailLevelUseCase())
    val detailLevel: StateFlow<DetailLevel> = _detailLevel.asStateFlow()

    private val _terminalConfig = MutableStateFlow(
        getTerminalConfigUseCase?.invoke() ?: com.yumedev.taptopayandroid.domain.model.TerminalConfig()
    )
    val terminalConfig: StateFlow<com.yumedev.taptopayandroid.domain.model.TerminalConfig> = _terminalConfig.asStateFlow()

    fun updateThemeMode(mode: String) {
        updateThemeModeUseCase(mode)
        _themeMode.value = mode
    }

    fun updateSoundEnabled(enabled: Boolean) {
        updateSoundEnabledUseCase(enabled)
        _soundEnabled.value = enabled
    }

    fun updateDetailLevel(level: DetailLevel) {
        updateDetailLevelUseCase(level)
        _detailLevel.value = level
    }

    fun updateTerminalConfig(config: com.yumedev.taptopayandroid.domain.model.TerminalConfig) {
        updateTerminalConfigUseCase?.invoke(config)
        _terminalConfig.value = config
    }

    fun resetTerminalConfig() {
        resetTerminalConfigUseCase?.invoke()
        _terminalConfig.value = com.yumedev.taptopayandroid.domain.model.TerminalConfig()
    }

    fun updateCurrency(code: String, symbol: String, exponent: Int) {
        val updated = _terminalConfig.value.copy(
            currencyCode = code,
            currencySymbol = symbol,
            currencyExponent = exponent
        )
        updateTerminalConfig(updated)
    }

    fun updateCountry(code: String) {
        val updated = _terminalConfig.value.copy(countryCode = code)
        updateTerminalConfig(updated)
    }

    fun updateTransactionType(type: String) {
        val updated = _terminalConfig.value.copy(transactionType = type)
        updateTerminalConfig(updated)
    }

    fun updateTtq(ttqHex: String) {
        val updated = _terminalConfig.value.copy(ttqHex = ttqHex)
        updateTerminalConfig(updated)
    }

    fun updateTtqBit(byteIndex: Int, bitMask: Int, enabled: Boolean) {
        val updated = _terminalConfig.value.withTtqBit(byteIndex, bitMask, enabled)
        updateTerminalConfig(updated)
    }

    fun updateTerminalCapabilities(capabilitiesHex: String) {
        val updated = _terminalConfig.value.copy(terminalCapabilitiesHex = capabilitiesHex)
        updateTerminalConfig(updated)
    }

    fun updateTerminalType(typeHex: String) {
        val updated = _terminalConfig.value.copy(terminalTypeHex = typeHex)
        updateTerminalConfig(updated)
    }

    fun updateMerchantDetails(merchantName: String, ifdSerial: String, mcc: String) {
        val updated = _terminalConfig.value.copy(
            merchantName = merchantName,
            ifdSerialNumber = ifdSerial,
            merchantCategoryCode = mcc
        )
        updateTerminalConfig(updated)
    }

    fun updateLedColorMode(mode: com.yumedev.taptopayandroid.domain.model.PosLedColorMode) {
        val updated = _terminalConfig.value.copy(ledColorMode = mode)
        updateTerminalConfig(updated)
    }

    fun updateFloorLimit(limitCents: Long) {
        val updated = _terminalConfig.value.copy(floorLimit = limitCents)
        updateTerminalConfig(updated)
    }

    fun updateTvrMode(mode: com.yumedev.taptopayandroid.domain.model.TvrMode) {
        val updated = _terminalConfig.value.copy(tvrMode = mode)
        updateTerminalConfig(updated)
    }

    fun updateManualTvr(tvrHex: String) {
        val updated = _terminalConfig.value.copy(manualTvrHex = tvrHex)
        updateTerminalConfig(updated)
    }

    fun updateManualTvrBit(byteIndex: Int, bitMask: Int, enabled: Boolean) {
        val updated = _terminalConfig.value.withManualTvrBit(byteIndex, bitMask, enabled)
        updateTerminalConfig(updated)
    }

    fun updateGenAcMode(mode: com.yumedev.taptopayandroid.domain.model.GenAcRequestMode) {
        val updated = _terminalConfig.value.copy(genAcRequestMode = mode)
        updateTerminalConfig(updated)
    }

    fun updateStrictOnlineAuthDisplay(strict: Boolean) {
        val updated = _terminalConfig.value.copy(strictOnlineAuthDisplay = strict)
        updateTerminalConfig(updated)
    }
}
