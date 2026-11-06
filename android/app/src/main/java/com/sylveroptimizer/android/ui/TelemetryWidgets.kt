package com.sylveroptimizer.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sylveroptimizer.android.services.ColorThemeDefinition

val ColorWarningAmber = Color(0xFFFFB300)
val ColorCriticalRed = Color(0xFFFF5252)

fun resolveLoadColor(
    percent: Double,
    isCpu: Boolean,
    theme: ColorThemeDefinition
): Color {
    return when {
        percent >= 85.0 -> ColorCriticalRed
        percent >= 68.0 -> ColorWarningAmber
        isCpu -> theme.accentSecondary
        else -> theme.accentPrimary
    }
}

@Composable
fun CircularMetricGauge(
    percent: Double,
    primaryText: String,
    secondaryText: String,
    arcColor: Color,
    trackColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcDiameter = size.minDimension - inset * 2f
            val topLeft = Offset(
                (size.width - arcDiameter) / 2f,
                (size.height - arcDiameter) / 2f
            )
            val arcSize = Size(arcDiameter, arcDiameter)

            // Anillo base
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            // Arco dinámico de carga
            val clamped = percent.coerceIn(0.5, 99.9).toFloat()
            val sweep = (clamped / 100f) * 360f
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primaryText,
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = secondaryText,
                color = textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RealTimeAreaChart(
    title: String,
    values: List<Double>,
    lineColor: Color,
    areaColor: Color,
    subCardBg: Color,
    subCardBorder: Color,
    textMuted: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(subCardBg)
            .border(1.dp, subCardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            text = title,
            color = textMuted,
            fontSize = 10.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 8f || h <= 8f || values.size < 2) return@Canvas

            val maxPoints = values.size.coerceAtLeast(2)
            val stepX = w / (maxPoints - 1).toFloat()

            val linePath = Path()
            val areaPath = Path()
            areaPath.moveTo(0f, h)

            values.forEachIndexed { index, rawVal ->
                val x = index * stepX
                val normalized = (rawVal / 100.0).coerceIn(0.0, 1.0).toFloat()
                val y = h - (normalized * (h - 6.dp.toPx())) - 3.dp.toPx()

                if (index == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }
                areaPath.lineTo(x, y)
            }

            areaPath.lineTo(w, h)
            areaPath.close()

            drawPath(path = areaPath, color = areaColor)
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun MetricDetailTile(
    label: String,
    value: String,
    valueColor: Color,
    theme: ColorThemeDefinition,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.subCardBg)
            .border(1.dp, theme.subCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = theme.textSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ThemePreviewCard(
    themeItem: ColorThemeDefinition,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) themeItem.accentPrimary else themeItem.cardBorder
    val statusText = if (isSelected) "✓ TEMA ACTIVO" else "Aplicar este tema"
    val statusColor = if (isSelected) themeItem.accentPrimary else themeItem.textSecondary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() },
        color = themeItem.windowBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = themeItem.name,
                    color = themeItem.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(themeItem.cardBg)
                        .border(1.dp, themeItem.cardBorder, RoundedCornerShape(5.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = themeItem.badgeText,
                        color = themeItem.accentPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Miniatura visual de la paleta de colores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeItem.cardBg)
                    .border(1.dp, themeItem.cardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(themeItem.accentPrimary)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(themeItem.accentSecondary)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(themeItem.accentTertiary)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(themeItem.accentPrimary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(themeItem.accentSecondary)
                    )
                }

                Text(
                    text = "⚡",
                    color = themeItem.accentPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = themeItem.subtitle,
                color = themeItem.textSecondary,
                fontSize = 11.sp,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, themeItem.secondaryButtonBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = themeItem.secondaryButtonBg,
                    contentColor = statusColor
                )
            ) {
                Text(
                    text = statusText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

