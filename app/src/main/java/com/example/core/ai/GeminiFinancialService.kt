package com.example.core.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedTransactionResult(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Long = 0L,
    val categoryId: String = "other_expense",
    val categoryName: String = "سایر هزینه‌ها",
    val categoryIcon: String = "📦",
    val note: String = "",
    val confidence: String = "high"
)

data class AiInsightItem(
    val title: String,
    val description: String,
    val type: String, // "warning", "tip", "success"
    val icon: String
)

object GeminiFinancialService {
    private const val TAG = "GeminiFinancialService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Parses natural language Persian text into a structured transaction.
     * Uses Gemini API if configured; falls back to rule-based Persian NLP engine.
     */
    suspend fun parseExpenseText(inputText: String): ParsedTransactionResult = withContext(Dispatchers.IO) {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            return@withContext ParsedTransactionResult()
        }

        // Try Gemini AI API if API key is present
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (!apiKey.isNullOrEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiForTransactionParsing(apiKey, trimmed)
                if (aiResult != null && aiResult.amount > 0) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error, using offline heuristic", e)
            }
        }

        // Fallback: Intelligent Persian NLP Rule Parser
        return@withContext parseWithOfflineNlp(trimmed)
    }

    /**
     * Generates smart financial advice / answers natural language queries about expenses.
     */
    suspend fun askFinancialAssistant(
        userQuery: String,
        totalIncome: Long,
        totalExpense: Long,
        topCategoryName: String,
        topCategoryAmount: Long,
        transactionsSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (!apiKey.isNullOrEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = """
                    شما دستیار هوش مصنوعی مدیریت مالی «هوش حساب» هستید.
                    اطلاعات مالی کاربر:
                    - کل درآمد این ماه: $totalIncome تومان
                    - کل هزینه این ماه: $totalExpense تومان
                    - بیشترین دسته هزینه: $topCategoryName با مبلغ $topCategoryAmount تومان
                    - خلاصه تراکنش‌ها: $transactionsSummary

                    سوال یا درخواست کاربر: "$userQuery"

                    لطفاً پاسخی بسیار دقیق، دوستانه، به زبان فارسی روان و خلاصه (حداکثر در ۳ الی ۴ جمله) به همراه راهکار عملی صرفه‌جویی ارائه دهید.
                """.trimIndent()

                val response = callGeminiRaw(apiKey, prompt)
                if (!response.isNullOrBlank()) {
                    return@withContext response
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini assistant call failed", e)
            }
        }

        // Offline Intelligent Financial Advisor responses
        return@withContext generateOfflineAdvisorAnswer(
            userQuery, totalIncome, totalExpense, topCategoryName, topCategoryAmount
        )
    }

    private fun callGeminiForTransactionParsing(apiKey: String, userText: String): ParsedTransactionResult? {
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val prompt = """
            متن زیر یک گزارش مالی به زبان فارسی است:
            "$userText"

            اطلاعات را استخراج کن و فقط یک JSON با فرمت زیر برگردان:
            {
              "type": "EXPENSE" | "INCOME",
              "amount": 120000,
              "categoryId": "food" | "shopping" | "transport" | "home" | "bills" | "entertainment" | "health" | "education" | "clothes" | "tech" | "car" | "travel" | "installment" | "gift" | "salary" | "freelance" | "investment" | "other_expense",
              "note": "توضیح کوتاه"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        val rootJson = JSONObject(responseBody)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        val text = parts.getJSONObject(0).optString("text", "")

        val parsedJson = JSONObject(text)
        val typeStr = parsedJson.optString("type", "EXPENSE")
        val amount = parsedJson.optLong("amount", 0L)
        val catId = parsedJson.optString("categoryId", "other_expense")
        val note = parsedJson.optString("note", userText)

        val catItem = DefaultCategories.findCategory(catId)
            ?: DefaultCategories.expenseCategories.first { it.id == "other_expense" }

        val type = if (typeStr == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE

        return ParsedTransactionResult(
            type = type,
            amount = amount,
            categoryId = catItem.id,
            categoryName = catItem.name,
            categoryIcon = catItem.icon,
            note = note,
            confidence = "AI"
        )
    }

    private fun callGeminiRaw(apiKey: String, prompt: String): String? {
        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder().url(url).post(requestBody).build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        val rootJson = JSONObject(responseBody)
        val candidates = rootJson.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null
        return parts.getJSONObject(0).optString("text", "")
    }

    /**
     * Fast offline Persian NLP rule-based parser
     */
    fun parseWithOfflineNlp(text: String): ParsedTransactionResult {
        val normalized = text.replace("ي", "ی").replace("ك", "ک")

        // 1. Determine Type
        val isIncome = normalized.contains("حقوق") || normalized.contains("درآمد") ||
                normalized.contains("واریز") || normalized.contains("فروش") ||
                normalized.contains("پاداش") || normalized.contains("سود")

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Extract Amount
        var amount = extractAmountFromPersianText(normalized)
        if (amount == 0L) {
            // fallback extract any digits
            val matcher = Pattern.compile("(\\d[\\d,]*)").matcher(normalized.replace("،", ""))
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "") ?: "0"
                amount = numStr.toLongOrNull() ?: 0L
            }
        }

        // 3. Determine Category
        val (categoryId, categoryName, categoryIcon) = when {
            normalized.contains("اسنپ") || normalized.contains("تپسی") || normalized.contains("تاکسی") ||
                    normalized.contains("مترو") || normalized.contains("اتوبوس") || normalized.contains("کرایه") ->
                Triple("transport", "حمل‌ونقل", "🚕")

            normalized.contains("بنزین") || normalized.contains("تعمیرگاه") || normalized.contains("روغن") ||
                    normalized.contains("مکانیک") || normalized.contains("کارواش") || normalized.contains("خودرو") ->
                Triple("car", "خودرو و بنزین", "🚗")

            normalized.contains("رستوران") || normalized.contains("کافه") || normalized.contains("غذا") ||
                    normalized.contains("ناهار") || normalized.contains("شام") || normalized.contains("صبحانه") ||
                    normalized.contains("پیتزا") || normalized.contains("فست‌فود") || normalized.contains("ساندویچ") ->
                Triple("food", "غذا و رستوران", "🍔")

            normalized.contains("سوپرمارکت") || normalized.contains("دیجیکالا") || normalized.contains("دیجی کالا") ||
                    normalized.contains("خرید") || normalized.contains("میوه") || normalized.contains("نان") ||
                    normalized.contains("فروشگاه") ->
                Triple("shopping", "خرید و بازار", "🛒")

            normalized.contains("اجاره") || normalized.contains("شارژ ساختمان") || normalized.contains("خانه") ||
                    normalized.contains("املاک") ->
                Triple("home", "مسکن و اجاره", "🏠")

            normalized.contains("قبض") || normalized.contains("برق") || normalized.contains("گاز") ||
                    normalized.contains("آب") || normalized.contains("تلفن") || normalized.contains("شهرداری") ->
                Triple("bills", "قبض و خدمات", "💡")

            normalized.contains("داروخانه") || normalized.contains("پزشک") || normalized.contains("دکتر") ||
                    normalized.contains("بیمارستان") || normalized.contains("درمانگاه") || normalized.contains("آزمایشگاه") ->
                Triple("health", "سلامت و درمان", "💊")

            normalized.contains("کتاب") || normalized.contains("دوره") || normalized.contains("شهریه") ||
                    normalized.contains("دانشگاه") || normalized.contains("مدرسه") || normalized.contains("آموزش") ->
                Triple("education", "آموزش و کتاب", "📚")

            normalized.contains("لباس") || normalized.contains("کفش") || normalized.contains("شلوار") ||
                    normalized.contains("پیراهن") || normalized.contains("مانتو") || normalized.contains("کت") ->
                Triple("clothes", "پوشاک و مد", "👕")

            normalized.contains("اینترنت") || normalized.contains("بسته") || normalized.contains("موبایل") ||
                    normalized.contains("لپ‌تاپ") || normalized.contains("شارژ ایرانسل") || normalized.contains("شارژ همراه اول") ->
                Triple("tech", "تکنولوژی و اینترنت", "💻")

            normalized.contains("سینما") || normalized.contains("بازی") || normalized.contains("گیم") ||
                    normalized.contains("استخر") || normalized.contains("باشگاه") || normalized.contains("تفریح") ->
                Triple("entertainment", "تفریح و سرگرمی", "🎮")

            normalized.contains("قسط") || normalized.contains("وام") || normalized.contains("چک") ->
                Triple("installment", "قسط و بدهی", "💳")

            normalized.contains("حقوق") ->
                Triple("salary", "حقوق و دستمزد", "💼")

            normalized.contains("پروژه") ->
                Triple("project", "پروژه و قرارداد", "💻")

            normalized.contains("فریلنس") ->
                Triple("freelance", "فریلنسری", "🧑‍💻")

            isIncome ->
                Triple("other_income", "سایر درآمدها", "💰")

            else ->
                Triple("other_expense", "سایر هزینه‌ها", "📦")
        }

        return ParsedTransactionResult(
            type = type,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            note = text,
            confidence = "Offline NLP"
        )
    }

    private fun extractAmountFromPersianText(text: String): Long {
        // Look for expressions like "۳۰۰ هزار", "۵۰ تومن", "۲ میلیون", "300000"
        val words = text.split(" ")
        var multiplier = 1L

        // Check for million
        if (text.contains("میلیون")) {
            val p = Pattern.compile("([\\d۰-۹]+(?:\\.[\\d۰-۹]+)?)\\s*میلیون")
            val m = p.matcher(text)
            if (m.find()) {
                val num = parsePersianOrEngDouble(m.group(1) ?: "1")
                return (num * 1_000_000L).toLong()
            }
        }

        // Check for thousand (هزار / تومن / تومان)
        if (text.contains("هزار")) {
            val p = Pattern.compile("([\\d۰-۹]+)\\s*هزار")
            val m = p.matcher(text)
            if (m.find()) {
                val num = parsePersianOrEngLong(m.group(1) ?: "0")
                return num * 1_000L
            }
        }

        // Check direct numbers before تومان / تومن
        val p = Pattern.compile("([\\d۰-۹,،]+)\\s*(?:تومان|تومن|ت)")
        val m = p.matcher(text)
        if (m.find()) {
            val numStr = m.group(1)?.replace(",", "")?.replace("،", "") ?: "0"
            return parsePersianOrEngLong(numStr)
        }

        return 0L
    }

    private fun parsePersianOrEngLong(str: String): Long {
        val engDigits = str.map { ch ->
            when (ch) {
                '۰' -> '0'
                '۱' -> '1'
                '۲' -> '2'
                '۳' -> '3'
                '۴' -> '4'
                '۵' -> '5'
                '۶' -> '6'
                '۷' -> '7'
                '۸' -> '8'
                '۹' -> '9'
                else -> ch
            }
        }.joinToString("")
        return engDigits.toLongOrNull() ?: 0L
    }

    private fun parsePersianOrEngDouble(str: String): Double {
        val engDigits = str.map { ch ->
            when (ch) {
                '۰' -> '0'
                '۱' -> '1'
                '۲' -> '2'
                '۳' -> '3'
                '۴' -> '4'
                '۵' -> '5'
                '۶' -> '6'
                '۷' -> '7'
                '۸' -> '8'
                '۹' -> '9'
                else -> ch
            }
        }.joinToString("")
        return engDigits.toDoubleOrNull() ?: 0.0
    }

    private fun generateOfflineAdvisorAnswer(
        query: String,
        income: Long,
        expense: Long,
        topCat: String,
        topCatAmt: Long
    ): String {
        val q = query.lowercase()
        val savings = (income - expense).coerceAtLeast(0L)
        val savingsPercent = if (income > 0) ((savings * 100) / income).toInt() else 0

        return when {
            q.contains("غذا") || q.contains("رستوران") -> {
                "این ماه بیشترین سهم مخارج شما در دسته $topCat به میزان ${com.example.core.utils.PersianFormatter.formatMoney(topCatAmt)} بوده است. پیشنهاد می‌شود با کاهش سفارش‌های بیرون‌بر تا ۲۰٪، ماهانه مبلغی قابل توجه پس‌انداز کنید."
            }
            q.contains("بیشترین") -> {
                "بیشترین هزینه ثبت‌شده شما در این ماه مربوط به دسته‌بندی «$topCat» با مبلغ ${com.example.core.utils.PersianFormatter.formatMoney(topCatAmt)} است."
            }
            q.contains("پس‌انداز") || q.contains("چقدر مانده") -> {
                "شما این ماه مبلغ ${com.example.core.utils.PersianFormatter.formatMoney(savings)} پس‌انداز خالص داشته‌اید که معادل $savingsPercent٪ از کل درآمدتان است."
            }
            q.contains("صرفه جویی") || q.contains("پیشنهاد") -> {
                "اگر هزینه‌های دسته «$topCat» را ۱۵٪ کنترل کنید، حدود ${com.example.core.utils.PersianFormatter.formatMoney((topCatAmt * 0.15).toLong())} بیشتر در ماه ذخیره خواهید کرد."
            }
            else -> {
                "مجموع هزینه‌های این ماه شما ${com.example.core.utils.PersianFormatter.formatMoney(expense)} و درآمدتان ${com.example.core.utils.PersianFormatter.formatMoney(income)} بوده است. وضعیت تعادل مالی شما مناسب است."
            }
        }
    }

    fun generateInsights(
        totalIncome: Long,
        totalExpense: Long,
        budgetLimit: Long,
        topCategory: String,
        topCategoryAmount: Long
    ): List<AiInsightItem> {
        val list = mutableListOf<AiInsightItem>()

        if (budgetLimit > 0) {
            val budgetUsage = ((totalExpense.toDouble() / budgetLimit) * 100).toInt()
            when {
                budgetUsage >= 100 -> {
                    list.add(
                        AiInsightItem(
                            title = "هشدار سقف بودجه",
                            description = "مخارج شما از سقف بودجه ماهانه (${com.example.core.utils.PersianFormatter.formatMoney(budgetLimit)}) عبور کرده است.",
                            type = "warning",
                            icon = "⚠️"
                        )
                    )
                }
                budgetUsage >= 80 -> {
                    list.add(
                        AiInsightItem(
                            title = "نزدیک به سقف بودجه",
                            description = "$budgetUsage٪ از بودجه ماهانه مصرف شده است. مخارج غیرضروری را کنترل کنید.",
                            type = "warning",
                            icon = "📊"
                        )
                    )
                }
            }
        }

        if (topCategoryAmount > 0) {
            list.add(
                AiInsightItem(
                    title = "بیشترین هزینه ماه",
                    description = "دسته «$topCategory» با مبلغ ${com.example.core.utils.PersianFormatter.formatMoney(topCategoryAmount)} بیشترین سهم را در مخارج داشته است.",
                    type = "tip",
                    icon = "💡"
                )
            )
        }

        val savings = totalIncome - totalExpense
        if (savings > 0 && totalIncome > 0) {
            val saveRate = ((savings.toDouble() / totalIncome) * 100).toInt()
            list.add(
                AiInsightItem(
                    title = "عملکرد عالی پس‌انداز",
                    description = "شما موفق شدید $saveRate٪ از درآمد این ماه خود (${com.example.core.utils.PersianFormatter.formatMoney(savings)}) را ذخیره کنید.",
                    type = "success",
                    icon = "🎯"
                )
            )
        }

        return list
    }
}
