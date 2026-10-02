package com.aira.app.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A simple bar chart: one rounded bar per value with its label underneath. [highlight] bars are drawn in
 * the strong [color], the others in a light version of it. With [showValues] each bar has its number on top.
 * An optional [target] is drawn as a dashed line across the chart, e.g. the 60-minute daylight goal, with
 * [targetLabel] ("Target 60m") at its right end. [valueSuffix] is added to the numbers on top ("45m").
 */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    highlight: (Int) -> Boolean = { false },
    showValues: Boolean = true,
    target: Int? = null,
    height: Dp = 150.dp,
    valueSuffix: String = "",
    targetLabel: String? = null,
) {
    val max = maxOf(values.maxOrNull() ?: 0, target ?: 0, 1)
    val valueSpace = if (showValues) 18.dp else 0.dp
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(height)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(if (values.size > 12) 2.dp else 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEach { value ->
                    val strong = highlight(value)
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (showValues) {
                            Text(
                                "$value$valueSuffix",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal,
                                color = if (strong) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        // Zero still gets a small stub, so every day is visible.
                        val barHeight = ((height - valueSpace) * (value.toFloat() / max)).coerceAtLeast(4.dp)
                        Box(
                            Modifier.fillMaxWidth().height(barHeight)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(if (strong) color else color.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
            if (target != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barArea = size.height - valueSpace.toPx()
                    val y = size.height - barArea * (target.toFloat() / max)
                    drawLine(
                        lineColor,
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }
                if (targetLabel != null) {
                    // Just above the line, at its right end.
                    val lineFromTop = valueSpace + (height - valueSpace) * (1f - target.toFloat() / max)
                    Text(
                        targetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = (lineFromTop - 16.dp).coerceAtLeast(0.dp))
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (values.size > 12) 2.dp else 8.dp)) {
            labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
