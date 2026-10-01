package com.example.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class JalaliCalendarHelperTest {

    @Test
    fun `gregorian to jalali known dates`() {
        // Nowruz 1403
        assertEquals(
            JalaliCalendarHelper.JalaliDate(1403, 1, 1),
            JalaliCalendarHelper.gregorianToJalali(2024, 3, 20)
        )
        // 1st of Mehr 1403
        assertEquals(
            JalaliCalendarHelper.JalaliDate(1403, 7, 1),
            JalaliCalendarHelper.gregorianToJalali(2024, 9, 22)
        )
        // Nowruz 1404
        assertEquals(
            JalaliCalendarHelper.JalaliDate(1404, 1, 1),
            JalaliCalendarHelper.gregorianToJalali(2025, 3, 21)
        )
    }

    @Test
    fun `jalali to gregorian known dates`() {
        assertEquals(
            JalaliCalendarHelper.JalaliDate(2024, 3, 20),
            JalaliCalendarHelper.jalaliToGregorian(1403, 1, 1)
        )
        // 30th of Esfand in the Jalali leap year 1403 — the old code dropped this day
        assertEquals(
            JalaliCalendarHelper.JalaliDate(2025, 3, 20),
            JalaliCalendarHelper.jalaliToGregorian(1403, 12, 30)
        )
        assertEquals(
            JalaliCalendarHelper.JalaliDate(2025, 3, 21),
            JalaliCalendarHelper.jalaliToGregorian(1404, 1, 1)
        )
    }

    @Test
    fun `roundtrip is identity day by day across leap and normal years`() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2023, Calendar.MARCH, 21) // 1402/1/1
        }
        // ~4.4 years: covers 1402, leap 1403, 1404, 1405
        repeat(1600) {
            val j = JalaliCalendarHelper.gregorianToJalali(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val g = JalaliCalendarHelper.jalaliToGregorian(j.year, j.month, j.day)
            assertEquals("year mismatch", cal.get(Calendar.YEAR), g.year)
            assertEquals("month mismatch", cal.get(Calendar.MONTH) + 1, g.month)
            assertEquals("day mismatch", cal.get(Calendar.DAY_OF_MONTH), g.day)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    @Test
    fun `jalali month lengths`() {
        assertEquals(31, JalaliCalendarHelper.jalaliDaysInMonth(1403, 1))
        assertEquals(30, JalaliCalendarHelper.jalaliDaysInMonth(1403, 7))
        // 1403 is a Jalali leap year → Esfand has 30 days
        assertEquals(30, JalaliCalendarHelper.jalaliDaysInMonth(1403, 12))
        assertEquals(29, JalaliCalendarHelper.jalaliDaysInMonth(1404, 12))
    }

    @Test
    fun `leap year detection`() {
        assertTrue(JalaliCalendarHelper.isJalaliLeapYear(1403))
        assertFalse(JalaliCalendarHelper.isJalaliLeapYear(1404))
    }
}
