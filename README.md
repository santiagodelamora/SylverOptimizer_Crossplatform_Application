# ⚡ SylverOptimizer — Optimización de RAM, Caché y CPU para Windows 11 & Android

> 🌐 **Idioma / Language:** **🇪🇸 Español** | [🇺🇸 English](./README.en.md)  
> 📖 **Documentación:** **[Manual de Usuario Detallado](./MANUAL_DE_USUARIO.md)** | **[Guía de Contribución](./CONTRIBUTING.md)**

Suite de optimización y telemetría de hardware en tiempo real con versiones **100% nativas** para **Windows 10/11 (`C# / WPF / .NET 10`)** y **Android (`Kotlin / Jetpack Compose`)**, diseñada para monitorear memoria RAM y procesador, liberar procesos en segundo plano y limpiar cachés del sistema utilizando exclusivamente APIs oficiales de cada sistema operativo.

## 📦 Instalación Directa (Windows x64 & Android APK)

Puedes descargar e instalar ambas versiones directamente desde la sección oficial de **GitHub Releases**:

- 🖥️ **Windows 10 / 11 (x64):** **[Descargar `SylverOptimizer_Setup_v1.0.0.exe` (Instalador Autocontenido)](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases/latest/download/SylverOptimizer_Setup_v1.0.0.exe)**
- 📱 **Android 8.0+ (APK Nativo):** **[Descargar `SylverOptimizer_Android_v1.0.0.apk`](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases/latest/download/SylverOptimizer_Android_v1.0.0.apk)**
- 📋 **[Ver todas las versiones en GitHub Releases](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases)**

## ✨ Características Principales

- **Monitoreo en Tiempo Real (Sin sobrecarga):**
  - Medidores circulares dinámicos e historial gráfico de 60 segundos para **Memoria RAM** y **Procesador (CPU / SoC)** en ambas plataformas.
  - **En Windows:** Desglose de *En Uso*, *Disponible*, *Caché / Standby List* y *Commit (Paginación)*, más métricas de CPU vía `GetSystemTimes` (`kernel32.dll`), procesos, hilos y *handles*.
  - **En Android:** Lectura nativa de `ActivityManager.MemoryInfo` y `/proc/meminfo` (*En Uso*, *Disponible*, *Caché Kernel* y *Swap / ZRAM*), frecuencias en vivo por núcleo de CPU, modelo del SoC y temperatura de batería (`BatteryManager`).
- **Liberación Segura de RAM y Procesos en 2º Plano:**
  - **En Windows (`EmptyWorkingSet`):** Recorta páginas inactivas del *Working Set* sin afectar procesos críticos protegidos por el kernel, globalmente o por proceso individual.
  - **En Android (`killBackgroundProcesses`):** Detiene procesos inactivos y servicios en caché en segundo plano de forma global o por aplicación individual desde la lista de **Apps Activas** (con soporte opcional de permiso *Acceso a Uso* `PACKAGE_USAGE_STATS`).
- **Purga de Caché de Memoria:**
  - **En Windows (`NtSetSystemInformation`):** Vacía la *Standby List* y la *Modified Page List* del kernel de Windows 11 al ejecutarse como Administrador.
  - **En Android:** Purga paquetes en espera (*Cached Background Apps*) y recolecta memoria inactiva del runtime.
- **Limpiador de Caché del Sistema:**
  - **En Windows:** Elimina archivos temporales huérfanos en `%TEMP%`, `C:\Windows\Temp`, `C:\Windows\Prefetch`, miniaturas inactivas (`thumbcache_*.db`) y vacía la caché DNS (`ipconfig /flushdns`).
  - **En Android:** Escanea y limpia directorios de caché temporal y archivos residuales (`.tmp`, `.log`).
- **Auto-Optimización Inteligente:**
  - Modo opcional para liberar RAM automáticamente cuando el consumo supere el umbral configurable (**65% – 95%**, por defecto **85%**) con enfriamiento de 60 segundos.
- **Sección de Configuración y 8 Temas de Color Personalizables:**
  - Selector visual en tiempo real con **8 temas idénticos en Windows y Android**: *Esmeralda Cyber*, *Océano Ártico*, *Amatista Neón*, *Carmesí Gaming*, *Solar Dorado*, *Modo Claro Cristal*, *Gris Oscuro* y *Gris Claro*.
  - Ajustes persistentes de frecuencia de telemetría (`0.5s`, `1.0s`, `2.0s`) guardados automáticamente en `%LocalAppData%\SylverOptimizer\settings.json` (Windows) y `SharedPreferences` (Android).

## 🛠️ Compilación desde el Código Fuente

### 🖥️ Versión de Escritorio (`windows/` — Windows / .NET 10)
```powershell
cd windows

# Compilar en modo Release
dotnet build SylverOptimizer.csproj --configuration Release

# Ejecutar la aplicación
dotnet run --configuration Release
```
- Para ejecutar la app rápidamente: [`windows/Ejecutar_SylverOptimizer.bat`](./windows/Ejecutar_SylverOptimizer.bat)
- Para volver a generar el instalador con **Inno Setup 6**: [`windows/Crear_Instalador.bat`](./windows/Crear_Instalador.bat)

### 📱 Versión Móvil Nativa (`android/` — Android / Kotlin + Jetpack Compose)
```powershell
cd android
gradle assembleRelease
```
- El proyecto Android se encuentra en la carpeta [`android/`](./android) y puede abrirse directamente en **Android Studio** (JDK 17+, Android SDK 35).

## 📂 Estructura del Proyecto

- `windows/` — Aplicación nativa de escritorio para Windows 10/11 (**C# / WPF / .NET 10**):
  - `windows/Assets/app.ico` — Icono de la aplicación y del instalador de Windows.
  - `windows/Installer/SylverOptimizer.iss` — Script de configuración del instalador para Inno Setup 6 (distribuido en [GitHub Releases](https://github.com/santiagodelamora/WinOptimizer_Desktop_Application/releases)).
  - `windows/Services/` — Servicios nativos de Windows (`NativeMethods.cs`, `SystemMonitorService.cs`, `OptimizerService.cs`, `AppSettingsService.cs`).
  - `windows/MainWindow.xaml` / `windows/MainWindow.xaml.cs` — Interfaz gráfica WPF para Windows 10/11.
- `android/` — Aplicación nativa para Android escrita en **Kotlin + Jetpack Compose**:
  - `android/app/src/main/java/com/sylveroptimizer/android/services/` — Servicios equivalentes para Android (`SystemMonitorService.kt`, `OptimizerService.kt`, `AppSettingsService.kt`).
  - `android/app/src/main/java/com/sylveroptimizer/android/ui/` — Interfaz móvil adaptable en Jetpack Compose con medidores circulares, gráficas de 60s y los 8 temas de color (`SylverOptimizerScreen.kt`, `TelemetryWidgets.kt`).
- [`LICENSE`](./LICENSE) — Licencia de software libre **GNU General Public License v3.0 (GPLv3)**.

## 📄 Licencia

Este proyecto es software libre y se distribuye bajo los términos de la licencia **[GNU General Public License v3.0 (GPL-3.0)](./LICENSE)**.  
Copyright © 2026 **Santiago De la Mora**.
