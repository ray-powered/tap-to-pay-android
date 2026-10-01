package com.yumedev.taptopayandroid.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "tap_to_pay_preferences"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DETAIL_LEVEL = "detail_level"

        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"

        const val DETAIL_LEVEL_SIMPLE = "simple"
        const val DETAIL_LEVEL_DETAILED = "detailed"

        // Terminal Configuration Keys
        private const val KEY_CURRENCY_CODE = "terminal_currency_code"
        private const val KEY_CURRENCY_SYMBOL = "terminal_currency_symbol"
        private const val KEY_CURRENCY_EXPONENT = "terminal_currency_exponent"
        private const val KEY_COUNTRY_CODE = "terminal_country_code"
        private const val KEY_TRANSACTION_TYPE = "terminal_transaction_type"
        private const val KEY_TERMINAL_TTQ = "terminal_ttq"
        private const val KEY_TERMINAL_CAPABILITIES = "terminal_capabilities"
        private const val KEY_TERMINAL_TYPE = "terminal_type"
        private const val KEY_MERCHANT_NAME = "terminal_merchant_name"
        private const val KEY_IFD_SERIAL_NUMBER = "terminal_ifd_serial_number"
        private const val KEY_MERCHANT_CATEGORY_CODE = "terminal_mcc"
        private const val KEY_ADDITIONAL_TERMINAL_CAPABILITIES = "terminal_additional_capabilities"
        private const val KEY_LED_COLOR_MODE = "terminal_led_color_mode"
        private const val KEY_FLOOR_LIMIT = "terminal_floor_limit"
        private const val KEY_TVR_MODE = "terminal_tvr_mode"
        private const val KEY_MANUAL_TVR = "terminal_manual_tvr"
        private const val KEY_GEN_AC_MODE = "terminal_gen_ac_mode"
        private const val KEY_STRICT_ONLINE_AUTH_DISPLAY = "terminal_strict_online_auth_display"
        private const val KEY_BRAND_SUCCESS_THEME = "terminal_brand_success_theme"

        @Volatile
        private var instance: PreferencesManager? = null

        @Deprecated("Use Hilt injection instead", ReplaceWith("Inject PreferencesManager via constructor"))
        fun getInstance(context: Context): PreferencesManager {
            Log.w(TAG, "Using deprecated getInstance(). Please migrate to Hilt injection.")
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // Sound preferences
    var isSoundEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = sharedPreferences.edit { putBoolean(KEY_SOUND_ENABLED, value) }

    // Theme preferences
    var themeMode: String
        get() = sharedPreferences.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = sharedPreferences.edit { putString(KEY_THEME_MODE, value) }

    // Detail level preferences
    var detailLevel: String
        get() = sharedPreferences.getString(KEY_DETAIL_LEVEL, DETAIL_LEVEL_DETAILED) ?: DETAIL_LEVEL_DETAILED
        set(value) = sharedPreferences.edit { putString(KEY_DETAIL_LEVEL, value) }

    // Terminal Configuration
    fun getTerminalConfig(): com.yumedev.taptopayandroid.domain.model.TerminalConfig {
        return com.yumedev.taptopayandroid.domain.model.TerminalConfig(
            currencyCode = sharedPreferences.getString(KEY_CURRENCY_CODE, "0840") ?: "0840",
            currencySymbol = sharedPreferences.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$",
            currencyExponent = sharedPreferences.getInt(KEY_CURRENCY_EXPONENT, 2),
            countryCode = sharedPreferences.getString(KEY_COUNTRY_CODE, "0840") ?: "0840",
            transactionType = sharedPreferences.getString(KEY_TRANSACTION_TYPE, "00") ?: "00",
            ttqHex = (sharedPreferences.getString(KEY_TERMINAL_TTQ, "76204000") ?: "76204000").let {
                if (it.equals("36204000", ignoreCase = true)) "76204000" else it
            },
            terminalCapabilitiesHex = sharedPreferences.getString(KEY_TERMINAL_CAPABILITIES, "E0F8C8") ?: "E0F8C8",
            terminalTypeHex = sharedPreferences.getString(KEY_TERMINAL_TYPE, "22") ?: "22",
            merchantName = sharedPreferences.getString(KEY_MERCHANT_NAME, "TAP TO PAY SHOP") ?: "TAP TO PAY SHOP",
            ifdSerialNumber = sharedPreferences.getString(KEY_IFD_SERIAL_NUMBER, "12345678") ?: "12345678",
            merchantCategoryCode = sharedPreferences.getString(KEY_MERCHANT_CATEGORY_CODE, "5411") ?: "5411",
            additionalTerminalCapabilitiesHex = sharedPreferences.getString(KEY_ADDITIONAL_TERMINAL_CAPABILITIES, "6000F0A001") ?: "6000F0A001",
            ledColorMode = try {
                com.yumedev.taptopayandroid.domain.model.PosLedColorMode.valueOf(
                    sharedPreferences.getString(
                        KEY_LED_COLOR_MODE,
                        com.yumedev.taptopayandroid.domain.model.PosLedColorMode.EMV_GREEN.name
                    ) ?: com.yumedev.taptopayandroid.domain.model.PosLedColorMode.EMV_GREEN.name
                )
            } catch (e: Exception) {
                com.yumedev.taptopayandroid.domain.model.PosLedColorMode.EMV_GREEN
            },
            floorLimit = sharedPreferences.getLong(KEY_FLOOR_LIMIT, 10000L),
            tvrMode = try {
                com.yumedev.taptopayandroid.domain.model.TvrMode.valueOf(
                    sharedPreferences.getString(KEY_TVR_MODE, com.yumedev.taptopayandroid.domain.model.TvrMode.AUTOMATIC.name)
                        ?: com.yumedev.taptopayandroid.domain.model.TvrMode.AUTOMATIC.name
                )
            } catch (e: Exception) {
                com.yumedev.taptopayandroid.domain.model.TvrMode.AUTOMATIC
            },
            manualTvrHex = sharedPreferences.getString(KEY_MANUAL_TVR, "0000000000") ?: "0000000000",
            genAcRequestMode = try {
                com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.valueOf(
                    sharedPreferences.getString(KEY_GEN_AC_MODE, com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.AUTO_TAA.name)
                        ?: com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.AUTO_TAA.name
                )
            } catch (e: Exception) {
                com.yumedev.taptopayandroid.domain.model.GenAcRequestMode.AUTO_TAA
            },
            strictOnlineAuthDisplay = sharedPreferences.getBoolean(KEY_STRICT_ONLINE_AUTH_DISPLAY, false),
            brandSuccessTheme = try {
                com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.valueOf(
                    sharedPreferences.getString(
                        KEY_BRAND_SUCCESS_THEME,
                        com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.AUTO.name
                    ) ?: com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.AUTO.name
                )
            } catch (e: Exception) {
                com.yumedev.taptopayandroid.domain.model.BrandSuccessTheme.AUTO
            }
        )
    }

    fun saveTerminalConfig(config: com.yumedev.taptopayandroid.domain.model.TerminalConfig) {
        sharedPreferences.edit {
            putString(KEY_CURRENCY_CODE, config.currencyCode)
            putString(KEY_CURRENCY_SYMBOL, config.currencySymbol)
            putInt(KEY_CURRENCY_EXPONENT, config.currencyExponent)
            putString(KEY_COUNTRY_CODE, config.countryCode)
            putString(KEY_TRANSACTION_TYPE, config.transactionType)
            putString(KEY_TERMINAL_TTQ, config.ttqHex)
            putString(KEY_TERMINAL_CAPABILITIES, config.terminalCapabilitiesHex)
            putString(KEY_TERMINAL_TYPE, config.terminalTypeHex)
            putString(KEY_MERCHANT_NAME, config.merchantName)
            putString(KEY_IFD_SERIAL_NUMBER, config.ifdSerialNumber)
            putString(KEY_MERCHANT_CATEGORY_CODE, config.merchantCategoryCode)
            putString(KEY_ADDITIONAL_TERMINAL_CAPABILITIES, config.additionalTerminalCapabilitiesHex)
            putString(KEY_LED_COLOR_MODE, config.ledColorMode.name)
            putLong(KEY_FLOOR_LIMIT, config.floorLimit)
            putString(KEY_TVR_MODE, config.tvrMode.name)
            putString(KEY_MANUAL_TVR, config.manualTvrHex)
            putString(KEY_GEN_AC_MODE, config.genAcRequestMode.name)
            putBoolean(KEY_STRICT_ONLINE_AUTH_DISPLAY, config.strictOnlineAuthDisplay)
            putString(KEY_BRAND_SUCCESS_THEME, config.brandSuccessTheme.name)
        }
    }

    fun resetTerminalConfig() {
        saveTerminalConfig(com.yumedev.taptopayandroid.domain.model.TerminalConfig())
    }
}
