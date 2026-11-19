package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.attemptAutoSignIn
import com.example.core.onGoogleSignInClicked
import com.example.ui.components.AccessibleAlertBanner
import com.example.ui.components.accessibleFocusRing
import com.example.ui.theme.LocalMasteryColors

/**
 * Tela de Entrada do Método Nilo com Google Sign-In (Credential Manager).
 * Acessibilidade: contraste WCAG AA, tipografia >= 16sp com altura de linha 1.5x,
 * alertas com ícone + texto e botão principal >= 54dp com foco visível.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val masteryColors = LocalMasteryColors.current

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Hero Editorial Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(masteryColors.heroNavy)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mentor_banner_1795048557582),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        masteryColors.heroNavy.copy(alpha = 0.62f),
                                        masteryColors.heroNavy.copy(alpha = 0.94f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Surface(
                            color = masteryColors.goldAccent.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = "METACOGNIÇÃO • AUTORREGULAÇÃO • QCONCURSOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE6B0),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Método Nilo",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFFF7F8FA),
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Plataforma inteligente que cruza seu edital com seu histórico de questões para revelar exatamente o que estudar agora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF7F8FA).copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                FeatureHighlightRow(
                    icon = Icons.Rounded.MenuBook,
                    title = "Edital Verticalizado Inteligente",
                    description = "Anexe o PDF, print ou texto do seu edital e organize automaticamente em Disciplina › Assunto › Subassunto."
                )
                Spacer(Modifier.height(16.dp))
                FeatureHighlightRow(
                    icon = Icons.Rounded.Insights,
                    title = "Mapa Inteligente de Desempenho",
                    description = "Visualize % de acertos e erros, pontos fortes a manter e pontos fracos prioritários em atualização contínua."
                )
                Spacer(Modifier.height(16.dp))
                FeatureHighlightRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Diagnóstico IA e Plano de Revisão",
                    description = "Cruze lacunas teóricas vs. desatenção com dispositivos legais, jurisprudência STF/STJ e data da prova."
                )
                Spacer(Modifier.height(16.dp))
                FeatureHighlightRow(
                    icon = Icons.Rounded.AccessibilityNew,
                    title = "Design Inclusivo e Acessível (WCAG AA)",
                    description = "Modo Claro e Escuro confortáveis, ícones semânticos para daltonismo, suporte a leitor de tela e redução de movimento."
                )
            }

            Column(modifier = Modifier.padding(top = 24.dp)) {
                if (!errorMessage.isNullOrBlank()) {
                    AccessibleAlertBanner(
                        message = errorMessage.orEmpty(),
                        isError = true,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .accessibleFocusRing(RoundedCornerShape(14.dp))
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Entrar com Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Seu edital, histórico de questões, diagnósticos e revisões ficam salvos com segurança na nuvem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
