package com.sylveroptimizer.android.services

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AppSettingsData(
    var themeId: String = "EmeraldCyber",
    var autoOptimizeEnabled: Boolean = false,
    var autoOptimizeThresholdPercent: Int = 85,
    var telemetryIntervalMs: Int = 1000,
    var deepCleanResiduals: Boolean = true,
    var purgeCachedAppsOnOneClick: Boolean = true
)

data class ColorThemeDefinition(
    val id: String,
    val name: String,
    val subtitle: String,
    val badgeText: String,
    val windowBg: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val subCardBg: Color,
    val subCardBorder: Color,
    val logBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentPrimary: Color,
    val accentPrimaryButtonBg: Color,
    val accentPrimaryButtonFg: Color,
    val accentSecondary: Color,
    val accentSecondaryBg: Color,
    val accentSecondaryBorder: Color,
    val accentSecondaryFg: Color,
    val accentTertiary: Color,
    val secondaryButtonBg: Color,
    val secondaryButtonHoverBg: Color,
    val secondaryButtonBorder: Color,
    val bannerGradStart: Color,
    val bannerGradMid: Color,
    val bannerGradEnd: Color,
    val bannerBorder: Color
)

class AppSettingsService(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val themes: List<ColorThemeDefinition> = buildThemes()

    var current: AppSettingsData = loadSettings()
        private set

    fun getThemeById(id: String?): ColorThemeDefinition {
        if (!id.isNullOrBlank()) {
            themes.firstOrNull { it.id.equals(id, ignoreCase = true) }?.let { return it }
        }
        return themes.first()
    }

    fun saveSettings(updated: AppSettingsData = current) {
        current = updated.copy(
            autoOptimizeThresholdPercent = updated.autoOptimizeThresholdPercent.coerceIn(65, 95),
            telemetryIntervalMs = if (updated.telemetryIntervalMs in listOf(500, 1000, 2000)) {
                updated.telemetryIntervalMs
            } else {
                1000
            }
        )
        prefs.edit()
            .putString(KEY_THEME_ID, current.themeId)
            .putBoolean(KEY_AUTO_OPTIMIZE, current.autoOptimizeEnabled)
            .putInt(KEY_AUTO_THRESHOLD, current.autoOptimizeThresholdPercent)
            .putInt(KEY_TELEMETRY_INTERVAL, current.telemetryIntervalMs)
            .putBoolean(KEY_DEEP_CLEAN_RESIDUALS, current.deepCleanResiduals)
            .putBoolean(KEY_PURGE_CACHED_ONE_CLICK, current.purgeCachedAppsOnOneClick)
            .apply()
    }

    fun resetToDefaults(): AppSettingsData {
        val defaults = AppSettingsData()
        saveSettings(defaults)
        return current
    }

    private fun loadSettings(): AppSettingsData {
        val threshold = prefs.getInt(KEY_AUTO_THRESHOLD, 85).coerceIn(65, 95)
        val rawInterval = prefs.getInt(KEY_TELEMETRY_INTERVAL, 1000)
        val interval = if (rawInterval in listOf(500, 1000, 2000)) rawInterval else 1000

        return AppSettingsData(
            themeId = prefs.getString(KEY_THEME_ID, "EmeraldCyber") ?: "EmeraldCyber",
            autoOptimizeEnabled = prefs.getBoolean(KEY_AUTO_OPTIMIZE, false),
            autoOptimizeThresholdPercent = threshold,
            telemetryIntervalMs = interval,
            deepCleanResiduals = prefs.getBoolean(KEY_DEEP_CLEAN_RESIDUALS, true),
            purgeCachedAppsOnOneClick = prefs.getBoolean(KEY_PURGE_CACHED_ONE_CLICK, true)
        )
    }

    companion object {
        private const val PREFS_NAME = "sylver_optimizer_settings"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_AUTO_OPTIMIZE = "auto_optimize_enabled"
        private const val KEY_AUTO_THRESHOLD = "auto_optimize_threshold"
        private const val KEY_TELEMETRY_INTERVAL = "telemetry_interval_ms"
        private const val KEY_DEEP_CLEAN_RESIDUALS = "deep_clean_residuals"
        private const val KEY_PURGE_CACHED_ONE_CLICK = "purge_cached_one_click"

        private fun hex(hexString: String): Color {
            val clean = hexString.removePrefix("#")
            val argb = when (clean.length) {
                6 -> ("FF$clean").toLong(16)
                8 -> clean.toLong(16)
                else -> 0xFF000000L
            }
            return Color(argb)
        }

        private fun buildThemes(): List<ColorThemeDefinition> = listOf(
            ColorThemeDefinition(
                id = "EmeraldCyber",
                name = "Esmeralda Cyber",
                subtitle = "Oscuro profundo con acentos verde neón y azul cian estilo AVG Cleaner.",
                badgeText = "Predeterminado",
                windowBg = hex("#0D1117"),
                cardBg = hex("#161B22"),
                cardBorder = hex("#262C36"),
                subCardBg = hex("#0F141C"),
                subCardBorder = hex("#21262D"),
                logBg = hex("#0A0E14"),
                textPrimary = hex("#F0F6FC"),
                textSecondary = hex("#8B949E"),
                textMuted = hex("#6E7681"),
                accentPrimary = hex("#00E676"),
                accentPrimaryButtonBg = hex("#00C853"),
                accentPrimaryButtonFg = hex("#07120B"),
                accentSecondary = hex("#00B0FF"),
                accentSecondaryBg = hex("#112A3E"),
                accentSecondaryBorder = hex("#0091EA"),
                accentSecondaryFg = hex("#40C4FF"),
                accentTertiary = hex("#D2A8FF"),
                secondaryButtonBg = hex("#21262D"),
                secondaryButtonHoverBg = hex("#29323D"),
                secondaryButtonBorder = hex("#30363D"),
                bannerGradStart = hex("#13222E"),
                bannerGradMid = hex("#161B22"),
                bannerGradEnd = hex("#122723"),
                bannerBorder = hex("#263545")
            ),
            ColorThemeDefinition(
                id = "ArcticOcean",
                name = "Océano Ártico",
                subtitle = "Azul marino profundo con acentos turquesa eléctrico y azul zafiro.",
                badgeText = "Azul / Cian",
                windowBg = hex("#09111E"),
                cardBg = hex("#111C2D"),
                cardBorder = hex("#1F314D"),
                subCardBg = hex("#0B1524"),
                subCardBorder = hex("#192841"),
                logBg = hex("#070D17"),
                textPrimary = hex("#F0F7FF"),
                textSecondary = hex("#8CA3C0"),
                textMuted = hex("#627B9C"),
                accentPrimary = hex("#00E5FF"),
                accentPrimaryButtonBg = hex("#00B8D4"),
                accentPrimaryButtonFg = hex("#04151F"),
                accentSecondary = hex("#448AFF"),
                accentSecondaryBg = hex("#12264A"),
                accentSecondaryBorder = hex("#2979FF"),
                accentSecondaryFg = hex("#82B1FF"),
                accentTertiary = hex("#80D8FF"),
                secondaryButtonBg = hex("#17263D"),
                secondaryButtonHoverBg = hex("#213554"),
                secondaryButtonBorder = hex("#284066"),
                bannerGradStart = hex("#0F2742"),
                bannerGradMid = hex("#111C2D"),
                bannerGradEnd = hex("#0E2B38"),
                bannerBorder = hex("#23456B")
            ),
            ColorThemeDefinition(
                id = "AmethystNeon",
                name = "Amatista Neón",
                subtitle = "Estética synthwave violeta oscuro con acentos púrpura neón y magenta.",
                badgeText = "Púrpura / Synth",
                windowBg = hex("#0F0B18"),
                cardBg = hex("#181226"),
                cardBorder = hex("#2D2246"),
                subCardBg = hex("#120D1D"),
                subCardBorder = hex("#241B38"),
                logBg = hex("#0A0711"),
                textPrimary = hex("#F6F0FF"),
                textSecondary = hex("#9E90B8"),
                textMuted = hex("#73648F"),
                accentPrimary = hex("#E040FB"),
                accentPrimaryButtonBg = hex("#AA00FF"),
                accentPrimaryButtonFg = hex("#FFFFFF"),
                accentSecondary = hex("#00E5FF"),
                accentSecondaryBg = hex("#24143D"),
                accentSecondaryBorder = hex("#AB47BC"),
                accentSecondaryFg = hex("#EA80FC"),
                accentTertiary = hex("#FF80AB"),
                secondaryButtonBg = hex("#231A36"),
                secondaryButtonHoverBg = hex("#30234A"),
                secondaryButtonBorder = hex("#3B2C59"),
                bannerGradStart = hex("#2A133D"),
                bannerGradMid = hex("#181226"),
                bannerGradEnd = hex("#191838"),
                bannerBorder = hex("#432866")
            ),
            ColorThemeDefinition(
                id = "CrimsonGaming",
                name = "Carmesí Gaming",
                subtitle = "Alto contraste en negro obsidiana con acentos rojo rubí y naranja.",
                badgeText = "Rojo / Gaming",
                windowBg = hex("#110B0D"),
                cardBg = hex("#1C1215"),
                cardBorder = hex("#362026"),
                subCardBg = hex("#150D10"),
                subCardBorder = hex("#2B191E"),
                logBg = hex("#0C0709"),
                textPrimary = hex("#FFF0F3"),
                textSecondary = hex("#B08D95"),
                textMuted = hex("#82626A"),
                accentPrimary = hex("#FF1744"),
                accentPrimaryButtonBg = hex("#D50000"),
                accentPrimaryButtonFg = hex("#FFFFFF"),
                accentSecondary = hex("#FF9100"),
                accentSecondaryBg = hex("#33141B"),
                accentSecondaryBorder = hex("#FF1744"),
                accentSecondaryFg = hex("#FF8A80"),
                accentTertiary = hex("#FFD180"),
                secondaryButtonBg = hex("#29181D"),
                secondaryButtonHoverBg = hex("#382128"),
                secondaryButtonBorder = hex("#452830"),
                bannerGradStart = hex("#2E1218"),
                bannerGradMid = hex("#1C1215"),
                bannerGradEnd = hex("#2B1912"),
                bannerBorder = hex("#4D222B")
            ),
            ColorThemeDefinition(
                id = "SolarAmber",
                name = "Solar Dorado",
                subtitle = "Carbón titanio con elegantes acentos en oro cálido y ámbar brillante.",
                badgeText = "Ámbar / Oro",
                windowBg = hex("#12110E"),
                cardBg = hex("#1C1915"),
                cardBorder = hex("#332D24"),
                subCardBg = hex("#15130F"),
                subCardBorder = hex("#29241D"),
                logBg = hex("#0D0B09"),
                textPrimary = hex("#FDF8F0"),
                textSecondary = hex("#A89E8E"),
                textMuted = hex("#7A7163"),
                accentPrimary = hex("#FFAB00"),
                accentPrimaryButtonBg = hex("#FF8F00"),
                accentPrimaryButtonFg = hex("#140D02"),
                accentSecondary = hex("#00E5FF"),
                accentSecondaryBg = hex("#2E2412"),
                accentSecondaryBorder = hex("#FFAB00"),
                accentSecondaryFg = hex("#FFD740"),
                accentTertiary = hex("#FFE57F"),
                secondaryButtonBg = hex("#26221B"),
                secondaryButtonHoverBg = hex("#332E24"),
                secondaryButtonBorder = hex("#40392D"),
                bannerGradStart = hex("#2B2111"),
                bannerGradMid = hex("#1C1915"),
                bannerGradEnd = hex("#1F2416"),
                bannerBorder = hex("#473920")
            ),
            ColorThemeDefinition(
                id = "NordicLight",
                name = "Modo Claro Cristal",
                subtitle = "Interfaz clara y luminosa con verde esmeralda y azul.",
                badgeText = "Modo Claro",
                windowBg = hex("#F2F5F9"),
                cardBg = hex("#FFFFFF"),
                cardBorder = hex("#D5DFEC"),
                subCardBg = hex("#F5F8FC"),
                subCardBorder = hex("#E1E8F2"),
                logBg = hex("#EDF2F8"),
                textPrimary = hex("#16202E"),
                textSecondary = hex("#526277"),
                textMuted = hex("#748499"),
                accentPrimary = hex("#00A854"),
                accentPrimaryButtonBg = hex("#009E4F"),
                accentPrimaryButtonFg = hex("#FFFFFF"),
                accentSecondary = hex("#0078D4"),
                accentSecondaryBg = hex("#E6F2FF"),
                accentSecondaryBorder = hex("#0078D4"),
                accentSecondaryFg = hex("#005A9E"),
                accentTertiary = hex("#7B2CBF"),
                secondaryButtonBg = hex("#EBF0F7"),
                secondaryButtonHoverBg = hex("#DCE5F2"),
                secondaryButtonBorder = hex("#C5D2E3"),
                bannerGradStart = hex("#E3F5EC"),
                bannerGradMid = hex("#FFFFFF"),
                bannerGradEnd = hex("#E6F2FF"),
                bannerBorder = hex("#B8D4E8")
            ),
            ColorThemeDefinition(
                id = "DarkGray",
                name = "Gris Oscuro",
                subtitle = "Tonos grafito y pizarra neutros con acentos plata brillante y acero.",
                badgeText = "Gris / Grafito",
                windowBg = hex("#1E1F22"),
                cardBg = hex("#2B2D31"),
                cardBorder = hex("#3F4248"),
                subCardBg = hex("#232529"),
                subCardBorder = hex("#35383E"),
                logBg = hex("#191A1D"),
                textPrimary = hex("#F2F3F5"),
                textSecondary = hex("#B5BAC1"),
                textMuted = hex("#80848E"),
                accentPrimary = hex("#E2E8F0"),
                accentPrimaryButtonBg = hex("#D4D8E0"),
                accentPrimaryButtonFg = hex("#18191C"),
                accentSecondary = hex("#94A3B8"),
                accentSecondaryBg = hex("#353840"),
                accentSecondaryBorder = hex("#64748B"),
                accentSecondaryFg = hex("#E2E8F0"),
                accentTertiary = hex("#CBD5E1"),
                secondaryButtonBg = hex("#35373C"),
                secondaryButtonHoverBg = hex("#43464D"),
                secondaryButtonBorder = hex("#4E525A"),
                bannerGradStart = hex("#282A2E"),
                bannerGradMid = hex("#2B2D31"),
                bannerGradEnd = hex("#25272B"),
                bannerBorder = hex("#484B52")
            ),
            ColorThemeDefinition(
                id = "LightGray",
                name = "Gris Claro",
                subtitle = "Interfaz en tonos gris perla y aluminio suave de bajo cansancio visual.",
                badgeText = "Gris / Plata",
                windowBg = hex("#D8DCE3"),
                cardBg = hex("#E6E9EF"),
                cardBorder = hex("#BFC6D1"),
                subCardBg = hex("#DCE0E8"),
                subCardBorder = hex("#C8CFD9"),
                logBg = hex("#D2D7E0"),
                textPrimary = hex("#1E2229"),
                textSecondary = hex("#4E5562"),
                textMuted = hex("#6C7483"),
                accentPrimary = hex("#374151"),
                accentPrimaryButtonBg = hex("#374151"),
                accentPrimaryButtonFg = hex("#F9FAFB"),
                accentSecondary = hex("#4B5563"),
                accentSecondaryBg = hex("#D0D6E0"),
                accentSecondaryBorder = hex("#6B7280"),
                accentSecondaryFg = hex("#1F2937"),
                accentTertiary = hex("#52525B"),
                secondaryButtonBg = hex("#D5DAE2"),
                secondaryButtonHoverBg = hex("#C6CDD8"),
                secondaryButtonBorder = hex("#AEB7C4"),
                bannerGradStart = hex("#DCE1E9"),
                bannerGradMid = hex("#E6E9EF"),
                bannerGradEnd = hex("#D8DEE8"),
                bannerBorder = hex("#B4BCC9")
            )
        )
    }
}

