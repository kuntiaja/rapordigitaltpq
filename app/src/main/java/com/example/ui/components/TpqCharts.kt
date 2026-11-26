package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.NilaiRecord
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.HadirGreen
import com.example.ui.theme.JadeSecondary

@Composable
fun SurahMasteryRing(
    completedCount: Int,
    totalCount: Int = 114,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary
    val progress = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)

    Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = accentColor.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = 14f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedCount",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "/ $totalCount Surat",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TajwidBreakdownChart(
    nilaiList: List<NilaiRecord>,
    modifier: Modifier = Modifier
) {
    val avgKelancaran = if (nilaiList.isEmpty()) 0f else nilaiList.map { it.scoreKelancaran }.average().toFloat()
    val avgMakhraj = if (nilaiList.isEmpty()) 0f else nilaiList.map { it.scoreMakhraj }.average().toFloat()
    val avgHukum = if (nilaiList.isEmpty()) 0f else nilaiList.map { it.scoreHukumTajwid }.average().toFloat()
    val avgMad = if (nilaiList.isEmpty()) 0f else nilaiList.map { it.scoreMadQalqalah }.average().toFloat()
    val avgFashohah = if (nilaiList.isEmpty()) 0f else nilaiList.map { it.scoreFashohah }.average().toFloat()

    val metrics = listOf(
        Triple("Kelancaran", avgKelancaran, EmeraldPrimary),
        Triple("Makhraj", avgMakhraj, JadeSecondary),
        Triple("Tajwid", avgHukum, GoldTertiary),
        Triple("Mad/Qal.", avgMad, HadirGreen),
        Triple("Fashohah", avgFashohah, MaterialTheme.colorScheme.primary)
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.forEach { (label, score, barColor) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                ) {
                    val radius = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(
                        color = trackColor,
                        size = size,
                        cornerRadius = radius
                    )
                    val fillW = (size.width * (score / 100f).coerceIn(0f, 1f))
                    if (fillW > 0f) {
                        drawRoundRect(
                            color = barColor,
                            size = Size(fillW, size.height),
                            cornerRadius = radius
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${score.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(30.dp)
                )
            }
        }
    }
}
