package com.example.core.security

import android.content.Context
import android.content.SharedPreferences

/**
 * تنظیمات کاربر «هوش حساب نسخه ۲».
 * در نسخهٔ ۲ (مطابق PWA) قفل PIN، آنبوردینگ و هشدار بودجه حذف شدند؛
 * فقط ترجیحات ساده (مخفی‌سازی مبالغ، واحد پول) باقی می‌ماند.
 */
class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kharjyar_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HIDE_BALANCE = "hide_balance"
        private const val KEY_CURRENCY = "currency_unit" // "تومان", "ریال"
    }

    var isHideBalance: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BALANCE, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_BALANCE, value).apply()

    var currencyUnit: String
        get() = prefs.getString(KEY_CURRENCY, "تومان") ?: "تومان"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()
}
