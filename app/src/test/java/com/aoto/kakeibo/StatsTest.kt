package com.aoto.kakeibo

import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.stats.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {

    private fun exp(amount: Long, cat: String = "食費") =
        TxnEntity(timestamp = 0, amount = amount, source = "現金", category = cat, isIncome = false)

    private fun inc(amount: Long, cat: String = "給与") =
        TxnEntity(timestamp = 0, amount = amount, source = "収入", category = cat, isIncome = true)

    @Test fun totalsAndBalance() {
        val t = Stats.totals(listOf(inc(300_000), exp(1_000), exp(2_000)))
        assertEquals(300_000L, t.income)
        assertEquals(3_000L, t.expense)
        assertEquals(297_000L, t.balance)
    }

    @Test fun byCategoryExpenseSortedDescAndExcludesIncome() {
        val list = listOf(exp(500, "食費"), exp(1_500, "日用品"), exp(800, "食費"), inc(300_000, "給与"))
        val byCat = Stats.byCategory(list, income = false)
        assertEquals(2, byCat.size)
        assertEquals("日用品", byCat[0].first)
        assertEquals(1_500L, byCat[0].second)
        assertEquals("食費", byCat[1].first)
        assertEquals(1_300L, byCat[1].second)
        assertEquals(2, byCat[1].third)
    }

    @Test fun savingsRate() {
        assertEquals(90, Stats.savingsRate(Stats.Totals(income = 100_000, expense = 10_000)))
        assertEquals(0, Stats.savingsRate(Stats.Totals(income = 50_000, expense = 50_000)))
        assertNull(Stats.savingsRate(Stats.Totals(income = 0, expense = 5_000)))
    }
}
