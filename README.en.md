# ⚡ SylverOptimizer — Windows 11 RAM, Cache & CPU Optimizer

> 🌐 **Language / Idioma:** [🇪🇸 Español](./README.md) | **🇺🇸 English**  
> 📖 **Documentation:** **[Detailed User Manual](./USER_MANUAL.en.md)** | **[Contribution Guide](./CONTRIBUTING.md)**

A native dark-mode desktop application built with **C# and WPF (.NET 10)** for **Windows 11**, designed to monitor hardware in real time, free up inactive RAM, and clean system caches using official Windows APIs (`Win32` and `NT Native API`).

## 📦 Direct Installation (Windows 10 / 11 x64)

You can download and install the application directly from the official **GitHub Releases** page (no separate .NET runtime installation required, as it is self-contained):

- 👉 **[Download `SylverOptimizer_Setup_v1.0.0.exe` (Latest Release)](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases/latest/download/SylverOptimizer_Setup_v1.0.0.exe)**
- 📋 **[View all versions on GitHub Releases](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases)**

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
  - Optional background toggle that automatically trims inactive RAM working sets when memory usage exceeds a configurable threshold (**65% – 95%**, **85%** by default).
- **Settings Section & Customizable Color Themes:**
  - Real-time visual theme switcher featuring **8 color palettes**: *Emerald Cyber*, *Arctic Ocean*, *Amethyst Neon*, *Crimson Gaming*, *Solar Amber*, *Crystal Light Mode*, *Dark Gray*, and *Light Gray*.
  - Persistent preferences for telemetry refresh rate (`0.5s`, `1.0s`, `2.0s`), DNS flushing, and Standby List purging automatically saved in `%LocalAppData%\SylverOptimizer\settings.json`.

## 🛠️ Building from Source

```powershell
# Build in Release mode
dotnet build --configuration Release

# Run the application
dotnet run --configuration Release
```

- Quick-launch script: [`Ejecutar_SylverOptimizer.bat`](./Ejecutar_SylverOptimizer.bat)
- Rebuild the **Inno Setup 6** installer: [`Crear_Instalador.bat`](./Crear_Instalador.bat)

## 📂 Project Structure

- `Assets/app.ico` — Custom application and installer icon.
- `Installer/SylverOptimizer.iss` — Inno Setup 6 installer script (distributed via [GitHub Releases](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases)).
- `Services/NativeMethods.cs` — P/Invoke declarations (`psapi.dll`, `kernel32.dll`, `ntdll.dll`, `advapi32.dll`).
- `Services/SystemMonitorService.cs` — Real-time CPU, RAM, cache, and top process telemetry service.
- `Services/OptimizerService.cs` — RAM working set trimmer, Standby List cleaner, and disk/DNS cache cleaner.
- `Services/AppSettingsService.cs` — Dynamic color theme engine and JSON configuration persistence service.
- `MainWindow.xaml` / `MainWindow.xaml.cs` — Themeable WPF GUI with real-time gauges, charts, and settings panel.
- [`LICENSE`](./LICENSE) — **GNU General Public License v3.0 (GPLv3)** free software license.

## 📄 License

This project is free software distributed under the terms of the **[GNU General Public License v3.0 (GPL-3.0)](./LICENSE)**.  
Copyright © 2026 **Santiago De la Mora**.
