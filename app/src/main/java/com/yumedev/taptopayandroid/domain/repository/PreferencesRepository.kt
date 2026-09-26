package com.yumedev.taptopayandroid.domain.repository

import com.yumedev.taptopayandroid.domain.model.DetailLevel

interface PreferencesRepository {

    fun getThemeMode(): String

    fun setThemeMode(mode: String)

    fun isSoundEnabled(): Boolean

    fun setSoundEnabled(enabled: Boolean)

    fun getDetailLevel(): DetailLevel

    fun setDetailLevel(level: DetailLevel)

    fun getTerminalConfig(): com.yumedev.taptopayandroid.domain.model.TerminalConfig

    fun setTerminalConfig(config: com.yumedev.taptopayandroid.domain.model.TerminalConfig)

    fun resetTerminalConfig()

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }
}
