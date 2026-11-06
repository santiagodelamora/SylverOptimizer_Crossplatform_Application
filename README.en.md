# ⚡ SylverOptimizer — RAM, Cache & CPU Optimizer for Windows 11 & Android

> 🌐 **Language / Idioma:** [🇪🇸 Español](./README.md) | **🇺🇸 English**  
> 📖 **Documentation:** **[Detailed User Manual](./USER_MANUAL.en.md)** | **[Contribution Guide](./CONTRIBUTING.md)**

A real-time hardware telemetry and optimization suite featuring **100% native applications** for **Windows 10/11 (`C# / WPF / .NET 10`)** and **Android (`Kotlin / Jetpack Compose`)**, designed to monitor physical RAM and CPU load, trim inactive background processes, and clean system caches using official OS APIs.

## 📦 Direct Installation (Windows x64 & Android APK)

You can download and install both versions directly from the official **GitHub Releases** page:

- 🖥️ **Windows 10 / 11 (x64):** **[Download `SylverOptimizer_Setup_v1.0.0.exe` (Self-Contained Installer)](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases/latest/download/SylverOptimizer_Setup_v1.0.0.exe)**
- 📱 **Android 8.0+ (Native APK):** **[Download `SylverOptimizer_Android_v1.0.0.apk`](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases/latest/download/SylverOptimizer_Android_v1.0.0.apk)**
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

### 🖥️ Desktop Version (`windows/` — Windows / .NET 10)
```powershell
cd windows

# Build in Release mode
dotnet build SylverOptimizer.csproj --configuration Release

# Run the application
dotnet run --configuration Release
```
- Quick-launch script: [`windows/Ejecutar_SylverOptimizer.bat`](./windows/Ejecutar_SylverOptimizer.bat)
- Rebuild the **Inno Setup 6** installer: [`windows/Crear_Instalador.bat`](./windows/Crear_Instalador.bat)

### 📱 Native Mobile Version (`android/` — Android / Kotlin + Jetpack Compose)
```powershell
cd android
gradle assembleRelease
```
- The Android project resides in [`android/`](./android) and can be opened directly in **Android Studio** (JDK 17+, Android SDK 35).

## 📂 Project Structure

- `windows/` — Native Windows 10/11 desktop application (**C# / WPF / .NET 10**):
  - `windows/Assets/app.ico` — Custom Windows application and installer icon.
  - `windows/Installer/SylverOptimizer.iss` — Inno Setup 6 installer script (distributed via [GitHub Releases](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases)).
  - `windows/Services/` — Native Windows services (`NativeMethods.cs`, `SystemMonitorService.cs`, `OptimizerService.cs`, `AppSettingsService.cs`).
  - `windows/MainWindow.xaml` / `windows/MainWindow.xaml.cs` — Themeable WPF GUI for Windows 10/11.
- `android/` — Native Android application written in **Kotlin + Jetpack Compose**:
  - `android/app/src/main/java/com/sylveroptimizer/android/services/` — Android telemetry, optimization, and theme services (`SystemMonitorService.kt`, `OptimizerService.kt`, `AppSettingsService.kt`).
  - `android/app/src/main/java/com/sylveroptimizer/android/ui/` — Jetpack Compose dashboard with circular gauges, 60s history charts, and 8 color themes (`SylverOptimizerScreen.kt`, `TelemetryWidgets.kt`).
- [`LICENSE`](./LICENSE) — **GNU General Public License v3.0 (GPLv3)** free software license.

## 📄 License

This project is free software distributed under the terms of the **[GNU General Public License v3.0 (GPL-3.0)](./LICENSE)**.  
Copyright © 2026 **Santiago De la Mora**.
