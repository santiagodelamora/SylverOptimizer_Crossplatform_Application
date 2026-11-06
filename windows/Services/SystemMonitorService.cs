using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using Microsoft.Win32;

namespace SylverOptimizer.Services;

public sealed record SystemMetricsSnapshot(
    double CpuUsagePercent,
    double RamUsagePercent,
    double TotalRamGb,
    double UsedRamGb,
    double AvailableRamGb,
    double SystemCacheGb,
    double CommitTotalGb,
    double CommitLimitGb,
    ulong AvailableRamBytes,
    ulong SystemCacheBytes,
    uint ProcessCount,
    uint ThreadCount,
    uint HandleCount);

public sealed record ProcessMemoryItem(
    int Pid,
    string Name,
    double WorkingSetMb,
    string FormattedRam);

public sealed class SystemMonitorService
{
    private ulong _prevIdleTime;
    private ulong _prevKernelTime;
    private ulong _prevUserTime;
    private double _lastCpuPercent;

    public string CpuModelName { get; }
    public int LogicalCores { get; }

    public SystemMonitorService()
    {
        LogicalCores = Environment.ProcessorCount;
        CpuModelName = ReadCpuModelName();
        InitializeCpuTimes();
    }

    private static string ReadCpuModelName()
    {
        try
        {
            using var key = Registry.LocalMachine.OpenSubKey(@"HARDWARE\DESCRIPTION\System\CentralProcessor\0");
            var name = key?.GetValue("ProcessorNameString")?.ToString()?.Trim();
            if (!string.IsNullOrWhiteSpace(name))
            {
                return name;
            }
        }
        catch
        {
            // Fallback silencioso si el registro no es accesible
        }

        return $"Procesador Windows ({Environment.ProcessorCount} núcleos lógicos)";
    }

    private void InitializeCpuTimes()
    {
        if (NativeMethods.GetSystemTimes(out var idle, out var kernel, out var user))
        {
            _prevIdleTime = idle.ToUInt64();
            _prevKernelTime = kernel.ToUInt64();
            _prevUserTime = user.ToUInt64();
        }
    }

    public SystemMetricsSnapshot GetMetricsSnapshot()
    {
        double cpuPercent = ReadCpuUsagePercent();

        var memStatus = NativeMethods.MEMORYSTATUSEX.Create();
        NativeMethods.GlobalMemoryStatusEx(ref memStatus);

        ulong totalPhys = memStatus.ullTotalPhys;
        ulong availPhys = memStatus.ullAvailPhys;
        ulong usedPhys = totalPhys > availPhys ? totalPhys - availPhys : 0;

        double ramPercent = totalPhys > 0
            ? (double)usedPhys * 100.0 / totalPhys
            : memStatus.dwMemoryLoad;

        ulong cacheBytes = 0;
        ulong commitTotalBytes = 0;
        ulong commitLimitBytes = 0;
        uint processCount = 0;
        uint threadCount = 0;
        uint handleCount = 0;

        uint perfSize = (uint)Marshal.SizeOf<NativeMethods.PERFORMANCE_INFORMATION>();
        if (NativeMethods.GetPerformanceInfo(out var perfInfo, perfSize))
        {
            ulong pageSize = perfInfo.PageSize.ToUInt64();
            cacheBytes = perfInfo.SystemCache.ToUInt64() * pageSize;
            commitTotalBytes = perfInfo.CommitTotal.ToUInt64() * pageSize;
            commitLimitBytes = perfInfo.CommitLimit.ToUInt64() * pageSize;
            processCount = perfInfo.ProcessCount;
            threadCount = perfInfo.ThreadCount;
            handleCount = perfInfo.HandleCount;
        }

        const double bytesPerGb = 1024.0 * 1024.0 * 1024.0;

        return new SystemMetricsSnapshot(
            CpuUsagePercent: Math.Clamp(cpuPercent, 0.0, 100.0),
            RamUsagePercent: Math.Clamp(ramPercent, 0.0, 100.0),
            TotalRamGb: totalPhys / bytesPerGb,
            UsedRamGb: usedPhys / bytesPerGb,
            AvailableRamGb: availPhys / bytesPerGb,
            SystemCacheGb: cacheBytes / bytesPerGb,
            CommitTotalGb: commitTotalBytes / bytesPerGb,
            CommitLimitGb: commitLimitBytes / bytesPerGb,
            AvailableRamBytes: availPhys,
            SystemCacheBytes: cacheBytes,
            ProcessCount: processCount,
            ThreadCount: threadCount,
            HandleCount: handleCount);
    }

    private double ReadCpuUsagePercent()
    {
        if (!NativeMethods.GetSystemTimes(out var idle, out var kernel, out var user))
        {
            return _lastCpuPercent;
        }

        ulong idleNow = idle.ToUInt64();
        ulong kernelNow = kernel.ToUInt64();
        ulong userNow = user.ToUInt64();

        ulong idleDelta = idleNow - _prevIdleTime;
        ulong kernelDelta = kernelNow - _prevKernelTime;
        ulong userDelta = userNow - _prevUserTime;

        _prevIdleTime = idleNow;
        _prevKernelTime = kernelNow;
        _prevUserTime = userNow;

        // En GetSystemTimes, kernelTime incluye el tiempo Idle.
        ulong totalSystem = kernelDelta + userDelta;
        if (totalSystem == 0)
        {
            return _lastCpuPercent;
        }

        ulong activeTime = totalSystem > idleDelta ? totalSystem - idleDelta : 0;
        _lastCpuPercent = (double)activeTime * 100.0 / totalSystem;
        return _lastCpuPercent;
    }

    public List<ProcessMemoryItem> GetTopRamProcesses(int count = 12)
    {
        var list = new List<ProcessMemoryItem>(64);
        int currentPid = Environment.ProcessId;

        Process[] processes;
        try
        {
            processes = Process.GetProcesses();
        }
        catch
        {
            return list;
        }

        foreach (var proc in processes)
        {
            try
            {
                if (proc.Id <= 4 || proc.Id == currentPid)
                {
                    continue;
                }

                long ws = proc.WorkingSet64;
                if (ws <= 0)
                {
                    continue;
                }

                double mb = ws / (1024.0 * 1024.0);
                list.Add(new ProcessMemoryItem(
                    Pid: proc.Id,
                    Name: proc.ProcessName,
                    WorkingSetMb: mb,
                    FormattedRam: $"{mb:N1} MB"));
            }
            catch
            {
                // Ignorar procesos protegidos que no permiten lectura
            }
            finally
            {
                proc.Dispose();
            }
        }

        return list
            .OrderByDescending(p => p.WorkingSetMb)
            .Take(count)
            .ToList();
    }
}
