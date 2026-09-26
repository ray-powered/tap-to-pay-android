package com.yumedev.taptopayandroid.domain.usecase

import android.nfc.Tag
import com.yumedev.taptopayandroid.domain.model.EmvCardData
import com.yumedev.taptopayandroid.domain.repository.NfcRepository
import javax.inject.Inject

import com.yumedev.taptopayandroid.domain.model.TerminalConfig

class ReadCardUseCase @Inject constructor(
    private val nfcRepository: NfcRepository
) {
    suspend operator fun invoke(
        tag: Tag,
        amountCents: Long? = null,
        terminalConfig: TerminalConfig? = null
    ): Result<EmvCardData> {
        return nfcRepository.readCard(tag, amountCents, terminalConfig)
    }
}
