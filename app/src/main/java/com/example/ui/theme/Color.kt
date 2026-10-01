package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * پالت «هوش حساب نسخه ۲» — عیناً از PWA نسخه ۲ (hoosha-hesab-v2/index.html):
 *   پس‌زمینه: گرادیان ۱۳۵° از #0f0c29 → #302b63 → #24243e
 *   کارت‌ها: سفید نیمه‌شفاف (6.5٪) با حاشیه سفید ۱۰٪
 *   طلایی اصلی: #ffd700 (هدرها، مبالغ، دکمه‌های طلایی)
 *   گرادیان دکمهٔ طلایی: #f7971e → #ffd200 (متن تیره #1b1735)
 *   درآمد: #00e676 | هزینه: #ff7272 | موجودی حساب: #64b5f6
 */

// ── پس‌زمینه و سطوح ──────────────────────────────────────────
val BgDeep1 = Color(0xFF0F0C29)      // ابتدای گرادیان
val BgDeep2 = Color(0xFF302B63)      // میانهٔ گرادیان
val BgDeep3 = Color(0xFF24243E)      // انتهای گرادیان
val TextPrimary = Color(0xFFE8E8EE)  // متن اصلی
val TextMuted = Color(0xFF9C9CB0)    // متن کم‌رنگ (sub)
val TextHeader = Color(0xFFCFCFDA)   // لیبل کارت هیرو

val CardBg = Color(0xFFFFFF1A)       // rgba(255,255,255,.10) — در کاتلین ARGB
val CardBgSoft = Color(0x14FFFFFF)   // rgba(255,255,255,.08)
val CardBorder = Color(0x1AFFFFFF)   // rgba(255,255,255,.10)
val CardBorderStrong = Color(0x24FFFFFF) // rgba(255,255,255,.14)

// ── طلایی ────────────────────────────────────────────────────
val GoldPrimary = Color(0xFFFFD700)      // #ffd700
val GoldGradientStart = Color(0xFFF7971E) // #f7971e
val GoldGradientEnd = Color(0xFFFFD200)   // #ffd200
val GoldOnDark = Color(0xFF1B1735)        // متن روی دکمهٔ طلایی
val HeroBgStart = Color(0x21FFD700)       // rgba(255,215,0,.13)
val HeroBgEnd = Color(0x1AF7971E)         // rgba(247,151,30,.10)
val HeroBorder = Color(0x40FFD700)        // rgba(255,215,0,.25)

// ── معنایی مالی (دقیق PWA) ───────────────────────────────────
val IncomeGreen = Color(0xFF00E676)
val ExpenseRed = Color(0xFFFF7272)
val TransferBlue = Color(0xFF64B5F6)
val AccountBalanceBlue = Color(0xFF64B5F6)
val DangerBtnBg = Color(0x29FF5252)       // rgba(255,82,82,.16)
val DangerBtnText = Color(0xFFFF7272)

// ── سازگاری با کدهای قدیمی اپ (نگاشت به پالت جدید) ───────────
val EmeraldPrimary = GoldPrimary
val EmeraldLight = GoldPrimary
val EmeraldDark = Color(0xFFB8860B)
val EmeraldContainerLight = Color(0x33FFD700)
val EmeraldContainerDark = Color(0x29FFD700)

val IndigoSecondary = Color(0xFF7C7CE0)
val IndigoLight = Color(0xFF9E9EF0)
val IndigoDark = Color(0xFF3D3D80)
val IndigoContainerLight = Color(0x337C7CE0)

val AmberAccent = GoldPrimary
val AmberLight = Color(0xFFFFE566)
val AmberDark = GoldGradientStart

val WarningYellow = GoldPrimary

// ── سطوح کلاسیک (برای سازگاری) ───────────────────────────────
val BackgroundLight = BgDeep1
val SurfaceLight = Color(0xFF221F4A)
val SurfaceVariantLight = Color(0xFF2C2857)
val OnSurfaceLight = TextPrimary
val OnSurfaceVariantLight = TextMuted

val BackgroundDark = BgDeep1
val SurfaceDark = Color(0xFF221F4A)      // کارت بنفش تیره (به‌جای خاکستری قدیمی)
val SurfaceVariantDark = Color(0xFF2C2857)
val OnSurfaceDark = TextPrimary
val OnSurfaceVariantDark = TextMuted
