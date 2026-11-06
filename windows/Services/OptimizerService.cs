using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Security.Principal;
using System.Threading.Tasks;

namespace SylverOptimizer.Services;

public sealed record RamOptimizationResult(
    bool Success,
    int ProcessesTrimmed,
    int ProcessesProtectedOrSkipped,
    double FreedRamMb,
    double CachePurgedMb,
    bool StandbyPurgedKernel,
    string SummaryMessage);

public sealed record DiskCacheScanResult(
    int FileCount,
    double TotalSizeMb,
    string DetailsMessage);

public sealed record DiskCacheCleanResult(
    int FilesDeleted,
    int FilesInUseSkipped,
    double FreedDiskMb,
    bool DnsFlushed,
    string SummaryMessage);

public sealed class OptimizerService
{
    public bool IsRunningAsAdmin { get; }

    public OptimizerService()
    {
        IsRunningAsAdmin = CheckIsAdministrator();
        if (IsRunningAsAdmin)
        {
            EnablePrivilege(NativeMethods.SE_PROFILE_SINGLE_PROCESS_NAME);
            EnablePrivilege(NativeMethods.SE_INCREASE_QUOTA_NAME);
        }
    }

    private static bool CheckIsAdministrator()
    {
        try
        {
            using var identity = WindowsIdentity.GetCurrent();
            var principal = new WindowsPrincipal(identity);
            return principal.IsInRole(WindowsBuiltInRole.Administrator);
        }
        catch
        {
            return false;
        }
    }

    public bool TryRestartAsAdministrator()
    {
        try
        {
            string? exePath = Environment.ProcessPath;
            if (string.IsNullOrWhiteSpace(exePath))
            {
                return false;
            }

            var startInfo = new ProcessStartInfo
            {
                FileName = exePath,
                UseShellExecute = true,
                Verb = "runas"
            };

            Process.Start(startInfo);
            return true;
        }
        catch
        {
            // El usuario canceló el cuadro de diálogo UAC
            return false;
        }
    }

    private static bool EnablePrivilege(string privilegeName)
    {
        IntPtr tokenHandle = IntPtr.Zero;
        try
        {
            if (!NativeMethods.OpenProcessToken(
                    NativeMethods.GetCurrentProcess(),
                    NativeMethods.TOKEN_ADJUST_PRIVILEGES | NativeMethods.TOKEN_QUERY,
                    out tokenHandle))
            {
                return false;
            }

            if (!NativeMethods.LookupPrivilegeValue(null, privilegeName, out var luid))
            {
                return false;
            }

            var tp = new NativeMethods.TOKEN_PRIVILEGES
            {
                PrivilegeCount = 1,
                Luid = luid,
                Attributes = NativeMethods.SE_PRIVILEGE_ENABLED
            };

            return NativeMethods.AdjustTokenPrivileges(
                tokenHandle,
                false,
                ref tp,
                0,
                IntPtr.Zero,
                IntPtr.Zero);
        }
        catch
        {
            return false;
        }
        finally
        {
            if (tokenHandle != IntPtr.Zero)
            {
                NativeMethods.CloseHandle(tokenHandle);
            }
        }
    }

    private static (ulong AvailableBytes, ulong CacheBytes) GetCurrentMemoryState()
    {
        var mem = NativeMethods.MEMORYSTATUSEX.Create();
        NativeMethods.GlobalMemoryStatusEx(ref mem);

        ulong cacheBytes = 0;
        uint perfSize = (uint)System.Runtime.InteropServices.Marshal.SizeOf<NativeMethods.PERFORMANCE_INFORMATION>();
        if (NativeMethods.GetPerformanceInfo(out var perf, perfSize))
        {
            cacheBytes = perf.SystemCache.ToUInt64() * perf.PageSize.ToUInt64();
        }

        return (mem.ullAvailPhys, cacheBytes);
    }

