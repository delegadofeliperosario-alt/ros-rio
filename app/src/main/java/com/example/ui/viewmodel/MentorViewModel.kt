package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiDiagnosticReport
import com.example.data.model.EditalItem
import com.example.data.model.EditalItemDraft
import com.example.data.model.ImportedAttemptDraft
import com.example.data.model.QuestionAttempt
import com.example.data.model.RevisionTask
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AiMentorService
import com.example.data.repository.MentorRepository
import com.example.domain.PerformanceAnalyticsEngine
import com.example.domain.PerformanceAnalyticsSnapshot
import com.example.domain.QConcursosParser
import com.example.ui.theme.AccessibilityConfig
import com.example.ui.theme.ThemeMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MentorChatState(
    val isStreaming: Boolean = false,
    val lastQuestion: String = "",
    val responseText: String = "",
    val errorMessage: String? = null
)

class MentorViewModel(
    private val repository: MentorRepository,
    private val aiService: AiMentorService,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    private val profileRaw: StateFlow<UiState<UserProfile?>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Falha ao carregar perfil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val profileState: StateFlow<UiState<UserProfile?>> = profileRaw

    val editalItemsState: StateFlow<UiState<List<EditalItem>>> = repository.observeEditalItems(currentUserId)
        .map<List<EditalItem>, UiState<List<EditalItem>>> { UiState.Success(it) }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Falha ao sincronizar itens do edital"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val attemptsState: StateFlow<UiState<List<QuestionAttempt>>> = repository.observeAttempts(currentUserId)
        .map<List<QuestionAttempt>, UiState<List<QuestionAttempt>>> { UiState.Success(it) }
        .catch { error ->
            emit(unwrapFirestoreError(error, "Falha ao sincronizar histórico de questões"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val diagnosticsState: StateFlow<UiState<List<AiDiagnosticReport>>> =
        repository.observeDiagnostics(currentUserId)
            .map<List<AiDiagnosticReport>, UiState<List<AiDiagnosticReport>>> { UiState.Success(it) }
            .catch { error ->
                emit(unwrapFirestoreError(error, "Falha ao carregar diagnósticos"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val revisionTasksState: StateFlow<UiState<List<RevisionTask>>> =
        repository.observeRevisionTasks(currentUserId)
            .map<List<RevisionTask>, UiState<List<RevisionTask>>> { UiState.Success(it) }
            .catch { error ->
                emit(unwrapFirestoreError(error, "Falha ao carregar plano de revisão"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val analyticsState: StateFlow<PerformanceAnalyticsSnapshot> = combine(
        attemptsState,
        editalItemsState,
        profileState
    ) { attemptsUi, editalUi, profileUi ->
        val attempts = (attemptsUi as? UiState.Success)?.data.orEmpty()
        val editalItems = (editalUi as? UiState.Success)?.data.orEmpty()
        val profile = (profileUi as? UiState.Success)?.data
        PerformanceAnalyticsEngine.analyze(attempts, editalItems, profile)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = PerformanceAnalyticsSnapshot()
    )

    private val _diagnosticGenerationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val diagnosticGenerationState: StateFlow<UiState<Unit>> = _diagnosticGenerationState.asStateFlow()

    private val _revisionGenerationState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val revisionGenerationState: StateFlow<UiState<Unit>> = _revisionGenerationState.asStateFlow()

    private val _importExtractionState = MutableStateFlow<UiState<List<ImportedAttemptDraft>>>(UiState.Idle)
    val importExtractionState: StateFlow<UiState<List<ImportedAttemptDraft>>> = _importExtractionState.asStateFlow()

    private val _editalImportState = MutableStateFlow<UiState<List<EditalItemDraft>>>(UiState.Idle)
    val editalImportState: StateFlow<UiState<List<EditalItemDraft>>> = _editalImportState.asStateFlow()

    private val _mentorChatState = MutableStateFlow(MentorChatState())
    val mentorChatState: StateFlow<MentorChatState> = _mentorChatState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _activePortalUrl = MutableStateFlow(QConcursosParser.QUESTOES_URL)
    val activePortalUrl: StateFlow<String> = _activePortalUrl.asStateFlow()

    private val _accessibilityConfig = MutableStateFlow(AccessibilityConfig())
    val accessibilityConfig: StateFlow<AccessibilityConfig> = _accessibilityConfig.asStateFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(defaultDisplayName.ifBlank { "Estudante" })
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun setThemeMode(mode: ThemeMode) {
        _accessibilityConfig.value = _accessibilityConfig.value.copy(themeMode = mode)
    }

    fun setReducedMotion(enabled: Boolean) {
        _accessibilityConfig.value = _accessibilityConfig.value.copy(reducedMotion = enabled)
    }

    fun setPortalUrl(url: String) {
        _activePortalUrl.value = QConcursosParser.parseUrl(url).normalizedUrl
    }

    fun saveProfile(
        studentName: String,
        targetExam: String,
        examBoard: String,
        examDateIso: String,
        dailyGoalQuestions: Int,
        qconcursosLink: String
    ) {
        viewModelScope.launch {
            val result = repository.updateUserProfile(
                studentName = studentName,
                targetExam = targetExam,
                examBoard = examBoard,
                examDateIso = examDateIso,
                dailyGoalQuestions = dailyGoalQuestions,
                qconcursosLink = qconcursosLink
            )
            _feedbackMessage.value = if (result.isSuccess) {
                if (qconcursosLink.isNotBlank()) {
                    _activePortalUrl.value = QConcursosParser.parseUrl(qconcursosLink).normalizedUrl
                }
                "Perfil e metas atualizados com sucesso."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Erro ao salvar perfil."
            }
        }
    }

    // --- Edital Management ---

    fun addEditalItem(draft: EditalItemDraft, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val currentCount = (editalItemsState.value as? UiState.Success)?.data?.size ?: 0
            val result = repository.addEditalItem(draft, currentCount)
            if (result.isSuccess) {
                _feedbackMessage.value = "Item adicionado ao edital e sincronizado."
                onSaved()
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Não foi possível salvar o item no edital."
            }
        }
    }

    fun deleteEditalItem(itemId: String) {
        viewModelScope.launch {
            val result = repository.deleteEditalItem(itemId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao remover item do edital."
            }
        }
    }

    fun clearEdital() {
        viewModelScope.launch {
            val result = repository.clearEdital()
            _feedbackMessage.value = if (result.isSuccess) {
                "Edital limpo com sucesso."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Erro ao limpar edital."
            }
        }
    }

    fun extractEditalFromText(rawText: String, fallbackTitle: String) {
        if (rawText.isBlank()) {
            _editalImportState.value = UiState.Error("Cole o texto do conteúdo programático do edital primeiro.")
            return
        }
        viewModelScope.launch {
            _editalImportState.value = UiState.Loading
            val title = fallbackTitle.ifBlank {
                (profileState.value as? UiState.Success)?.data?.targetExam.orEmpty()
            }
            val result = aiService.extractEditalFromText(rawText, title)
            _editalImportState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Falha ao estruturar edital.") }
            )
        }
    }

    fun extractEditalFromPdf(pdfBytes: ByteArray, fallbackTitle: String) {
        viewModelScope.launch {
            _editalImportState.value = UiState.Loading
            val title = fallbackTitle.ifBlank {
                (profileState.value as? UiState.Success)?.data?.targetExam.orEmpty()
            }
            val result = aiService.extractEditalFromPdfBytes(pdfBytes, title)
            _editalImportState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Falha ao ler PDF do edital.") }
            )
        }
    }

    fun extractEditalFromImage(bitmap: Bitmap, fallbackTitle: String) {
        viewModelScope.launch {
            _editalImportState.value = UiState.Loading
            val title = fallbackTitle.ifBlank {
                (profileState.value as? UiState.Success)?.data?.targetExam.orEmpty()
            }
            val result = aiService.extractEditalFromImage(bitmap, title)
            _editalImportState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Falha ao ler imagem do edital.") }
            )
        }
    }

    fun confirmEditalDrafts(drafts: List<EditalItemDraft>, replaceExisting: Boolean) {
        viewModelScope.launch {
            val result = repository.addEditalItemsBatch(drafts, replaceExisting)
            if (result.isSuccess) {
                val count = result.getOrDefault(drafts.size)
                _editalImportState.value = UiState.Idle
                _feedbackMessage.value = "$count tópico(s) do edital salvos e cruzados com seu desempenho!"
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao salvar estrutura do edital."
            }
        }
    }

    fun clearEditalPreview() {
        _editalImportState.value = UiState.Idle
    }

    // --- Question Attempts ---

    fun recordAttempt(draft: ImportedAttemptDraft, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.addAttempt(draft)
            if (result.isSuccess) {
                _feedbackMessage.value = "Desempenho registrado e atualizado no Mapa Inteligente."
                onSaved()
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Não foi possível registrar a questão."
            }
        }
    }

    fun deleteAttempt(attemptId: String) {
        viewModelScope.launch {
            val result = repository.deleteAttempt(attemptId)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao remover registro."
            }
        }
    }

    fun extractFromScreenshot(bitmap: Bitmap) {
        viewModelScope.launch {
            _importExtractionState.value = UiState.Loading
            val defaultBoard = (profileState.value as? UiState.Success)?.data?.examBoard ?: "Cebraspe"
            val result = aiService.extractFromQConcursosImage(bitmap, defaultBoard)
            _importExtractionState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Falha ao analisar print.") }
            )
        }
    }

    fun extractFromPastedText(rawText: String, useAiFallback: Boolean = true) {
        if (rawText.isBlank()) {
            _importExtractionState.value = UiState.Error("Cole o texto ou CSV das estatísticas do QConcursos primeiro.")
            return
        }
        viewModelScope.launch {
            _importExtractionState.value = UiState.Loading
            val defaultBoard = (profileState.value as? UiState.Success)?.data?.examBoard ?: "Cebraspe"
            val csvDrafts = QConcursosParser.parseCsvReport(rawText, defaultBoard)
            if (csvDrafts.isNotEmpty()) {
                _importExtractionState.value = UiState.Success(csvDrafts)
                return@launch
            }
            if (!useAiFallback) {
                _importExtractionState.value = UiState.Error("Formato CSV não reconhecido.")
                return@launch
            }
            val result = aiService.extractFromRawText(rawText, defaultBoard)
            _importExtractionState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Falha ao interpretar texto.") }
            )
        }
    }

    fun confirmImportedDrafts(drafts: List<ImportedAttemptDraft>) {
        viewModelScope.launch {
            val result = repository.addAttemptsBatch(drafts)
            if (result.isSuccess) {
                val count = result.getOrDefault(drafts.size)
                _importExtractionState.value = UiState.Idle
                _feedbackMessage.value = "$count registro(s) importado(s) do QConcursos com sucesso!"
            } else {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao importar lote de questões."
            }
        }
    }

    fun clearImportPreview() {
        _importExtractionState.value = UiState.Idle
    }

    // --- AI Diagnostics & Revision ---

    fun generateDeepDiagnostic() {
        val snapshot = analyticsState.value
        val attempts = (attemptsState.value as? UiState.Success)?.data.orEmpty()
        val profile = (profileState.value as? UiState.Success)?.data
        if (snapshot.totalQuestions == 0) {
            _diagnosticGenerationState.value = UiState.Error(
                "Registre ou importe ao menos 1 questão do QConcursos para gerar seu diagnóstico metacognitivo."
            )
            return
        }

        viewModelScope.launch {
            _diagnosticGenerationState.value = UiState.Loading
            val aiResult = aiService.generateDeepDiagnostic(profile, snapshot, attempts)
            aiResult.fold(
                onSuccess = { report ->
                    val saveResult = repository.saveDiagnosticReport(report)
                    if (saveResult.isSuccess) {
                        _diagnosticGenerationState.value = UiState.Success(Unit)
                        _feedbackMessage.value = "Diagnóstico metacognitivo atualizado com IA."
                    } else {
                        _diagnosticGenerationState.value = UiState.Error(
                            saveResult.exceptionOrNull()?.localizedMessage
                                ?: "Diagnóstico gerado, mas falhou ao salvar."
                        )
                    }
                },
                onFailure = { err ->
                    _diagnosticGenerationState.value = UiState.Error(
                        err.localizedMessage ?: "Erro ao consultar o Mentor IA."
                    )
                }
            )
        }
    }

    fun generateSmartRevisionPlan() {
        val snapshot = analyticsState.value
        val attempts = (attemptsState.value as? UiState.Success)?.data.orEmpty()
        val profile = (profileState.value as? UiState.Success)?.data
        if (snapshot.totalQuestions == 0) {
            _revisionGenerationState.value = UiState.Error(
                "Registre ou importe questões primeiro para montar um plano baseado nos seus erros reais."
            )
            return
        }

        viewModelScope.launch {
            _revisionGenerationState.value = UiState.Loading
            val aiResult = aiService.generateRevisionPlan(profile, snapshot, attempts)
            aiResult.fold(
                onSuccess = { tasks ->
                    val saveResult = repository.replaceRevisionPlan(tasks)
                    if (saveResult.isSuccess) {
                        _revisionGenerationState.value = UiState.Success(Unit)
                        _feedbackMessage.value = "Plano de revisão inteligente gerado (${tasks.size} prioridades)."
                    } else {
                        _revisionGenerationState.value = UiState.Error(
                            saveResult.exceptionOrNull()?.localizedMessage ?: "Erro ao salvar plano."
                        )
                    }
                },
                onFailure = { err ->
                    _revisionGenerationState.value = UiState.Error(
                        err.localizedMessage ?: "Falha ao gerar plano de revisão com IA."
                    )
                }
            )
        }
    }

    fun toggleRevisionTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            val result = repository.toggleRevisionTaskCompleted(taskId, completed)
            if (result.isFailure) {
                _feedbackMessage.value = result.exceptionOrNull()?.localizedMessage
                    ?: "Erro ao atualizar tarefa."
            }
        }
    }

    fun deleteRevisionTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteRevisionTask(taskId)
        }
    }

    fun askMentor(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return
        val snapshot = analyticsState.value
        val profile = (profileState.value as? UiState.Success)?.data

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _mentorChatState.value = MentorChatState(
                isStreaming = true,
                lastQuestion = trimmed,
                responseText = "",
                errorMessage = null
            )
            try {
                aiService.streamMentorAnswer(trimmed, profile, snapshot).collect { text ->
                    _mentorChatState.value = _mentorChatState.value.copy(
                        isStreaming = true,
                        responseText = text
                    )
                }
                _mentorChatState.value = _mentorChatState.value.copy(isStreaming = false)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Mentor chat timed out", e)
                _mentorChatState.value = _mentorChatState.value.copy(
                    isStreaming = false,
                    errorMessage = "A resposta do Mentor excedeu 60s. Tente novamente."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Mentor chat failed", e)
                _mentorChatState.value = _mentorChatState.value.copy(
                    isStreaming = false,
                    errorMessage = e.localizedMessage ?: "Erro ao consultar o Mentor IA."
                )
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable, fallbackMessage: String): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, fallbackMessage, rootError)
        return UiState.Error(rootError.localizedMessage ?: fallbackMessage)
    }

    private companion object {
        const val TAG = "MentorViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
