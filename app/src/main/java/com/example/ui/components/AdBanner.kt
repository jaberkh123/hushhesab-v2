/*
 * ─────────────────────────────────────────────────────────────────────────────
 *  AdBanner.kt — کومپوزابل‌های UI بنر تبلیغ آیکونی «پیشنهاد ما»
 *  (روش تبلیغ / ravesh-tabligh — تطبیق‌یافته با تم تیره و طلایی «هوش حساب»)
 * ─────────────────────────────────────────────────────────────────────────────
 *  محتوا:
 *   • adRatioToAspect  — تبدیل «1:1»/«9:3» سرور به float
 *   • AdIconsBanner    — بنر اصلی: ۳ آیکون در عرض، ترتیب رندوم، ورود پلکانی
 *   • IconAdItem       — سلول هر آیکون: انیمیشن fade-in با تاخیر ۱ثانیه‌ای
 *
 *  وابستگی‌ها: coil-compose (AsyncImage)، androidx.compose.*، kotlinx-coroutines
 */
package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import coil.compose.AsyncImage
import com.example.data.AdManager
import com.example.data.AdsUiState
import com.example.data.IconAd
import kotlinx.coroutines.delay

/** تبدیل رشتهٔ نسبت تصویر API («1:1» یا «9:3») به float عرض÷ارتفاع. */
fun adRatioToAspect(ratio: String?, fallback: Float = 1f): Float =
    ratio?.split(":")?.takeIf { it.size == 2 }
        ?.mapNotNull { it.trim().toFloatOrNull() }
        ?.takeIf { it.size == 2 && it[1] != 0f }
        ?.let { (w, h) -> w / h }
        ?: fallback

/**
 * بنر تبلیغات آیکونی (پایین صفحهٔ اول) — طبق روش تبلیغ:
 *  • فقط در حالت Ready و با آیکون‌های واقعیِ API رندر می‌شود؛
 *    در Loading/Hidden هیچ‌چیز نشان داده نمی‌شود (بدون خطا، بدون کرش، بدون قفل شدن).
 *  • در عرض صفحه دقیقاً ۳ آیکون جا می‌گیرد؛ اگر تعداد آیکون‌ها بیشتر باشد،
 *    ردیف به‌صورت افقی اسکرول می‌خورد (سرریز به سمتِ راست).
 *  • تپ روی هر آیکون → مقصد طبق دستور سرور: destination=bazaar → باز شدن مستقیم اپِ
 *    کافه‌بازار (bazaar://details?id=<package_name> + ثبت آمار via=app در پس‌زمینه)؛
 *    destination=web → باز شدن click_url در مرورگر (شمارندهٔ سرور + ریدایرکت).
 *  • انیمیشن ورود پلکانی: هر بار صفحه لود می‌شود آیکون‌ها به ترتیب از چپ به راست،
 *    هر کدام ۱ ثانیه بعد از قبلی، با fade-in نرم ظاهر می‌شوند (جلب توجه به تبلیغ).
 */
@Composable
fun AdIconsBanner(modifier: Modifier = Modifier) {
    val st = AdManager.state
    if (st !is AdsUiState.Ready || st.icons.isEmpty()) return

    val ctx = LocalContext.current
    // تم تیره و طلایی هوش حساب
    val cardBg = Color(0xFF0F172A).copy(alpha = 0.55f)
    val borderColor = Color(0xFFFBBF24).copy(alpha = 0.30f)
    // سربرگ بنر با رنگ طلایی تم؛ عنوان آیکون‌ها سفید برای خوانایی روی تم تیره
    val headerColor = Color(0xFFFBBF24)
    val titleColor = Color.White

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // عرض هر آیتم = (عرض داخل کارت − دو فاصلهٔ بین سه آیتم) ÷ ۳ → دقیقاً ۳ آیکون در صفحه
        val hPad = 12.dp
        val spacing = 10.dp
        val itemWidth = ((maxWidth - hPad * 2) - spacing * 2) / 3
        val iconAspect = adRatioToAspect(st.iconsRatio)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp)
        ) {
            // سربرگ کوچک بنر
            Row(
                modifier = Modifier.padding(horizontal = hPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = headerColor, modifier = Modifier.size(13.dp))
                Text(
                    text = "پیشنهاد ما",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = headerColor, fontWeight = FontWeight.Bold, fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ردیف آیکون‌ها — ترتیب آیکون‌ها هر بار رندوم است (اولین آیکون هر fetch
            // می‌تواند متفاوت باشد)؛ جهتِ ردیف همیشه LTR می‌ماند تا آیکون‌های بیشتر
            // با اسکرول به سمتِ راست ظاهر شوند (مستقل از زبان دستگاه).
            // ورود پلکانی: index هر آیتم → تأخیر ۱ثانیه‌ای پشت‌سرهم در IconAdItem.
            val displayIcons = remember(st.icons) { st.icons.shuffled() }
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = hPad),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    itemsIndexed(displayIcons, key = { _, it -> it.slot }) { index, icon ->
                        IconAdItem(
                            icon = icon,
                            appearIndex = index,
                            reloadKey = st.icons,
                            itemWidth = itemWidth,
                            aspect = iconAspect,
                            titleColor = titleColor
                        ) {
                            AdManager.openAd(ctx, icon)
                        }
                    }
                }
            }
        }
    }
}

/**
 * سلول یک آیکون تبلیغ: تصویر ۹۰٪ عرض سلول + عنوان یک‌خطی زیر آن.
 * انیمیشن ورود پلکانی: هر بار صفحه لود می‌شود (یا لیست تبلیغ refresh می‌شود)
 * آیکون‌ها به ترتیب از چپ به راست ظاهر می‌شوند — هر آیکون ۱ ثانیه بعد از قبلی.
 */
@Composable
private fun IconAdItem(
    icon: IconAd,
    appearIndex: Int,
    reloadKey: Any?,
    itemWidth: Dp,
    aspect: Float,
    titleColor: Color,
    onClick: () -> Unit
) {
    val appearAlpha = remember { Animatable(0f) }
    LaunchedEffect(reloadKey, icon.slot) {
        appearAlpha.snapTo(0f)
        delay(appearIndex * 1_000L)           // آیکون n → n ثانیه بعد از لود صفحه
        appearAlpha.animateTo(1f, tween(700)) // fade-in نرم ~۰٫۷ ثانیه
    }

    Column(
        modifier = Modifier
            .graphicsLayer { alpha = appearAlpha.value }
            .width(itemWidth)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = icon.imageUrl,
            contentDescription = icon.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(12.dp))
        )
        icon.title?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = titleColor, fontSize = 10.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}
