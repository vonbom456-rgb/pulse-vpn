package app.pulse.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PulseColors {
    val Ink = Color(0xFF090A0E)
    val Surface = Color(0xFF12141A)
    val SurfaceHigh = Color(0xFF191C24)
    val Violet = Color(0xFF7467F0)
    val Cyan = Color(0xFF14D9C5)
    val Success = Color(0xFF00E6A0)
    val Danger = Color(0xFFFF5C7A)
    val Text = Color(0xFFF5F6FA)
    val Muted = Color(0xFF9299AA)
    val Stroke = Color(0x22FFFFFF)
}

private fun darkColors(accent: String) = darkPalette(accent).let { palette -> darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.secondaryContainer,
    onSecondaryContainer = palette.onSecondaryContainer,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onSurface,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.outline,
    error = PulseColors.Danger,
)}

private fun lightColors(accent: String) = lightPalette(accent).let { palette -> lightColorScheme(
    primary = palette.primary,
    onPrimary = palette.onPrimary,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onPrimaryContainer,
    secondary = palette.secondary,
    onSecondary = palette.onSecondary,
    secondaryContainer = palette.secondaryContainer,
    onSecondaryContainer = palette.onSecondaryContainer,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onSurface,
    surfaceVariant = palette.surfaceVariant,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.outline,
    error = PulseColors.Danger,
)}

private data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
)

private fun darkPalette(accent: String): ThemePalette = when (accent) {
    "ocean" -> ThemePalette(Color(0xFF4AA8FF), Color(0xFF002F55), Color(0xFF07517E), Color(0xFFCBE6FF), Color(0xFF38D8CE), Color(0xFF003735), Color(0xFF00514D), Color(0xFF83F2E9), Color(0xFF061721), Color(0xFFE3F3FF), Color(0xFF0C222F), Color(0xFFE3F3FF), Color(0xFF123747), Color(0xFFBBD9E6), Color(0xFF8BAFBE))
    "ember" -> ThemePalette(Color(0xFFFF8060), Color(0xFF4B0A00), Color(0xFF72200F), Color(0xFFFFDAD0), Color(0xFFFFBD61), Color(0xFF442800), Color(0xFF653D00), Color(0xFFFFDDB0), Color(0xFF1B0D0A), Color(0xFFFFEDE8), Color(0xFF2A1511), Color(0xFFFFEDE8), Color(0xFF43221A), Color(0xFFF0C4B8), Color(0xFFC99486))
    "mono" -> ThemePalette(Color(0xFFD0D3DF), Color(0xFF292B34), Color(0xFF454853), Color(0xFFE8E9F0), Color(0xFFE0E2EA), Color(0xFF292B34), Color(0xFF454853), Color(0xFFE8E9F0), Color(0xFF101114), Color(0xFFE8E9F0), Color(0xFF1B1C21), Color(0xFFE8E9F0), Color(0xFF292B31), Color(0xFFC7C9D2), Color(0xFF989BA8))
    "midnight" -> ThemePalette(Color(0xFF9C91FF), Color(0xFF2D206E), Color(0xFF4A3C96), Color(0xFFE8DEFF), Color(0xFFB9A5FF), Color(0xFF30205A), Color(0xFF4C3781), Color(0xFFEBDDFF), Color(0xFF0B0A18), Color(0xFFF0ECFF), Color(0xFF17142A), Color(0xFFF0ECFF), Color(0xFF242041), Color(0xFFD5C9FA), Color(0xFFAFA1D7))
    else -> ThemePalette(PulseColors.Violet, Color.White, Color(0xFF40377F), Color(0xFFE8E3FF), PulseColors.Cyan, Color(0xFF003734), Color(0xFF00514C), Color(0xFF8DF4E9), PulseColors.Ink, PulseColors.Text, PulseColors.Surface, PulseColors.Text, PulseColors.SurfaceHigh, Color(0xFFD0C9F7), Color(0xFF9692A9))
}

private fun lightPalette(accent: String): ThemePalette = when (accent) {
    "ocean" -> ThemePalette(Color(0xFF0066A8), Color.White, Color(0xFFCAE6FF), Color(0xFF001D36), Color(0xFF006B67), Color.White, Color(0xFF9BF1E7), Color(0xFF00201E), Color(0xFFF0F8FF), Color(0xFF0A1B28), Color.White, Color(0xFF0A1B28), Color(0xFFD9EDF7), Color(0xFF123B4B), Color(0xFF52717F))
    "ember" -> ThemePalette(Color(0xFFB63820), Color.White, Color(0xFFFFDAD0), Color(0xFF3C0800), Color(0xFF925C00), Color.White, Color(0xFFFFDDB0), Color(0xFF2F1800), Color(0xFFFFF6F3), Color(0xFF26130F), Color.White, Color(0xFF26130F), Color(0xFFF8DED7), Color(0xFF57251C), Color(0xFF8B6259))
    "mono" -> ThemePalette(Color(0xFF545968), Color.White, Color(0xFFE1E2E8), Color(0xFF191A20), Color(0xFF646875), Color.White, Color(0xFFE2E3E9), Color(0xFF1B1C22), Color(0xFFF5F5F7), Color(0xFF1A1B20), Color.White, Color(0xFF1A1B20), Color(0xFFE5E6EB), Color(0xFF353740), Color(0xFF747783))
    "midnight" -> ThemePalette(Color(0xFF5546B8), Color.White, Color(0xFFE5DEFF), Color(0xFF16005D), Color(0xFF7654B9), Color.White, Color(0xFFEEDCFF), Color(0xFF2A0A4E), Color(0xFFF7F5FF), Color(0xFF17132C), Color.White, Color(0xFF17132C), Color(0xFFE8E1FF), Color(0xFF373061), Color(0xFF716991))
    else -> ThemePalette(Color(0xFF5B4DDA), Color.White, Color(0xFFE4DFFF), Color(0xFF19005D), Color(0xFF008D80), Color.White, Color(0xFF9AF0E5), Color(0xFF00201D), Color(0xFFF4F5F8), Color(0xFF101116), Color.White, Color(0xFF101116), Color(0xFFE9EBF1), Color(0xFF30313A), Color(0xFF757985))
}

@Composable
fun PulseTheme(dark: Boolean, accent: String = "pulse", content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) darkColors(accent) else lightColors(accent),
        typography = androidx.compose.material3.Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 56.sp, letterSpacing = (-2).sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.8).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.4).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        ),
        content = content,
    )
}
