package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.EditalItemDraft
import com.example.data.model.EditalWeight
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.domain.EditalCoverageSummary
import com.example.domain.EditalDisciplineNode
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.EditorialCard
import com.example.ui.components.MasteryPill
import com.example.ui.components.SectionHeader
import com.example.ui.components.UnstudiedPill
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.LocalAccessibilityConfig
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily
import kotlin.math.roundToInt

/**
 * Área dedicada ao Edital Verticalizado (Método Nilo).
 * Acessibilidade aplicada:
 * - Seleção de PDF/TXT via Storage Access Framework (OpenDocument) e prints via Photo Picker (zero permissões intrusivas).
 * - Rótulos visíveis acima de cada campo de formulário (AccessibleFieldLabel).
 * - Respeito a prefers-reduced-motion ao expandir/recolher disciplinas.
 * - Alvos de toque >= 48dp e anel de foco de alto contraste para teclado.
 */
@Composable
fun EditalScreen(
    profile: UserProfile?,
    editalCoverage: EditalCoverageSummary,
    editalItemsUiState: UiState<*>,
    editalImportState: UiState<List<EditalItemDraft>>,
    onAddManualItem: (EditalItemDraft, () -> Unit) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClearEdital: () -> Unit,
    onExtractFromPdf: (ByteArray, String) -> Unit,
    onExtractFromImage: (Bitmap, String) -> Unit,
    onExtractFromText: (String, String) -> Unit,
    onConfirmImportedDrafts: (List<EditalItemDraft>, Boolean) -> Unit,
    onClearImportPreview: () -> Unit,
    onPracticeSubtopicInQConcursos: (discipline: String, subtopic: String, board: String) -> Unit
) {
    val context = LocalContext.current
    val masteryColors = LocalMasteryColors.current
    val reducedMotion = LocalAccessibilityConfig.current.reducedMotion

    var showManualDialog by remember { mutableStateOf(false) }
    var showPasteTextBox by rememberSaveable { mutableStateOf(false) }
    var pastedEditalText by rememberSaveable { mutableStateOf("") }
    var editalTitleInput by rememberSaveable(profile?.targetExam) {
        mutableStateOf(
            editalCoverage.editalTitle.ifBlank { profile?.targetExam.orEmpty() }
        )
    }
    var fileReadError by remember { mutableStateOf<String?>(null) }
    var expandedDiscipline by rememberSaveable { mutableStateOf<String?>(null) }

    // SAF OpenDocument: zero storage permissions required (Google Play compliant)
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            fileReadError = null
            runCatching {
                val mime = context.contentResolver.getType(uri).orEmpty()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (mime.contains("text") || uri.toString().endsWith(".txt", ignoreCase = true)) {
                        val txt = stream.bufferedReader().readText()
                        onExtractFromText(txt, editalTitleInput)
                    } else {
                        val bytes = stream.readBytes()
                        onExtractFromPdf(bytes, editalTitleInput)
                    }
                } ?: error("Não foi possível ler o arquivo selecionado.")
            }.onFailure { err ->
                fileReadError = err.localizedMessage ?: "Falha ao abrir documento do edital."
            }
        }
    }

    // Photo Picker for Edital screenshots (zero storage permissions)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            fileReadError = null
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: error("Não foi possível decodificar a imagem selecionada.")
            }.onSuccess { bmp ->
                onExtractFromImage(bmp, editalTitleInput)
            }.onFailure { err ->
                fileReadError = err.localizedMessage ?: "Falha ao abrir imagem do edital."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edital_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SectionHeader(
                title = "Meu Edital Verticalizado",
                subtitle = "Cadastre ou anexe seu edital em Disciplina › Assunto › Subassunto para cruzamento automático com suas questões",
                actionLabel = "+ Item",
                onActionClick = { showManualDialog = true }
            )
        }

        // Coverage Summary Banner when Edital has items
        if (editalCoverage.totalItems > 0) {
            item {
                val covPct = editalCoverage.coveragePercent.roundToInt()
                EditorialCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = editalCoverage.editalTitle.ifBlank { "Edital Ativo" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${editalCoverage.studiedItems} de ${editalCoverage.totalItems} tópicos praticados ($covPct%) • ${editalCoverage.masteredItems} dominado(s) • ${editalCoverage.criticalItems} crítico(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = onClearEdital,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .accessibleFocusRing(RoundedCornerShape(10.dp))
                            ) {
                                Text("Limpar")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (editalCoverage.coveragePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .semantics {
                                    contentDescription = "Cobertura do edital em $covPct por cento."
                                },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }
        }

        // 3-Way Accessible Edital Ingestion Card
        item {
            EditorialCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Anexar ou Estruturar Edital",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = "Envie o PDF ou arquivo TXT do conteúdo programático, um print do edital ou cole o texto. Nossa IA organiza tudo automaticamente em árvore hierárquica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column {
                        AccessibleFieldLabel(text = "Nome do Concurso / Edital")
                        OutlinedTextField(
                            value = editalTitleInput,
                            onValueChange = { editalTitleInput = it },
                            placeholder = { Text("Ex: Polícia Federal 2026 — Agente") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edital_title_input")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                documentPickerLauncher.launch(arrayOf("application/pdf", "text/plain"))
                            },
                            enabled = editalImportState !is UiState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .accessibleFocusRing(RoundedCornerShape(14.dp))
                                .testTag("edital_attach_pdf_button")
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF / TXT", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = editalImportState !is UiState.Loading,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .accessibleFocusRing(RoundedCornerShape(14.dp))
                                .testTag("edital_attach_image_button")
                        ) {
                            Icon(Icons.Rounded.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print / Foto", maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasteTextBox = !showPasteTextBox },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .accessibleFocusRing(RoundedCornerShape(14.dp))
                                .testTag("edital_toggle_paste_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (showPasteTextBox) "Ocultar texto" else "Colar texto", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showManualDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .accessibleFocusRing(RoundedCornerShape(14.dp))
                                .testTag("edital_add_manual_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Manual", maxLines = 1)
                        }
                    }

                    if (!fileReadError.isNullOrBlank()) {
                        AccessibleAlertBanner(message = fileReadError.orEmpty(), isError = true)
                    }

                    if (showPasteTextBox) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccessibleFieldLabel(
                                text = "Conteúdo programático do edital",
                                required = true
                            )
                            OutlinedTextField(
                                value = pastedEditalText,
                                onValueChange = { pastedEditalText = it },
                                minLines = 4,
                                maxLines = 8,
                                placeholder = {
                                    Text("Ex: DIREITO PENAL: 1. Aplicação da lei penal. 2. Teoria do crime: tipicidade, ilicitude e culpabilidade...")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edital_paste_input")
                            )
                            Button(
                                onClick = { onExtractFromText(pastedEditalText, editalTitleInput) },
                                enabled = editalImportState !is UiState.Loading && pastedEditalText.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .accessibleFocusRing(RoundedCornerShape(14.dp))
                                    .testTag("edital_extract_text_button")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Estruturar Edital com IA")
                            }
                        }
                    }
                }
            }
        }

        // AI Extraction Feedback & Preview before saving
        when (editalImportState) {
            is UiState.Loading -> {
                item {
                    EditorialCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Organizando disciplinas, assuntos e subassuntos do edital...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    AccessibleAlertBanner(message = editalImportState.message, isError = true)
                }
            }
            is UiState.Success -> {
                val drafts = editalImportState.data
                item {
                    EditorialCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Pré-visualização do Edital (${drafts.size} subassunto(s))",
                                style = MaterialTheme.typography.titleMedium
                            )
                            drafts.take(10).forEach { d ->
                                Text(
                                    text = "• ${d.discipline} › ${d.topic} › ${d.subtopic} (${d.weight.label})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (drafts.size > 10) {
                                Text(
                                    text = "... e mais ${drafts.size - 10} tópico(s) prontos para salvar.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onClearImportPreview,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .accessibleFocusRing()
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Descartar")
                                }
                                Button(
                                    onClick = {
                                        onConfirmImportedDrafts(drafts, false)
                                        pastedEditalText = ""
                                        showPasteTextBox = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .accessibleFocusRing()
                                        .testTag("edital_confirm_import_button")
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Salvar Edital")
                                }
                            }
                        }
                    }
                }
            }
            UiState.Idle -> Unit
        }

        // Loading state for Firestore Edital list
        if (editalItemsUiState is UiState.Loading) {
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

        if (editalItemsUiState is UiState.Error) {
            item {
                AccessibleAlertBanner(message = editalItemsUiState.message, isError = true)
            }
        }

        // Empty State or Hierarchical Edital Tree
        if (editalCoverage.disciplines.isEmpty()) {
            item {
                EditorialCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Nenhum tópico do edital cadastrado ainda",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Anexe o PDF do seu edital, envie um print, cole o conteúdo programático ou adicione tópicos manualmente acima. Cada item será cruzado automaticamente com suas questões do QConcursos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                SectionHeader(
                    title = "Estrutura Hierárquica × Desempenho",
                    subtitle = "Toque em uma disciplina para inspecionar acertos, erros e lacunas por subassunto"
                )
            }

            items(editalCoverage.disciplines, key = { it.discipline }) { discNode ->
                EditalDisciplineCard(
                    node = discNode,
                    isExpanded = expandedDiscipline == discNode.discipline,
                    reducedMotion = reducedMotion,
                    onToggle = {
                        expandedDiscipline = if (expandedDiscipline == discNode.discipline) null else discNode.discipline
                    },
                    onDeleteItem = onDeleteItem,
                    onPracticeSubtopic = { sub ->
                        onPracticeSubtopicInQConcursos(
                            discNode.discipline,
                            sub,
                            profile?.examBoard ?: "Cebraspe"
                        )
                    }
                )
            }
        }
    }

    if (showManualDialog) {
        AddEditalItemDialog(
            defaultEditalTitle = editalTitleInput.ifBlank { profile?.targetExam.orEmpty() },
            onDismiss = { showManualDialog = false },
            onSave = { draft ->
                onAddManualItem(draft) {
                    showManualDialog = false
                }
            }
        )
    }
}

