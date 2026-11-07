package com.sylveroptimizer.android.services

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.max

data class SystemMetricsSnapshot(
    val cpuUsagePercent: Double,
    val ramUsagePercent: Double,
    val totalRamGb: Double,
    val usedRamGb: Double,
    val availableRamGb: Double,
    val systemCacheGb: Double,
    val swapUsedGb: Double,
    val swapTotalGb: Double,
    val availableRamBytes: Long,
    val systemCacheBytes: Long,
    val activePackageCount: Int,
    val threadCount: Int,
    val batteryTempCelsius: Double
)

data class ProcessMemoryItem(
    val packageName: String,
    val appName: String,
    val estimatedMb: Double,
    val formattedRam: String,
    val isSystemApp: Boolean
)

class SystemMonitorService(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    val logicalCores: Int = Runtime.getRuntime().availableProcessors()
    val cpuModelName: String = readCpuModelName()
    val deviceArchitecture: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    private var lastCpuPercent: Double = 18.0
    @Volatile
    private var cachedActivePackageCount: Int = 24
    @Volatile
    private var lastPackageCountTimeMs: Long = 0L

    fun invalidatePackageCache() {
        lastPackageCountTimeMs = 0L
    }

    fun createInitialSnapshot(): SystemMetricsSnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalBytes = memInfo.totalMem.coerceAtLeast(1L)
        val availBytes = memInfo.availMem.coerceAtLeast(0L)
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)
        val ramPercent = (usedBytes.toDouble() * 100.0 / totalBytes.toDouble()).coerceIn(0.0, 100.0)
        val bytesPerGb = 1024.0 * 1024.0 * 1024.0

        return SystemMetricsSnapshot(
            cpuUsagePercent = lastCpuPercent,
            ramUsagePercent = ramPercent,
            totalRamGb = totalBytes / bytesPerGb,
            usedRamGb = usedBytes / bytesPerGb,
            availableRamGb = availBytes / bytesPerGb,
            systemCacheGb = 0.0,
            swapUsedGb = 0.0,
            swapTotalGb = 0.0,
            availableRamBytes = availBytes,
            systemCacheBytes = 0L,
            activePackageCount = cachedActivePackageCount,
            threadCount = (logicalCores * 3),
            batteryTempCelsius = 32.0
        )
    }

    suspend fun getMetricsSnapshotAsync(): SystemMetricsSnapshot = withContext(Dispatchers.IO) {
        getMetricsSnapshot()
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun getMetricsSnapshot(): SystemMetricsSnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalBytes = memInfo.totalMem.coerceAtLeast(1L)
        val availBytes = memInfo.availMem.coerceAtLeast(0L)
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)
        val ramPercent = (usedBytes.toDouble() * 100.0 / totalBytes.toDouble()).coerceIn(0.0, 100.0)

        val procMem = readProcMemInfo()
        val cachedBytes = procMem.cachedPlusBuffersBytes
        val swapTotalBytes = procMem.swapTotalBytes
        val swapUsedBytes = (procMem.swapTotalBytes - procMem.swapFreeBytes).coerceAtLeast(0L)

        val cpuPercent = readCpuUsagePercent()
        val batteryTemp = readBatteryTemperatureCelsius()
        val threads = readActiveThreadCount()
        val activeApps = countActiveBackgroundPackages()

        val bytesPerGb = 1024.0 * 1024.0 * 1024.0

        return SystemMetricsSnapshot(
            cpuUsagePercent = cpuPercent.coerceIn(0.0, 100.0),
            ramUsagePercent = ramPercent,
            totalRamGb = totalBytes / bytesPerGb,
            usedRamGb = usedBytes / bytesPerGb,
            availableRamGb = availBytes / bytesPerGb,
            systemCacheGb = cachedBytes / bytesPerGb,
            swapUsedGb = swapUsedBytes / bytesPerGb,
            swapTotalGb = swapTotalBytes / bytesPerGb,
            availableRamBytes = availBytes,
            systemCacheBytes = cachedBytes,
            activePackageCount = activeApps,
            threadCount = threads,
            batteryTempCelsius = batteryTemp
        )
    }

    /**
     * Calcula la carga de CPU leyendo las frecuencias actuales y máximas de cada núcleo
     * en /sys/devices/system/cpu/cpuX/cpufreq/, compatible con Android 8 a Android 15 sin Root.
     */
    private fun readCpuUsagePercent(): Double {
        try {
            var activeRatioSum = 0.0
            var readableCores = 0

            for (core in 0 until logicalCores) {
                val curFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/scaling_cur_freq")
                val maxFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")
                val minFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_min_freq")

                if (curFile.canRead() && maxFile.canRead()) {
                    val curFreq = curFile.readText().trim().toLongOrNull() ?: continue
                    val maxFreq = maxFile.readText().trim().toLongOrNull() ?: continue
                    val minFreq = if (minFile.canRead()) {
                        minFile.readText().trim().toLongOrNull() ?: 0L
                    } else {
                        0L
                    }

                    if (maxFreq > 0L) {
                        val span = (maxFreq - minFreq).coerceAtLeast(1L)
                        val normalized = ((curFreq - minFreq).coerceAtLeast(0L).toDouble() / span.toDouble())
                            .coerceIn(0.05, 1.0)
                        activeRatioSum += normalized
                        readableCores++
                    }
                }
            }

            if (readableCores > 0) {
                val measured = (activeRatioSum / readableCores.toDouble()) * 100.0
                lastCpuPercent = (lastCpuPercent * 0.35 + measured * 0.65).coerceIn(2.0, 100.0)
                return lastCpuPercent
            }
        } catch (_: Exception) {
        }

        // Fallback basado en memoria de proceso y carga del runtime si el dispositivo restringe sysfs
        val rt = Runtime.getRuntime()
        val heapRatio = (rt.totalMemory() - rt.freeMemory()).toDouble() / rt.maxMemory().coerceAtLeast(1L)
        val estimated = (12.0 + heapRatio * 45.0).coerceIn(4.0, 95.0)
        lastCpuPercent = (lastCpuPercent * 0.5 + estimated * 0.5)
        return lastCpuPercent
    }

    private fun readBatteryTemperatureCelsius(): Double {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
            rawTemp / 10.0
        } catch (_: Exception) {
            32.0
        }
    }

    private data class ProcMemData(
        val cachedPlusBuffersBytes: Long,
        val swapTotalBytes: Long,
        val swapFreeBytes: Long
    )

    private fun readProcMemInfo(): ProcMemData {
        var cachedKb = 0L
        var buffersKb = 0L
        var swapTotalKb = 0L
        var swapFreeKb = 0L

        try {
            val file = File("/proc/meminfo")
            if (file.canRead()) {
                file.forEachLine { line ->
                    when {
                        line.startsWith("Cached:") -> cachedKb = extractKbValue(line)
                        line.startsWith("Buffers:") -> buffersKb = extractKbValue(line)
                        line.startsWith("SwapTotal:") -> swapTotalKb = extractKbValue(line)
                        line.startsWith("SwapFree:") -> swapFreeKb = extractKbValue(line)
                    }
                }
            }
        } catch (_: Exception) {
        }

        return ProcMemData(
            cachedPlusBuffersBytes = (cachedKb + buffersKb) * 1024L,
            swapTotalBytes = swapTotalKb * 1024L,
            swapFreeBytes = swapFreeKb * 1024L
        )
    }

    private fun extractKbValue(line: String): Long {
        return line.substringAfter(':')
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.toLongOrNull() ?: 0L
    }

    private fun readCpuModelName(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = Build.SOC_MODEL
            if (!soc.isNullOrBlank() && !soc.equals("unknown", ignoreCase = true)) {
                return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} • $soc"
            }
        }

        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.canRead()) {
                val detected = cpuInfo.useLines { lines ->
                    lines.firstNotNullOfOrNull { line ->
                        if (line.startsWith("Hardware", ignoreCase = true) ||
                            line.startsWith("model name", ignoreCase = true)
                        ) {
                            line.substringAfter(':').trim().takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                    }
                }
                if (detected != null) {
                    return detected
                }
            }
        } catch (_: Exception) {
        }

        return "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (${Build.HARDWARE})"
    }

    private fun readActiveThreadCount(): Int {
        try {
            val statusFile = File("/proc/self/status")
            if (statusFile.canRead()) {
                val parsed = statusFile.useLines { lines ->
                    lines.firstNotNullOfOrNull { line ->
                        if (line.startsWith("Threads:")) {
                            line.substringAfter(':').trim().toIntOrNull()
                        } else {
                            null
                        }
                    }
                }
                if (parsed != null && parsed > 0) {
                    return parsed.coerceAtLeast(logicalCores * 3)
                }
            }
        } catch (_: Exception) {
        }
        return Thread.activeCount().coerceAtLeast(logicalCores * 3)
    }

    private fun countActiveBackgroundPackages(): Int {
        val now = System.currentTimeMillis()
        if ((now - lastPackageCountTimeMs) in 1L..4_000L) {
            return cachedActivePackageCount
        }
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            val count = apps.count { app ->
                (app.flags and ApplicationInfo.FLAG_STOPPED) == 0 &&
                    app.packageName != context.packageName
            }.coerceAtLeast(1)
            cachedActivePackageCount = count
            lastPackageCountTimeMs = now
            count
        } catch (_: Exception) {
            cachedActivePackageCount
        }
    }

    /**
     * Obtiene las aplicaciones activas / en caché con mayor impacto en memoria.
     * Utiliza UsageStatsManager si el permiso está activo, o la lista de paquetes activos no detenidos.
     */
    fun getTopRamProcesses(limit: Int = 12): List<ProcessMemoryItem> {
        val pm = context.packageManager
        val ownPkg = context.packageName
        val results = mutableListOf<ProcessMemoryItem>()

        try {
            val recentPackages = mutableMapOf<String, Long>()
            if (hasUsageStatsPermission()) {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 1000L * 60L * 60L * 12L,
                    now
                )
                stats?.forEach { stat ->
                    if (stat.packageName != ownPkg && stat.lastTimeUsed > 0) {
                        recentPackages[stat.packageName] = max(
                            recentPackages[stat.packageName] ?: 0L,
                            stat.lastTimeUsed
                        )
                    }
                }
            }

            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val activeCandidates = installedApps.filter { app ->
                app.packageName != ownPkg &&
                    (app.flags and ApplicationInfo.FLAG_STOPPED) == 0 &&
                    (recentPackages.containsKey(app.packageName) ||
                        (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0)
            }

            for (app in activeCandidates) {
                val label = try {
                    pm.getApplicationLabel(app).toString()
                } catch (_: Exception) {
                    app.packageName.substringAfterLast('.')
                }

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val apkFile = try {
                    File(app.sourceDir ?: "")
                } catch (_: Exception) {
                    null
                }
                val apkMb = if (apkFile != null && apkFile.exists()) {
                    apkFile.length().toDouble() / (1024.0 * 1024.0)
                } else {
                    42.0
                }

                val recencyBoost = if (recentPackages.containsKey(app.packageName)) 1.35 else 0.85
                val estimatedWorkingSetMb = ((apkMb * 1.4 + 48.0) * recencyBoost).coerceIn(28.0, 680.0)

                results.add(
                    ProcessMemoryItem(
                        packageName = app.packageName,
                        appName = label,
                        estimatedMb = estimatedWorkingSetMb,
                        formattedRam = String.format(Locale.US, "%.1f MB", estimatedWorkingSetMb),
                        isSystemApp = isSystem
                    )
                )
            }
        } catch (_: Exception) {
        }

        return results
            .sortedWith(
                compareByDescending<ProcessMemoryItem> { !it.isSystemApp }
                    .thenByDescending { it.estimatedMb }
            )
            .take(limit)
    }
}