    /// <summary>
    /// Libera la memoria RAM (Working Sets) de todos los procesos accesibles usando EmptyWorkingSet.
    /// </summary>
    public Task<RamOptimizationResult> ReleaseRamWorkingSetsAsync()
    {
        return Task.Run(() =>
        {
            var (availBefore, _) = GetCurrentMemoryState();
            int trimmedCount = 0;
            int skippedCount = 0;
            long measuredProcessReductionBytes = 0;
            int currentPid = Environment.ProcessId;

            Process[] processes = Process.GetProcesses();
            foreach (var proc in processes)
            {
                try
                {
                    if (proc.Id <= 4 || proc.Id == currentPid)
                    {
                        skippedCount++;
                        continue;
                    }

                    long wsBefore = proc.WorkingSet64;
                    bool ok = NativeMethods.EmptyWorkingSet(proc.Handle);
                    if (ok)
                    {
                        trimmedCount++;
                        try
                        {
                            proc.Refresh();
                            long wsAfter = proc.WorkingSet64;
                            if (wsBefore > wsAfter)
                            {
                                measuredProcessReductionBytes += (wsBefore - wsAfter);
                            }
                        }
                        catch
                        {
                            // Ignorar si el proceso terminó durante la medición
                        }
                    }
                    else
                    {
                        skippedCount++;
                    }
                }
                catch
                {
                    skippedCount++;
                }
                finally
                {
                    proc.Dispose();
                }
            }

            // Si tiene permisos de Administrador, ejecutar también el vaciado global del kernel
            if (IsRunningAsAdmin)
            {
                EnablePrivilege(NativeMethods.SE_PROFILE_SINGLE_PROCESS_NAME);
                int cmdEmptyWs = (int)NativeMethods.SYSTEM_MEMORY_LIST_COMMAND.MemoryEmptyWorkingSets;
                NativeMethods.NtSetSystemInformation(
                    NativeMethods.SystemMemoryListInformation,
                    ref cmdEmptyWs,
                    sizeof(int));
            }

            GC.Collect(2, GCCollectionMode.Forced, true, true);
            GC.WaitForPendingFinalizers();

            var (availAfter, _) = GetCurrentMemoryState();
            double netSystemFreedMb = availAfter > availBefore
                ? (availAfter - availBefore) / (1024.0 * 1024.0)
                : 0.0;
            double processTrimmedMb = measuredProcessReductionBytes / (1024.0 * 1024.0);
            double effectiveFreedMb = Math.Max(netSystemFreedMb, processTrimmedMb * 0.75);

            string summary = $"RAM liberada: {effectiveFreedMb:N1} MB ({trimmedCount} procesos optimizados, {skippedCount} protegidos del sistema omitidos).";

            return new RamOptimizationResult(
                Success: true,
                ProcessesTrimmed: trimmedCount,
                ProcessesProtectedOrSkipped: skippedCount,
                FreedRamMb: effectiveFreedMb,
                CachePurgedMb: 0,
                StandbyPurgedKernel: false,
                SummaryMessage: summary);
        });
    }

