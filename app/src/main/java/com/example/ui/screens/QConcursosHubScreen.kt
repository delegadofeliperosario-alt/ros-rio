package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ConcursoTaxonomy
import com.example.data.model.EditalItem
import com.example.data.model.ErrorCause
import com.example.data.model.ImportedAttemptDraft
import com.example.data.model.SourceType
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.domain.QConcursosParser
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.EditorialCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily
import kotlinx.coroutines.delay

/**
 * Hub de Integração de Questões (QConcursos — Método Nilo).
 * Acessibilidade: rótulos persistentes acima de cada campo, Photo Picker sem permissões invasivas,
 * alvos mínimos de 48dp e integração direta com as disciplinas/assuntos/subassuntos do Edital cadastrado.
 */
@Composable
fun QConcursosHubScreen(
    profile: UserProfile?,
    editalItems: List<EditalItem>,
    activePortalUrl: String,
    importExtractionState: UiState<List<ImportedAttemptDraft>>,
    onPortalUrlChange: (String) -> Unit,
    onRecordAttempt: (ImportedAttemptDraft, () -> Unit) -> Unit,
    onExtractFromScreenshot: (Bitmap) -> Unit,
    onExtractFromText: (String) -> Unit,
    onConfirmImportedDrafts: (List<ImportedAttemptDraft>) -> Unit,
    onClearImportPreview: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Portal Q", "Importar IA", "Registrar")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qconcursos_hub_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("qconcursos_tab_$index")
                    ) {
                        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> QConcursosPortalTab(
                initialUrl = activePortalUrl,
                defaultBoard = profile?.examBoard ?: "Cebraspe",
                editalItems = editalItems,
                onPortalUrlChange = onPortalUrlChange,
                onRecordAttempt = onRecordAttempt
            )
            1 -> QConcursosImportTab(
                importExtractionState = importExtractionState,
                onExtractFromScreenshot = onExtractFromScreenshot,
                onExtractFromText = onExtractFromText,
                onConfirmImportedDrafts = onConfirmImportedDrafts,
                onClearImportPreview = onClearImportPreview
            )
            else -> ManualAttemptLoggerTab(
                defaultBoard = profile?.examBoard ?: "Cebraspe",
                initialUrl = activePortalUrl,
                editalItems = editalItems,
                onRecordAttempt = onRecordAttempt
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun QConcursosPortalTab(
    initialUrl: String,
    defaultBoard: String,
    editalItems: List<EditalItem>,
    onPortalUrlChange: (String) -> Unit,
    onRecordAttempt: (ImportedAttemptDraft, () -> Unit) -> Unit
) {
    val masteryColors = LocalMasteryColors.current
    var urlInput by remember(initialUrl) { mutableStateOf(initialUrl) }
    var currentLoadedUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var showQuickCaptureDialog by remember { mutableStateOf(false) }

    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val parsedUrl = remember(currentLoadedUrl) { QConcursosParser.parseUrl(currentLoadedUrl) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webViewRef?.goBack() },
                modifier = Modifier
                    .size(48.dp)
                    .accessibleFocusRing(RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar página no portal")
            }
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = true,
                placeholder = { Text("Link do QConcursos ou caderno") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("qconcursos_url_input"),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = {
                    val normalized = QConcursosParser.parseUrl(urlInput).normalizedUrl
                    currentLoadedUrl = normalized
                    onPortalUrlChange(normalized)
                    webViewRef?.loadUrl(normalized)
                },
                modifier = Modifier
                    .size(48.dp)
                    .accessibleFocusRing(RoundedCornerShape(12.dp))
                    .testTag("qconcursos_url_go_button")
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Abrir link no Portal QConcursos")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val shortcuts = listOf(
                "Questões" to QConcursosParser.QUESTOES_URL,
                "Estatísticas" to QConcursosParser.ESTATISTICAS_URL,
                "Cadernos" to QConcursosParser.CADERNOS_URL,
                "Simulados" to QConcursosParser.SIMULADOS_URL
            )
            shortcuts.forEach { (title, url) ->
                FilterChip(
                    selected = currentLoadedUrl.startsWith(url),
                    onClick = {
                        urlInput = url
                        currentLoadedUrl = url
                        onPortalUrlChange(url)
                        webViewRef?.loadUrl(url)
                    },
                    label = { Text(title) },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!url.isNullOrBlank()) {
                                    currentLoadedUrl = url
                                    urlInput = url
                                }
                            }
                        }
                        loadUrl(initialUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    if (view.url != initialUrl && currentLoadedUrl == initialUrl) {
                        if (view.url.isNullOrBlank() || initialUrl.contains("?search=")) {
                            view.loadUrl(initialUrl)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Surface(
            color = masteryColors.heroNavy,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { timerRunning = !timerRunning },
                        modifier = Modifier
                            .size(48.dp)
                            .accessibleFocusRing(RoundedCornerShape(12.dp))
                            .testTag("portal_timer_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (timerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (timerRunning) "Pausar cronômetro" else "Iniciar cronômetro",
                            tint = masteryColors.goldAccent
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFFF7F8FA).copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatTimer(elapsedSeconds),
                                fontFamily = MonoTabularFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF7F8FA)
                            )
                        }
                        Text(
                            text = parsedUrl.pageKind,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF7F8FA).copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        timerRunning = false
                        showQuickCaptureDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = masteryColors.goldAccent,
                        contentColor = masteryColors.heroNavy
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .accessibleFocusRing(RoundedCornerShape(12.dp))
                        .testTag("portal_capture_question_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddTask,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQuickCaptureDialog) {
        val firstEdital = editalItems.firstOrNull()
        QuickPortalCaptureDialog(
            detectedCode = parsedUrl.questionCode,
            sourceUrl = currentLoadedUrl,
            elapsedSeconds = elapsedSeconds.coerceAtLeast(15),
            defaultBoard = defaultBoard,
            defaultDiscipline = firstEdital?.discipline ?: "Direito Penal",
            defaultTopic = firstEdital?.topic ?: "Teoria do Crime",
            defaultSubtopic = firstEdital?.subtopic ?: "Culpabilidade",
            onDismiss = { showQuickCaptureDialog = false },
            onSave = { draft ->
                onRecordAttempt(draft) {
                    elapsedSeconds = 0
                    showQuickCaptureDialog = false
                }
            }
        )
    }
}

@Composable
private fun QConcursosImportTab(
    importExtractionState: UiState<List<ImportedAttemptDraft>>,
    onExtractFromScreenshot: (Bitmap) -> Unit,
    onExtractFromText: (String) -> Unit,
    onConfirmImportedDrafts: (List<ImportedAttemptDraft>) -> Unit,
    onClearImportPreview: () -> Unit
) {
    val context = LocalContext.current
    val masteryColors = LocalMasteryColors.current
    var rawText by rememberSaveable { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bitmap ->
                onExtractFromScreenshot(bitmap)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Importação Autorizada do QConcursos",
                subtitle = "Envie prints de estatísticas/questões ou cole relatórios para cruzamento com seu edital"
            )
        }

        item {
            EditorialCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ImageSearch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Leitor de Print QConcursos (IA Multimodal)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Selecione um print da aba Estatísticas, de um Caderno ou de uma Questão resolvida no QConcursos. A IA extrai automaticamente disciplina, assunto, subassunto, banca, acertos e erros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = importExtractionState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .accessibleFocusRing(RoundedCornerShape(14.dp))
                            .testTag("pick_qconcursos_screenshot_button")
                    ) {
                        Icon(Icons.Rounded.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar Print do QConcursos")
                    }
                }
            }
        }

        item {
            EditorialCard {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null,
                            tint = masteryColors.goldAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Colar Estatísticas ou CSV do QConcursos",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Cole texto copiado da página de desempenho ou linhas CSV no formato: Disciplina;Assunto;Subassunto;Banca;Acertos;Erros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    AccessibleFieldLabel("Dados de desempenho copiados ou CSV")
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        minLines = 4,
                        maxLines = 8,
                        placeholder = {
                            Text("Ex:\nDireito Penal;Teoria do Crime;Culpabilidade;Cebraspe;3;7")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("qconcursos_paste_input")
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { onExtractFromText(rawText) },
                        enabled = importExtractionState !is UiState.Loading && rawText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .accessibleFocusRing(RoundedCornerShape(14.dp))
                            .testTag("parse_qconcursos_text_button")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Interpretar e Pré-visualizar Dados")
                    }
                }
            }
        }

        when (importExtractionState) {
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
                            Text("Extraindo disciplinas, subassuntos e acertos...")
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    AccessibleAlertBanner(message = importExtractionState.message, isError = true)
                }
            }
            is UiState.Success -> {
                val drafts = importExtractionState.data
                item {
                    SectionHeader(
                        title = "Pré-visualização (${drafts.size} item(ns))",
                        subtitle = "Revise antes de gravar no seu histórico analítico"
                    )
                }
                itemsIndexed(drafts) { _, d ->
                    EditorialCard {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${d.discipline} › ${d.topic} › ${d.subtopic}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (d.microtopic.isNotBlank()) {
                                Text(
                                    text = "Microtópico: ${d.microtopic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${d.correctCount}/${d.questionsCount} acertos • Banca: ${d.examBoard}" +
                                    if (d.qconcursosCode.isNotBlank()) " • ${d.qconcursosCode}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClearImportPreview,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .accessibleFocusRing()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Descartar")
                        }
                        Button(
                            onClick = {
                                onConfirmImportedDrafts(drafts)
                                rawText = ""
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                                .accessibleFocusRing()
                                .testTag("confirm_import_batch_button")
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Salvar Tudo")
                        }
                    }
                }
            }
            UiState.Idle -> Unit
        }
    }
}

