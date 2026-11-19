package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiDiagnosticReport
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.domain.PerformanceAnalyticsEngine
import com.example.domain.PerformanceAnalyticsSnapshot
import com.example.domain.SubtopicMetrics
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.AccuracyEvolutionChart
import com.example.ui.components.DifficultyHeatmapGrid
import com.example.ui.components.EditorialCard
import com.example.ui.components.EmptyDataGuidanceCard
import com.example.ui.components.ErrorEtiologyBar
import com.example.ui.components.KpiTile
import com.example.ui.components.MasteryPill
import com.example.ui.components.MasteryRingGauge
import com.example.ui.components.SectionHeader
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily
import kotlin.math.roundToInt

/**
 * Painel Principal do Método Nilo — Mapa Inteligente de Desempenho e Metacognição.
 * Decisões de acessibilidade aplicadas:
 * - Hierarquia visual limpa com respiro generoso (18dp entre seções) e paleta 60-30-10 WCAG AA.
 * - Pontos Fortes (a manter) e Pontos Fracos (a aprimorar) combinam cor, ícone e percentuais explícitos de acertos e erros.
 * - Todos os cartões e botões interativos garantem área de toque >= 48dp e foco de teclado visível.
 */
@Composable
fun DashboardScreen(
    profile: UserProfile?,
    analytics: PerformanceAnalyticsSnapshot,
    latestDiagnostic: AiDiagnosticReport?,
    attemptsUiState: UiState<*>,
    onOpenEdital: () -> Unit,
    onOpenQConcursosHub: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenRevisionPlan: () -> Unit,
    onSelectSubtopic: (SubtopicMetrics) -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    val studentName = profile?.studentName?.ifBlank { "Estudante" } ?: "Estudante"
    val daysToExam = profile?.examDateIso?.let { PerformanceAnalyticsEngine.daysUntilExam(it) }
    val dailyGoal = (profile?.dailyGoalQuestions ?: 30L).toInt()
    val edital = analytics.editalCoverage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Hero Header Método Nilo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(masteryColors.heroNavy)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_mentor_banner_1795048557582),
                    contentDescription = null, // Decorativo — informações transmitidas pelos textos acessíveis
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    masteryColors.heroNavy.copy(alpha = 0.95f),
                                    masteryColors.heroNavy.copy(alpha = 0.82f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MÉTODO NILO • OLÁ, ${studentName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE6B0)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = profile?.targetExam?.ifBlank { "Metacognição & Autorregulação" }
                                ?: "Metacognição & Autorregulação",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFF7F8FA),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "Banca: ${profile?.examBoard ?: "Cebraspe"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF7F8FA),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            if (daysToExam != null && daysToExam >= 0) {
                                Surface(
                                    color = masteryColors.goldAccent.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = "${daysToExam}d p/ prova",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDE6B0),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))
                    MasteryRingGauge(
                        accuracyPercent = analytics.overallAccuracyPercent,
                        mastery = analytics.overallMastery,
                        size = 88.dp
                    )
                }
            }
        }

        // 2. Edital Coverage Progress Card (always visible to guide onboarding or show live progress)
        item {
            EditorialCard(
                modifier = Modifier
                    .accessibleFocusRing(RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Abrir meu edital verticalizado",
                        onClick = onOpenEdital
                    )
                    .testTag("dashboard_edital_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (edital.totalItems > 0) edital.editalTitle else "Cadastre ou Anexe seu Edital",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (edital.totalItems > 0) {
                                        "${edital.studiedItems}/${edital.totalItems} subassuntos cobertos (${edital.coveragePercent.roundToInt()}%) • ${edital.masteredItems} dominado(s)"
                                    } else {
                                        "Envie PDF, foto ou texto do edital para cruzar automaticamente com suas questões"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Ir para o Edital",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (edital.totalItems > 0) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (edital.coveragePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (attemptsUiState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (attemptsUiState is UiState.Error) {
            item {
                AccessibleAlertBanner(message = attemptsUiState.message, isError = true)
            }
        }

        if (analytics.totalQuestions == 0) {
            item {
                EmptyDataGuidanceCard(
                    icon = Icons.Rounded.Link,
                    title = "Conecte seu histórico do QConcursos",
                    description = "Após cadastrar seu edital, abra a aba Questões para estudar no Portal QConcursos, importar prints de desempenho com IA ou registrar suas baterias. O Método Nilo gerará automaticamente seu Mapa Inteligente de Desempenho.",
                    primaryActionLabel = "Conectar Questões Agora",
                    onPrimaryAction = onOpenQConcursosHub,
                    actionTestTag = "empty_open_qconcursos_button"
                )
            }
            return@LazyColumn
        }

        // 3. Metacognitive Daily Direction Card
        item {
            EditorialCard(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = masteryColors.goldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (latestDiagnostic != null) "DIRECIONAMENTO METACOGNITIVO IA" else "DIAGNÓSTICO ORIENTADO POR DADOS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = latestDiagnostic?.headline?.ifBlank { analytics.quickMentorHeadline }
                            ?: analytics.quickMentorHeadline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            onClick = onOpenDiagnostics,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                                .testTag("dashboard_open_diagnosis_cta")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Diagnóstico",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Surface(
                            onClick = onOpenRevisionPlan,
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                                .testTag("dashboard_open_revision_cta")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Plano de Revisão",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. KPI Grid (2x2) with explicit hit & error metrics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiTile(
                        label = "Acertos vs. Erros",
                        value = "${analytics.overallAccuracyPercent.roundToInt()}% / ${analytics.overallErrorPercent.roundToInt()}%",
                        caption = "${analytics.totalCorrect} acertos • ${analytics.totalWrong} erros (${analytics.totalQuestions}q)",
                        icon = Icons.Rounded.CheckCircle,
                        accentColor = masteryColors.excellent,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Meta de Hoje",
                        value = "${analytics.todayQuestions}/$dailyGoal",
                        caption = "Sequência ativa: ${analytics.studyStreakDays} dia(s)",
                        icon = Icons.Rounded.Bolt,
                        accentColor = masteryColors.goldAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiTile(
                        label = "Tempo Médio / Q",
                        value = "${analytics.avgTimeSecondsPerQuestion}s",
                        caption = if (analytics.avgTimeSecondsPerQuestion > 95) "Ritmo lento — revisar base" else "Ritmo equilibrado",
                        icon = Icons.Rounded.Schedule,
                        accentColor = masteryColors.good,
                        modifier = Modifier.weight(1f)
                    )
                    KpiTile(
                        label = "Pontos Críticos",
                        value = "${analytics.rankedDifficultSubtopics.count { it.accuracyPercent < 65.0 }}",
                        caption = "${analytics.repeatedErrors.size} erro(s) recorrente(s)",
                        icon = Icons.Rounded.WarningAmber,
                        accentColor = masteryColors.critical,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Smart Performance Map: Pontos Fortes (Manter) vs. Pontos Fracos (Aprimorar)
        item {
            SectionHeader(
                title = "Mapa Inteligente de Desempenho",
                subtitle = "Separação clara entre pontos fortes a manter e fragilidades prioritárias",
                actionLabel = "Raio-X",
                onActionClick = onOpenDiagnostics
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Pontos Fortes Card
                EditorialCard(containerColor = masteryColors.excellentContainer.copy(alpha = 0.55f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Pontos fortes",
                                tint = masteryColors.excellent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Pontos Fortes — Manter com Revisão Espaçada",
                                style = MaterialTheme.typography.titleSmall,
                                color = masteryColors.excellent
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (analytics.masteredSubtopics.isEmpty()) {
                            Text(
                                text = "Continue resolvendo questões: subassuntos acima de 78% de acertos serão destacados aqui como consolidados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            analytics.masteredSubtopics.take(3).forEach { strong ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strong.subtopic,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = strong.discipline,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${strong.accuracyPercent.roundToInt()}% acertos",
                                        fontFamily = MonoTabularFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = masteryColors.excellent
                                    )
                                }
                            }
                        }
                    }
                }

                // Visual Heatmap Grid
                EditorialCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Mapa de Calor por Subassunto (Acertos e Erros)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(10.dp))
                        DifficultyHeatmapGrid(
                            subtopics = analytics.rankedDifficultSubtopics.ifEmpty { analytics.masteredSubtopics },
                            onSelectSubtopic = onSelectSubtopic
                        )
                    }
                }
            }
        }

        // 6. Historical Evolution Chart
        item {
            SectionHeader(
                title = "Evolução Contínua",
                subtitle = "Acompanhamento automático conforme novas questões são resolvidas"
            )
            Spacer(Modifier.height(10.dp))
            EditorialCard {
                Box(modifier = Modifier.padding(16.dp)) {
                    AccuracyEvolutionChart(points = analytics.dailyEvolution)
                }
            }
        }

        // 7. Error Etiology
        item {
            SectionHeader(
                title = "Lacuna Teórica vs. Desatenção",
                subtitle = "Autorregulação: entenda o motivo real dos seus erros"
            )
            Spacer(Modifier.height(10.dp))
            EditorialCard {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorEtiologyBar(etiology = analytics.errorEtiology)
                }
            }
        }

        // 8. Priority Weaknesses Ranking
        if (analytics.rankedDifficultSubtopics.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Pontos Fracos — Prioridade de Revisão",
                    subtitle = "Ordenados por percentual de erros, reincidência e gravidade",
                    actionLabel = "Analisar IA",
                    onActionClick = onOpenDiagnostics
                )
            }
            items(analytics.rankedDifficultSubtopics.take(5)) { sub ->
                val accInt = sub.accuracyPercent.roundToInt()
                val errInt = sub.errorPercent.roundToInt()
                EditorialCard(
                    modifier = Modifier
                        .accessibleFocusRing(RoundedCornerShape(16.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Abrir diagnóstico de ${sub.subtopic}"
                        ) { onSelectSubtopic(sub) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${sub.subtopic}, ${sub.discipline}. $accInt% acertos e $errInt% erros. Nível ${sub.mastery.label}."
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${sub.discipline} › ${sub.topic}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = sub.subtopic,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Acertos: $accInt% • Erros: $errInt% • ${sub.inferredCauseLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        MasteryPill(level = sub.mastery, compact = true)
                    }
                }
            }
        }
    }
}
