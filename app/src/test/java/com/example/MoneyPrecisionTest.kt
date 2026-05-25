package com.example

import com.example.data.database.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyPrecisionTest {
    @Test
    fun convertsMajorAmountsToMinorUnitsWithoutBinaryDrift() {
        assertEquals(30L, Money.toMinor(0.1 + 0.2))
        assertEquals(20870L, Money.toMinor(208.7))
        assertEquals(20868L, Money.toMinor(208.675))
    }

    @Test
    fun convertsMinorUnitsBackToDisplayMajorAmount() {
        assertEquals(1500.75, Money.toMajor(150075L), 0.0)
        assertEquals("1,500.75", Money.formatMinor(150075L))
    }
}
