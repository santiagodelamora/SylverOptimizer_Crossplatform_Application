# ⚡ WinOptimizer Pro — Windows 11 RAM, Cache & CPU Optimizer

> 🌐 **Language / Idioma:** [🇪🇸 Español](./README.md) | **🇺🇸 English**

A native dark-mode desktop application built with **C# and WPF (.NET 10)** for **Windows 11**, designed to monitor hardware in real time, free up inactive RAM, and clean system caches using official Windows APIs (`Win32` and `NT Native API`).

## 📦 Direct Installation (Windows 10 / 11 x64)

You can install the application directly using the self-contained installer included in the repository (no separate .NET runtime installation required):

- 👉 **[`Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe`](./Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe)**

## ✨ Key Features

- **Zero-Overhead Real-Time Telemetry:**
  - Dynamic circular gauges and 60-second area history charts for **Physical RAM** and **CPU**.
  - Detailed memory breakdown: *In Use*, *Available*, *Cached / Standby List*, and *Committed (Pagefile)*.
  - Low-level CPU metrics via `GetSystemTimes` (`kernel32.dll`): processor model name, logical cores, active processes, threads, and open handles.
- **Safe RAM Working Set Release (`EmptyWorkingSet`):**
  - Trims inactive pages from the *Working Set* of all accessible processes without affecting kernel-protected system processes.
  - Supports 1-click global optimization or per-process memory trimming directly from the **Top Processes** table.
- **Kernel RAM Cache / Standby List Purge (`NtSetSystemInformation`):**
  - Flushes the *Modified Page List* and purges the *Standby List* on Windows 11 (automatically enabling `SeProfileSingleProcessPrivilege` when running as Administrator).
- **System & Network Cache Cleaner:**
  - Scans and removes orphaned temporary files across `%TEMP%`, `C:\Windows\Temp`, `C:\Windows\Prefetch`, and inactive Explorer thumbnail caches (`thumbcache_*.db`).
  - Flushes the Windows DNS resolver cache (`ipconfig /flushdns`).
- **Smart Auto-Optimization:**
  - Optional background toggle that automatically trims inactive RAM working sets when memory usage exceeds **85%**.

## 🛠️ Building from Source

```powershell
# Build in Release mode
dotnet build --configuration Release

# Run the application
dotnet run --configuration Release
```

- Quick-launch script: [`Ejecutar_WinOptimizerPro.bat`](./Ejecutar_WinOptimizerPro.bat)
- Rebuild the **Inno Setup 6** installer: [`Crear_Instalador.bat`](./Crear_Instalador.bat)

## 📂 Project Structure

- `Assets/app.ico` — Custom application and installer icon.
- `Installer/WinOptimizerPro.iss` — Inno Setup 6 installer script.
- `Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe` — Self-contained Windows x64 Setup executable.
- `Services/NativeMethods.cs` — P/Invoke declarations (`psapi.dll`, `kernel32.dll`, `ntdll.dll`, `advapi32.dll`).
- `Services/SystemMonitorService.cs` — Real-time CPU, RAM, cache, and top process telemetry service.
- `Services/OptimizerService.cs` — RAM working set trimmer, Standby List cleaner, and disk/DNS cache cleaner.
- `MainWindow.xaml` / `MainWindow.xaml.cs` — Dark-themed WPF GUI with real-time gauges and charts.
