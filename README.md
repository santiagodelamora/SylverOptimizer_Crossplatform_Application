# ⚡ WinOptimizer Pro — Optimización de RAM, Caché y CPU para Windows 11

Aplicación de escritorio nativa para **Windows 11** construida con **C# y WPF (.NET 10)** que permite monitorear el hardware en tiempo real, liberar memoria RAM inactiva y limpiar cachés del sistema utilizando exclusivamente las APIs oficiales de Windows (`Win32` y `NT Native API`).

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
  - Modo opcional para liberar RAM automáticamente cuando el consumo supere el **85%**.

## 🛠️ Requisitos

- **Sistema Operativo:** Windows 10 / Windows 11 (x64 / ARM64)
- **SDK / Runtime:** [.NET 10.0 SDK](https://dotnet.microsoft.com/)

## 🚀 Compilación y Ejecución

```powershell
# Compilar en modo Release
dotnet build --configuration Release

# Ejecutar la aplicación
dotnet run --configuration Release
```

También puedes ejecutar directamente [`Ejecutar_WinOptimizerPro.bat`](./Ejecutar_WinOptimizerPro.bat).

## 📂 Estructura del Proyecto

- `Services/NativeMethods.cs` — Declaraciones P/Invoke (`psapi.dll`, `kernel32.dll`, `ntdll.dll`, `advapi32.dll`).
- `Services/SystemMonitorService.cs` — Telemetría de CPU, RAM, caché y procesos en tiempo real.
- `Services/OptimizerService.cs` — Motor de liberación de RAM, purga de Standby List y limpieza de caché de disco/DNS.
- `MainWindow.xaml` / `MainWindow.xaml.cs` — Interfaz gráfica en modo oscuro con gráficos y controles interactivos.
