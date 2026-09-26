package com.yumedev.taptopayandroid.presentation.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yumedev.taptopayandroid.domain.model.EmvCardData
import com.yumedev.taptopayandroid.domain.model.NfcState
import com.yumedev.taptopayandroid.domain.repository.NfcEventRepository
import com.yumedev.taptopayandroid.domain.usecase.PlayFailedSoundUseCase
import com.yumedev.taptopayandroid.domain.usecase.PlaySuccessSoundUseCase
import com.yumedev.taptopayandroid.domain.usecase.ReadCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.yumedev.taptopayandroid.data.datasource.nfc.NfcCardReader
import com.yumedev.taptopayandroid.domain.model.CvmRequirement
import com.yumedev.taptopayandroid.domain.model.TerminalConfig
import com.yumedev.taptopayandroid.domain.model.TransactionDecision
import com.yumedev.taptopayandroid.domain.usecase.GetTerminalConfigUseCase

@HiltViewModel
class TapToPayViewModel @Inject constructor(
    private val readCardUseCase: ReadCardUseCase,
    private val playSuccessSoundUseCase: PlaySuccessSoundUseCase,
    private val playFailedSoundUseCase: PlayFailedSoundUseCase,
    private val nfcEventRepository: NfcEventRepository,
    private val getTerminalConfigUseCase: GetTerminalConfigUseCase? = null
) : ViewModel() {

    private val _nfcState = MutableStateFlow<NfcState>(NfcState.Waiting)
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()

    // Store last successful card data for detail screen
    private val _lastEmvCardData = MutableStateFlow<EmvCardData?>(null)
    val lastEmvCardData: StateFlow<EmvCardData?> = _lastEmvCardData.asStateFlow()

    // Store last transaction amount
    private val _lastAmount = MutableStateFlow("0.00")
    val lastAmount: StateFlow<String> = _lastAmount.asStateFlow()

    // Store current terminal configuration
    private val _terminalConfig = MutableStateFlow(
        getTerminalConfigUseCase?.invoke() ?: TerminalConfig()
    )
    val terminalConfig: StateFlow<TerminalConfig> = _terminalConfig.asStateFlow()

    // APDU Transmission Tracking for POS LED 2 blinking & Live APDU UI
    private val _currentApduCommand = MutableStateFlow<String?>(null)
    val currentApduCommand: StateFlow<String?> = _currentApduCommand.asStateFlow()

    private val _apduPulseCount = MutableStateFlow(0)
    val apduPulseCount: StateFlow<Int> = _apduPulseCount.asStateFlow()

    init {
        viewModelScope.launch {
            nfcEventRepository.nfcTagFlow.collect { tag ->
                processNfcTag(tag)
            }
        }
        viewModelScope.launch {
            NfcCardReader.apduEventFlow.collect { apduName ->
                _currentApduCommand.value = apduName
                _apduPulseCount.value += 1
            }
        }
    }

    private fun parseAmountToCents(amount: String): Long {
        val clean = amount.filter { it.isDigit() || it == '.' }.trim()
        val parts = clean.split(".")
        val dollars = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val centsPart = parts.getOrNull(1) ?: "00"
        val cents = (centsPart + "00").take(2).toLongOrNull() ?: 0L
        return (dollars * 100 + cents).coerceAtLeast(0L)
    }

    private fun processNfcTag(tag: Tag) {
        viewModelScope.launch {
            _currentApduCommand.value = null
            _apduPulseCount.value = 0
            _nfcState.value = NfcState.Reading
            val amountCents = parseAmountToCents(_lastAmount.value)
            val currentConfig = getTerminalConfigUseCase?.invoke() ?: _terminalConfig.value
            _terminalConfig.value = currentConfig
            val result = readCardUseCase(tag, amountCents, currentConfig)

            _currentApduCommand.value = null
            _nfcState.value = result.fold(
                onSuccess = { emvCardData ->
                    _lastEmvCardData.value = emvCardData
                    val isSeePhone = emvCardData.transactionAnalysis.requiresScreenCheck ||
                        emvCardData.transactionAnalysis.decision == TransactionDecision.SEE_PHONE_CDCVM ||
                        emvCardData.transactionAnalysis.cvmRequirement == CvmRequirement.CONSUMER_DEVICE_CVM_REQUIRED

                    if (isSeePhone) {
                        playFailedSoundUseCase()
                        NfcState.SeePhone(
                            instructions = emvCardData.transactionAnalysis.screenCheckInstructions
                                ?: "Customer must authenticate on device (Face ID / Fingerprint / Passcode), then tap again.",
                            lastData = emvCardData
                        )
                    } else {
                        playSuccessSoundUseCase()
                        NfcState.Success(emvCardData)
                    }
                },
                onFailure = { exception ->
                    val errorMsg = exception.message ?: "Unknown error reading card"
                    if (errorMsg.contains("69 86") || errorMsg.contains("See Phone", ignoreCase = true)) {
                        playFailedSoundUseCase()
                        NfcState.SeePhone(
                            instructions = "Card or device requires on-device authentication. Please unlock your phone and tap again."
                        )
                    } else {
                        playFailedSoundUseCase()
                        NfcState.Error(errorMsg)
                    }
                }
            )
        }
    }

    fun clearStateOnly() {
        _nfcState.value = NfcState.Waiting
        _currentApduCommand.value = null
        _apduPulseCount.value = 0
    }

    fun startNewTransaction(amount: String) {
        _nfcState.value = NfcState.Waiting
        _currentApduCommand.value = null
        _apduPulseCount.value = 0
        _lastAmount.value = amount
        getTerminalConfigUseCase?.invoke()?.let {
            _terminalConfig.value = it
        }
    }
}
