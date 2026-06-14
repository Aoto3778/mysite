package com.aoto.kakeibo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 明暗どちらのテーマでも読める固定色。
val IncomeColor = Color(0xFF2E9E5B)
val ExpenseColor = Color(0xFFE0533D)

private val CategoryPalette = listOf(
    Color(0xFF4C8C7B), Color(0xFFE8A33D), Color(0xFF2E7CF6), Color(0xFFE0533D),
    Color(0xFF9B7EDE), Color(0xFF49B6A8), Color(0xFFEC6A9C), Color(0xFF7BB23C),
    Color(0xFFF2B705), Color(0xFF8896A6)
)

fun categoryColor(index: Int): Color = CategoryPalette[index % CategoryPalette.size]

/** 支出カテゴリ等のドーナツ。values は (色, 金額)。 */
@Composable
fun DonutChart(
    values: List<Pair<Color, Long>>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
) {
    val total = values.sumOf { it.second }.coerceAtLeast(1L)
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val diameter = size.minDimension - stroke
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var start = -90f
        for ((color, value) in values) {
            val sweep = value.toFloat() / total.toFloat() * 360f
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            start += sweep
        }
    }
}

data class TrendPoint(val label: String, val income: Long, val expense: Long)

/** 直近数か月の収入/支出を対バーで表示（Canvas不使用）。 */
@Composable
fun MonthlyTrendBars(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val maxVal = (points.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1L).coerceAtLeast(1L)
    Row(
        modifier.fillMaxWidth().height(160.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (p in points) {
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.width(10.dp)
                            .fillMaxHeight((p.income.toFloat() / maxVal).coerceIn(0f, 1f))
                            .background(IncomeColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier.width(10.dp)
                            .fillMaxHeight((p.expense.toFloat() / maxVal).coerceIn(0f, 1f))
                            .background(ExpenseColor, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(p.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

/** 色の凡例ドット＋ラベル。 */
@Composable
fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
