package com.aoto.kakeibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MonthHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = "前の月") }
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "次の月") }
        }
    }
}

/** 収入・支出・差引の3カード。 */
@Composable
fun SummaryCards(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val balance = income - expense
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("収入", income, IncomeColor, Modifier.weight(1f))
        StatCard("支出", expense, ExpenseColor, Modifier.weight(1f))
        StatCard(
            if (balance >= 0) "差引（黒字）" else "差引（赤字）",
            balance,
            if (balance >= 0) IncomeColor else ExpenseColor,
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                yen(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SourceChip(source: String) {
    val color = when (source) {
        "iD" -> Color(0xFFE8A33D)
        "Vpass" -> Color(0xFF2E7CF6)
        "収入" -> IncomeColor
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(
            source,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
