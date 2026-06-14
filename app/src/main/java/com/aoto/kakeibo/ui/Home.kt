package com.aoto.kakeibo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aoto.kakeibo.MainViewModel
import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.util.NotiAccess

@Composable
fun HomeScreen(
    vm: MainViewModel,
    txns: List<TxnEntity>,
    modifier: Modifier = Modifier,
    onEdit: (TxnEntity) -> Unit
) {
    val month = vm.selectedMonth
    val monthTxns = txns.filter { vm.monthOf(it.timestamp) == month }
    val total = monthTxns.sumOf { it.amount }

    Column(modifier.fillMaxSize()) {
        MonthHeader(monthLabel(month), total, onPrev = { vm.prevMonth() }, onNext = { vm.nextMonth() })
        if (!rememberNotiGranted()) NotiBanner()
        if (monthTxns.isEmpty()) {
            EmptyHint()
        } else {
            val grouped = monthTxns.groupBy { localDateOf(it.timestamp) }
            LazyColumn(Modifier.fillMaxSize()) {
                grouped.forEach { (date, list) ->
                    val dayTotal = list.sumOf { it.amount }
                    item(key = "head-$date") {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dateLabel(list.first().timestamp), style = MaterialTheme.typography.labelLarge)
                            Text(yen(dayTotal), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    items(list, key = { it.id }) { t ->
                        TxnRow(t) { onEdit(t) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TxnRow(t: TxnEntity, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceChip(t.source)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.merchant?.ifBlank { null } ?: t.category,
                style = MaterialTheme.typography.bodyLarge
            )
            Text("${t.category}・${timeLabel(t.timestamp)}", style = MaterialTheme.typography.bodySmall)
        }
        Text(yen(t.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotiBanner() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("通知アクセスが未許可です", fontWeight = FontWeight.Bold)
            Text(
                "決済通知を自動で記録するには、本アプリに通知アクセスを許可してください。",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { NotiAccess.openSettings(context) }) { Text("通知アクセスを許可") }
        }
    }
}

@Composable
private fun EmptyHint() {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("この月の記録はまだありません。", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "決済通知が届くと自動で追加されます。右下の ＋ から手動入力もできます。",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
