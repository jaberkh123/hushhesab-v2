package com.example.core.ai

import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineNlpParserTest {

    @Test
    fun `parses amount with thousand multiplier and shopping category`() {
        val result = GeminiFinancialService.parseWithOfflineNlp("خرید نان ۵۰ هزار تومان")
        assertEquals(50_000L, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("shopping", result.categoryId)
    }

    @Test
    fun `parses million multiplier and salary as income`() {
        val result = GeminiFinancialService.parseWithOfflineNlp("حقوق این ماه ۲ میلیون تومان")
        assertEquals(2_000_000L, result.amount)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("salary", result.categoryId)
    }

    @Test
    fun `parses plain digits without unit`() {
        val result = GeminiFinancialService.parseWithOfflineNlp("اسنپ به محل کار 35000")
        assertEquals(35_000L, result.amount)
        assertEquals("transport", result.categoryId)
    }

    @Test
    fun `unknown expense falls back to other_expense`() {
        val result = GeminiFinancialService.parseWithOfflineNlp("مخارج متفرقه 1000")
        assertEquals("other_expense", result.categoryId)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(1_000L, result.amount)
    }

    @Test
    fun `empty text yields zero amount`() {
        val result = GeminiFinancialService.parseWithOfflineNlp("")
        assertEquals(0L, result.amount)
    }
}
