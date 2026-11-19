package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiDiagnosticReport
import com.example.data.model.UiState
import com.example.domain.DisciplineMetrics
import com.example.domain.PerformanceAnalyticsSnapshot
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.EditorialCard
import com.example.ui.components.EmptyDataGuidanceCard
import com.example.ui.components.ErrorEtiologyBar
import com.example.ui.components.MasteryPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.LocalAccessibilityConfig
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily
import com.example.ui.viewmodel.MentorChatState
import kotlin.math.roundToInt

/**
 * Tela de Diagnóstico Metacognitivo (Método Nilo).
 * Acessibilidade: respeita prefers-reduced-motion nas expansões de disciplina,
 * usa rótulos visíveis em campos, alertas com liveRegion Polite e alvos de toque >= 48dp.
 */
@Composable
fun DiagnosticsScreen(
    analytics: PerformanceAnalyticsSnapshot,
    latestDiagnostic: AiDiagnosticReport?,
    diagnosticGenState: UiState<Unit>,
    mentorChatState: MentorChatState,
    highlightedDiscipline: String?,
    onGenerateDiagnostic: () -> Unit,
    onAskMentor: (String) -> Unit,
    onOpenQConcursosHub: () -> Unit,
    onPracticeSubtopicInQConcursos: (discipline: String, subtopic: String) -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    val reducedMotion = LocalAccessibilityConfig.current.reducedMotion
    var expandedDiscipline by rememberSaveable(highlightedDiscipline) {
        mutableStateOf(highlightedDiscipline ?: analytics.disciplines.firstOrNull()?.discipline)
    }
    var mentorPrompt by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("diagnostics_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeader(
                title = "Diagnóstico Metacognitivo",
                subtitle = "Análise profunda: Disciplina › Assunto › Subassunto › Microtópico"
            )
        }

        if (analytics.totalQuestions == 0) {
            item {
                EmptyDataGuidanceCard(
                    icon = Icons.Rounded.Psychology,
                    title = "Sem questões para diagnosticar",
                    description = "Importe ou registre questões resolvidas no QConcursos para que o Método Nilo identifique exatamente o que você não domina, por que erra e quais artigos de lei revisar.",
                    primaryActionLabel = "Registrar ou Importar Questões",
                    onPrimaryAction = onOpenQConcursosHub,
                    actionTestTag = "diag_empty_open_hub_button"
                )
            }
            return@LazyColumn
        }

        // 1. Trigger Deep AI Pedagogical Diagnosis
        item {
            EditorialCard(containerColor = masteryColors.heroNavy) {
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
                            text = "MÉTODO NILO IA • DIAGNÓSTICO EM 5 PERGUNTAS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE6B0)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = latestDiagnostic?.headline?.ifBlank { analytics.quickMentorHeadline }
                            ?: analytics.quickMentorHeadline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF7F8FA)
                    )
                    Spacer(Modifier.height(16.dp))

                    if (diagnosticGenState is UiState.Error) {
                        AccessibleAlertBanner(
                            message = diagnosticGenState.message,
                            isError = true,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = onGenerateDiagnostic,
                        enabled = diagnosticGenState !is UiState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = masteryColors.goldAccent,
                            contentColor = masteryColors.heroNavy
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .accessibleFocusRing(RoundedCornerShape(14.dp))
                            .testTag("generate_ai_diagnostic_button")
                    ) {
                        if (diagnosticGenState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = masteryColors.heroNavy,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Cruzando edital, erros e jurisprudência...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (latestDiagnostic == null) {
                                    "Gerar Diagnóstico Metacognitivo IA"
                                } else {
                                    "Atualizar Diagnóstico com Novos Dados"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. The 5 Core Metacognitive Questions
        if (latestDiagnostic != null) {
            item {
                EditorialCard {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DiagnosticQuestionBlock("1. O QUE EU AINDA NÃO DOMINO?", latestDiagnostic.whatIDontKnow)
                        HorizontalDivider(color = masteryColors.border)
                        DiagnosticQuestionBlock("2. ONDE ESTOU ERRANDO?", latestDiagnostic.whereIAmFailing)
                        HorizontalDivider(color = masteryColors.border)
                        DiagnosticQuestionBlock("3. POR QUE ESTOU ERRANDO?", latestDiagnostic.whyIAmFailing)
                        HorizontalDivider(color = masteryColors.border)
                        DiagnosticQuestionBlock("4. QUAL SUBTÓPICO ESTUDAR E QUAIS PONTOS FORTES MANTER?", latestDiagnostic.whichSubtopicToStudy)
                        HorizontalDivider(color = masteryColors.border)
                        DiagnosticQuestionBlock("5. O QUE DEVO REVISAR EM SEGUIDA?", latestDiagnostic.whatToReviewNow)
                    }
                }
            }

            if (latestDiagnostic.legalFoundations.isNotEmpty() || latestDiagnostic.jurisprudenceNotes.isNotEmpty()) {
                item {
                    EditorialCard {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (latestDiagnostic.legalFoundations.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Balance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Dispositivos Legais e Fundamentos",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                latestDiagnostic.legalFoundations.forEach { item ->
                                    BulletDetailRow(item)
                                }
                            }
                            if (latestDiagnostic.jurisprudenceNotes.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Gavel,
                                        contentDescription = null,
                                        tint = masteryColors.goldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Entendimentos Jurisprudenciais (STF / STJ)",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                latestDiagnostic.jurisprudenceNotes.forEach { item ->
                                    BulletDetailRow(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Etiology Breakdown & Repeated Errors
        item {
            SectionHeader(
                title = "Padrão de Erros e Reincidência",
                subtitle = "Separação entre lacuna conceitual, desatenção e pegadinhas"
            )
            Spacer(Modifier.height(10.dp))
            EditorialCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    ErrorEtiologyBar(etiology = analytics.errorEtiology)
                    if (analytics.repeatedErrors.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = masteryColors.border)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = null,
                                tint = masteryColors.critical,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Erros Repetidos Detectados",
                                style = MaterialTheme.typography.titleSmall,
                                color = masteryColors.critical
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        analytics.repeatedErrors.take(5).forEach { rep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rep.identifier} • ${rep.discipline}",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${rep.topic} › ${rep.subtopic}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = masteryColors.criticalContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${rep.occurrences}x erros",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = masteryColors.critical,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Hierarchical Tree: Discipline -> Topic -> Subtopic -> Microtopic
        item {
            SectionHeader(
                title = "Raio-X por Disciplina e Subassunto",
                subtitle = "Toque em uma disciplina para expandir acertos, erros e microtópicos"
            )
        }

        items(analytics.disciplines, key = { it.discipline }) { disc ->
            DisciplineDrillDownCard(
                discipline = disc,
                isExpanded = expandedDiscipline == disc.discipline,
                reducedMotion = reducedMotion,
                onToggle = {
                    expandedDiscipline = if (expandedDiscipline == disc.discipline) null else disc.discipline
                },
                onPracticeSubtopic = { subtopic ->
                    onPracticeSubtopicInQConcursos(disc.discipline, subtopic)
                }
            )
        }

        // 5. Streaming Mentor Q&A Card
        item {
            SectionHeader(
                title = "Consultar Mentor Método Nilo",
                subtitle = "Tire dúvidas jurídicas ou de estratégia com base no seu edital e desempenho"
            )
            Spacer(Modifier.height(10.dp))
            EditorialCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AccessibleFieldLabel(text = "Sua pergunta para o Mentor IA")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = mentorPrompt,
                            onValueChange = { mentorPrompt = it },
                            placeholder = {
                                Text("Ex: Por que erro tanto em Culpabilidade?")
                            },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mentor_chat_input")
                        )
                        Spacer(Modifier.width(10.dp))
                        IconButton(
                            onClick = {
                                if (mentorPrompt.isNotBlank()) {
                                    onAskMentor(mentorPrompt)
                                    mentorPrompt = ""
                                }
                            },
                            enabled = !mentorChatState.isStreaming && mentorPrompt.isNotBlank(),
                            modifier = Modifier
                                .size(52.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("mentor_chat_send_button")
                        ) {
                            if (mentorChatState.isStreaming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Enviar pergunta ao Mentor",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (!mentorChatState.errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        AccessibleAlertBanner(
                            message = mentorChatState.errorMessage,
                            isError = true
                        )
                    }

                    if (mentorChatState.responseText.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Você: ${mentorChatState.lastQuestion}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = mentorChatState.responseText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticQuestionBlock(question: String, answer: String) {
    Column {
        Text(
            text = question,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = answer.ifBlank { "Sem observações adicionais." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BulletDetailRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DisciplineDrillDownCard(
    discipline: DisciplineMetrics,
    isExpanded: Boolean,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
    onPracticeSubtopic: (String) -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    val accInt = discipline.accuracyPercent.roundToInt()
    val errInt = discipline.errorPercent.roundToInt()

    EditorialCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .accessibleFocusRing(RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isExpanded) "Recolher ${discipline.discipline}" else "Expandir ${discipline.discipline}",
                        onClick = onToggle
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = discipline.discipline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Acertos: $accInt% • Erros: $errInt% (${discipline.correctQuestions}/${discipline.totalQuestions}) • ${discipline.avgTimeSeconds}s/q",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                MasteryPill(level = discipline.mastery, compact = true)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val expandedContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = masteryColors.border)
                    discipline.topics.forEach { topic ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topic.topic,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${topic.accuracyPercent.roundToInt()}% acertos • ${topic.errorPercent.roundToInt()}% erros",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = MonoTabularFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            topic.subtopics.forEach { sub ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sub.subtopic,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            MasteryPill(level = sub.mastery, compact = true)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "Acertos: ${sub.accuracyPercent.roundToInt()}% • Erros: ${sub.errorPercent.roundToInt()}% (${sub.correctQuestions}/${sub.totalQuestions}) • ${sub.inferredCauseLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (sub.microtopics.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            sub.microtopics.forEach { micro ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "↳ ${micro.microtopic}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "${micro.accuracyPercent.roundToInt()}% (${micro.correctQuestions}/${micro.totalQuestions})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontFamily = MonoTabularFamily,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Surface(
                                            onClick = { onPracticeSubtopic(sub.subtopic) },
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .accessibleFocusRing(RoundedCornerShape(10.dp))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = "Treinar este subassunto no QConcursos",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (reducedMotion) {
                if (isExpanded) expandedContent()
            } else {
                AnimatedVisibility(visible = isExpanded) { expandedContent() }
            }
        }
    }
}
