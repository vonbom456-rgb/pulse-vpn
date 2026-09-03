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

private val DarkColors = darkColorScheme(
    primary = PulseColors.Violet,
    secondary = PulseColors.Cyan,
    background = PulseColors.Ink,
    surface = PulseColors.Surface,
    surfaceVariant = PulseColors.SurfaceHigh,
    onPrimary = Color.White,
    onBackground = PulseColors.Text,
    onSurface = PulseColors.Text,
    error = PulseColors.Danger,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4DDA),
    secondary = Color(0xFF008D80),
    background = Color(0xFFF4F5F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EBF1),
    onBackground = Color(0xFF101116),
    onSurface = Color(0xFF101116),
)

@Composable
fun PulseTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
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
