package com.yumedev.taptopayandroid.domain.usecase

import com.yumedev.taptopayandroid.domain.model.TerminalConfig
import com.yumedev.taptopayandroid.domain.repository.PreferencesRepository
import javax.inject.Inject

class UpdateTerminalConfigUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(config: TerminalConfig) {
        preferencesRepository.setTerminalConfig(config)
    }
}
