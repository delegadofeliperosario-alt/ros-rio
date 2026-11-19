package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.RevisionPriority
import com.example.data.model.RevisionTask
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.domain.PerformanceAnalyticsEngine
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.EditorialCard
import com.example.ui.components.EmptyDataGuidanceCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.accessibleFocusRing
import com.example.ui.components.priorityColors
import com.example.ui.components.prioritySemanticIcon
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily

/**
 * Plano de Revisão Inteligente (Autorregulação — Método Nilo).
 * Acessibilidade: prioridades combinam cor + ícone geométrico + texto explícito,
 * campos de diálogo usam rótulos visíveis (AccessibleFieldLabel) e todos os botões possuem >= 48dp.
 */
@Composable
fun RevisionPlanScreen(
    profile: UserProfile?,
    tasksState: UiState<List<RevisionTask>>,
    revisionGenState: UiState<Unit>,
    hasQuestions: Boolean,
    onGeneratePlan: () -> Unit,
    onToggleTask: (taskId: String, completed: Boolean) -> Unit,
    onDeleteTask: (taskId: String) -> Unit,
    onUpdateExamTarget: (name: String, exam: String, board: String, dateIso: String, dailyGoal: Int, qLink: String) -> Unit,
    onPracticeInQConcursos: (discipline: String, subtopic: String, board: String) -> Unit,
    onOpenQConcursosHub: () -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    var showTargetDialog by remember { mutableStateOf(false) }

    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val completedCount = tasks.count { it.completed }
    val progress = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size.toFloat()
    val daysLeft = profile?.examDateIso?.let { PerformanceAnalyticsEngine.daysUntilExam(it) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("revision_plan_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeader(
                title = "Plano de Revisão Inteligente",
                subtitle = "Priorização por gravidade, cobertura do edital, banca e data da prova"
            )
        }

        // Exam Target & Strategy Header Card
        item {
            EditorialCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.targetExam?.ifBlank { "Concurso Alvo não definido" }
                                    ?: "Concurso Alvo não definido",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Banca: ${profile?.examBoard ?: "Cebraspe"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = masteryColors.goldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            daysLeft == null -> "Sem data definida"
                                            daysLeft < 0 -> "Prova realizada"
                                            daysLeft == 0L -> "Prova hoje!"
                                            else -> "Faltam $daysLeft dias"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showTargetDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                                .testTag("edit_exam_target_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Editar concurso e data da prova"
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (revisionGenState is UiState.Error) {
                        AccessibleAlertBanner(
                            message = revisionGenState.message,
                            isError = true,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Button(
                        onClick = onGeneratePlan,
                        enabled = revisionGenState !is UiState.Loading && hasQuestions,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .accessibleFocusRing(RoundedCornerShape(14.dp))
                            .testTag("generate_revision_plan_button")
                    ) {
                        if (revisionGenState is UiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Calculando ordem ótima de revisão...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (tasks.isEmpty()) "Gerar Plano de Revisão IA" else "Recalcular Plano com IA",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (tasks.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progresso do ciclo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$completedCount / ${tasks.size} concluídas",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonoTabularFamily
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .semantics {
                                    contentDescription = "$completedCount de ${tasks.size} revisões concluídas."
                                },
                            color = masteryColors.excellent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        if (!hasQuestions) {
            item {
                EmptyDataGuidanceCard(
                    icon = Icons.Rounded.School,
                    title = "Registre questões para personalizar sua revisão",
                    description = "O plano de revisão inteligente do Método Nilo cruza seu edital e suas maiores dificuldades reais no QConcursos com a incidência da banca examinadora e a data da prova.",
                    primaryActionLabel = "Ir para Questões",
                    onPrimaryAction = onOpenQConcursosHub,
                    actionTestTag = "revision_empty_open_hub_button"
                )
            }
            return@LazyColumn
        }

        if (tasksState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (tasks.isEmpty()) {
            item {
                EditorialCard {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Pronto para montar sua trilha",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Toque em \"Gerar Plano de Revisão IA\" acima para receber a sequência exata de tópicos que trazem o maior ganho de pontuação para sua banca.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(tasks, key = { it.id.ifBlank { "${it.orderIndex}_${it.subtopic}" } }) { task ->
                RevisionTaskCard(
                    task = task,
                    examBoard = profile?.examBoard ?: "Cebraspe",
                    onToggle = { onToggleTask(task.id, !task.completed) },
                    onDelete = { onDeleteTask(task.id) },
                    onPracticeInQConcursos = {
                        onPracticeInQConcursos(
                            task.discipline,
                            task.subtopic,
                            profile?.examBoard ?: "Cebraspe"
                        )
                    }
                )
            }
        }
    }

    if (showTargetDialog) {
        EditExamTargetDialog(
            profile = profile,
            onDismiss = { showTargetDialog = false },
            onSave = { name, exam, board, dateIso, goal, link ->
                onUpdateExamTarget(name, exam, board, dateIso, goal, link)
                showTargetDialog = false
            }
        )
    }
}

@Composable
private fun RevisionTaskCard(
    task: RevisionTask,
    examBoard: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onPracticeInQConcursos: () -> Unit
) {
    val priority = RevisionPriority.fromWire(task.priority)
    val (fg, bg) = priorityColors(priority)
    val masteryColors = LocalMasteryColors.current

    EditorialCard(
        containerColor = if (task.completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(48.dp)
                        .accessibleFocusRing(RoundedCornerShape(12.dp))
                        .testTag("toggle_revision_task_${task.orderIndex}")
                ) {
                    Icon(
                        imageVector = if (task.completed) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.RadioButtonUnchecked
                        },
                        contentDescription = if (task.completed) "Marcar ${task.subtopic} como pendente" else "Concluir revisão de ${task.subtopic}",
                        tint = if (task.completed) masteryColors.excellent else MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, fg.copy(alpha = 0.25f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = prioritySemanticIcon(priority),
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "#${task.orderIndex + 1} • ${priority.label.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${task.estimatedMinutes} min • ${task.targetQuestions}q",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${task.discipline} › ${task.topic}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = task.subtopic,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .accessibleFocusRing(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remover tarefa ${task.subtopic}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Por que priorizar: ${task.rationale}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = masteryColors.border)
            Spacer(Modifier.height(10.dp))
            Text(
                text = task.studyActions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (task.legalReferences.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Base legal / Jurisprudência: ${task.legalReferences}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onPracticeInQConcursos,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .accessibleFocusRing(RoundedCornerShape(12.dp))
                    .testTag("practice_task_qconcursos_${task.orderIndex}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Filtrar ${task.targetQuestions} questões ($examBoard) no QConcursos")
            }
        }
    }
}

@Composable
fun EditExamTargetDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, exam: String, board: String, dateIso: String, goal: Int, qLink: String) -> Unit
) {
    var name by remember { mutableStateOf(profile?.studentName ?: "") }
    var targetExam by remember { mutableStateOf(profile?.targetExam ?: "") }
    var board by remember { mutableStateOf(profile?.examBoard ?: "Cebraspe") }
    var examDate by remember { mutableStateOf(profile?.examDateIso ?: "") }
    var dailyGoal by remember { mutableStateOf((profile?.dailyGoalQuestions ?: 30L).toString()) }
    var qLink by remember {
        mutableStateOf(
            profile?.qconcursosLink?.ifBlank { "https://www.qconcursos.com/questoes-de-concursos/questoes" }
                ?: "https://www.qconcursos.com/questoes-de-concursos/questoes"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meta de Concurso e QConcursos") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    AccessibleFieldLabel("Seu nome (para diagnósticos)", required = true)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Concurso / Cargo alvo")
                    OutlinedTextField(
                        value = targetExam,
                        onValueChange = { targetExam = it },
                        placeholder = { Text("Ex: Analista Judiciário TRF") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Banca examinadora principal", required = true)
                    OutlinedTextField(
                        value = board,
                        onValueChange = { board = it },
                        placeholder = { Text("Ex: Cebraspe, FGV, FCC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Data da prova (AAAA-MM-DD)")
                    OutlinedTextField(
                        value = examDate,
                        onValueChange = { examDate = it },
                        placeholder = { Text("Ex: 2026-12-20") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Meta diária de questões")
                    OutlinedTextField(
                        value = dailyGoal,
                        onValueChange = { dailyGoal = it.filter(Char::isDigit) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Link padrão do QConcursos / Caderno")
                    OutlinedTextField(
                        value = qLink,
                        onValueChange = { qLink = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.ifBlank { "Estudante" },
                        targetExam,
                        board.ifBlank { "Cebraspe" },
                        examDate,
                        dailyGoal.toIntOrNull()?.coerceIn(1, 500) ?: 30,
                        qLink
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_exam_target_button")
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
