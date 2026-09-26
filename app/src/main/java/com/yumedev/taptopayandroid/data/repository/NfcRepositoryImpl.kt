package com.yumedev.taptopayandroid.data.repository

import android.nfc.Tag
import com.yumedev.taptopayandroid.data.datasource.nfc.NfcCardReader
import com.yumedev.taptopayandroid.domain.model.EmvCardData
import com.yumedev.taptopayandroid.domain.repository.NfcRepository

import com.yumedev.taptopayandroid.domain.model.TerminalConfig

class NfcRepositoryImpl(
    private val nfcCardReader: NfcCardReader
) : NfcRepository {

    override suspend fun readCard(
        tag: Tag,
        amountCents: Long?,
        terminalConfig: TerminalConfig?
    ): Result<EmvCardData> {
        return nfcCardReader.readCard(tag, amountCents, terminalConfig)
    }
}