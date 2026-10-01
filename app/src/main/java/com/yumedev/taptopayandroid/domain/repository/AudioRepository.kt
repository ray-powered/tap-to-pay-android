package com.yumedev.taptopayandroid.domain.repository

import com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme

interface AudioRepository {

    fun playSuccess()

    fun playSuccess(brandTheme: BrandSuccessTheme)

    fun playError()
}
