package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ErrorCause
import com.example.data.model.MasteryLevel
import com.example.data.model.QuestionAttempt
import com.example.data.model.SourceType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.EditorialCard
import com.example.ui.components.EmptyDataGuidanceCard
import com.example.ui.components.MasteryPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.AccessibilityConfig
import com.example.ui.theme.MonoTabularFamily
import com.example.ui.theme.ThemeMode
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Tela de Histórico, Perfil e Preferências de Acessibilidade (Método Nilo).
 * Inclui controles explícitos de Modo Claro/Escuro (conforto ocular) e Redução de Animações
 * (sensibilidade vestibular), além de rótulos visíveis e alvos de toque >= 48dp.
 */
@Composable
fun HistoryAndProfileScreen(
    profile: UserProfile?,
    userEmail: String?,
    attemptsState: UiState<List<QuestionAttempt>>,
    accessibilityConfig: AccessibilityConfig,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onDeleteAttempt: (String) -> Unit,
    onUpdateProfile: (name: String, exam: String, board: String, dateIso: String, dailyGoal: Int, qLink: String) -> Unit,
    onSignOut: () -> Unit,
    onOpenQConcursosHub: () -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDiscipline by rememberSaveable { mutableStateOf<String?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val attempts = (attemptsState as? UiState.Success)?.data.orEmpty()
    val disciplines = remember(attempts) {
        attempts.map { it.discipline }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredAttempts = remember(attempts, searchQuery, selectedDiscipline) {
        attempts.filter { item ->
            val matchesDisc = selectedDiscipline == null || item.discipline == selectedDiscipline
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                item.discipline.lowercase().contains(q) ||
                item.topic.lowercase().contains(q) ||
                item.subtopic.lowercase().contains(q) ||
                item.microtopic.lowercase().contains(q) ||
                item.qconcursosCode.lowercase().contains(q) ||
                item.notes.lowercase().contains(q)
            matchesDisc && matchesSearch
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onNavigateBack != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .accessibleFocusRing(RoundedCornerShape(12.dp))
                            .testTag("history_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Voltar ao painel principal"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Histórico, Conta e Acessibilidade",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // Account & Exam Profile Summary Card
        item {
            EditorialCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = "Perfil do estudante",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.studentName?.ifBlank { "Estudante" } ?: "Estudante",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = userEmail ?: "Conta Google conectada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                                .testTag("open_profile_settings_button")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Configurar perfil e prova")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .accessibleFocusRing(RoundedCornerShape(12.dp))
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sair da conta")
                    }
                }
            }
        }

        // Accessibility & Visual Comfort Card
        item {
            EditorialCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccessibilityNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Acessibilidade e Conforto Visual",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Column {
                        AccessibleFieldLabel("Tema de Cores (WCAG AA sem branco/preto puros)")
                        val modes = listOf(
                            ThemeMode.SYSTEM to "Sistema",
                            ThemeMode.LIGHT to "Claro",
                            ThemeMode.DARK to "Escuro"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { idx, (mode, label) ->
                                SegmentedButton(
                                    selected = accessibilityConfig.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reduzir animações",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Desativa transições bruscas para conforto vestibular e neurodivergência",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = accessibilityConfig.reducedMotion,
                            onCheckedChange = onSetReducedMotion,
                            modifier = Modifier.testTag("reduced_motion_switch")
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Histórico de Questões (${filteredAttempts.size})",
                subtitle = "Todos os registros sincronizados em tempo real"
            )
        }

        if (attemptsState is UiState.Loading) {
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

        if (attempts.isEmpty()) {
            item {
                EmptyDataGuidanceCard(
                    icon = Icons.Rounded.History,
                    title = "Seu histórico está vazio",
                    description = "Cada questão ou bateria registrada no Portal QConcursos, por print IA ou manualmente aparecerá aqui.",
                    primaryActionLabel = "Registrar Primeira Questão",
                    onPrimaryAction = onOpenQConcursosHub,
                    actionTestTag = "history_empty_open_hub_button"
                )
            }
            return@LazyColumn
        }

        item {
            Column {
                AccessibleFieldLabel("Filtrar histórico")
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Disciplina, subassunto ou código Q...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }
            if (disciplines.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDiscipline == null,
                        onClick = { selectedDiscipline = null },
                        label = { Text("Todas") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    disciplines.forEach { d ->
                        FilterChip(
                            selected = selectedDiscipline == d,
                            onClick = { selectedDiscipline = if (selectedDiscipline == d) null else d },
                            label = { Text(d) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }
        }

        items(filteredAttempts, key = { it.id.ifBlank { "${it.hashCode()}" } }) { item ->
            val mastery = MasteryLevel.fromAccuracy(item.accuracyPercent)
            val source = SourceType.fromWire(item.sourceType)
            val cause = ErrorCause.fromWire(item.errorCause)
            val dateStr = item.createdAt?.toDate()?.let { dateFormatter.format(it) } ?: "Agora"
            val accInt = item.accuracyPercent.roundToInt()
            val errInt = (100 - accInt).coerceAtLeast(0)

            EditorialCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.discipline} › ${item.topic}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.subtopic + if (item.microtopic.isNotBlank()) " • ${item.microtopic}" else "",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        IconButton(
                            onClick = { onDeleteAttempt(item.id) },
                            modifier = Modifier
                                .size(48.dp)
                                .accessibleFocusRing(RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Excluir registro de ${item.subtopic}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.correctCount}/${item.questionsCount} ($accInt% acertos / $errInt% erros) • ${item.examBoard}",
                            fontFamily = MonoTabularFamily,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        MasteryPill(level = mastery, compact = true)
                    }

                    Spacer(Modifier.height(6.dp))
                    val metaParts = buildList {
                        add(source.label)
                        if (item.qconcursosCode.isNotBlank()) add(item.qconcursosCode)
                        if (cause != ErrorCause.NONE) add(cause.label)
                        add("${item.timeSpentSeconds}s")
                        add(dateStr)
                    }
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (item.notes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        EditExamTargetDialog(
            profile = profile,
            onDismiss = { showProfileDialog = false },
            onSave = { name, exam, board, dateIso, goal, link ->
                onUpdateProfile(name, exam, board, dateIso, goal, link)
                showProfileDialog = false
            }
        )
    }
}
