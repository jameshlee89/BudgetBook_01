package com.budgetbook.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetbook.app.domain.CategorySlice
import com.budgetbook.app.domain.MoneyFormat
import com.budgetbook.app.domain.MonthlySummary
import com.budgetbook.app.ui.components.MonthSelector
import java.time.YearMonth

/** [REQ-CHART-001] [REQ-CHART-002] [REQ-CHART-003] 월별 항목별 지출 도넛 그래프 */
@Composable
fun StatsScreen(
    month: YearMonth,
    summary: MonthlySummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { MonthSelector(month, onPreviousMonth, onNextMonth) }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                DonutChart(summary, Modifier.size(240.dp))
            }
        }
        if (summary.slices.isEmpty()) {
            item {
                Text(
                    "이번 달 지출 내역이 없습니다.",
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(summary.slices, key = { it.category.name }) { slice ->
            LegendRow(slice)
            HorizontalDivider()
        }
    }
}

@Composable
fun DonutChart(summary: MonthlySummary, modifier: Modifier = Modifier) {
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.18f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            if (summary.slices.isEmpty()) {
                drawArc(emptyColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
            } else {
                var startAngle = -90f
                summary.slices.forEach { slice ->
                    val sweep = 360f * slice.ratio
                    drawArc(Color(slice.category.colorArgb), startAngle, sweep, false, topLeft, arcSize, style = Stroke(strokeWidth))
                    startAngle += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("총 지출", style = MaterialTheme.typography.labelMedium)
            Text(MoneyFormat.won(summary.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegendRow(slice: CategorySlice) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(Color(slice.category.colorArgb), CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(slice.category.label, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                "%.1f%%".format(slice.ratio * 100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(MoneyFormat.won(slice.amount), style = MaterialTheme.typography.bodyLarge)
    }
}
