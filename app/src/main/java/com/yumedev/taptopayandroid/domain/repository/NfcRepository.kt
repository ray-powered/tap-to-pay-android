package com.yumedev.taptopayandroid.domain.repository

import android.nfc.Tag
import com.yumedev.taptopayandroid.domain.model.EmvCardData

import com.yumedev.taptopayandroid.domain.model.TerminalConfig

interface NfcRepository {
    suspend fun readCard(
        tag: Tag,
        amountCents: Long? = null,
        terminalConfig: TerminalConfig? = null
    ): Result<EmvCardData>
}