    /// <summary>
    /// Purga la caché de memoria RAM en espera (Standby List y Modified Page List) mediante NtSetSystemInformation.
    /// </summary>
    public Task<RamOptimizationResult> PurgeRamStandbyCacheAsync()
    {
        return Task.Run(() =>
        {
            var (availBefore, cacheBefore) = GetCurrentMemoryState();

            EnablePrivilege(NativeMethods.SE_PROFILE_SINGLE_PROCESS_NAME);
            EnablePrivilege(NativeMethods.SE_INCREASE_QUOTA_NAME);

            int cmdFlushModified = (int)NativeMethods.SYSTEM_MEMORY_LIST_COMMAND.MemoryFlushModifiedList;
            NativeMethods.NtSetSystemInformation(
                NativeMethods.SystemMemoryListInformation,
                ref cmdFlushModified,
                sizeof(int));

            int cmdPurgeLowStandby = (int)NativeMethods.SYSTEM_MEMORY_LIST_COMMAND.MemoryPurgeLowPriorityStandbyList;
            NativeMethods.NtSetSystemInformation(
                NativeMethods.SystemMemoryListInformation,
                ref cmdPurgeLowStandby,
                sizeof(int));

            int cmdPurgeStandby = (int)NativeMethods.SYSTEM_MEMORY_LIST_COMMAND.MemoryPurgeStandbyList;
            uint status = NativeMethods.NtSetSystemInformation(
                NativeMethods.SystemMemoryListInformation,
                ref cmdPurgeStandby,
                sizeof(int));

            bool kernelSuccess = status == 0;

            var (availAfter, cacheAfter) = GetCurrentMemoryState();

            double cachePurgedMb = cacheBefore > cacheAfter
                ? (cacheBefore - cacheAfter) / (1024.0 * 1024.0)
                : 0.0;
            double availGainMb = availAfter > availBefore
                ? (availAfter - availBefore) / (1024.0 * 1024.0)
                : 0.0;
            double reportedMb = Math.Max(cachePurgedMb, availGainMb);

            if (!kernelSuccess)
            {
                return new RamOptimizationResult(
                    Success: false,
                    ProcessesTrimmed: 0,
                    ProcessesProtectedOrSkipped: 0,
                    FreedRamMb: 0,
                    CachePurgedMb: 0,
                    StandbyPurgedKernel: false,
                    SummaryMessage: "Para vaciar la Standby List del kernel de Windows 11 se requieren permisos de Administrador. Haz clic en 'Elevar a Administrador' arriba a la derecha.");
            }

            string msg = reportedMb > 1.0
                ? $"Caché de RAM (Standby List) vaciada: {reportedMb:N1} MB purgados correctamente."
                : "Standby List y páginas modificadas de RAM purgadas correctamente (la caché ya estaba optimizada).";

            return new RamOptimizationResult(
                Success: true,
                ProcessesTrimmed: 0,
                ProcessesProtectedOrSkipped: 0,
                FreedRamMb: availGainMb,
                CachePurgedMb: reportedMb,
                StandbyPurgedKernel: true,
                SummaryMessage: msg);
        });
    }

    /// <summary>
    /// Libera el Working Set de un proceso específico por PID.
    /// </summary>
    public (bool Success, double FreedMb, string Message) TrimSingleProcess(int pid, string processName)
    {
        try
        {
            using var proc = Process.GetProcessById(pid);
            long before = proc.WorkingSet64;
            bool ok = NativeMethods.EmptyWorkingSet(proc.Handle);
            if (!ok)
            {
                return (false, 0, $"El proceso '{processName}' (PID {pid}) está protegido por el sistema.");
            }

            proc.Refresh();
            long after = proc.WorkingSet64;
            double freedMb = before > after ? (before - after) / (1024.0 * 1024.0) : 0.0;
            return (true, freedMb, $"Proceso '{processName}' (PID {pid}) optimizado: {freedMb:N1} MB liberados.");
        }
        catch (Exception ex)
        {
            return (false, 0, $"No se pudo optimizar '{processName}' (PID {pid}): {ex.Message}");
        }
    }

    private static List<string> GetSystemCacheDirectories()
    {
        var dirs = new List<string>();

        string userTemp = Path.GetTempPath();
        if (Directory.Exists(userTemp))
        {
            dirs.Add(userTemp);
        }

        string winDir = Environment.GetFolderPath(Environment.SpecialFolder.Windows);
        if (!string.IsNullOrWhiteSpace(winDir))
        {
            string winTemp = Path.Combine(winDir, "Temp");
            if (Directory.Exists(winTemp))
            {
                dirs.Add(winTemp);
            }

            string prefetch = Path.Combine(winDir, "Prefetch");
            if (Directory.Exists(prefetch))
            {
                dirs.Add(prefetch);
            }
        }

        return dirs;
    }

