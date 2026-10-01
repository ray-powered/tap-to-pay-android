package com.yumedev.taptopayandroid.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.yumedev.taptopayandroid.domain.model.DetailLevel
import com.yumedev.taptopayandroid.domain.model.GenAcRequestMode
import com.yumedev.taptopayandroid.domain.model.TvrMode
import com.yumedev.taptopayandroid.domain.repository.PreferencesRepository
import com.yumedev.taptopayandroid.domain.usecase.GetDetailLevelUseCase
import com.yumedev.taptopayandroid.domain.usecase.GetSoundEnabledUseCase
import com.yumedev.taptopayandroid.domain.usecase.GetThemeModeUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateDetailLevelUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateSoundEnabledUseCase
import com.yumedev.taptopayandroid.domain.usecase.UpdateThemeModeUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getThemeModeUseCase: GetThemeModeUseCase
    private lateinit var updateThemeModeUseCase: UpdateThemeModeUseCase
    private lateinit var getSoundEnabledUseCase: GetSoundEnabledUseCase
    private lateinit var updateSoundEnabledUseCase: UpdateSoundEnabledUseCase
    private lateinit var getDetailLevelUseCase: GetDetailLevelUseCase
    private lateinit var updateDetailLevelUseCase: UpdateDetailLevelUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getThemeModeUseCase = mockk()
        updateThemeModeUseCase = mockk(relaxed = true)
        getSoundEnabledUseCase = mockk()
        updateSoundEnabledUseCase = mockk(relaxed = true)
        getDetailLevelUseCase = mockk()
        updateDetailLevelUseCase = mockk(relaxed = true)

        every { getThemeModeUseCase() } returns PreferencesRepository.THEME_SYSTEM
        every { getSoundEnabledUseCase() } returns true
        every { getDetailLevelUseCase() } returns DetailLevel.DETAILED

        viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial theme mode is loaded from use case`() {
        every { getThemeModeUseCase() } returns PreferencesRepository.THEME_DARK

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_DARK)
        verify(atLeast = 1) { getThemeModeUseCase() }
    }

    @Test
    fun `updateThemeMode updates state to LIGHT`() {
        viewModel.updateThemeMode(PreferencesRepository.THEME_LIGHT)

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_LIGHT)
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_LIGHT) }
    }

    @Test
    fun `updateThemeMode updates state to DARK`() {
        viewModel.updateThemeMode(PreferencesRepository.THEME_DARK)

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_DARK)
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_DARK) }
    }

    @Test
    fun `updateThemeMode updates state to SYSTEM`() {
        viewModel.updateThemeMode(PreferencesRepository.THEME_SYSTEM)

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_SYSTEM)
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_SYSTEM) }
    }

    @Test
    fun `multiple theme mode changes are reflected correctly`() {
        viewModel.updateThemeMode(PreferencesRepository.THEME_DARK)
        viewModel.updateThemeMode(PreferencesRepository.THEME_LIGHT)
        viewModel.updateThemeMode(PreferencesRepository.THEME_SYSTEM)

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_SYSTEM)
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_DARK) }
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_LIGHT) }
        verify(exactly = 1) { updateThemeModeUseCase(PreferencesRepository.THEME_SYSTEM) }
    }

    @Test
    fun `initial sound enabled is loaded from use case as true`() {
        every { getSoundEnabledUseCase() } returns true

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.soundEnabled.value).isTrue()
        verify(atLeast = 1) { getSoundEnabledUseCase() }
    }

    @Test
    fun `initial sound enabled is loaded from use case as false`() {
        every { getSoundEnabledUseCase() } returns false

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.soundEnabled.value).isFalse()
    }

    @Test
    fun `updateSoundEnabled enables sound`() {
        viewModel.updateSoundEnabled(true)

        assertThat(viewModel.soundEnabled.value).isTrue()
        verify(exactly = 1) { updateSoundEnabledUseCase(true) }
    }

    @Test
    fun `updateSoundEnabled disables sound`() {
        viewModel.updateSoundEnabled(false)

        assertThat(viewModel.soundEnabled.value).isFalse()
        verify(exactly = 1) { updateSoundEnabledUseCase(false) }
    }

    @Test
    fun `toggling sound on and off works correctly`() {
        viewModel.updateSoundEnabled(false)
        viewModel.updateSoundEnabled(true)
        viewModel.updateSoundEnabled(false)

        assertThat(viewModel.soundEnabled.value).isFalse()
        verify(exactly = 2) { updateSoundEnabledUseCase(false) }
        verify(exactly = 1) { updateSoundEnabledUseCase(true) }
    }

    @Test
    fun `initial detail level is loaded from use case as SIMPLE`() {
        every { getDetailLevelUseCase() } returns DetailLevel.SIMPLE

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.SIMPLE)
        verify(atLeast = 1) { getDetailLevelUseCase() }
    }

    @Test
    fun `initial detail level is loaded from use case as DETAILED`() {
        every { getDetailLevelUseCase() } returns DetailLevel.DETAILED

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.DETAILED)
    }

    @Test
    fun `updateDetailLevel changes to SIMPLE`() {
        viewModel.updateDetailLevel(DetailLevel.SIMPLE)

        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.SIMPLE)
        verify(exactly = 1) { updateDetailLevelUseCase(DetailLevel.SIMPLE) }
    }

    @Test
    fun `updateDetailLevel changes to DETAILED`() {
        viewModel.updateDetailLevel(DetailLevel.DETAILED)

        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.DETAILED)
        verify(exactly = 1) { updateDetailLevelUseCase(DetailLevel.DETAILED) }
    }

    @Test
    fun `switching between detail levels works correctly`() {
        viewModel.updateDetailLevel(DetailLevel.SIMPLE)
        viewModel.updateDetailLevel(DetailLevel.DETAILED)
        viewModel.updateDetailLevel(DetailLevel.SIMPLE)

        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.SIMPLE)
        verify(exactly = 2) { updateDetailLevelUseCase(DetailLevel.SIMPLE) }
        verify(exactly = 1) { updateDetailLevelUseCase(DetailLevel.DETAILED) }
    }

    @Test
    fun `all settings can be updated independently`() {
        viewModel.updateThemeMode(PreferencesRepository.THEME_DARK)
        viewModel.updateSoundEnabled(false)
        viewModel.updateDetailLevel(DetailLevel.SIMPLE)

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_DARK)
        assertThat(viewModel.soundEnabled.value).isFalse()
        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.SIMPLE)
    }

    @Test
    fun `initial state reflects all use case values`() {
        every { getThemeModeUseCase() } returns PreferencesRepository.THEME_LIGHT
        every { getSoundEnabledUseCase() } returns false
        every { getDetailLevelUseCase() } returns DetailLevel.SIMPLE

        val viewModel = SettingsViewModel(
            getThemeModeUseCase,
            updateThemeModeUseCase,
            getSoundEnabledUseCase,
            updateSoundEnabledUseCase,
            getDetailLevelUseCase,
            updateDetailLevelUseCase
        )

        assertThat(viewModel.themeMode.value).isEqualTo(PreferencesRepository.THEME_LIGHT)
        assertThat(viewModel.soundEnabled.value).isFalse()
        assertThat(viewModel.detailLevel.value).isEqualTo(DetailLevel.SIMPLE)
    }

    @Test
    fun `updateCurrency updates terminal config state`() {
        viewModel.updateCurrency("0156", "¥", 2)

        assertThat(viewModel.terminalConfig.value.currencyCode).isEqualTo("0156")
        assertThat(viewModel.terminalConfig.value.currencySymbol).isEqualTo("¥")
        assertThat(viewModel.terminalConfig.value.currencyExponent).isEqualTo(2)
    }

    @Test
    fun `updateTransactionType updates transaction type code`() {
        viewModel.updateTransactionType("20")

        assertThat(viewModel.terminalConfig.value.transactionType).isEqualTo("20")
    }

    @Test
    fun `updateTtq updates TTQ hex`() {
        viewModel.updateTtq("26204000")

        assertThat(viewModel.terminalConfig.value.ttqHex).isEqualTo("26204000")
    }

    @Test
    fun `resetTerminalConfig restores default terminal config`() {
        viewModel.updateCurrency("0156", "¥", 2)
        viewModel.updateTransactionType("20")
        viewModel.resetTerminalConfig()

        assertThat(viewModel.terminalConfig.value.currencyCode).isEqualTo("0840")
        assertThat(viewModel.terminalConfig.value.transactionType).isEqualTo("00")
    }

    @Test
    fun `updateFloorLimit updates floor limit`() {
        viewModel.updateFloorLimit(25000L)
        assertThat(viewModel.terminalConfig.value.floorLimit).isEqualTo(25000L)
    }

    @Test
    fun `updateTvrMode updates tvr mode`() {
        viewModel.updateTvrMode(TvrMode.MANUAL)
        assertThat(viewModel.terminalConfig.value.tvrMode).isEqualTo(TvrMode.MANUAL)
    }

    @Test
    fun `updateManualTvr updates manual tvr hex`() {
        viewModel.updateManualTvr("8000408000")
        assertThat(viewModel.terminalConfig.value.manualTvrHex).isEqualTo("8000408000")
    }

    @Test
    fun `updateManualTvrBit toggles manual TVR bits`() {
        viewModel.updateManualTvr("0000000000")
        viewModel.updateManualTvrBit(3, 0x80, true)
        assertThat(viewModel.terminalConfig.value.manualTvrHex).isEqualTo("0000008000")

        viewModel.updateManualTvrBit(3, 0x80, false)
        assertThat(viewModel.terminalConfig.value.manualTvrHex).isEqualTo("0000000000")
    }

    @Test
    fun `updateGenAcMode updates GenAC mode and synchronizes TTQ`() {
        viewModel.updateGenAcMode(GenAcRequestMode.FORCE_ARQC)
        assertThat(viewModel.terminalConfig.value.genAcRequestMode).isEqualTo(GenAcRequestMode.FORCE_ARQC)
        assertThat(viewModel.terminalConfig.value.ttqOnlineCryptogramRequired).isTrue()
        assertThat(viewModel.terminalConfig.value.ttqReaderOfflineOnly).isFalse()

        viewModel.updateGenAcMode(GenAcRequestMode.FORCE_TC)
        assertThat(viewModel.terminalConfig.value.genAcRequestMode).isEqualTo(GenAcRequestMode.FORCE_TC)
        assertThat(viewModel.terminalConfig.value.ttqReaderOfflineOnly).isTrue()
        assertThat(viewModel.terminalConfig.value.ttqOnlineCryptogramRequired).isFalse()
    }

    @Test
    fun `updateStrictOnlineAuthDisplay updates strict mode boolean`() {
        viewModel.updateStrictOnlineAuthDisplay(true)
        assertThat(viewModel.terminalConfig.value.strictOnlineAuthDisplay).isTrue()
    }
}
