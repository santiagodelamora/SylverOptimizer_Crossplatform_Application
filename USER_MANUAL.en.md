# 📖 User Manual — SylverOptimizer (Windows & Android)

> 🌐 **Language / Idioma:** [🇪🇸 Español](./MANUAL_DE_USUARIO.md) | **🇺🇸 English**

Welcome to the official user manual for **SylverOptimizer**, a native real-time hardware monitoring, RAM optimization, and system cache cleaning suite built for **Windows 10 / 11 (x64)** and **Android 8.0+**.

---

## 📑 Table of Contents

1. [Introduction & System Safety](#1-introduction--system-safety)
2. [Installation & Execution Modes (Windows & Android)](#2-installation--execution-modes-windows--android)
3. [Global Header Bar & Navigation](#3-global-header-bar--navigation)
4. [System Health Banner & 1-Click Optimization](#4-system-health-banner--1-click-optimization)
5. [Physical RAM & Cache Module](#5-physical-ram--cache-module)
6. [Processor (CPU / SoC) & System Cache Module](#6-processor-cpu--soc--system-cache-module)
7. [Top RAM-Consuming Processes / Apps Panel](#7-top-ram-consuming-processes--apps-panel)
8. [Optimization & Activity Log](#8-optimization--activity-log)
9. [Settings Section & Color Themes](#9-settings-section--color-themes)
10. [Frequently Asked Questions & Usage Tips (FAQ)](#10-frequently-asked-questions--usage-tips-faq)

---

## 1. Introduction & System Safety

**SylverOptimizer** lets you monitor your PC or mobile device's memory and processor load in real time, reclaim inactive RAM, and recover storage space occupied by orphaned temporary files.

### How does it work safely without risking system stability?
Unlike legacy memory boosters that artificially exhausted memory to force paging, **SylverOptimizer** relies exclusively on official native OS APIs:

- **🖥️ On Windows (`Win32` & `NT Native API`):**
  - **`EmptyWorkingSet` (`psapi.dll`)**: Instructs Windows to trim only inactive memory pages from process working sets without closing applications or losing unsaved work.
  - **`NtSetSystemInformation` (`ntdll.dll`)**: Natively flushes the Windows memory manager's *Modified Page List* and purges the *Standby List*.
  - **NTFS Lock Compliance**: During disk cleanup, any temporary file actively locked by Windows or open programs is safely skipped automatically.

- **📱 On Android (`ActivityManager`, `UsageStatsManager` & `/proc`):**
  - **`ActivityManager.killBackgroundProcesses`**: Safely stops inactive background processes and cached services without harming system stability or deleting personal app data.
  - **Native Linux Telemetry (`/proc/meminfo` & `sysfs`)**: Directly reads Android kernel memory metrics (*MemAvailable*, *Cached*, *Buffers*, *Swap / ZRAM*) and real-time per-core CPU scaling frequencies without requiring *Root* access.

---

## 2. Installation & Execution Modes (Windows & Android)

### 🖥️ On Windows 10 / 11 (x64)
1. **Windows Installer**: Download `SylverOptimizer_Setup_v1.1.0.exe` from *GitHub Releases* (or `windows/Installer/Output/`) to install the app with Start Menu and Desktop shortcuts.
2. **Portable Mode (No installation required)**: Run `windows/publish/SylverOptimizer.exe` directly (bundles the `.NET 10` runtime).
3. **Quick Launch Script**: Double-click `windows/Ejecutar_SylverOptimizer.bat`.

| Feature on Windows | Standard User | Administrator (`🛡️`) |
| :--- | :---: | :---: |
| Real-time monitoring of RAM, CPU, threads, and processes | ✅ Yes | ✅ Yes |
| User process RAM release (`Working Sets`) | ✅ Yes | ✅ Yes |
| User temporary folder (`%TEMP%`) & thumbnail cache cleanup | ✅ Yes | ✅ Yes |
| DNS network resolver cache flush (`ipconfig /flushdns`) | ✅ Yes | ✅ Yes |
| **Deep Kernel RAM Cache Purge (`Standby List`)** | ❌ Requires Admin | ✅ **Enabled** |
| **Cleanup of `C:\Windows\Temp` and `C:\Windows\Prefetch`** | ⚠️ Partial | ✅ **Enabled** |

### 📱 On Android 8.0 or newer
1. **Direct APK Installation**: Download `SylverOptimizer_Android_v1.1.0.apk` from *GitHub Releases* onto your Android phone or tablet and tap it to install (allow installation from your browser/file manager if prompted).
2. **From Android Studio**: Open the `android/` directory in Android Studio and press **Run (`▶`)** on a connected device or emulator.

| Feature on Android | Standard Mode | With Usage Access (`🛡️`) |
| :--- | :---: | :---: |
| Real-time monitoring of RAM, Kernel Cache, and Swap / ZRAM | ✅ Yes | ✅ Yes |
| Real-time CPU/SoC core frequencies & battery temperature | ✅ Yes | ✅ Yes |
| Global background RAM release & temporary cache cleanup | ✅ Yes | ✅ Yes |
| **Accurate detection of recent active background apps** | ⚠️ Basic | ✅ **Full (`UsageStatsManager`)** |

> **💡 Tip:** On Android, you can grant optional usage permission at any time by tapping **`🛡️ Acceso a Uso`** in the top-right corner, which opens the official system settings screen.

---

## 3. Global Header Bar & Navigation

At the top of the screen (on both Windows and Android) you will find the global controls:

1. **Application Title**: Displays the gradient `⚡` icon and **SylverOptimizer** branding.
2. **View Switcher (`📊 Panel Principal` | `⚙️ Configuración`)**: Instantly toggles between the main monitoring/optimization dashboard and the customization/settings screen.
3. **Quick Toggle `Auto-liberar RAM (>85%)` / `Auto-RAM (>85%)`**: Enables or disables automatic RAM optimization with a single click/tap whenever memory usage exceeds your configured threshold.
4. **Privilege / Permission Status Button**:
   - **On Windows:** Displays **`🛡️ Elevar a Administrador`** (prompts Windows UAC confirmation to enable kernel *Standby List* purging) or **`⚡ Modo Administrador Activo`**.
   - **On Android:** Displays **`🛡️ Acceso a Uso`** (opens `PACKAGE_USAGE_STATS` settings for deeper background app visibility) or **`⚡ Uso Activo`**.

---

## 4. System Health Banner & 1-Click Optimization

Right below the header bar sits the primary diagnostic and quick-action banner:

### System Health Indicator
A status dot and dynamic title evaluate your combined hardware load on every cycle:
- 🟢 **Optimal Performance (*Rendimiento Óptimo*)**: RAM usage below `68%` and CPU usage below `70%`.
- 🟡 **Moderate Load (*Carga Moderada*)**: RAM usage between `68%` and `84%`, or CPU usage between `70%` and `89%`.
- 🔴 **High Load (*Carga Elevada*)**: RAM usage at or above `85%`, or CPU usage at or above `90%`.

### Cumulative Session Counters
- **`RAM LIBERADA (SESIÓN)`**: Total `MB` or `GB` of physical RAM and standby cache reclaimed since opening the application.
- **`CACHÉ LIMPIADA (SESIÓN)`**: Total storage space (`MB` / `GB`) recovered by deleting temporary files during the current session.

### `🚀 OPTIMIZAR TODO (1-CLIC)` Button
The all-in-one maintenance button. Clicking or tapping it executes the following steps asynchronously in sequence:
1. **RAM Release** (*Working Set* trimming on Windows / background process cleanup on Android).
2. **Standby Memory Cache Purge** (kernel *Standby List* purge on Windows Admin / cached standby app sweep on Android).
3. **Temporary Cache Cleanup** (`%TEMP%`, `Prefetch`, and DNS flush on Windows / temporary cache folders and `.tmp`/`.log` residuals on Android).
4. **Automatic Refresh** of all telemetry gauges, top processes/apps, and session counters.

---

## 5. Physical RAM & Cache Module

Provides detailed memory telemetry and dedicated RAM controls:

### Visual Elements
- **Header**: Displays total installed physical RAM (e.g., `16.0 GB Totales` or `8.0 GB Totales`).
- **Circular RAM Gauge**: Shows the exact percentage of physical memory in use and the `Used GB / Total GB` ratio, shifting color automatically to amber or red under heavy pressure.
- **60-Second RAM History Chart**: Real-time area graph plotting memory usage over the last minute.

### Memory Breakdown Grid
1. **In Use (*En Uso*)**: Physical RAM currently occupied by the OS and active applications.
2. **Available (*Disponible*)**: Physical RAM immediately ready for allocation.
3. **Cache / Standby** *(Windows)* | **Kernel Cache (*Caché Kernel*)** *(Android)*: Memory held by the OS for file caching, standby lists, and kernel buffers (`Cached + Buffers` in `/proc/meminfo` on Android).
4. **Commit (*Commit (Pag.)*)** *(Windows)* | **Swap / ZRAM** *(Android)*: Virtual memory committed in Windows (`pagefile.sys`) or compressed swap memory (*ZRAM*) on Android.

### Memory Action Buttons
- **`🧹 Liberar RAM (Working Sets / 2º Plano)`**: Trims inactive pages from open processes on Windows or stops secondary background processes on Android.
- **`⚡ Purgar Caché RAM`**: Purges the Windows 11 kernel *Standby List* (requires Administrator) or sweeps cached standby packages on Android.

---

## 6. Processor (CPU / SoC) & System Cache Module

Pairs real-time CPU telemetry with storage cache tools:

### Visual Elements
- **Header & Info Button (`ⓘ`)**: Tapping **`ⓘ`** opens a dialog displaying your full processor or SoC model name (Intel/AMD on Windows; Snapdragon/MediaTek/Tensor/Exynos on Android), logical core count, architecture (`x64` / `arm64-v8a`), and—on Android—current battery/device temperature.
- **Circular CPU Gauge**: Real-time CPU utilization percentage and logical core count.
- **60-Second CPU History Chart**: Plots processor utilization over the last 60 seconds.

### Details Grid
- **On Windows**: **Processes**, **Threads**, **Handles**, and **Temp/Prefetch Cache**.
- **On Android**: **Active Apps (*Apps Activas*)**, **Threads (*Hilos*)**, **Battery Temp (`°C`)**, and **Temp Cache (*Caché Temp.*)**.

### System Cache Buttons
- **`🔍 Analizar Caché`**: Scans temporary and residual folders to calculate file count and size **without deleting anything**.
- **`🗑️ Limpiar Caché...`**:
  - **On Windows**: Deletes orphaned temporary files in `%TEMP%`, `Windows\Temp`, `Prefetch`, inactive thumbnails, and flushes DNS (`ipconfig /flushdns`).
  - **On Android**: Cleans accessible temporary cache directories and sweeps residual `.tmp` and `.log` files.

---

## 7. Top RAM-Consuming Processes / Apps Panel

Displays a live ranking of the **top 12 processes (Windows) or active apps (Android)** with the highest memory impact:

- **Row Details**:
  - **Windows**: Executable name, Process ID (`PID`), and physical RAM (`Working Set`) in `MB`.
  - **Android**: Application label, package name (`com.example.app`), and estimated memory footprint in `MB`.
- **Individual `Liberar` Button**: Optimizes just that specific process or background app without touching anything else.
- **`🔄 Actualizar` Button**: Forces an immediate refresh of the list (in addition to automatic updates every 4 telemetry cycles).

---

## 8. Optimization & Activity Log

Real-time event console logging every action with an exact timestamp (`[HH:mm:ss]`):

- Records startup state, privilege/permission status, freed RAM amounts, cache cleanup summaries, and **Smart Auto-Optimization** triggers.
- **`Limpiar Log` Button**: Clears the visual history of the event console.

---

## 9. Settings Section & Color Themes

Opening **`⚙️ Configuración`** lets you customize appearance and behavior in real time. All changes **are saved automatically**.

### 🎨 1. All 8 Color Themes (Identical Across Windows & Android)
1. **Esmeralda Cyber *(Default)***: Deep dark palette inspired by AVG Cleaner with neon green (`#00E676`) and cyan (`#00B0FF`) accents.
2. **Océano Ártico *(Blue / Cyan)***: Deep navy background with electric turquoise and sapphire blue accents.
3. **Amatista Neón *(Purple / Synth)***: Dark violet synthwave aesthetic with neon purple and magenta highlights.
4. **Carmesí Gaming *(Red / Gaming)***: High-contrast obsidian black with ruby red and bright orange accents.
5. **Solar Dorado *(Amber / Gold)***: Sleek titanium charcoal paired with warm gold and amber accents.
6. **Modo Claro Cristal *(Light Mode)***: Bright light interface with crisp white cards and emerald/royal blue accents.
7. **Gris Oscuro *(Gray / Graphite)***: Neutral graphite and slate tones with bright silver and steel accents.
8. **Gris Claro *(Gray / Silver)***: Soft pearl gray and brushed aluminum palette designed for low eye strain.

### 🧠 2. Automation & System Preferences
- **Auto-release RAM when exceeding threshold**: Background monitoring with a **60-second cooldown** between automatic optimizations.
- **RAM Usage Trigger Threshold (`Slider` from `65%` to `95%`)**: `85%` recommended default.
- **Telemetry Refresh Rate**: **`⚡ 0.5s`** (Fast), **`⏱️ 1.0s`** (Standard), or **`🍃 2.0s`** (Eco / Battery saver).
- **Local Persistence**:
  - **On Windows**: `%LocalAppData%\SylverOptimizer\settings.json`.
  - **On Android**: App-private `SharedPreferences` (`sylver_optimizer_settings`).
- **`🔄 Restaurar Predeterminados` / `🔄 Restaurar` Button**: Reverts to **Esmeralda Cyber** and factory defaults.

---

## 10. Frequently Asked Questions & Usage Tips (FAQ)

### ❓ Will clicking "Liberar RAM" close my open apps or lose unsaved data?
- **On Windows:** **No.** `EmptyWorkingSet` never terminates open applications or erases active data; it simply moves inactive memory pages out of the immediate working set.
- **On Android:** `killBackgroundProcesses` only stops secondary background processes; the foreground app and all your saved data remain untouched.

### ❓ What does the "🛡️ Acceso a Uso" button do on Android?
Starting with Android 5.1+, Android restricts visibility into recently active third-party apps unless the user grants the **Usage Access (`PACKAGE_USAGE_STATS`)** permission. Enabling it allows **SylverOptimizer** to accurately identify which apps are actively consuming resources in the background.

### ❓ What is "Swap / ZRAM" in the Android version?
Modern Android devices allocate a compressed block of RAM called **ZRAM** (RAM expansion / Swap) to compress infrequently accessed pages. Displaying `Swap / ZRAM` shows virtual memory pressure on Android, serving the same diagnostic purpose as `Commit (Pag.)` on Windows.

### ❓ When should I use "Purgar Caché RAM (Standby)" on Windows?
Right before launching a demanding game, virtual machine, or video render when Windows 11 has accumulated gigabytes of cached files in the *Standby List* (requires running as Administrator).
