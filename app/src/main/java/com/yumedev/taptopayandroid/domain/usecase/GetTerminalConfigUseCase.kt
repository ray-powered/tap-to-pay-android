package com.yumedev.taptopayandroid.domain.usecase

import com.yumedev.taptopayandroid.domain.model.TerminalConfig
import com.yumedev.taptopayandroid.domain.repository.PreferencesRepository
import javax.inject.Inject

class GetTerminalConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): TerminalConfig {
        return preferencesRepository.getTerminalConfig()
    }
}
