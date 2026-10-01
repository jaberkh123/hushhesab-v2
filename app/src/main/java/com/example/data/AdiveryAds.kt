package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.adivery.sdk.Adivery
import com.adivery.sdk.AdiveryListener

/**
 * ────────────────────────────────────────────────────────────────────────────
 *  AdiveryAds — مدیریت تبلیغات ادیوری برای «هوش حساب»
 *  (طبق الگوی اجراشده در ریپوی jadval-fantasy)
 * ────────────────────────────────────────────────────────────────────────────
 *   • «بازگشت به برنامه» (App Open): فقط وقتی کاربر از بیرون برنامه (بک‌گراند/
 *     اپ دیگر) به برنامه برمی‌گردد «یکبار در میان» نمایش داده می‌شود —
 *     با بازگشت‌های داخلی صفحات (دکمهٔ بازگشت) هیچ ارتباطی ندارد و هرگز
 *     نمایش داده نمی‌شود. فلگ در SharedPreferences ذخیره می‌شود تا
 *     یک‌درمیان بودن حتی بعد از بسته‌شدن برنامه حفظ شود.
 *   • «میان‌صفحه‌ای» (Interstitial): هر ۲۰ بار کلیک روی دکمه‌های اصلی نمایش
 *     داده می‌شود.
 *   • تبلیغ فقط وقتی واقعاً نمایش داده شد نوبت را عوض می‌کند؛ اگر آماده نبود،
 *     همان نوبت دوباره تلاش می‌شود.
 *   • هیچ‌وقت کرش نمی‌کند؛ همهٔ فراخوانی‌های SDK داخل try/catch هستند.
 */
object AdiveryAds {

    // ── شناسه‌های ادیوری (از کاربر — 2026/09) ────────────────────────────────
    /** شناسه اپلیکیشن ادیوری */
    const val ADIVERY_APP_ID = "7c982ac3-0d8d-45ee-ae30-c4295fea7ae6"

    /** شناسه تبلیغگاه «بازگشت به برنامه» (App Open) */
    const val ADIVERY_APP_OPEN_PLACEMENT = "ad13951b-5324-4c0d-bb72-7eb8eea4efc7"

    /** شناسه تبلیغگاه «میان‌صفحه‌ای» (Interstitial) */
    const val ADIVERY_INTERSTITIAL_PLACEMENT = "57d65a1f-b8de-4fa3-af4e-2f8c263461d7"

    /** بعد از هر چند کلیک روی دکمه‌ها، تبلیغ میان‌صفحه‌ای نمایش داده شود */
    private const val INTERSTITIAL_EVERY_N_CLICKS = 20

    /** فلگ سراسری: الان یک تبلیغ تمام‌صفحه روی صفحه است (جلوگیری از تداخل) */
    @Volatile
    var isShowingFullscreenAd = false

    private var initialized = false
    private var appOpenPrefs: SharedPreferences? = null
    private var clickCount = 0

    /** تازه یک تبلیغ تمام‌صفحه بسته شده و resume بعدی ناشی از بستن آن است */
    @Volatile
    var foregroundJustAfterAd = false

    /** کلید فلگ یک‌درمیان‌بودن نمایش اپ‌اوپن در SharedPreferences */
    private const val KEY_APP_OPEN_SHOWN_LAST = "app_open_shown_last"

    /** از MainActivity.onCreate صدا زده می‌شود */
    fun init(activity: Activity) {
        if (initialized) return
        initialized = true
        try {
            appOpenPrefs = activity.getSharedPreferences("app_open_ad_prefs", Context.MODE_PRIVATE)

            // افزودن Listener سراسری قبل از پیکربندی و درخواست تبلیغ
            Adivery.addGlobalListener(object : AdiveryListener() {
                override fun onAppOpenAdLoaded(placementId: String) {
                    Log.d("Adivery", "App Open loaded: $placementId")
                }

                override fun onAppOpenAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                    Log.d("Adivery", "App Open shown: $placementId")
                }

                override fun onAppOpenAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    Log.d("Adivery", "App Open closed: $placementId")
                    // بارگذاری مجدد تبلیغ بازگشت به برنامه پس از بسته‌شدن
                    Adivery.prepareAppOpenAd(activity, ADIVERY_APP_OPEN_PLACEMENT)
                    foregroundJustAfterAd = true
                }

                override fun onInterstitialAdLoaded(placementId: String) {
                    Log.d("Adivery", "Interstitial loaded: $placementId")
                }

                override fun onInterstitialAdShown(placementId: String) {
                    isShowingFullscreenAd = true
                    Log.d("Adivery", "Interstitial shown: $placementId")
                }

                override fun onInterstitialAdClosed(placementId: String) {
                    isShowingFullscreenAd = false
                    // بارگذاری خودکار تبلیغ بعدی پس از بسته‌شدن
                    Adivery.prepareInterstitialAd(activity, ADIVERY_INTERSTITIAL_PLACEMENT)
                    foregroundJustAfterAd = true
                }

                override fun log(placementId: String, log: String) {
                    Log.d("Adivery", "$placementId -> $log")
                }
            })

