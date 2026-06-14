package com.aoto.kakeibo.stats

import com.aoto.kakeibo.data.TxnEntity

/** 収支の集計（Android非依存・テスト対象）。 */
object Stats {

    data class Totals(val income: Long, val expense: Long) {
        val balance: Long get() = income - expense
    }

    fun totals(txns: List<TxnEntity>): Totals {
        var income = 0L
        var expense = 0L
        for (t in txns) {
            if (t.isIncome) income += t.amount else expense += t.amount
        }
        return Totals(income, expense)
    }

    /** カテゴリ別の合計と件数を降順で返す（income=true なら収入側、false なら支出側）。 */
    fun byCategory(txns: List<TxnEntity>, income: Boolean): List<Triple<String, Long, Int>> =
        txns.filter { it.isIncome == income }
            .groupBy { it.category }
            .map { (cat, list) -> Triple(cat, list.sumOf { it.amount }, list.size) }
            .sortedByDescending { it.second }

    /** 貯蓄率（％）。収入が0以下のときは null。 */
    fun savingsRate(t: Totals): Int? {
        if (t.income <= 0) return null
        return ((t.balance.toDouble() / t.income.toDouble()) * 100).toInt()
    }
}
