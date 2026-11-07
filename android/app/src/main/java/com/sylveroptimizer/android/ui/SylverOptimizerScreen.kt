package com.sylveroptimizer.android.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import com.sylveroptimizer.android.services.AppSettingsService
import com.sylveroptimizer.android.services.ColorThemeDefinition
import com.sylveroptimizer.android.services.OptimizerService
import com.sylveroptimizer.android.services.ProcessMemoryItem
import com.sylveroptimizer.android.services.SystemMetricsSnapshot
import com.sylveroptimizer.android.services.SystemMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val HISTORY_MAX_POINTS = 60

@Composable
fun SylverOptimizerScreen(
    lifecycle: Lifecycle,
    settingsService: AppSettingsService,
    monitorService: SystemMonitorService,
    optimizerService: OptimizerService,
    onThemeChanged: (ColorThemeDefinition) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentSettings by remember { mutableStateOf(settingsService.current) }
    var activeTheme by remember { mutableStateOf(settingsService.getThemeById(currentSettings.themeId)) }
    var showSettingsView by remember { mutableStateOf(false) }
    var showCpuInfoDialog by remember { mutableStateOf(false) }

    var snapshot by remember { mutableStateOf(monitorService.createInitialSnapshot()) }
    val ramHistory = remember { mutableStateListOf<Double>().apply { repeat(HISTORY_MAX_POINTS) { add(0.0) } } }
    val cpuHistory = remember { mutableStateListOf<Double>().apply { repeat(HISTORY_MAX_POINTS) { add(0.0) } } }
    val activityLog = remember { mutableStateListOf<String>() }
    val topProcesses = remember { mutableStateListOf<ProcessMemoryItem>() }

    var sessionFreedRamMb by remember { mutableDoubleStateOf(0.0) }
    var sessionCleanedCacheMb by remember { mutableDoubleStateOf(0.0) }
    var diskCacheSizeText by remember { mutableStateOf("Calculando...") }
    var lastActionBannerText by remember {
        mutableStateOf("Listo para liberar memoria RAM en 2º plano, purgar apps en espera y limpiar caché temporal de Android.")
    }
    var isBusyOptimizing by remember { mutableStateOf(false) }
    var hasUsageAccess by remember { mutableStateOf(monitorService.hasUsageStatsPermission()) }
    var tickCount by remember { mutableIntStateOf(0) }
    var lastAutoOptimizeMs by remember { mutableLongStateOf(0L) }

    fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        activityLog.add(0, "[$time] $message")
        while (activityLog.size > 100) {
            activityLog.removeAt(activityLog.lastIndex)
        }
    }

    fun formatSessionCounter(mb: Double): String {
        return if (mb >= 1024.0) {
            String.format(Locale.US, "%.2f GB", mb / 1024.0)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }

    suspend fun refreshProcessesList() {
        hasUsageAccess = monitorService.hasUsageStatsPermission()
        val list = withContext(Dispatchers.IO) { monitorService.getTopRamProcesses(12) }
        topProcesses.clear()
        topProcesses.addAll(list)
    }

    suspend fun executeReleaseRam() {
        if (isBusyOptimizing) return
        isBusyOptimizing = true
        try {
            val result = optimizerService.releaseRamWorkingSetsAsync()
            sessionFreedRamMb += result.freedRamMb
            lastActionBannerText = "✅ ${result.summaryMessage}"
            addLog(result.summaryMessage)
            monitorService.invalidatePackageCache()
            snapshot = monitorService.getMetricsSnapshotAsync()
            refreshProcessesList()
        } finally {
            isBusyOptimizing = false
        }
    }

    // Actualizar permisos y lista al regresar desde Ajustes del sistema (ON_RESUME)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updatedAccess = monitorService.hasUsageStatsPermission()
                if (updatedAccess != hasUsageAccess) {
                    hasUsageAccess = updatedAccess
                    addLog(
                        if (updatedAccess) {
                            "Permiso de Acceso a Uso concedido: Detección precisa de apps en 2º plano activada."
                        } else {
                            "Permiso de Acceso a Uso desactivado: Usando detección estándar."
                        }
                    )
                    scope.launch { refreshProcessesList() }
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Inicialización al arrancar la aplicación
    LaunchedEffect(Unit) {
        onThemeChanged(activeTheme)
        addLog("SylverOptimizer iniciado en Android. Monitoreando RAM (/proc/meminfo) y frecuencias de CPU.")
        if (hasUsageAccess) {
            addLog("Permiso de Acceso a Uso activo: Detección detallada de apps en 2º plano habilitada.")
        } else {
            addLog("Modo Estándar activo: Puedes conceder 'Acceso a Uso' arriba a la derecha para mayor precisión de procesos.")
        }
        refreshProcessesList()
        val initialScan = optimizerService.scanSystemCacheAsync()
        diskCacheSizeText = String.format(Locale.US, "%.2f MB", initialScan.totalSizeMb)
    }

    // Bucle de telemetría en tiempo real (en Dispatchers.IO y pausado automáticamente en segundo plano)
    LaunchedEffect(lifecycle, currentSettings.telemetryIntervalMs) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                val snap = monitorService.getMetricsSnapshotAsync()
                snapshot = snap

                if (ramHistory.size >= HISTORY_MAX_POINTS) ramHistory.removeAt(0)
                ramHistory.add(snap.ramUsagePercent)

                if (cpuHistory.size >= HISTORY_MAX_POINTS) cpuHistory.removeAt(0)
                cpuHistory.add(snap.cpuUsagePercent)

                tickCount++
                if (tickCount % 4 == 0 && !isBusyOptimizing) {
                    refreshProcessesList()
                }

                // Auto-optimización inteligente con enfriamiento de 60s
                val nowMs = System.currentTimeMillis()
                if (currentSettings.autoOptimizeEnabled &&
                    !isBusyOptimizing &&
                    snap.ramUsagePercent >= currentSettings.autoOptimizeThresholdPercent &&
                    (nowMs - lastAutoOptimizeMs) >= 60_000L
                ) {
                    lastAutoOptimizeMs = nowMs
                    addLog(
                        String.format(
                            Locale.US,
                            "[Auto-Optimización] Uso de RAM alto (%.1f%% >= %d%%). Liberando procesos en 2º plano...",
                            snap.ramUsagePercent,
                            currentSettings.autoOptimizeThresholdPercent
                        )
                    )
                    executeReleaseRam()
                }

                delay(currentSettings.telemetryIntervalMs.toLong())
            }
        }
    }

    val logoGradient = Brush.linearGradient(
        colors = listOf(activeTheme.accentPrimary, activeTheme.accentSecondary)
    )
    val bannerGradient = Brush.horizontalGradient(
        colors = listOf(activeTheme.bannerGradStart, activeTheme.bannerGradMid, activeTheme.bannerGradEnd)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(activeTheme.windowBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // ================================================================
        // BARRA SUPERIOR GLOBAL Y NAVEGACIÓN
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(logoGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 20.sp,
                            color = activeTheme.accentPrimaryButtonFg
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SylverOptimizer",
                            color = activeTheme.textPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Optimizador de RAM, Caché y Monitor de CPU para Android",
                            color = activeTheme.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Botón de permiso de Acceso a Uso
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        } catch (_: Exception) {
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (hasUsageAccess) activeTheme.accentPrimary else activeTheme.secondaryButtonBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = activeTheme.secondaryButtonBg,
                        contentColor = if (hasUsageAccess) activeTheme.accentPrimary else activeTheme.textPrimary
                    )
                ) {
                    Text(
                        text = if (hasUsageAccess) "⚡ Uso Activo" else "🛡️ Acceso a Uso",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de pestañas + Toggle rápido de Auto-optimización
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeTheme.cardBg)
                        .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(8.dp))
                        .padding(3.dp)
                ) {
                    val dashBg = if (!showSettingsView) activeTheme.accentSecondaryBg else Color.Transparent
                    val dashFg = if (!showSettingsView) activeTheme.accentSecondaryFg else activeTheme.textPrimary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(dashBg)
                            .clickable { showSettingsView = false }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📊 Panel Principal",
                            color = dashFg,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    val setBg = if (showSettingsView) activeTheme.accentSecondaryBg else Color.Transparent
                    val setFg = if (showSettingsView) activeTheme.accentSecondaryFg else activeTheme.textPrimary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(setBg)
                            .clickable { showSettingsView = true }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "⚙️ Configuración",
                            color = setFg,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Toggle rápido de Auto-liberar RAM
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeTheme.cardBg)
                        .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            val next = !currentSettings.autoOptimizeEnabled
                            val updated = currentSettings.copy(autoOptimizeEnabled = next)
                            settingsService.saveSettings(updated)
                            currentSettings = settingsService.current
                            addLog(
                                if (next) {
                                    "[Configuración] Auto-optimización de RAM activada (>${currentSettings.autoOptimizeThresholdPercent}%)."
                                } else {
                                    "[Configuración] Auto-optimización de RAM desactivada."
                                }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = currentSettings.autoOptimizeEnabled,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = activeTheme.accentPrimary,
                            uncheckedColor = activeTheme.textSecondary,
                            checkmarkColor = activeTheme.accentPrimaryButtonFg
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-RAM (>${currentSettings.autoOptimizeThresholdPercent}%)",
                        color = activeTheme.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ================================================================
        // CONTENIDO PRINCIPAL: PANEL DE MONITOREO vs CONFIGURACIÓN
        // ================================================================
        if (!showSettingsView) {
            DashboardContent(
                snapshot = snapshot,
                ramHistory = ramHistory,
                cpuHistory = cpuHistory,
                topProcesses = topProcesses,
                activityLog = activityLog,
                sessionFreedRamText = formatSessionCounter(sessionFreedRamMb),
                sessionCleanedCacheText = formatSessionCounter(sessionCleanedCacheMb),
                diskCacheSizeText = diskCacheSizeText,
                lastActionBannerText = lastActionBannerText,
                deepCleanResiduals = currentSettings.deepCleanResiduals,
                isBusyOptimizing = isBusyOptimizing,
                logicalCores = monitorService.logicalCores,
                theme = activeTheme,
                bannerGradient = bannerGradient,
                onShowCpuInfo = { showCpuInfoDialog = true },
                onOneClickOptimize = {
                    if (isBusyOptimizing) return@DashboardContent
                    scope.launch {
                        isBusyOptimizing = true
                        try {
                            addLog("=== Iniciando Optimización Integral 1-Clic ===")

                            val ramRes = optimizerService.releaseRamWorkingSetsAsync()
                            sessionFreedRamMb += ramRes.freedRamMb
                            addLog(ramRes.summaryMessage)

                            var standbyMb = 0.0
                            if (currentSettings.purgeCachedAppsOnOneClick) {
                                val standbyRes = optimizerService.purgeRamStandbyCacheAsync()
                                standbyMb = standbyRes.cachePurgedMb
                                sessionFreedRamMb += standbyRes.freedRamMb
                                addLog(standbyRes.summaryMessage)
                            }

                            val diskRes = optimizerService.cleanSystemCacheAsync(currentSettings.deepCleanResiduals)
                            sessionCleanedCacheMb += diskRes.freedDiskMb
                            addLog(diskRes.summaryMessage)

                            monitorService.invalidatePackageCache()
                            snapshot = monitorService.getMetricsSnapshotAsync()
                            refreshProcessesList()

                            val postScan = optimizerService.scanSystemCacheAsync()
                            diskCacheSizeText = String.format(Locale.US, "%.2f MB", postScan.totalSizeMb)

                            val totalRamGain = ramRes.freedRamMb + standbyMb
                            lastActionBannerText = String.format(
                                Locale.US,
                                "✅ Optimización completada: %.1f MB de RAM optimizados y %.2f MB de caché eliminados.",
                                totalRamGain,
                                diskRes.freedDiskMb
                            )
                            addLog("=== Optimización Integral 1-Clic finalizada con éxito ===")
                        } finally {
                            isBusyOptimizing = false
                        }
                    }
                },
                onReleaseRam = {
                    scope.launch { executeReleaseRam() }
                },
                onPurgeStandby = {
                    if (isBusyOptimizing) return@DashboardContent
                    scope.launch {
                        isBusyOptimizing = true
                        try {
                            val res = optimizerService.purgeRamStandbyCacheAsync()
                            sessionFreedRamMb += res.cachePurgedMb
                            lastActionBannerText = "✅ ${res.summaryMessage}"
                            addLog(res.summaryMessage)
                            monitorService.invalidatePackageCache()
                            snapshot = monitorService.getMetricsSnapshotAsync()
                            refreshProcessesList()
                        } finally {
                            isBusyOptimizing = false
                        }
                    }
                },
                onScanDiskCache = {
                    if (isBusyOptimizing) return@DashboardContent
                    scope.launch {
                        isBusyOptimizing = true
                        try {
                            diskCacheSizeText = "Escaneando..."
                            val scan = optimizerService.scanSystemCacheAsync()
                            diskCacheSizeText = String.format(Locale.US, "%.2f MB", scan.totalSizeMb)
                            lastActionBannerText = "🔍 ${scan.detailsMessage}"
                            addLog(scan.detailsMessage)
                        } finally {
                            isBusyOptimizing = false
                        }
                    }
                },
                onCleanDiskCache = {
                    if (isBusyOptimizing) return@DashboardContent
                    scope.launch {
                        isBusyOptimizing = true
                        try {
                            val clean = optimizerService.cleanSystemCacheAsync(currentSettings.deepCleanResiduals)
                            sessionCleanedCacheMb += clean.freedDiskMb
                            lastActionBannerText = "✅ ${clean.summaryMessage}"
                            addLog(clean.summaryMessage)

                            val scanAfter = optimizerService.scanSystemCacheAsync()
                            diskCacheSizeText = String.format(Locale.US, "%.2f MB", scanAfter.totalSizeMb)
                        } finally {
                            isBusyOptimizing = false
                        }
                    }
                },
                onTrimSingleProcess = { item ->
                    scope.launch {
                        val (ok, freedMb, msg) = withContext(Dispatchers.IO) {
                            optimizerService.trimSingleProcess(item.packageName, item.appName, item.estimatedMb)
                        }
                        if (ok && freedMb > 0) {
                            sessionFreedRamMb += freedMb
                        }
                        lastActionBannerText = if (ok) "✅ $msg" else "ℹ️ $msg"
                        addLog(msg)
                        monitorService.invalidatePackageCache()
                        snapshot = monitorService.getMetricsSnapshotAsync()
                        refreshProcessesList()
                    }
                },
                onRefreshProcesses = {
                    scope.launch {
                        refreshProcessesList()
                        addLog("Lista de aplicaciones en segundo plano actualizada.")
                    }
                },
                onClearLog = {
                    activityLog.clear()
                    addLog("Registro de eventos limpiado.")
                }
            )
        } else {
            SettingsContent(
                themes = settingsService.themes,
                activeTheme = activeTheme,
                currentSettings = currentSettings,
                bannerGradient = bannerGradient,
                onSelectTheme = { themeId ->
                    val newTheme = settingsService.getThemeById(themeId)
                    activeTheme = newTheme
                    val updated = currentSettings.copy(themeId = newTheme.id)
                    settingsService.saveSettings(updated)
                    currentSettings = settingsService.current
                    onThemeChanged(newTheme)
                },
                onUpdateSettings = { updated, logMessage ->
                    settingsService.saveSettings(updated)
                    currentSettings = settingsService.current
                    if (logMessage != null) {
                        addLog(logMessage)
                    }
                },
                onResetDefaults = {
                    val defaults = settingsService.resetToDefaults()
                    currentSettings = defaults
                    val defTheme = settingsService.getThemeById(defaults.themeId)
                    activeTheme = defTheme
                    onThemeChanged(defTheme)
                    addLog("[Configuración] Se han restaurado los ajustes predeterminados.")
                },
                onBackToDashboard = { showSettingsView = false }
            )
        }
    }

    if (showCpuInfoDialog) {
        AlertDialog(
            onDismissRequest = { showCpuInfoDialog = false },
            containerColor = activeTheme.cardBg,
            titleContentColor = activeTheme.textPrimary,
            textContentColor = activeTheme.textSecondary,
            title = {
                Text(
                    text = "📱 INFORMACIÓN DEL PROCESADOR / SoC",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = monitorService.cpuModelName,
                        color = activeTheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${monitorService.logicalCores} Núcleos Lógicos • Arquitectura ${monitorService.deviceArchitecture}",
                        color = activeTheme.accentSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(
                            Locale.US,
                            "Temperatura de Batería/SoC: %.1f °C",
                            snapshot.batteryTempCelsius
                        ),
                        color = activeTheme.textSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCpuInfoDialog = false }) {
                    Text(
                        text = "Cerrar",
                        color = activeTheme.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun DashboardContent(
    snapshot: SystemMetricsSnapshot,
    ramHistory: List<Double>,
    cpuHistory: List<Double>,
    topProcesses: List<ProcessMemoryItem>,
    activityLog: List<String>,
    sessionFreedRamText: String,
    sessionCleanedCacheText: String,
    diskCacheSizeText: String,
    lastActionBannerText: String,
    deepCleanResiduals: Boolean,
    isBusyOptimizing: Boolean,
    logicalCores: Int,
    theme: ColorThemeDefinition,
    bannerGradient: Brush,
    onShowCpuInfo: () -> Unit,
    onOneClickOptimize: () -> Unit,
    onReleaseRam: () -> Unit,
    onPurgeStandby: () -> Unit,
    onScanDiskCache: () -> Unit,
    onCleanDiskCache: () -> Unit,
    onTrimSingleProcess: (ProcessMemoryItem) -> Unit,
    onRefreshProcesses: () -> Unit,
    onClearLog: () -> Unit
) {
    val scrollState = rememberScrollState()

    val (healthDotColor, healthTitle) = when {
        snapshot.ramUsagePercent >= 85.0 || snapshot.cpuUsagePercent >= 90.0 ->
            ColorCriticalRed to String.format(
                Locale.US,
                "Estado del Sistema: Carga Elevada (RAM %.0f%% • CPU %.0f%%)",
                snapshot.ramUsagePercent,
                snapshot.cpuUsagePercent
            )
        snapshot.ramUsagePercent >= 68.0 || snapshot.cpuUsagePercent >= 70.0 ->
            ColorWarningAmber to String.format(
                Locale.US,
                "Estado del Sistema: Carga Moderada (RAM %.0f%% • CPU %.0f%%)",
                snapshot.ramUsagePercent,
                snapshot.cpuUsagePercent
            )
        else ->
            theme.accentPrimary to String.format(
                Locale.US,
                "Estado del Sistema: Rendimiento Óptimo (RAM %.0f%% • CPU %.0f%%)",
                snapshot.ramUsagePercent,
                snapshot.cpuUsagePercent
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================================================================
        // 1. BANNER DE OPTIMIZACIÓN RÁPIDA (1-CLIC)
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bannerGradient)
                .border(1.dp, theme.bannerBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(healthDotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = healthTitle,
                    color = theme.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastActionBannerText,
                color = theme.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.subCardBg)
                        .border(1.dp, theme.subCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RAM LIBERADA (SESIÓN)",
                        color = theme.textSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sessionFreedRamText,
                        color = theme.accentPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.subCardBg)
                        .border(1.dp, theme.subCardBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "CACHÉ LIMPIADA (SESIÓN)",
                        color = theme.textSecondary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sessionCleanedCacheText,
                        color = theme.accentSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onOneClickOptimize,
                enabled = !isBusyOptimizing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accentPrimaryButtonBg,
                    contentColor = theme.accentPrimaryButtonFg
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = if (isBusyOptimizing) "⏳ OPTIMIZANDO SISTEMA..." else "🚀 OPTIMIZAR TODO (1-CLIC)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ================================================================
        // 2. TARJETA DE MEMORIA RAM FÍSICA & CACHÉ
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.cardBg)
                .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMORIA RAM FÍSICA & CACHÉ",
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "%.1f GB Totales", snapshot.totalRamGb),
                    color = theme.accentPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularMetricGauge(
                    percent = snapshot.ramUsagePercent,
                    primaryText = String.format(Locale.US, "%.0f%%", snapshot.ramUsagePercent),
                    secondaryText = String.format(
                        Locale.US,
                        "%.1f / %.1f GB",
                        snapshot.usedRamGb,
                        snapshot.totalRamGb
                    ),
                    arcColor = resolveLoadColor(snapshot.ramUsagePercent, isCpu = false, theme = theme),
                    trackColor = theme.subCardBorder,
                    textPrimary = theme.textPrimary,
                    textSecondary = theme.textSecondary
                )

                Spacer(modifier = Modifier.width(12.dp))

                RealTimeAreaChart(
                    title = "Historial RAM (60s)",
                    values = ramHistory,
                    lineColor = theme.accentPrimary,
                    areaColor = theme.accentPrimary.copy(alpha = 0.18f),
                    subCardBg = theme.subCardBg,
                    subCardBorder = theme.subCardBorder,
                    textMuted = theme.textMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricDetailTile(
                    label = "En Uso",
                    value = String.format(Locale.US, "%.2f GB", snapshot.usedRamGb),
                    valueColor = theme.textPrimary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Disponible",
                    value = String.format(Locale.US, "%.2f GB", snapshot.availableRamGb),
                    valueColor = theme.accentPrimary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Caché Kernel",
                    value = String.format(Locale.US, "%.2f GB", snapshot.systemCacheGb),
                    valueColor = theme.accentSecondary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Swap / ZRAM",
                    value = String.format(
                        Locale.US,
                        "%.1f / %.1f GB",
                        snapshot.swapUsedGb,
                        snapshot.swapTotalGb
                    ),
                    valueColor = theme.accentTertiary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReleaseRam,
                    enabled = !isBusyOptimizing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.accentSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.accentSecondaryBg,
                        contentColor = theme.accentSecondaryFg
                    )
                ) {
                    Text(
                        text = "🧹 Liberar RAM (2º Plano)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPurgeStandby,
                    enabled = !isBusyOptimizing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.secondaryButtonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.secondaryButtonBg,
                        contentColor = theme.textPrimary
                    )
                ) {
                    Text(
                        text = "⚡ Purgar Caché RAM",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ================================================================
        // 3. TARJETA DE CPU / SoC & CACHÉ DEL SISTEMA
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.cardBg)
                .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROCESADOR (CPU / SoC) & CACHÉ DEL SISTEMA",
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(theme.accentSecondaryBg)
                        .border(1.dp, theme.accentSecondaryBorder, RoundedCornerShape(6.dp))
                        .clickable { onShowCpuInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ⓘ",
                        color = theme.accentSecondaryFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularMetricGauge(
                    percent = snapshot.cpuUsagePercent,
                    primaryText = String.format(Locale.US, "%.0f%%", snapshot.cpuUsagePercent),
                    secondaryText = "$logicalCores Núcleos",
                    arcColor = resolveLoadColor(snapshot.cpuUsagePercent, isCpu = true, theme = theme),
                    trackColor = theme.subCardBorder,
                    textPrimary = theme.textPrimary,
                    textSecondary = theme.textSecondary
                )

                Spacer(modifier = Modifier.width(12.dp))

                RealTimeAreaChart(
                    title = "Historial CPU (60s)",
                    values = cpuHistory,
                    lineColor = theme.accentSecondary,
                    areaColor = theme.accentSecondary.copy(alpha = 0.18f),
                    subCardBg = theme.subCardBg,
                    subCardBorder = theme.subCardBorder,
                    textMuted = theme.textMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricDetailTile(
                    label = "Apps Activas",
                    value = snapshot.activePackageCount.toString(),
                    valueColor = theme.textPrimary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Hilos (Threads)",
                    value = snapshot.threadCount.toString(),
                    valueColor = theme.accentSecondary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Temp. Batería",
                    value = String.format(Locale.US, "%.1f °C", snapshot.batteryTempCelsius),
                    valueColor = theme.textPrimary,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
                MetricDetailTile(
                    label = "Caché Temp.",
                    value = diskCacheSizeText,
                    valueColor = ColorWarningAmber,
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onScanDiskCache,
                    enabled = !isBusyOptimizing,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.secondaryButtonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.secondaryButtonBg,
                        contentColor = theme.textPrimary
                    )
                ) {
                    Text(
                        text = "🔍 Analizar Caché",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onCleanDiskCache,
                    enabled = !isBusyOptimizing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.accentSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.accentSecondaryBg,
                        contentColor = theme.accentSecondaryFg
                    )
                ) {
                    Text(
                        text = if (deepCleanResiduals) {
                            "🗑️ Limpiar Caché + Residuales"
                        } else {
                            "🗑️ Limpiar Caché Temporal"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ================================================================
        // 4. TOP APLICACIONES CON MAYOR IMPACTO EN RAM
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.cardBg)
                .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "APPS ACTIVAS CON MAYOR USO DE RAM",
                        color = theme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca 'Liberar' para detener los procesos en 2º plano de una app individual",
                        color = theme.textMuted,
                        fontSize = 10.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onRefreshProcesses,
                    enabled = !isBusyOptimizing,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.secondaryButtonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.secondaryButtonBg,
                        contentColor = theme.textPrimary
                    )
                ) {
                    Text(
                        text = "🔄 Actualizar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (topProcesses.isEmpty()) {
                Text(
                    text = "No se detectaron aplicaciones secundarias pesadas en ejecución.",
                    color = theme.textMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    topProcesses.forEach { proc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.subCardBg)
                                .border(1.dp, theme.subCardBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proc.appName,
                                    color = theme.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = proc.packageName,
                                    color = theme.textMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = proc.formattedRam,
                                color = theme.accentPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.secondaryButtonBg)
                                    .border(1.dp, theme.secondaryButtonBorder, RoundedCornerShape(6.dp))
                                    .clickable { onTrimSingleProcess(proc) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Liberar",
                                    color = theme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================================================================
        // 5. REGISTRO DE OPTIMIZACIÓN Y EVENTOS (LOG EN VIVO)
        // ================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.cardBg)
                .border(1.dp, theme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTRO DE OPTIMIZACIÓN Y EVENTOS",
                    color = theme.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onClearLog,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, theme.secondaryButtonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = theme.secondaryButtonBg,
                        contentColor = theme.textPrimary
                    )
                ) {
                    Text(
                        text = "Limpiar Log",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.logBg)
                    .border(1.dp, theme.subCardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                activityLog.take(16).forEach { entry ->
                    Text(
                        text = entry,
                        color = theme.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsContent(
    themes: List<ColorThemeDefinition>,
    activeTheme: ColorThemeDefinition,
    currentSettings: com.sylveroptimizer.android.services.AppSettingsData,
    bannerGradient: Brush,
    onSelectTheme: (String) -> Unit,
    onUpdateSettings: (com.sylveroptimizer.android.services.AppSettingsData, String?) -> Unit,
    onResetDefaults: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner de Configuración
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bannerGradient)
                .border(1.dp, activeTheme.bannerBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "⚙️ Configuración de SylverOptimizer",
                color = activeTheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Elige entre los 8 temas de color en tiempo real y personaliza el comportamiento del optimizador. Los cambios se guardan automáticamente.",
                color = activeTheme.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, activeTheme.secondaryButtonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = activeTheme.secondaryButtonBg,
                        contentColor = activeTheme.textPrimary
                    )
                ) {
                    Text(
                        text = "🔄 Restaurar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeTheme.accentPrimaryButtonBg,
                        contentColor = activeTheme.accentPrimaryButtonFg
                    )
                ) {
                    Text(
                        text = "← Volver al Panel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tarjeta de Selector de 8 Temas de Color
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(activeTheme.cardBg)
                .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎨 TEMAS DE COLOR Y APARIENCIA VISUAL",
                        color = activeTheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Toca cualquier paleta para cambiar el aspecto visual al instante.",
                        color = activeTheme.textMuted,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "Activo: ${activeTheme.name}",
                    color = activeTheme.accentPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cuadrícula de 2 columnas para los 8 temas en pantallas móviles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { item ->
                            ThemePreviewCard(
                                themeItem = item,
                                isSelected = item.id.equals(activeTheme.id, ignoreCase = true),
                                onSelect = { onSelectTheme(item.id) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Automatización y Optimización de RAM
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(activeTheme.cardBg)
                .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🧠 AUTOMATIZACIÓN Y OPTIMIZACIÓN DE RAM",
                color = activeTheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Auto-optimizar RAM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.subCardBg)
                    .border(1.dp, activeTheme.subCardBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        val next = !currentSettings.autoOptimizeEnabled
                        onUpdateSettings(
                            currentSettings.copy(autoOptimizeEnabled = next),
                            if (next) {
                                "[Configuración] Auto-optimización de RAM activada (>${currentSettings.autoOptimizeThresholdPercent}%)."
                            } else {
                                "[Configuración] Auto-optimización de RAM desactivada."
                            }
                        )
                    }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentSettings.autoOptimizeEnabled,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = activeTheme.accentPrimary,
                            uncheckedColor = activeTheme.textSecondary,
                            checkmarkColor = activeTheme.accentPrimaryButtonFg
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liberar RAM automáticamente cuando supere el umbral",
                        color = activeTheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detiene procesos inactivos en segundo plano (con enfriamiento de 60s) para evitar saturación de memoria.",
                    color = activeTheme.textSecondary,
                    fontSize = 11.5.sp
                )
            }

            // Slider de Umbral
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.subCardBg)
                    .border(1.dp, activeTheme.subCardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Umbral de disparo de uso de RAM:",
                        color = activeTheme.textPrimary,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "${currentSettings.autoOptimizeThresholdPercent}%",
                        color = activeTheme.accentPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = currentSettings.autoOptimizeThresholdPercent.toFloat(),
                    onValueChange = { raw ->
                        val stepped = ((raw / 5f).roundToInt() * 5).coerceIn(65, 95)
                        if (stepped != currentSettings.autoOptimizeThresholdPercent) {
                            onUpdateSettings(
                                currentSettings.copy(autoOptimizeThresholdPercent = stepped),
                                null
                            )
                        }
                    },
                    valueRange = 65f..95f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = activeTheme.accentPrimary,
                        activeTrackColor = activeTheme.accentPrimary,
                        inactiveTrackColor = activeTheme.subCardBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("65% (Agresivo)", color = activeTheme.textMuted, fontSize = 10.sp)
                    Text("85% (Recomendado)", color = activeTheme.textMuted, fontSize = 10.sp)
                    Text("95% (Conservador)", color = activeTheme.textMuted, fontSize = 10.sp)
                }
            }

            // Purgar apps en espera en 1-Clic
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.subCardBg)
                    .border(1.dp, activeTheme.subCardBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        val next = !currentSettings.purgeCachedAppsOnOneClick
                        onUpdateSettings(
                            currentSettings.copy(purgeCachedAppsOnOneClick = next),
                            null
                        )
                    }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentSettings.purgeCachedAppsOnOneClick,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = activeTheme.accentPrimary,
                            uncheckedColor = activeTheme.textSecondary,
                            checkmarkColor = activeTheme.accentPrimaryButtonFg
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Purgar Caché de RAM en espera al usar 'Optimizar Todo (1-Clic)'",
                        color = activeTheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Caché del Sistema y Frecuencia de Monitoreo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(activeTheme.cardBg)
                .border(1.dp, activeTheme.cardBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ CACHÉ DEL SISTEMA Y MONITOREO EN TIEMPO REAL",
                color = activeTheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            // Barrido profundo de residuales
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.subCardBg)
                    .border(1.dp, activeTheme.subCardBorder, RoundedCornerShape(8.dp))
                    .clickable {
                        val next = !currentSettings.deepCleanResiduals
                        onUpdateSettings(
                            currentSettings.copy(deepCleanResiduals = next),
                            null
                        )
                    }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = currentSettings.deepCleanResiduals,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = activeTheme.accentPrimary,
                            uncheckedColor = activeTheme.textSecondary,
                            checkmarkColor = activeTheme.accentPrimaryButtonFg
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Incluir limpieza de archivos residuales (.tmp / .log)",
                        color = activeTheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Frecuencia de actualización
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeTheme.subCardBg)
                    .border(1.dp, activeTheme.subCardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Frecuencia de actualización de gráficos y telemetría:",
                    color = activeTheme.textPrimary,
                    fontSize = 12.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val intervals = listOf(
                        500 to "⚡ 0.5s",
                        1000 to "⏱️ 1.0s",
                        2000 to "🍃 2.0s"
                    )
                    intervals.forEach { (ms, label) ->
                        val selected = currentSettings.telemetryIntervalMs == ms
                        OutlinedButton(
                            onClick = {
                                onUpdateSettings(
                                    currentSettings.copy(telemetryIntervalMs = ms),
                                    String.format(
                                        Locale.US,
                                        "[Configuración] Intervalo del monitor ajustado a %.1fs.",
                                        ms / 1000.0
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(7.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) activeTheme.accentSecondaryBorder else activeTheme.secondaryButtonBorder
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) activeTheme.accentSecondaryBg else activeTheme.secondaryButtonBg,
                                contentColor = if (selected) activeTheme.accentSecondaryFg else activeTheme.textPrimary
                            ),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

