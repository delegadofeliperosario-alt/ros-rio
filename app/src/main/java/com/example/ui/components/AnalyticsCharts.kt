package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MasteryLevel
import com.example.domain.DailyEvolutionPoint
import com.example.domain.ErrorEtiologySummary
import com.example.domain.SubtopicMetrics
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily
import kotlin.math.roundToInt

/**
 * Indicador circular de aproveitamento com descrição acessível completa para leitores de tela
 * e combinação de cor + ícone/texto para acessibilidade a daltonismo.
 */
@Composable
fun MasteryRingGauge(
    accuracyPercent: Double,
    mastery: MasteryLevel,
    size: Dp = 88.dp,
    trackColor: Color = Color.White.copy(alpha = 0.2f),
    labelColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val activeColor = masteryForegroundColor(mastery)
    val accInt = accuracyPercent.roundToInt()
    val errInt = (100 - accInt).coerceAtLeast(0)
    Box(
        modifier = modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                contentDescription = "Aproveitamento de $accInt por cento de acertos e $errInt por cento de erros. Nível ${mastery.label}."
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = 8.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val sweep = ((accuracyPercent.coerceIn(0.0, 100.0) / 100.0) * 360.0).toFloat()
            drawArc(
                color = activeColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$accInt%",
                fontFamily = MonoTabularFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = labelColor
            )
            Text(
                text = mastery.shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun AccuracyEvolutionChart(
    points: List<DailyEvolutionPoint>,
    modifier: Modifier = Modifier
) {
    val palette = LocalMasteryColors.current
    val lineColor = palette.goldAccent
    val gridColor = palette.border
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val chartSummary = if (points.isEmpty()) {
        "Sem histórico diário registrado ainda."
    } else {
        val first = points.first()
        val last = points.last()
        "Evolução de desempenho em ${points.size} dias. Início em ${first.shortLabel} com ${first.accuracyPercent.roundToInt()}% e mais recente em ${last.shortLabel} com ${last.accuracyPercent.roundToInt()}%."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = chartSummary }
    ) {
        if (points.isEmpty()) {
            Text(
                text = "Resolva questões ao longo dos dias para visualizar sua curva de evolução.",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            return
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .padding(top = 8.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                val padX = 12.dp.toPx()
                val padY = 14.dp.toPx()
                val chartW = (size.width - padX * 2f).coerceAtLeast(1f)
                val chartH = (size.height - padY * 2f).coerceAtLeast(1f)

                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                    val y = padY + chartH * (1f - frac)
                    drawLine(
                        color = gridColor.copy(alpha = 0.8f),
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val coords = points.mapIndexed { idx, pt ->
                    val x = if (points.size == 1) {
                        padX + chartW / 2f
                    } else {
                        padX + (idx.toFloat() / (points.size - 1).toFloat()) * chartW
                    }
                    val y = padY + chartH * (1f - (pt.accuracyPercent.toFloat().coerceIn(0f, 100f) / 100f))
                    Offset(x, y)
                }

                if (coords.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, padY + chartH)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, padY + chartH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.25f),
                                lineColor.copy(alpha = 0.02f)
                            ),
                            startY = padY,
                            endY = padY + chartH
                        )
                    )

                    val strokePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                coords.forEachIndexed { idx, offset ->
                    val dotColor = when {
                        points[idx].accuracyPercent < 50.0 -> palette.critical
                        points[idx].accuracyPercent < 75.0 -> palette.intermediate
                        else -> palette.excellent
                    }
                    drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = offset)
                    drawCircle(color = dotColor, radius = 4.5.dp.toPx(), center = offset)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = if (points.size <= 4) points else listOf(
                points.first(),
                points[points.size / 2],
                points.last()
            )
            labels.forEach { pt ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                    Text(
                        text = "${pt.accuracyPercent.roundToInt()}% (${pt.totalQuestions}q)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Mapa de calor inclusivo: cada bloco combina cor, ícone semântico distinto, % de acertos,
 * % de erros e nível de domínio explícito, com foco de teclado e área mínima acessível.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultyHeatmapGrid(
    subtopics: List<SubtopicMetrics>,
    onSelectSubtopic: (SubtopicMetrics) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (subtopics.isEmpty()) {
            Text(
                text = "O mapa inteligente aparecerá assim que você registrar questões por subassunto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            subtopics.take(12).forEach { sub ->
                val fg = masteryForegroundColor(sub.mastery)
                val bg = masteryContainerColor(sub.mastery)
                val accInt = sub.accuracyPercent.roundToInt()
                val errInt = sub.errorPercent.roundToInt()
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, fg.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .heightIn(min = 84.dp)
                        .accessibleFocusRing(RoundedCornerShape(14.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Ver detalhes de ${sub.subtopic}"
                        ) { onSelectSubtopic(sub) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${sub.subtopic} em ${sub.discipline}. $accInt% de acertos, $errInt% de erros. Nível ${sub.mastery.label}."
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .width(152.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = masterySemanticIcon(sub.mastery),
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$accInt%",
                                    fontFamily = MonoTabularFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = fg
                                )
                            }
                            Text(
                                text = "Erro $errInt%",
                                style = MaterialTheme.typography.labelSmall,
                                color = fg
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = sub.subtopic,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${sub.discipline} • ${sub.mastery.shortLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorEtiologyBar(
    etiology: ErrorEtiologySummary,
    modifier: Modifier = Modifier
) {
    val palette = LocalMasteryColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (etiology.totalErrors <= 0) {
            Text(
                text = "Nenhum erro analisado ainda — quando errar uma questão, classificamos automaticamente lacuna teórica vs. desatenção.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val total = etiology.totalErrors.toFloat().coerceAtLeast(1f)
        val gapFrac = etiology.knowledgeGapCount / total
        val inattFrac = etiology.inattentionCount / total
        val trapFrac = etiology.boardTrapCount / total
        val forgetFrac = etiology.forgettingCount / total

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    contentDescription = "Distribuição de ${etiology.totalErrors} erros: ${etiology.knowledgeGapCount} lacuna teórica, ${etiology.inattentionCount} desatenção, ${etiology.boardTrapCount} pegadinha de banca e ${etiology.forgettingCount} esquecimento."
                }
        ) {
            if (gapFrac > 0f) Box(Modifier.weight(gapFrac).height(14.dp).background(palette.critical))
            if (inattFrac > 0f) Box(Modifier.weight(inattFrac).height(14.dp).background(palette.intermediate))
            if (trapFrac > 0f) Box(Modifier.weight(trapFrac).height(14.dp).background(palette.good))
            if (forgetFrac > 0f) Box(Modifier.weight(forgetFrac).height(14.dp).background(palette.low))
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EtiologyLegendItem("Lacuna teórica", etiology.knowledgeGapCount, palette.critical, Icons.Rounded.Psychology)
            EtiologyLegendItem("Desatenção", etiology.inattentionCount, palette.intermediate, Icons.Rounded.VisibilityOff)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EtiologyLegendItem("Pegadinha banca", etiology.boardTrapCount, palette.good, Icons.Rounded.ReportProblem)
            EtiologyLegendItem("Esquecimento", etiology.forgettingCount, palette.low, Icons.Rounded.BookmarkRemove)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = etiology.primaryDiagnosis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EtiologyLegendItem(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