@Composable
private fun EditalDisciplineCard(
    node: EditalDisciplineNode,
    isExpanded: Boolean,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onPracticeSubtopic: (String) -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    val accInt = node.accuracyPercent.roundToInt()
    val errInt = node.errorPercent.roundToInt()

    EditorialCard {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .accessibleFocusRing(RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isExpanded) "Recolher ${node.discipline}" else "Expandir ${node.discipline}",
                        onClick = onToggle
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.discipline,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (node.totalQuestions > 0) {
                            "${node.studiedSubtopics}/${node.totalSubtopics} tópicos • $accInt% acertos • $errInt% erros (${node.totalQuestions}q)"
                        } else {
                            "0/${node.totalSubtopics} tópicos praticados • Nenhuma questão registrada"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (node.mastery != null) {
                    MasteryPill(level = node.mastery, compact = true)
                } else {
                    UnstudiedPill()
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Recolher disciplina" else "Expandir disciplina",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val contentBlock: @Composable () -> Unit = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = masteryColors.border)
                    node.topics.forEach { topicNode ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = topicNode.topic,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (topicNode.totalQuestions > 0) {
                                    Text(
                                        text = "${topicNode.accuracyPercent.roundToInt()}% acertos (${topicNode.correctQuestions}/${topicNode.totalQuestions})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = MonoTabularFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            topicNode.subtopics.forEach { sub ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, masteryColors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sub.subtopic,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = if (sub.studied) {
                                                        "Acertos: ${sub.accuracyPercent.roundToInt()}% • Erros: ${sub.errorPercent.roundToInt()}% (${sub.correctQuestions}/${sub.totalQuestions}) • ${sub.weight.label}"
                                                    } else {
                                                        "${sub.weight.label} • Ainda sem questões resolvidas"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteItem(sub.itemId) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .accessibleFocusRing(RoundedCornerShape(12.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Remover ${sub.subtopic} do edital",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (sub.mastery != null) {
                                                MasteryPill(level = sub.mastery, compact = true)
                                            } else {
                                                UnstudiedPill()
                                            }
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
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = "Praticar",
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
            }

            if (reducedMotion) {
                if (isExpanded) contentBlock()
            } else {
                AnimatedVisibility(visible = isExpanded) { contentBlock() }
            }
        }
    }
}

@Composable
private fun AddEditalItemDialog(
    defaultEditalTitle: String,
    onDismiss: () -> Unit,
    onSave: (EditalItemDraft) -> Unit
) {
    var editalTitle by remember { mutableStateOf(defaultEditalTitle.ifBlank { "Meu Edital" }) }
    var discipline by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var subtopic by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(EditalWeight.MEDIA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Item ao Edital") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    AccessibleFieldLabel("Nome do Edital", required = true)
                    OutlinedTextField(
                        value = editalTitle,
                        onValueChange = { editalTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Disciplina", required = true)
                    OutlinedTextField(
                        value = discipline,
                        onValueChange = { discipline = it },
                        placeholder = { Text("Ex: Direito Penal") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edital_manual_discipline")
                    )
                }
                Column {
                    AccessibleFieldLabel("Assunto", required = true)
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        placeholder = { Text("Ex: Teoria do Crime") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edital_manual_topic")
                    )
                }
                Column {
                    AccessibleFieldLabel("Subassunto", required = true)
                    OutlinedTextField(
                        value = subtopic,
                        onValueChange = { subtopic = it },
                        placeholder = { Text("Ex: Culpabilidade") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edital_manual_subtopic")
                    )
                }
                Column {
                    AccessibleFieldLabel("Peso / Relevância na Prova")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditalWeight.entries.forEach { w ->
                            FilterChip(
                                selected = weight == w,
                                onClick = { weight = w },
                                label = { Text(w.label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        EditalItemDraft(
                            editalTitle = editalTitle.ifBlank { "Meu Edital" },
                            discipline = discipline.trim(),
                            topic = topic.trim().ifBlank { discipline.trim() },
                            subtopic = subtopic.trim().ifBlank { topic.trim().ifBlank { discipline.trim() } },
                            weight = weight
                        )
                    )
                },
                enabled = discipline.isNotBlank() && topic.isNotBlank(),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("edital_manual_save_button")
            ) {
                Text("Salvar Item")
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
