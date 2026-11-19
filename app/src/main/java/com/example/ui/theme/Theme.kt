package com.example.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro")
}

@Immutable
data class AccessibilityConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reducedMotion: Boolean = false
)

@Immutable
data class MasteryColorPalette(
    val critical: Color,
    val criticalContainer: Color,
    val low: Color,
    val lowContainer: Color,
    val intermediate: Color,
    val intermediateContainer: Color,
    val good: Color,
    val goodContainer: Color,
    val excellent: Color,
    val excellentContainer: Color,
    val heroNavy: Color,
    val heroNavyAccent: Color,
    val goldAccent: Color,
    val border: Color,
    val focusRing: Color
)

val LightMasteryColors = MasteryColorPalette(
    critical = MasteryCriticalLight,
    criticalContainer = MasteryCriticalBgLight,
    low = MasteryLowLight,
    lowContainer = MasteryLowBgLight,
    intermediate = MasteryIntermediateLight,
    intermediateContainer = MasteryIntermediateBgLight,
    good = MasteryGoodLight,
    goodContainer = MasteryGoodBgLight,
    excellent = MasteryExcellentLight,
    excellentContainer = MasteryExcellentBgLight,
    heroNavy = NileDeepNavy,
    heroNavyAccent = NileDeepNavyElevated,
    goldAccent = NileGoldAccent,
    border = LightBorder,
    focusRing = LightFocusRing
)

val DarkMasteryColors = MasteryColorPalette(
    critical = Color(0xFFFF8597),
    criticalContainer = Color(0xFF421420),
    low = Color(0xFFFFA76E),
    lowContainer = Color(0xFF40210F),
    intermediate = Color(0xFFF6C65B),
    intermediateContainer = Color(0xFF3A2D0D),
    good = Color(0xFF72C3F5),
    goodContainer = Color(0xFF102D44),
    excellent = Color(0xFF53DFAB),
    excellentContainer = Color(0xFF0E3427),
    heroNavy = Color(0xFF15243A),
    heroNavyAccent = Color(0xFF1E3352),
    goldAccent = NileGoldAccentBright,
    border = DarkBorder,
    focusRing = DarkFocusRing
)

val LocalMasteryColors = staticCompositionLocalOf { LightMasteryColors }
val LocalAccessibilityConfig = staticCompositionLocalOf { AccessibilityConfig() }

private val LightColorScheme = lightColorScheme(
    primary = LightPrimaryAction,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFDCE7F5),
    onPrimaryContainer = Color(0xFF0C2E52),
    secondary = LightSecondarySupport,
    onSecondary = Color(0xFFF6FAF9),
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = MasteryGoodLight,
    onTertiary = Color(0xFFF6FAFD),
    tertiaryContainer = MasteryGoodBgLight,
    onTertiaryContainer = Color(0xFF0B3A5B),
    background = LightCanvasNeutral,
    onBackground = LightTextPrimary,
    surface = LightSurfaceSoft,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFDDE5EE),
    error = MasteryCriticalLight,
    onError = Color(0xFFFFF8F9),
    errorContainer = MasteryCriticalBgLight,
    onErrorContainer = Color(0xFF780E20)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF1D3554),
    onPrimaryContainer = Color(0xFFE3EEF9),
    secondary = DarkSecondarySupport,
    onSecondary = Color(0xFF082927),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Color(0xFF53DFAB),
    onTertiary = Color(0xFF07291C),
    background = DarkCanvasNeutral,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceSoft,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF28364A),
    error = Color(0xFFFF8597),
    onError = Color(0xFF3E000D),
    errorContainer = Color(0xFF4C1422),
    onErrorContainer = Color(0xFFFFDBE0)
)

val MetodoNiloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Detecta automaticamente a preferência do sistema Android de reduzir animações
 * (equivalente nativo a prefers-reduced-motion) e combina com a escolha explícita do usuário.
 */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun MetodoNiloTheme(
    accessibilityConfig: AccessibilityConfig = AccessibilityConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (accessibilityConfig.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val systemReducedMotion = rememberSystemReducedMotion()
    val effectiveConfig = accessibilityConfig.copy(
        reducedMotion = accessibilityConfig.reducedMotion || systemReducedMotion
    )

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val masteryPalette = if (useDark) DarkMasteryColors else LightMasteryColors

    CompositionLocalProvider(
        LocalMasteryColors provides masteryPalette,
        LocalAccessibilityConfig provides effectiveConfig
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = MetodoNiloShapes,
            content = content
        )
    }
}
