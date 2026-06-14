package com.aoto.kakeibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aoto.kakeibo.MainViewModel
import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.stats.Stats

@Composable
fun SummaryScreen(vm: MainViewModel, txns: List<TxnEntity>, modifier: Modifier = Modifier) {
    val month = vm.selectedMonth
    val monthTxns = txns.filter { vm.monthOf(it.timestamp) == month }
    val totals = Stats.totals(monthTxns)
    val expenseByCat = Stats.byCategory(monthTxns, income = false)
    val incomeByCat = Stats.byCategory(monthTxns, income = true)
    val rate = Stats.savingsRate(totals)

    Column(modifier.fillMaxSize()) {
        MonthHeader(monthLabel(month), onPrev = { vm.prevMonth() }, onNext = { vm.nextMonth() })
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {

            SummaryCards(totals.income, totals.expense)

            if (rate != null) {
                Text(
                    "貯蓄率 ${rate}%（差引 ${yen(totals.balance)}）",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (monthTxns.isEmpty()) {
                Text(
                    "この月の記録はまだありません。",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp)
                )
            }

            // 支出の内訳（ドーナツ＋凡例）
            if (totals.expense > 0 && expenseByCat.isNotEmpty()) {
                SectionHeader("支出の内訳")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        values = expenseByCat.mapIndexed { i, t -> categoryColor(i) to t.second },
                        modifier = Modifier.size(140.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        expenseByCat.take(6).forEachIndexed { i, t ->
                            LegendDot(
                                categoryColor(i),
                                "${t.first}  ${yen(t.second)}",
                                Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                expenseByCat.forEachIndexed { i, t ->
                    CategoryBar(t.first, t.second, t.third, totals.expense, categoryColor(i))
                }
            }

            // 収入の内訳
            if (totals.income > 0 && incomeByCat.isNotEmpty()) {
                SectionHeader("収入の内訳")
                incomeByCat.forEach { t ->
                    CategoryBar(t.first, t.second, t.third, totals.income, IncomeColor)
                }
            }

            // 月次の推移（直近6か月）
            SectionHeader("月次の推移（直近6か月）")
            val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
            val points = months.map { ym ->
                val tt = Stats.totals(txns.filter { vm.monthOf(it.timestamp) == ym })
                TrendPoint(label = "${ym.monthValue}月", income = tt.income, expense = tt.expense)
            }
            MonthlyTrendBars(points, Modifier.padding(horizontal = 16.dp))
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(IncomeColor, "収入")
                LegendDot(ExpenseColor, "支出")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun CategoryBar(name: String, amount: Long, count: Int, total: Long, color: Color) {
    val frac = if (total > 0) (amount.toFloat() / total.toFloat()) else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$name（${count}件）", style = MaterialTheme.typography.bodyMedium)
            Text(yen(amount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { frac },
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
