# ⚡ WinOptimizer — Optimización de RAM, Caché y CPU para Windows 11

> 🌐 **Idioma / Language:** **🇪🇸 Español** | [🇺🇸 English](./README.en.md)  
> 📖 **Documentación:** **[Manual de Usuario Detallado](./MANUAL_DE_USUARIO.md)** | **[Guía de Contribución](./CONTRIBUTING.md)**

Aplicación de escritorio nativa con **interfaz gráfica (WPF / .NET 10)** en modo oscuro para **Windows 11**, diseñada para monitorear el hardware en tiempo real, liberar memoria RAM inactiva y limpiar cachés del sistema utilizando exclusivamente las APIs oficiales de Windows (`Win32` y `NT Native API`).

## 📦 Instalación Directa (Windows 10 / 11 x64)

Puedes instalar la aplicación directamente ejecutando el instalador incluido en el repositorio (no requiere tener .NET instalado, ya que es autocontenido):

- 👉 **[`Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe`](./Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe)**

## ✨ Características Principales

- **Monitoreo en Tiempo Real (Sin sobrecarga):**
  - Medidores circulares dinámicos e historial gráfico de 60 segundos para **Memoria RAM** y **Procesador (CPU)**.
  - Desglose detallado de RAM: *En Uso*, *Disponible*, *Caché / Standby List* y *Commit (Paginación)*.
  - Métricas de CPU mediante `GetSystemTimes` (`kernel32.dll`): modelo del procesador, núcleos lógicos, procesos activos, hilos (*threads*) y *handles*.
- **Liberación Segura de RAM (`EmptyWorkingSet`):**
  - Recorta las páginas inactivas del *Working Set* de todos los procesos accesibles sin afectar procesos críticos protegidos por el kernel de Windows.
  - Permite optimizar todos los procesos a la vez o liberar la RAM de un proceso individual desde la lista de **Top Procesos**.
- **Purga de Caché de RAM / Standby List (`NtSetSystemInformation`):**
  - Vacía la *Standby List* y la *Modified Page List* del kernel de Windows 11 (habilitando el privilegio `SeProfileSingleProcessPrivilege` cuando se ejecuta como Administrador).
- **Limpiador de Caché del Sistema y Red:**
  - Escanea y elimina archivos temporales huérfanos en `%TEMP%`, `C:\Windows\Temp`, `C:\Windows\Prefetch` y caché de miniaturas inactivas (`thumbcache_*.db`).
  - Vacía la caché de resolución DNS (`ipconfig /flushdns`).
- **Auto-Optimización Inteligente:**
  - Modo opcional para liberar RAM automáticamente cuando el consumo supere el umbral configurable (**65% – 95%**, por defecto **85%**).
- **Sección de Configuración y Temas de Color Personalizables:**
  - Selector visual en tiempo real con **6 temas de color**: *Esmeralda Cyber*, *Océano Ártico*, *Amatista Neón*, *Carmesí Gaming*, *Solar Dorado* y *Modo Claro Cristal*.
  - Ajustes persistentes de frecuencia de telemetría (`0.5s`, `1.0s`, `2.0s`), limpieza DNS y purga de Standby List guardados automáticamente en `%LocalAppData%\WinOptimizerPro\settings.json`.

## 🛠️ Compilación desde el Código Fuente

```powershell
# Compilar en modo Release
dotnet build --configuration Release

# Ejecutar la aplicación
dotnet run --configuration Release
```

- Para ejecutar la app rápidamente: [`Ejecutar_WinOptimizerPro.bat`](./Ejecutar_WinOptimizerPro.bat)
- Para volver a generar el instalador con **Inno Setup 6**: [`Crear_Instalador.bat`](./Crear_Instalador.bat)

## 📂 Estructura del Proyecto

- `Assets/app.ico` — Icono de la aplicación y del instalador.
- `Installer/WinOptimizerPro.iss` — Script de configuración del instalador para Inno Setup 6.
- `Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe` — Instalador ejecutable autocontenido para Windows (x64).
- `Services/NativeMethods.cs` — Declaraciones P/Invoke (`psapi.dll`, `kernel32.dll`, `ntdll.dll`, `advapi32.dll`).
- `Services/SystemMonitorService.cs` — Telemetría de CPU, RAM, caché y procesos en tiempo real.
- `Services/OptimizerService.cs` — Motor de liberación de RAM, purga de Standby List y limpieza de caché de disco/DNS.
- `Services/AppSettingsService.cs` — Gestión de temas de color dinámicos y persistencia de configuración en JSON.
- `MainWindow.xaml` / `MainWindow.xaml.cs` — Interfaz gráfica con paneles de monitoreo, configuración, gráficos y controles interactivos.