@Composable
private fun ManualAttemptLoggerTab(
    defaultBoard: String,
    initialUrl: String,
    editalItems: List<EditalItem>,
    onRecordAttempt: (ImportedAttemptDraft, () -> Unit) -> Unit
) {
    // Combine user's registered Edital hierarchy first, falling back to ConcursoTaxonomy
    val availableDisciplines = remember(editalItems) {
        val fromEdital = editalItems.map { it.discipline }.filter { it.isNotBlank() }.distinct()
        if (fromEdital.isNotEmpty()) fromEdital else ConcursoTaxonomy.disciplines()
    }

    var discipline by rememberSaveable(availableDisciplines) {
        mutableStateOf(availableDisciplines.firstOrNull() ?: "Direito Penal")
    }
    val availableTopics = remember(editalItems, discipline) {
        val fromEdital = editalItems
            .filter { it.discipline.equals(discipline, ignoreCase = true) }
            .map { it.topic }
            .filter { it.isNotBlank() }
            .distinct()
        if (fromEdital.isNotEmpty()) fromEdital else ConcursoTaxonomy.topicsFor(discipline)
    }
    var topic by rememberSaveable(availableTopics) {
        mutableStateOf(availableTopics.firstOrNull() ?: "Teoria do Crime")
    }
    val availableSubtopics = remember(editalItems, discipline, topic) {
        val fromEdital = editalItems
            .filter {
                it.discipline.equals(discipline, ignoreCase = true) &&
                    it.topic.equals(topic, ignoreCase = true)
            }
            .map { it.subtopic }
            .filter { it.isNotBlank() }
            .distinct()
        if (fromEdital.isNotEmpty()) fromEdital else ConcursoTaxonomy.subtopicsFor(discipline, topic)
    }
    var subtopic by rememberSaveable(availableSubtopics) {
        mutableStateOf(availableSubtopics.firstOrNull() ?: "Culpabilidade")
    }
    var microtopic by rememberSaveable { mutableStateOf("") }
    var examBoard by rememberSaveable { mutableStateOf(defaultBoard) }
    var qCode by rememberSaveable { mutableStateOf("") }
    var questionsCountText by rememberSaveable { mutableStateOf("1") }
    var correctCountText by rememberSaveable { mutableStateOf("0") }
    var timeSecondsText by rememberSaveable { mutableStateOf("75") }
    var attemptNumText by rememberSaveable { mutableStateOf("1") }
    var selectedCause by rememberSaveable { mutableStateOf(ErrorCause.LACUNA_CONCEITUAL) }
    var notes by rememberSaveable { mutableStateOf("") }

    val availableMicrotopics = remember(discipline, topic, subtopic) {
        ConcursoTaxonomy.microtopicsFor(discipline, topic, subtopic)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionHeader(
                title = "Registro Granular de Questão ou Bateria",
                subtitle = if (editalItems.isNotEmpty()) {
                    "Sincronizado com as disciplinas e subassuntos do seu Edital cadastrado"
                } else {
                    "Detalhe até o microtópico e causa real do erro para precisão máxima do Método Nilo"
                }
            )
        }

        item {
            EditorialCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        AccessibleFieldLabel("Disciplina", required = true)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableDisciplines.forEach { d ->
                                FilterChip(
                                    selected = discipline == d,
                                    onClick = {
                                        discipline = d
                                        val nextTopics = editalItems
                                            .filter { it.discipline.equals(d, ignoreCase = true) }
                                            .map { it.topic }.distinct()
                                            .ifEmpty { ConcursoTaxonomy.topicsFor(d) }
                                        val firstTopic = nextTopics.firstOrNull().orEmpty()
                                        topic = firstTopic
                                        val nextSubs = editalItems
                                            .filter {
                                                it.discipline.equals(d, ignoreCase = true) &&
                                                    it.topic.equals(firstTopic, ignoreCase = true)
                                            }
                                            .map { it.subtopic }.distinct()
                                            .ifEmpty { ConcursoTaxonomy.subtopicsFor(d, firstTopic) }
                                        subtopic = nextSubs.firstOrNull().orEmpty()
                                        microtopic = ""
                                    },
                                    label = { Text(d) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = discipline,
                            onValueChange = { discipline = it },
                            placeholder = { Text("Ou digite outra disciplina") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_discipline_input")
                        )
                    }

                    Column {
                        AccessibleFieldLabel("Assunto", required = true)
                        if (availableTopics.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableTopics.forEach { t ->
                                    FilterChip(
                                        selected = topic == t,
                                        onClick = {
                                            topic = t
                                            val nextSubs = editalItems
                                                .filter {
                                                    it.discipline.equals(discipline, ignoreCase = true) &&
                                                        it.topic.equals(t, ignoreCase = true)
                                                }
                                                .map { it.subtopic }.distinct()
                                                .ifEmpty { ConcursoTaxonomy.subtopicsFor(discipline, t) }
                                            subtopic = nextSubs.firstOrNull().orEmpty()
                                            microtopic = ""
                                        },
                                        label = { Text(t) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        OutlinedTextField(
                            value = topic,
                            onValueChange = { topic = it },
                            placeholder = { Text("Ex: Teoria do Crime") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_topic_input")
                        )
                    }

                    Column {
                        AccessibleFieldLabel("Subassunto", required = true)
                        if (availableSubtopics.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableSubtopics.forEach { s ->
                                    FilterChip(
                                        selected = subtopic == s,
                                        onClick = {
                                            subtopic = s
                                            microtopic = ""
                                        },
                                        label = { Text(s) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        OutlinedTextField(
                            value = subtopic,
                            onValueChange = { subtopic = it },
                            placeholder = { Text("Ex: Culpabilidade") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_subtopic_input")
                        )
                    }

                    Column {
                        AccessibleFieldLabel("Microtópico (opcional)")
                        if (availableMicrotopics.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableMicrotopics.forEach { m ->
                                    FilterChip(
                                        selected = microtopic == m,
                                        onClick = { microtopic = if (microtopic == m) "" else m },
                                        label = { Text(m) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        OutlinedTextField(
                            value = microtopic,
                            onValueChange = { microtopic = it },
                            placeholder = { Text("Ex: Potencial consciência da ilicitude") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = LocalMasteryColors.current.border)

                    Column {
                        AccessibleFieldLabel("Banca Examinadora")
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConcursoTaxonomy.examBoards.forEach { b ->
                                FilterChip(
                                    selected = examBoard == b,
                                    onClick = { examBoard = b },
                                    label = { Text(b) },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            AccessibleFieldLabel("Código QConcursos")
                            OutlinedTextField(
                                value = qCode,
                                onValueChange = { qCode = it.uppercase() },
                                placeholder = { Text("Q2481920") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.width(120.dp)) {
                            AccessibleFieldLabel("Tentativa nº")
                            OutlinedTextField(
                                value = attemptNumText,
                                onValueChange = { attemptNumText = it.filter(Char::isDigit) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            AccessibleFieldLabel("Questões", required = true)
                            OutlinedTextField(
                                value = questionsCountText,
                                onValueChange = { questionsCountText = it.filter(Char::isDigit) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_questions_count_input")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AccessibleFieldLabel("Acertos", required = true)
                            OutlinedTextField(
                                value = correctCountText,
                                onValueChange = { correctCountText = it.filter(Char::isDigit) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_correct_count_input")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AccessibleFieldLabel("Tempo (s)")
                            OutlinedTextField(
                                value = timeSecondsText,
                                onValueChange = { timeSecondsText = it.filter(Char::isDigit) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val qTotal = questionsCountText.toIntOrNull()?.coerceIn(1, 500) ?: 1
                    val qCorrect = correctCountText.toIntOrNull()?.coerceIn(0, qTotal) ?: 0
                    if (qCorrect < qTotal) {
                        Column {
                            AccessibleFieldLabel("Causa principal do erro (metacognição)")
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ErrorCause.entries.filter { it != ErrorCause.NONE }.forEach { cause ->
                                    FilterChip(
                                        selected = selectedCause == cause,
                                        onClick = { selectedCause = cause },
                                        label = { Text(cause.label) },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        AccessibleFieldLabel("Anotação jurídica / reflexão do erro (opcional)")
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Ex: Confundi erro de proibição inevitável com evitável (art. 21 CP)") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val total = questionsCountText.toIntOrNull()?.coerceIn(1, 500) ?: 1
                            val correct = correctCountText.toIntOrNull()?.coerceIn(0, total) ?: 0
                            val seconds = timeSecondsText.toIntOrNull()?.coerceIn(0, 86400) ?: 60
                            val attemptNum = attemptNumText.toIntOrNull()?.coerceIn(1, 50) ?: 1
                            val draft = ImportedAttemptDraft(
                                qconcursosCode = qCode,
                                sourceUrl = initialUrl,
                                sourceType = SourceType.REGISTRO_MANUAL,
                                discipline = discipline.ifBlank { "Geral" },
                                topic = topic.ifBlank { discipline.ifBlank { "Geral" } },
                                subtopic = subtopic.ifBlank { topic.ifBlank { "Geral" } },
                                microtopic = microtopic,
                                examBoard = examBoard.ifBlank { defaultBoard },
                                questionsCount = total,
                                correctCount = correct,
                                timeSpentSeconds = seconds,
                                attemptNumber = attemptNum,
                                errorCause = if (correct == total) ErrorCause.NONE else selectedCause,
                                notes = notes
                            )
                            onRecordAttempt(draft) {
                                qCode = ""
                                notes = ""
                            }
                        },
                        enabled = discipline.isNotBlank() && topic.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .accessibleFocusRing(RoundedCornerShape(14.dp))
                            .testTag("save_manual_attempt_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar no Histórico Analítico", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPortalCaptureDialog(
    detectedCode: String,
    sourceUrl: String,
    elapsedSeconds: Int,
    defaultBoard: String,
    defaultDiscipline: String,
    defaultTopic: String,
    defaultSubtopic: String,
    onDismiss: () -> Unit,
    onSave: (ImportedAttemptDraft) -> Unit
) {
    var qCode by remember { mutableStateOf(detectedCode) }
    var discipline by remember { mutableStateOf(defaultDiscipline) }
    var topic by remember { mutableStateOf(defaultTopic) }
    var subtopic by remember { mutableStateOf(defaultSubtopic) }
    var microtopic by remember { mutableStateOf("") }
    var answeredCorrectly by remember { mutableStateOf(false) }
    var errorCause by remember { mutableStateOf(ErrorCause.LACUNA_CONCEITUAL) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar Questão do Portal")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tempo cronometrado: ${formatTimer(elapsedSeconds)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Column {
                    AccessibleFieldLabel("Código QConcursos")
                    OutlinedTextField(
                        value = qCode,
                        onValueChange = { qCode = it.uppercase() },
                        placeholder = { Text("Q2481920") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Disciplina", required = true)
                    OutlinedTextField(
                        value = discipline,
                        onValueChange = { discipline = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Assunto", required = true)
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Subassunto", required = true)
                    OutlinedTextField(
                        value = subtopic,
                        onValueChange = { subtopic = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column {
                    AccessibleFieldLabel("Microtópico (opcional)")
                    OutlinedTextField(
                        value = microtopic,
                        onValueChange = { microtopic = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = answeredCorrectly,
                        onClick = { answeredCorrectly = true },
                        label = { Text("Acertei") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                    FilterChip(
                        selected = !answeredCorrectly,
                        onClick = { answeredCorrectly = false },
                        label = { Text("Errei") },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
                if (!answeredCorrectly) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ErrorCause.LACUNA_CONCEITUAL,
                            ErrorCause.DESATENCAO,
                            ErrorCause.PEGADINHA_BANCA,
                            ErrorCause.ESQUECIMENTO
                        ).forEach { cause ->
                            FilterChip(
                                selected = errorCause == cause,
                                onClick = { errorCause = cause },
                                label = { Text(cause.label) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }
                }
                Column {
                    AccessibleFieldLabel("Anotação rápida (opcional)")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ImportedAttemptDraft(
                            qconcursosCode = qCode,
                            sourceUrl = sourceUrl,
                            sourceType = SourceType.PORTAL_QCONCURSOS,
                            discipline = discipline.ifBlank { "Geral" },
                            topic = topic.ifBlank { discipline.ifBlank { "Geral" } },
                            subtopic = subtopic.ifBlank { topic.ifBlank { "Geral" } },
                            microtopic = microtopic,
                            examBoard = defaultBoard,
                            questionsCount = 1,
                            correctCount = if (answeredCorrectly) 1 else 0,
                            timeSpentSeconds = elapsedSeconds,
                            attemptNumber = 1,
                            errorCause = if (answeredCorrectly) ErrorCause.NONE else errorCause,
                            notes = notes
                        )
                    )
                },
                enabled = discipline.isNotBlank(),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("portal_dialog_save_button")
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

private fun formatTimer(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
