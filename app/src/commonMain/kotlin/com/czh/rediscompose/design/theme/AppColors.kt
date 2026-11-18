package com.czh.rediscompose.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.czh.rediscompose.domain.settings.Accent

/**
 * Every color the UI is allowed to use.
 *
 * Feature code reads these through [LocalAppColors]; hard-coded `Color(0x...)` values
 * outside the theme files are not allowed.
 */
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val surface4: Color,
    val surfaceHover: Color,
    val border: Color,
    val borderStrong: Color,
    val borderEmphasis: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textQuaternary: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val accentSofter: Color,
    val accentLine: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val terminalBackground: Color,
    val terminalChrome: Color,
    val terminalText: Color,
    val terminalTextSecondary: Color,
    val terminalTextDim: Color,
    val terminalPrompt: Color,
    val terminalSuccess: Color,
    val terminalError: Color,
    val terminalWarning: Color,
    val terminalBorder: Color,
    val codeBackground: Color,
    val codeLineNumber: Color,
    val typeString: Color,
    val typeHash: Color,
    val typeList: Color,
    val typeSet: Color,
    val typeZset: Color,
    val typeStream: Color,
    val typeNone: Color,
    val jsonKey: Color,
    val jsonString: Color,
    val jsonNumber: Color,
    val jsonBoolean: Color,
    val jsonPunctuation: Color,
    val scrim: Color,
) {
    companion object {
        fun forTheme(
            dark: Boolean,
            accent: Accent,
        ): AppColors = if (dark) dark(accent) else light(accent)

        private fun dark(accent: Accent): AppColors {
            val accentBase = accent.base(dark = true)
            return AppColors(
                isDark = true,
                background = Color(0xFF07090D),
                surface = Color(0xFF0C1017),
                surface2 = Color(0xFF10151F),
                surface3 = Color(0xFF151B28),
                surface4 = Color(0xFF1A2130),
                surfaceHover = Color(0xFF171E2B),
                border = Color.White.copy(alpha = 0.075f),
                borderStrong = Color.White.copy(alpha = 0.13f),
                borderEmphasis = Color.White.copy(alpha = 0.20f),
                text = Color(0xFFEEF1F8),
                textSecondary = Color(0xFFA4ADC1),
                textTertiary = Color(0xFF6C7890),
                textQuaternary = Color(0xFF4B5568),
                accent = accentBase,
                accentStrong = accent.strong(dark = true),
                accentInk = accent.ink(dark = true),
                accentSoft = accentBase.copy(alpha = 0.13f),
                accentSofter = accentBase.copy(alpha = 0.06f),
                accentLine = accentBase.copy(alpha = 0.40f),
                success = Color(0xFF45E3BE),
                warning = Color(0xFFF7C566),
                danger = Color(0xFFFF7070),
                info = Color(0xFF6FB7FF),
                terminalBackground = Color(0xFF080B11),
                terminalChrome = Color(0xFF0E131C),
                terminalText = Color(0xFFDCE3F0),
                terminalTextSecondary = Color(0xFFB9C3D5),
                terminalTextDim = Color(0xFF667186),
                terminalPrompt = Color(0xFF918DFF),
                terminalSuccess = Color(0xFF45E3BE),
                terminalError = Color(0xFFFF8686),
                terminalWarning = Color(0xFFF7C566),
                terminalBorder = Color.White.copy(alpha = 0.08f),
                codeBackground = Color(0xFF090C12),
                codeLineNumber = Color(0xFF394254),
                typeString = Color(0xFF6FB7FF),
                typeHash = Color(0xFFF7C566),
                typeList = Color(0xFFB49BFF),
                typeSet = Color(0xFF45E3BE),
                typeZset = Color(0xFFFF7BA9),
                typeStream = Color(0xFF4FD8E8),
                typeNone = Color(0xFF6C7890),
                jsonKey = Color(0xFF9DB4FF),
                jsonString = Color(0xFF7EE7C8),
                jsonNumber = Color(0xFFF7C566),
                jsonBoolean = Color(0xFFC9A3FF),
                jsonPunctuation = Color(0xFF6C7890),
                scrim = Color(0xFF04060A).copy(alpha = 0.56f),
            )
        }

        private fun light(accent: Accent): AppColors {
            val accentBase = accent.base(dark = false)
            return AppColors(
                isDark = false,
                background = Color(0xFFF5F6FA),
                surface = Color(0xFFFFFFFF),
                surface2 = Color(0xFFF7F8FC),
                surface3 = Color(0xFFEEF1F7),
                surface4 = Color(0xFFE5E9F2),
                surfaceHover = Color(0xFFF1F3F9),
                border = Color.Black.copy(alpha = 0.085f),
                borderStrong = Color.Black.copy(alpha = 0.14f),
                borderEmphasis = Color.Black.copy(alpha = 0.22f),
                text = Color(0xFF131726),
                textSecondary = Color(0xFF58627A),
                textTertiary = Color(0xFF8A93A7),
                textQuaternary = Color(0xFFAAB2C2),
                accent = accentBase,
                accentStrong = accent.strong(dark = false),
                accentInk = accent.ink(dark = false),
                accentSoft = accentBase.copy(alpha = 0.10f),
                accentSofter = accentBase.copy(alpha = 0.05f),
                accentLine = accentBase.copy(alpha = 0.35f),
                success = Color(0xFF0C9F7F),
                warning = Color(0xFFA86A12),
                danger = Color(0xFFD64545),
                info = Color(0xFF2F7DD1),
                terminalBackground = Color(0xFF10151F),
                terminalChrome = Color(0xFF151B27),
                terminalText = Color(0xFFDCE3F0),
                terminalTextSecondary = Color(0xFFB9C3D5),
                terminalTextDim = Color(0xFF667186),
                terminalPrompt = Color(0xFF918DFF),
                terminalSuccess = Color(0xFF45E3BE),
                terminalError = Color(0xFFFF8686),
                terminalWarning = Color(0xFFF7C566),
                terminalBorder = Color.White.copy(alpha = 0.08f),
                codeBackground = Color(0xFFFBFCFF),
                codeLineNumber = Color(0xFFC3CAD6),
                typeString = Color(0xFF2F7DD1),
                typeHash = Color(0xFFA86A12),
                typeList = Color(0xFF7652D8),
                typeSet = Color(0xFF0C9F7F),
                typeZset = Color(0xFFD63F78),
                typeStream = Color(0xFF0A8FA3),
                typeNone = Color(0xFF8A93A7),
                jsonKey = Color(0xFF3A56C9),
                jsonString = Color(0xFF0B8267),
                jsonNumber = Color(0xFF9A640B),
                jsonBoolean = Color(0xFF6D45C4),
                jsonPunctuation = Color(0xFF8891A4),
                scrim = Color(0xFF04060A).copy(alpha = 0.46f),
            )
        }

        private fun Accent.base(dark: Boolean): Color =
            when (this) {
                Accent.IRIS -> if (dark) Color(0xFF918DFF) else Color(0xFF5B58E8)
                Accent.MINT -> if (dark) Color(0xFF45E3BE) else Color(0xFF0C9F7F)
                Accent.SKY -> if (dark) Color(0xFF6FB7FF) else Color(0xFF2F7DD1)
                Accent.AMBER -> if (dark) Color(0xFFF7C566) else Color(0xFFA86A12)
                Accent.ROSE -> if (dark) Color(0xFFFF7BA9) else Color(0xFFD63F78)
            }

        private fun Accent.strong(dark: Boolean): Color =
            when (this) {
                Accent.IRIS -> if (dark) Color(0xFF6B67F4) else Color(0xFF4A47D4)
                else -> lerp(base(dark), Color.Black, if (dark) 0.26f else 0.16f)
            }

        private fun Accent.ink(dark: Boolean): Color =
            when {
                this == Accent.IRIS -> Color.White
                dark -> Color(0xFF090B10)
                else -> Color.White
            }
    }
}