            // پیکربندی SDK با App ID و بارگذاری اولیه تبلیغ‌ها
            Adivery.configure(activity.application, ADIVERY_APP_ID)
            Adivery.prepareInterstitialAd(activity, ADIVERY_INTERSTITIAL_PLACEMENT)
            Adivery.prepareAppOpenAd(activity, ADIVERY_APP_OPEN_PLACEMENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── منطق نمایش یک‌درمیان تبلیغ بازگشت به برنامه ─────────────────────────
    // اگر بار آخر تبلیغ نمایش داده شد، دفعه بعد نمایش داده نمی‌شود و بالعکس.
    // فقط در صورت نمایش واقعی، نوبت به «نمایش‌شده» تغییر می‌کند.

    /**
     * فقط با «بازگشت واقعی به برنامه از بیرون» صدا زده می‌شود (از بک‌گراند/
     * لانچر/اپ دیگر به پیش‌زمینه) — نه با ناوبری داخلی بین صفحات.
     */
    fun onAppReturned(activity: Activity?) {
        if (activity == null || isShowingFullscreenAd) return
        val prefs = appOpenPrefs ?: return
        try {
            val shownLastTime = prefs.getBoolean(KEY_APP_OPEN_SHOWN_LAST, false)
            if (shownLastTime) {
                // نوبت نمایش نیست: نمایش داده نمی‌شود و نوبت بعدی دوباره فعال می‌شود
                prefs.edit().putBoolean(KEY_APP_OPEN_SHOWN_LAST, false).apply()
                Adivery.prepareAppOpenAd(activity, ADIVERY_APP_OPEN_PLACEMENT)
            } else {
                // نوبت نمایش است: اگر تبلیغ آماده بود نمایش داده می‌شود
                if (Adivery.isLoaded(ADIVERY_APP_OPEN_PLACEMENT)) {
                    Adivery.showAppOpenAd(activity, ADIVERY_APP_OPEN_PLACEMENT)
                    // فقط در صورت نمایش واقعی، نوبت به «نمایش‌شده» تغییر می‌کند
                    prefs.edit().putBoolean(KEY_APP_OPEN_SHOWN_LAST, true).apply()
                } else {
                    // آماده نیست؛ همان نوبت بدون نمایش می‌ماند و دفعه بعد دوباره تلاش می‌شود
                    Adivery.prepareAppOpenAd(activity, ADIVERY_APP_OPEN_PLACEMENT)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── شمارندهٔ کلیک دکمه‌ها برای تبلیغ میان‌صفحه‌ای ────────────────────────

    /**
     * روی کلیک دکمه‌های اصلی (ناوبری خانه، ثبت تراکنش و…) صدا زده می‌شود.
     * هر ۲۰ کلیک → نمایش تبلیغ میان‌صفحه‌ای (اگر آماده باشد).
     */
    fun registerButtonClick(activity: Activity?) {
        clickCount++
        if (clickCount % INTERSTITIAL_EVERY_N_CLICKS == 0) {
            showInterstitial(activity)
        }
    }

    private fun showInterstitial(activity: Activity?) {
        if (activity == null || isShowingFullscreenAd) return
        try {
            if (Adivery.isLoaded(ADIVERY_INTERSTITIAL_PLACEMENT)) {
                Adivery.showAd(ADIVERY_INTERSTITIAL_PLACEMENT)
            } else {
                // آماده نیست؛ برای دفعهٔ بعد آماده می‌شود
                Adivery.prepareInterstitialAd(activity, ADIVERY_INTERSTITIAL_PLACEMENT)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
