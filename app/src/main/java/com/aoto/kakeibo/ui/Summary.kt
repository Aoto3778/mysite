package com.aoto.kakeibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aoto.kakeibo.MainViewModel
import com.aoto.kakeibo.data.TxnEntity

@Composable
fun SummaryScreen(vm: MainViewModel, txns: List<TxnEntity>, modifier: Modifier = Modifier) {
    val month = vm.selectedMonth
    val monthTxns = txns.filter { vm.monthOf(it.timestamp) == month }
    val total = monthTxns.sumOf { it.amount }
    val byCat = monthTxns.groupBy { it.category }
        .map { (cat, list) -> Triple(cat, list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.second }

    Column(modifier.fillMaxSize()) {
        MonthHeader(monthLabel(month), total, onPrev = { vm.prevMonth() }, onNext = { vm.nextMonth() })
        if (byCat.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("この月の記録はまだありません。", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(byCat, key = { it.first }) { (cat, sum, count) ->
                    val frac = if (total > 0) sum.toFloat() / total else 0f
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$cat（${count}件）", style = MaterialTheme.typography.bodyLarge)
                            Text(yen(sum), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
