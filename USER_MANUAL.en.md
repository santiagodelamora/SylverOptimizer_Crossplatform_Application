# 📖 User Manual — WinOptimizer Pro

> 🌐 **Language / Idioma:** [🇪🇸 Español](./MANUAL_DE_USUARIO.md) | **🇺🇸 English**

Welcome to the official user manual for **WinOptimizer Pro**, a native real-time hardware monitoring, RAM optimization, and system cache cleaning utility built for **Windows 10 and Windows 11 (x64)**.

---

## 📑 Table of Contents

1. [Introduction & System Safety](#1-introduction--system-safety)
2. [Installation & Execution Modes](#2-installation--execution-modes)
3. [Global Header Bar & Navigation](#3-global-header-bar--navigation)
4. [System Health Banner & 1-Click Optimization](#4-system-health-banner--1-click-optimization)
5. [Physical RAM & Cache Module](#5-physical-ram--cache-module)
6. [Processor (CPU) & System Cache Module](#6-processor-cpu--system-cache-module)
7. [Top RAM-Consuming Processes Panel](#7-top-ram-consuming-processes-panel)
8. [Optimization & Activity Log](#8-optimization--activity-log)
9. [Settings Section & Color Themes](#9-settings-section--color-themes)
10. [Frequently Asked Questions & Usage Tips (FAQ)](#10-frequently-asked-questions--usage-tips-faq)

---

## 1. Introduction & System Safety

**WinOptimizer Pro** lets you monitor your computer's memory and processor load in real time, reclaim inactive RAM, and recover disk space occupied by orphaned temporary files.

### How does it work safely without risking system stability?
Unlike legacy memory boosters that used unstable workarounds (such as allocating huge dummy arrays to force paging), **WinOptimizer Pro** communicates directly with the Windows kernel using official Microsoft-documented APIs:
- **`EmptyWorkingSet` (`psapi.dll`)**: Instructs Windows to trim only inactive memory pages from process working sets without closing applications or losing unsaved work.
- **`NtSetSystemInformation` (`ntdll.dll`)**: Natively flushes the Windows memory manager's *Modified Page List* and purges the *Standby List*.
- **NTFS Lock Compliance**: During disk cleanup, any temporary file actively locked or in use by Windows or open programs is safely skipped automatically.

---

## 2. Installation & Execution Modes

### Ways to launch the application
1. **Windows Installer**: Run `Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe` to install the app with Start Menu and Desktop shortcuts.
2. **Portable Mode (No installation required)**: Run `publish/WinOptimizerPro.exe` directly (bundles the `.NET 10` runtime).
3. **Quick Launch Script**: Double-click `Ejecutar_WinOptimizerPro.bat` in the project root folder.

### Standard User Mode vs. Administrator Mode

| Feature | Standard User | Administrator (`🛡️`) |
| :--- | :---: | :---: |
| Real-time monitoring of RAM, CPU, threads, and processes | ✅ Yes | ✅ Yes |
| User process RAM release (`Working Sets`) | ✅ Yes | ✅ Yes |
| User temporary folder (`%TEMP%`) & thumbnail cache cleanup | ✅ Yes | ✅ Yes |
| DNS network resolver cache flush (`ipconfig /flushdns`) | ✅ Yes | ✅ Yes |
| **Deep Kernel RAM Cache Purge (`Standby List`)** | ❌ Requires Admin | ✅ **Enabled** |
| **Cleanup of `C:\Windows\Temp` and `C:\Windows\Prefetch`** | ⚠️ Partial | ✅ **Enabled** |

> **💡 Tip:** You can switch to Administrator mode at any time by clicking the **`🛡️ Elevar a Administrador (Standby List)`** button in the top-right corner of the window.

---

## 3. Global Header Bar & Navigation

At the top of the window you will find the global application controls:

1. **Application Title**: Displays the official **WinOptimizer Pro** icon, title, and subtitle.
2. **View Switcher (`📊 Panel Principal` | `⚙️ Configuración`)**: Instantly toggles between the main monitoring/optimization dashboard and the customization/settings screen.
3. **Quick Toggle `Auto-liberar RAM (>85%)`**: Enables or disables background automatic RAM optimization with a single click whenever RAM usage reaches your configured threshold.
4. **Privilege Status Button**:
   - When showing **`🛡️ Elevar a Administrador`**, clicking it prompts Windows UAC confirmation to restart the app with full kernel privileges enabled.
   - When already running as Administrator, it displays **`⚡ Modo Administrador Activo`**.

---

## 4. System Health Banner & 1-Click Optimization

Right below the header bar sits the primary diagnostic and quick-action banner:

### System Health Indicator
A status dot and dynamic title evaluate your combined hardware load every second:
- 🟢 **Optimal Performance (*Rendimiento Óptimo*)**: RAM usage below `68%` and CPU usage below `70%`.
- 🟡 **Moderate Load (*Carga Moderada*)**: RAM usage between `68%` and `84%`, or CPU usage between `70%` and `89%`.
- 🔴 **High Load (*Carga Elevada*)**: RAM usage at or above `85%`, or CPU usage at or above `90%`.

Below the status title, a summary line always displays the result of the latest diagnostic or optimization action.

### Cumulative Session Counters
- **`RAM LIBERADA (SESIÓN)`**: Total megabytes (`MB`) or gigabytes (`GB`) of physical RAM and *Standby* cache reclaimed since you opened the application.
- **`CACHÉ LIMPIADA (SESIÓN)`**: Total disk space (`MB` / `GB`) recovered by deleting temporary files during the current session.

### `🚀 OPTIMIZAR TODO (1-CLIC)` Button
The all-in-one maintenance button. Clicking it executes the following steps asynchronously in sequence:
1. **RAM Working Set Trimming** across all accessible processes.
2. **Kernel Standby List Purge** (when running as Administrator and enabled in Settings).
3. **Temporary Disk Cache Cleanup** (`%TEMP%`, `Windows\Temp`, `Prefetch`, inactive Explorer thumbnails) plus **DNS Cache Flush**.
4. **Automatic Refresh** of all telemetry gauges, top processes, and session counters.

---

## 5. Physical RAM & Cache Module

Located on the middle-left card of the **Main Dashboard**, this module provides detailed memory telemetry and dedicated RAM controls:

### Visual Elements
- **Header**: Displays total installed physical RAM (e.g., `16.0 GB Totales`).
- **Circular RAM Gauge**: Shows the exact percentage of physical memory in use and the `Used GB / Total GB` ratio. Its arc color automatically shifts to amber or red under heavy memory pressure.
- **60-Second RAM History Chart**: Real-time area graph plotting memory usage over the last minute.

### Memory Breakdown Grid
1. **In Use (*En Uso*)**: Exact amount of physical RAM currently occupied by Windows and active applications.
2. **Available (*Disponible*)**: Physical RAM immediately available for allocation by any program.
3. **Cache / Standby (*Caché / Standby*)**: Memory held by the OS for file caching and standby page lists.
4. **Commit (*Commit (Pag.)*)**: Total virtual memory committed by the system (physical RAM + `pagefile.sys`) relative to the system Commit limit.

### Memory Action Buttons
- **`🧹 Liberar RAM (Working Sets)`**: Iterates through all accessible processes and trims inactive memory pages back to available RAM. Safe to run at any time while working or gaming.
- **`⚡ Purgar Caché RAM (Standby)`**: Flushes the *Modified Page List* and purges the *Standby List* in the Windows 11 kernel. Especially useful before launching heavy games, virtual machines, or video editors when Windows has accumulated gigabytes of cached standby pages. *(Requires Administrator privileges).*

---

## 6. Processor (CPU) & System Cache Module

Located on the middle-right card of the **Main Dashboard**, this module pairs real-time CPU telemetry with storage and network cache tools:

### Visual Elements
- **Header**: Automatically detects and displays your processor's exact model name from the Windows hardware registry.
- **Circular CPU Gauge**: Real-time overall CPU utilization percentage and logical core count.
- **60-Second CPU History Chart**: Plots processor utilization over the last 60 seconds.

### CPU & Cache Details Grid
1. **Processes (*Procesos*)**: Total number of active processes running in Windows.
2. **Threads (*Hilos*)**: Total number of active execution threads across the system.
3. **Handles**: Total number of open kernel resource handles held by processes.
4. **Temp/Prefetch Cache (*Caché Temp/Pref.*)**: Total disk space in megabytes (`MB`) currently occupied by cleanable temporary files.

### System Cache Buttons
- **`🔍 Analizar Caché`**: Scans `%TEMP%`, `C:\Windows\Temp`, `C:\Windows\Prefetch`, and Windows Explorer thumbnail caches (`thumbcache_*.db`) to calculate how many temporary files exist and how much disk space they occupy **without deleting anything**.
- **`🗑️ Limpiar Caché Temporal + Prefetch + DNS`**:
  - Deletes orphaned and inactive temporary files across all scanned cache folders.
  - Safely skips any file locked by an open application.
  - Runs `ipconfig /flushdns` (if enabled in Settings) to clear the Windows DNS resolver cache.

---

## 7. Top RAM-Consuming Processes Panel

The bottom-left panel displays a live ranking of the **top 12 processes consuming the most physical RAM**:

- **Row Details**:
  - **Process Name** (e.g., `chrome`, `Code`, `explorer`).
  - **Process ID (`PID`)**.
  - **Current RAM Usage (`MB`)**.
- **Individual `Liberar` Button**: If a specific application is hogging memory and you only want to optimize that single program, click its **`Liberar`** button to trim just that process's *Working Set*.
- **`🔄 Actualizar` Button**: The process list refreshes automatically every 4 telemetry cycles, or you can click this button for an immediate refresh.

---

## 8. Optimization & Activity Log

The bottom-right panel provides a real-time event console:

- Logs every action with an exact timestamp (`[HH:mm:ss]`):
  - Application startup and privilege mode (Standard User or Administrator).
  - Amount of RAM freed and count of optimized processes vs. skipped system-protected processes.
  - Disk cache scan and cleanup summaries (deleted files, skipped in-use files, and DNS flush status).
  - Automatic triggers from **Smart Auto-Optimization**.
- **`Limpiar Log` Button**: Clears the visual history of the event console.

---

## 9. Settings Section & Color Themes

Clicking the **`⚙️ Configuración`** button in the top navigation bar opens the customization and preferences panel. All changes made here **are saved automatically**.

### 🎨 1. Color Themes & Visual Appearance
Click any of the **6 theme cards** to switch the entire application's color palette in real time:

1. **Esmeralda Cyber *(Default)***: Deep dark palette inspired by AVG Cleaner with neon green (`#00E676`) and cyan (`#00B0FF`) accents.
2. **Océano Ártico *(Blue / Cyan)***: Deep navy background with electric turquoise and sapphire blue accents.
3. **Amatista Neón *(Purple / Synth)***: Dark violet synthwave aesthetic with neon purple and magenta highlights.
4. **Carmesí Gaming *(Red / Gaming)***: High-contrast obsidian black with ruby red and bright orange accents.
5. **Solar Dorado *(Amber / Gold)***: Sleek titanium charcoal paired with warm gold and amber accents.
6. **Modo Claro Cristal *(Light Mode)***: Bright *Windows 11 Light* interface with crisp white cards, soft borders, and emerald/royal blue accents.

### 🧠 2. RAM Automation & Optimization
- **Auto-release RAM when exceeding threshold**: Enables background memory monitoring with a built-in **60-second cooldown** between automatic cleanups to avoid CPU overhead.
- **RAM Usage Trigger Threshold (`Slider` from `65%` to `95%`)**:
  - `65% – 75%` (*Aggressive*): Keeps physical RAM usage as low as possible on memory-constrained PCs.
  - `85%` (*Recommended*): Ideal balance to prevent Windows from heavily swapping to disk.
  - `90% – 95%` (*Conservative*): Only intervenes under critical memory saturation.
- **Purge Standby List on '1-Click Optimize'**: Chooses whether the 1-Click button also purges the kernel *Standby List* when running as Administrator.

### ⚡ 3. System Cache & Real-Time Monitoring
- **Flush DNS cache (`ipconfig /flushdns`) when cleaning cache**: Toggles network DNS cache flushing during temporary file cleanup.
- **Telemetry & Chart Refresh Rate**:
  - **`⚡ 0.5s (Fast)`**: Updates gauges and charts twice per second for high-precision diagnostics.
  - **`⏱️ 1.0s (Standard)`**: Recommended default refresh interval.
  - **`🍃 2.0s (Eco)`**: Reduces polling to once every 2 seconds to minimize battery usage on laptops.

### 💾 4. Persistence & `🔄 Restaurar Predeterminados` Button
- Your configuration is automatically stored as JSON in your local Windows user profile:
  `%LocalAppData%\WinOptimizerPro\settings.json`
- To revert to the **Esmeralda Cyber** theme and factory defaults (`85%` threshold, `1.0s` interval, DNS and Standby enabled), click **`🔄 Restaurar Predeterminados`** at the top of the Settings view.

---

## 10. Frequently Asked Questions & Usage Tips (FAQ)

### ❓ Will clicking "Liberar RAM" close my open apps or lose unsaved documents?
**No.** `EmptyWorkingSet` never terminates processes or erases active data. It simply instructs the Windows memory manager to move pages that haven't been accessed recently out of the process's immediate working set, making that physical RAM available for applications that actively need it.

### ❓ Why does cache cleanup report that some "in-use" files were skipped?
That is normal and safe Windows behavior. Open applications (such as your web browser, Discord, Visual Studio, or background Windows services) hold active locks on certain temporary files while running. **WinOptimizer Pro** respects those NTFS locks so running applications never crash or malfunction.

### ❓ When should I use "Purgar Caché RAM (Standby)"?
Windows 11 caches files from previously closed programs in the *Standby List* in case you reopen them. Sometimes this cached memory is not released fast enough when launching a demanding game or video editor, causing micro-stutters. Purging the *Standby List* as Administrator before starting a heavy workload makes all of that memory immediately free.
