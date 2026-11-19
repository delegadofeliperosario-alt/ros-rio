package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutUser
import com.example.data.model.UiState
import com.example.data.repository.AiMentorService
import com.example.data.repository.MentorRepository
import com.example.domain.QConcursosParser
import com.example.ui.components.AccessibleFieldLabel
import com.example.ui.components.accessibleFocusRing
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.EditalScreen
import com.example.ui.screens.HistoryAndProfileScreen
import com.example.ui.screens.QConcursosHubScreen
import com.example.ui.screens.RevisionPlanScreen
import com.example.ui.theme.AccessibilityConfig
import com.example.ui.theme.MetodoNiloTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.rememberSystemReducedMotion
import com.example.ui.viewmodel.MentorViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class MentorDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("dashboard", "Painel", Icons.Rounded.AutoGraph, Icons.Outlined.AutoGraph),
    EDITAL("edital", "Edital", Icons.Rounded.MenuBook, Icons.Outlined.MenuBook),
    DIAGNOSTICS("diagnostics", "Diagnóstico", Icons.Rounded.Psychology, Icons.Outlined.Psychology),
    REVISION("revision", "Revisão", Icons.Rounded.Checklist, Icons.Outlined.Checklist),
    QCONCURSOS("qconcursos", "Questões", Icons.Rounded.Language, Icons.Outlined.Language)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val systemReducedMotion = rememberSystemReducedMotion()
            var globalAccessibility by remember {
                mutableStateOf(AccessibilityConfig(reducedMotion = systemReducedMotion))
            }

            MetodoNiloTheme(accessibilityConfig = globalAccessibility) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        accessibilityConfig = globalAccessibility,
                        onUpdateAccessibility = { globalAccessibility = it }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    accessibilityConfig: AccessibilityConfig,
    onUpdateAccessibility: (AccessibilityConfig) -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedMentorShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName.orEmpty(),
                userEmail = user.email,
                accessibilityConfig = accessibilityConfig,
                onUpdateAccessibility = onUpdateAccessibility
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedMentorShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    accessibilityConfig: AccessibilityConfig,
    onUpdateAccessibility: (AccessibilityConfig) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MentorViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MentorViewModel(
                    repository = MentorRepository(db),
                    aiService = AiMentorService(),
                    currentUserId = currentUserId,
                    defaultDisplayName = userDisplayName.ifBlank {
                        userEmail?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Estudante"
                    }
                )
            }
        }
    )

    val profileUi by viewModel.profileState.collectAsStateWithLifecycle()
    val editalItemsUi by viewModel.editalItemsState.collectAsStateWithLifecycle()
    val attemptsUi by viewModel.attemptsState.collectAsStateWithLifecycle()
    val diagnosticsUi by viewModel.diagnosticsState.collectAsStateWithLifecycle()
    val revisionTasksUi by viewModel.revisionTasksState.collectAsStateWithLifecycle()
    val analytics by viewModel.analyticsState.collectAsStateWithLifecycle()
    val diagnosticGenState by viewModel.diagnosticGenerationState.collectAsStateWithLifecycle()
    val revisionGenState by viewModel.revisionGenerationState.collectAsStateWithLifecycle()
    val importExtractionState by viewModel.importExtractionState.collectAsStateWithLifecycle()
    val editalImportState by viewModel.editalImportState.collectAsStateWithLifecycle()
    val mentorChatState by viewModel.mentorChatState.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val activePortalUrl by viewModel.activePortalUrl.collectAsStateWithLifecycle()

    val profile = (profileUi as? UiState.Success)?.data
    val editalItems = (editalItemsUi as? UiState.Success)?.data.orEmpty()
    val latestDiagnostic = (diagnosticsUi as? UiState.Success)?.data?.firstOrNull()

    var currentDestination by rememberSaveable { mutableStateOf(MentorDestination.DASHBOARD) }
    var showHistoryAndProfile by rememberSaveable { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var highlightedDiscipline by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // BackHandler para retornar da tela secundária (Histórico/Perfil) ou abas para o Painel Principal
    if (showHistoryAndProfile) {
        BackHandler { showHistoryAndProfile = false }
    } else if (currentDestination != MentorDestination.DASHBOARD) {
        BackHandler { currentDestination = MentorDestination.DASHBOARD }
    }

    val openQConcursosSearch: (String, String, String) -> Unit = { discipline, subtopic, board ->
        viewModel.setPortalUrl(QConcursosParser.buildSearchUrl(discipline, subtopic, board))
        showHistoryAndProfile = false
        currentDestination = MentorDestination.QCONCURSOS
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!showHistoryAndProfile) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Método Nilo",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(
                                onClick = { showAccessibilityDialog = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .accessibleFocusRing(RoundedCornerShape(12.dp))
                                    .testTag("topbar_accessibility_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessibilityNew,
                                    contentDescription = "Ajustes de tema e acessibilidade"
                                )
                            }
                            IconButton(
                                onClick = { showHistoryAndProfile = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .accessibleFocusRing(RoundedCornerShape(12.dp))
                                    .testTag("topbar_history_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "Abrir histórico de questões e conta"
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useNavRail && !showHistoryAndProfile) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        MentorDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail && !showHistoryAndProfile) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                    ) {
                        MentorDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (showHistoryAndProfile) {
                        HistoryAndProfileScreen(
                            profile = profile,
                            userEmail = userEmail,
                            attemptsState = attemptsUi,
                            accessibilityConfig = accessibilityConfig,
                            onSetThemeMode = { mode ->
                                viewModel.setThemeMode(mode)
                                onUpdateAccessibility(accessibilityConfig.copy(themeMode = mode))
                            },
                            onSetReducedMotion = { enabled ->
                                viewModel.setReducedMotion(enabled)
                                onUpdateAccessibility(accessibilityConfig.copy(reducedMotion = enabled))
                            },
                            onDeleteAttempt = { viewModel.deleteAttempt(it) },
                            onUpdateProfile = { name, exam, board, dateIso, goal, link ->
                                viewModel.saveProfile(name, exam, board, dateIso, goal, link)
                            },
                            onSignOut = {
                                signOutUser(credentialManager = credentialManager, scope = scope)
                            },
                            onOpenQConcursosHub = {
                                showHistoryAndProfile = false
                                currentDestination = MentorDestination.QCONCURSOS
                            },
                            onNavigateBack = { showHistoryAndProfile = false }
                        )
                    } else {
                        when (currentDestination) {
                            MentorDestination.DASHBOARD -> DashboardScreen(
                                profile = profile,
                                analytics = analytics,
                                latestDiagnostic = latestDiagnostic,
                                attemptsUiState = attemptsUi,
                                onOpenEdital = { currentDestination = MentorDestination.EDITAL },
                                onOpenQConcursosHub = { currentDestination = MentorDestination.QCONCURSOS },
                                onOpenDiagnostics = { currentDestination = MentorDestination.DIAGNOSTICS },
                                onOpenRevisionPlan = { currentDestination = MentorDestination.REVISION },
                                onSelectSubtopic = { sub ->
                                    highlightedDiscipline = sub.discipline
                                    currentDestination = MentorDestination.DIAGNOSTICS
                                }
                            )

                            MentorDestination.EDITAL -> EditalScreen(
                                profile = profile,
                                editalCoverage = analytics.editalCoverage,
                                editalItemsUiState = editalItemsUi,
                                editalImportState = editalImportState,
                                onAddManualItem = { draft, onSaved -> viewModel.addEditalItem(draft, onSaved) },
                                onDeleteItem = { viewModel.deleteEditalItem(it) },
                                onClearEdital = { viewModel.clearEdital() },
                                onExtractFromPdf = { bytes, title -> viewModel.extractEditalFromPdf(bytes, title) },
                                onExtractFromImage = { bmp, title -> viewModel.extractEditalFromImage(bmp, title) },
                                onExtractFromText = { text, title -> viewModel.extractEditalFromText(text, title) },
                                onConfirmImportedDrafts = { drafts, replace ->
                                    viewModel.confirmEditalDrafts(drafts, replace)
                                },
                                onClearImportPreview = { viewModel.clearEditalPreview() },
                                onPracticeSubtopicInQConcursos = openQConcursosSearch
                            )

                            MentorDestination.DIAGNOSTICS -> DiagnosticsScreen(
                                analytics = analytics,
                                latestDiagnostic = latestDiagnostic,
                                diagnosticGenState = diagnosticGenState,
                                mentorChatState = mentorChatState,
                                highlightedDiscipline = highlightedDiscipline,
                                onGenerateDiagnostic = { viewModel.generateDeepDiagnostic() },
                                onAskMentor = { viewModel.askMentor(it) },
                                onOpenQConcursosHub = { currentDestination = MentorDestination.QCONCURSOS },
                                onPracticeSubtopicInQConcursos = { disc, sub ->
                                    openQConcursosSearch(disc, sub, profile?.examBoard ?: "Cebraspe")
                                }
                            )

                            MentorDestination.REVISION -> RevisionPlanScreen(
                                profile = profile,
                                tasksState = revisionTasksUi,
                                revisionGenState = revisionGenState,
                                hasQuestions = analytics.totalQuestions > 0,
                                onGeneratePlan = { viewModel.generateSmartRevisionPlan() },
                                onToggleTask = { id, done -> viewModel.toggleRevisionTask(id, done) },
                                onDeleteTask = { id -> viewModel.deleteRevisionTask(id) },
                                onUpdateExamTarget = { name, exam, board, dateIso, goal, link ->
                                    viewModel.saveProfile(name, exam, board, dateIso, goal, link)
                                },
                                onPracticeInQConcursos = openQConcursosSearch,
                                onOpenQConcursosHub = { currentDestination = MentorDestination.QCONCURSOS }
                            )

                            MentorDestination.QCONCURSOS -> QConcursosHubScreen(
                                profile = profile,
                                editalItems = editalItems,
                                activePortalUrl = activePortalUrl,
                                importExtractionState = importExtractionState,
                                onPortalUrlChange = { viewModel.setPortalUrl(it) },
                                onRecordAttempt = { draft, onSaved -> viewModel.recordAttempt(draft, onSaved) },
                                onExtractFromScreenshot = { viewModel.extractFromScreenshot(it) },
                                onExtractFromText = { viewModel.extractFromPastedText(it) },
                                onConfirmImportedDrafts = { viewModel.confirmImportedDrafts(it) },
                                onClearImportPreview = { viewModel.clearImportPreview() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAccessibilityDialog) {
        AccessibilityQuickDialog(
            config = accessibilityConfig,
            onSetThemeMode = { mode ->
                viewModel.setThemeMode(mode)
                onUpdateAccessibility(accessibilityConfig.copy(themeMode = mode))
            },
            onSetReducedMotion = { enabled ->
                viewModel.setReducedMotion(enabled)
                onUpdateAccessibility(accessibilityConfig.copy(reducedMotion = enabled))
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }
}

@Composable
private fun AccessibilityQuickDialog(
    config: AccessibilityConfig,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Acessibilidade e Aparência") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    AccessibleFieldLabel("Modo de Tema (Conforto Ocular)")
                    val modes = listOf(
                        ThemeMode.SYSTEM to "Sistema",
                        ThemeMode.LIGHT to "Claro",
                        ThemeMode.DARK to "Escuro"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = config.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                                modifier = Modifier.heightIn(min = 48.dp)
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
                        Text("Reduzir animações", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Desativa transições bruscas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = config.reducedMotion,
                        onCheckedChange = onSetReducedMotion
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Fechar")
            }
        }
    )
}