    /// <summary>
    /// Escanea archivos temporales y caché de sistema (%TEMP%, Windows\Temp, Prefetch, Thumbcache).
    /// </summary>
    public Task<DiskCacheScanResult> ScanSystemCacheAsync()
    {
        return Task.Run(() =>
        {
            int count = 0;
            long totalBytes = 0;

            foreach (string dir in GetSystemCacheDirectories())
            {
                ScanDirectorySafe(dir, ref count, ref totalBytes);
            }

            // Caché de miniaturas de Windows Explorer
            string explorerCache = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                @"Microsoft\Windows\Explorer");
            if (Directory.Exists(explorerCache))
            {
                try
                {
                    foreach (var file in Directory.EnumerateFiles(explorerCache, "thumbcache_*.db"))
                    {
                        try
                        {
                            var fi = new FileInfo(file);
                            totalBytes += fi.Length;
                            count++;
                        }
                        catch
                        {
                        }
                    }
                }
                catch
                {
                }
            }

            double mb = totalBytes / (1024.0 * 1024.0);
            return new DiskCacheScanResult(
                FileCount: count,
                TotalSizeMb: mb,
                DetailsMessage: $"Caché detectada: {mb:N1} MB en {count:N0} archivos temporales/prefetch.");
        });
    }

    private static void ScanDirectorySafe(string path, ref int count, ref long totalBytes)
    {
        try
        {
            foreach (string file in Directory.EnumerateFiles(path))
            {
                try
                {
                    var fi = new FileInfo(file);
                    totalBytes += fi.Length;
                    count++;
                }
                catch
                {
                }
            }

            foreach (string subDir in Directory.EnumerateDirectories(path))
            {
                ScanDirectorySafe(subDir, ref count, ref totalBytes);
            }
        }
        catch
        {
        }
    }

    /// <summary>
    /// Limpia archivos temporales huérfanos (%TEMP%, Windows\Temp, Prefetch), caché de miniaturas inactivas y vacía la caché DNS.
    /// Los archivos en uso por Windows o aplicaciones abiertas son bloqueados por NTFS y se omiten de forma segura.
    /// </summary>
    public Task<DiskCacheCleanResult> CleanSystemCacheAsync(bool flushDns = true)
    {
        return Task.Run(() =>
        {
            int deleted = 0;
            int skipped = 0;
            long freedBytes = 0;

            foreach (string dir in GetSystemCacheDirectories())
            {
                CleanDirectorySafe(dir, deleteRootDir: false, ref deleted, ref skipped, ref freedBytes);
            }

            string explorerCache = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                @"Microsoft\Windows\Explorer");
            if (Directory.Exists(explorerCache))
            {
                try
                {
                    foreach (var file in Directory.EnumerateFiles(explorerCache, "thumbcache_*.db"))
                    {
                        try
                        {
                            var fi = new FileInfo(file);
                            long len = fi.Length;
                            File.Delete(file);
                            freedBytes += len;
                            deleted++;
                        }
                        catch
                        {
                            skipped++;
                        }
                    }
                }
                catch
                {
                }
            }

            bool dnsFlushed = flushDns && FlushDnsCache();
            double freedMb = freedBytes / (1024.0 * 1024.0);

            string dnsText = dnsFlushed ? " + Caché DNS vaciada" : "";
            string msg = $"Caché del sistema limpiada: {freedMb:N1} MB eliminados ({deleted:N0} archivos borrados, {skipped:N0} en uso omitidos{dnsText}).";

            return new DiskCacheCleanResult(
                FilesDeleted: deleted,
                FilesInUseSkipped: skipped,
                FreedDiskMb: freedMb,
                DnsFlushed: dnsFlushed,
                SummaryMessage: msg);
        });
    }

    private static void CleanDirectorySafe(
        string path,
        bool deleteRootDir,
        ref int deleted,
        ref int skipped,
        ref long freedBytes)
    {
        try
        {
            foreach (string file in Directory.EnumerateFiles(path))
            {
                try
                {
                    var fi = new FileInfo(file);
                    long len = fi.Length;
                    fi.IsReadOnly = false;
                    File.Delete(file);
                    freedBytes += len;
                    deleted++;
                }
                catch
                {
                    // Archivo actualmente en uso por un proceso activo: se omite sin riesgo
                    skipped++;
                }
            }

            foreach (string subDir in Directory.EnumerateDirectories(path))
            {
                CleanDirectorySafe(subDir, deleteRootDir: true, ref deleted, ref skipped, ref freedBytes);
            }

            if (deleteRootDir)
            {
                try
                {
                    Directory.Delete(path, recursive: false);
                }
                catch
                {
                }
            }
        }
        catch
        {
        }
    }

    private static bool FlushDnsCache()
    {
        try
        {
            var psi = new ProcessStartInfo
            {
                FileName = "ipconfig.exe",
                Arguments = "/flushdns",
                CreateNoWindow = true,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            };

            using var proc = Process.Start(psi);
            if (proc == null)
            {
                return false;
            }

            proc.WaitForExit(4000);
            return proc.ExitCode == 0;
        }
        catch
        {
            return false;
        }
    }
}
