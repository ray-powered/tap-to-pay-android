package com.yumedev.taptopayandroid.data.repository

import com.yumedev.taptopayandroid.data.datasource.audio.SoundManager
import com.yumedev.taptopayandroid.domain.repository.AudioRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class AudioRepositoryImplTest {

    private lateinit var soundManager: SoundManager
    private lateinit var repository: AudioRepository

    @Before
    fun setup() {
        soundManager = mockk(relaxed = true)
        repository = AudioRepositoryImpl(soundManager)
    }

    @Test
    fun `playSuccess delegates to SoundManager playSuccess`() {
        repository.playSuccess()
        verify(exactly = 1) { soundManager.playSuccess() }
    }

    @Test
    fun `playError delegates to SoundManager playFailed`() {
        repository.playError()
        verify(exactly = 1) { soundManager.playFailed() }
    }

    @Test
    fun `multiple playSuccess calls are delegated correctly`() {
        repository.playSuccess()
        repository.playSuccess()
        repository.playSuccess()
        verify(exactly = 3) { soundManager.playSuccess() }
    }

    @Test
    fun `multiple playError calls are delegated correctly`() {
        repository.playError()
        repository.playError()
        verify(exactly = 2) { soundManager.playFailed() }
    }

    @Test
    fun `mixed success and error calls are delegated independently`() {
        repository.playSuccess()
        repository.playError()
        repository.playSuccess()
        repository.playError()
        repository.playError()

        verify(exactly = 2) { soundManager.playSuccess() }
        verify(exactly = 3) { soundManager.playFailed() }
    }

    @Test
    fun `playSuccess with brandTheme delegates to SoundManager playSuccess with brandTheme`() {
        repository.playSuccess(com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.MASTERCARD)
        verify(exactly = 1) { soundManager.playSuccess(com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.MASTERCARD) }
    }
}
