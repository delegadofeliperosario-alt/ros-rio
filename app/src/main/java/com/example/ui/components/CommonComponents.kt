package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MasteryLevel
import com.example.data.model.RevisionPriority
import com.example.ui.theme.LocalMasteryColors
import com.example.ui.theme.MonoTabularFamily

/**
 * Indicador de foco de alto contraste para navegação via teclado (Tab/Enter/Esc) e Switch Access,
 * equivalente nativo em Compose a :focus-visible.
 */
fun Modifier.accessibleFocusRing(shape: Shape = RoundedCornerShape(14.dp)): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val focusColor = LocalMasteryColors.current.focusRing
    this
        .onFocusChanged { isFocused = it.isFocused }
        .then(
            if (isFocused) {
                Modifier.border(width = 3.dp, color = focusColor, shape = shape)
            } else {
                Modifier
            }
        )
}

@Composable
fun masteryForegroundColor(level: MasteryLevel): Color {
    val palette = LocalMasteryColors.current
    return when (level) {
        MasteryLevel.CRITICO -> palette.critical
        MasteryLevel.BAIXO -> palette.low
        MasteryLevel.INTERMEDIARIO -> palette.intermediate
        MasteryLevel.BOM -> palette.good
        MasteryLevel.EXCELENTE -> palette.excellent
    }
}

@Composable
fun masteryContainerColor(level: MasteryLevel): Color {
    val palette = LocalMasteryColors.current
    return when (level) {
        MasteryLevel.CRITICO -> palette.criticalContainer
        MasteryLevel.BAIXO -> palette.lowContainer
        MasteryLevel.INTERMEDIARIO -> palette.intermediateContainer
        MasteryLevel.BOM -> palette.goodContainer
        MasteryLevel.EXCELENTE -> palette.excellentContainer
    }
}

/**
 * Acessibilidade para daltonismo: cada nível de domínio possui um ícone geométrico distinto
 * além da cor e do rótulo textual explícito.
 */
fun masterySemanticIcon(level: MasteryLevel): ImageVector = when (level) {
    MasteryLevel.CRITICO -> Icons.Rounded.ErrorOutline
    MasteryLevel.BAIXO -> Icons.Rounded.WarningAmber
    MasteryLevel.INTERMEDIARIO -> Icons.Rounded.Schedule
    MasteryLevel.BOM -> Icons.Rounded.TrendingUp
    MasteryLevel.EXCELENTE -> Icons.Rounded.CheckCircle
}

@Composable
fun priorityColors(priority: RevisionPriority): Pair<Color, Color> {
    val palette = LocalMasteryColors.current
    return when (priority) {
        RevisionPriority.CRITICA -> palette.critical to palette.criticalContainer
        RevisionPriority.ALTA -> palette.low to palette.lowContainer
        RevisionPriority.MEDIA -> palette.intermediate to palette.intermediateContainer
        RevisionPriority.MANUTENCAO -> palette.excellent to palette.excellentContainer
    }
}

fun prioritySemanticIcon(priority: RevisionPriority): ImageVector = when (priority) {
    RevisionPriority.CRITICA -> Icons.Rounded.ErrorOutline
    RevisionPriority.ALTA -> Icons.Rounded.WarningAmber
    RevisionPriority.MEDIA -> Icons.Rounded.Schedule
    RevisionPriority.MANUTENCAO -> Icons.Rounded.CheckCircle
}

/**
 * Selo de nível de domínio inclusivo: combina Cor + Ícone Semântico + Texto explícito.
 */
@Composable
fun MasteryPill(
    level: MasteryLevel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val fg = masteryForegroundColor(level)
    val bg = masteryContainerColor(level)
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = masterySemanticIcon(level),
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (compact) level.shortLabel else level.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@Composable
fun UnstudiedPill(modifier: Modifier = Modifier) {
    val fg = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Não iniciado",
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

/**
 * Cabeçalho semântico de seção marcado com heading() para navegação rápida em leitores de tela (TalkBack).
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.width(12.dp))
            Surface(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .accessibleFocusRing(RoundedCornerShape(12.dp))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Rótulo visível e persistente acima de campos de formulário para evitar dependência de placeholders.
 */
@Composable
fun AccessibleFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    required: Boolean = false
) {
    Text(
        text = if (required) "$text *" else text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 4.dp)
    )
}

/**
 * Alerta acessível que nunca depende apenas de cor: combina cor, ícone, texto e liveRegion Polite.
 */
@Composable
fun AccessibleAlertBanner(
    message: String,
    isError: Boolean = true,
    modifier: Modifier = Modifier
) {
    val bg = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val icon = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Info

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isError) "Alerta de erro" else "Informação",
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = fg
            )
        }
    }
}

@Composable
fun EditorialCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val border = LocalMasteryColors.current.border
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    EditorialCard(
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontFamily = MonoTabularFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyDataGuidanceCard(
    icon: ImageVector,
    title: String,
    description: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    actionTestTag: String,
    modifier: Modifier = Modifier
) {
    EditorialCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPrimaryAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .accessibleFocusRing(RoundedCornerShape(14.dp))
                    .testTag(actionTestTag)
            ) {
                Text(primaryActionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
