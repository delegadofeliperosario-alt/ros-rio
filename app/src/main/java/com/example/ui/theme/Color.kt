package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta 60-30-10 acessível (WCAG AA >= 4.5:1 para texto normal e >= 3:1 para ícones/títulos).
 * Evita propositalmente preto puro (#000000) e branco estourado (#FFFFFF) para conforto ocular,
 * reduzindo fadiga visual em sessões prolongadas, baixa visão e sensibilidade à luz.
 */

// --- Light Mode (60% Neutro suave, 30% Apoio ardósia, 10% Ação Azul-Nilo) ---
val LightCanvasNeutral = Color(0xFFF3F6F9)      // 60%: fundo neutro suave (sem branco estourado)
val LightSurfaceSoft = Color(0xFFFAFBFD)        // Superfície de cards confortável aos olhos
val LightSurfaceVariant = Color(0xFFE7EDF4)     // 30%: apoio estrutural e containers sutis
val LightPrimaryAction = Color(0xFF134678)      // 10%: Azul-Nilo profundo (contraste > 8.5:1)
val LightOnPrimary = Color(0xFFF8FAFC)          // Off-white suave sobre primária
val LightSecondarySupport = Color(0xFF0F665C)   // Verde-Nilo acessível (contraste > 6.4:1)
val LightSecondaryContainer = Color(0xFFDCEEEB)
val LightOnSecondaryContainer = Color(0xFF0A3D37)
val LightTextPrimary = Color(0xFF1A2230)        // Carvão suave (sem preto puro; contraste ~14.5:1)
val LightTextSecondary = Color(0xFF475569)      // Ardósia legível (contraste ~6.8:1 > 4.5:1)
val LightBorder = Color(0xFFCBD5E1)
val LightFocusRing = Color(0xFF0A3D91)          // Indicador nítido de foco para teclado/acesso por interruptor

// --- Dark Mode (60% Ardósia noturna suave, 30% Elevação calma, 10% Ciano-Nilo) ---
val DarkCanvasNeutral = Color(0xFF121821)       // 60%: fundo escuro suave (sem preto puro #000000)
val DarkSurfaceSoft = Color(0xFF1B2432)         // Superfície de cards elevada
val DarkSurfaceVariant = Color(0xFF243042)      // 30%: apoio estrutural
val DarkPrimaryAction = Color(0xFF6BB5FF)       // 10%: Azul-Nilo luminoso (contraste > 8.2:1 no escuro)
val DarkOnPrimary = Color(0xFF0E1B2B)
val DarkSecondarySupport = Color(0xFF4FD1C5)    // Ciano-Nilo suave (contraste > 9.5:1)
val DarkSecondaryContainer = Color(0xFF143536)
val DarkOnSecondaryContainer = Color(0xFFC8F3EE)
val DarkTextPrimary = Color(0xFFE6EDF5)         // Off-white confortável (sem branco puro #FFFFFF)
val DarkTextSecondary = Color(0xFFA8B7C9)       // Cinza-azulado legível (contraste > 7.5:1)
val DarkBorder = Color(0xFF33435A)
val DarkFocusRing = Color(0xFF5EEAD4)

// --- Hero / Accent Executive Tokens ---
val NileDeepNavy = Color(0xFF142842)
val NileDeepNavyElevated = Color(0xFF1D3759)
val NileGoldAccent = Color(0xFFC98A1B)
val NileGoldAccentBright = Color(0xFFEAB952)

// --- Espectro Semântico de Domínio (Sempre combinado com Ícone + Texto para daltonismo) ---
val MasteryCriticalLight = Color(0xFFB41730)    // Contraste > 6.2:1 em container claro
val MasteryCriticalBgLight = Color(0xFFFCE8EC)
val MasteryLowLight = Color(0xFFB54708)         // Contraste > 5.4:1
val MasteryLowBgLight = Color(0xFFFEF0E3)
val MasteryIntermediateLight = Color(0xFF8C5A00)// Contraste > 5.6:1
val MasteryIntermediateBgLight = Color(0xFFFCF5DC)
val MasteryGoodLight = Color(0xFF125A8A)        // Contraste > 6.9:1
val MasteryGoodBgLight = Color(0xFFE3F2FA)
val MasteryExcellentLight = Color(0xFF0B6E4F)   // Contraste > 5.9:1
val MasteryExcellentBgLight = Color(0xFFE1F5ED)
