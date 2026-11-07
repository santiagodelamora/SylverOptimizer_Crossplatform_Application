# 🤝 Guía de Contribución / Contribution Guide

> 🌐 **Idioma / Language:** [🇪🇸 Español](#-español--guía-de-contribución) | [🇺🇸 English](#-english--contribution-guide)

---

## 🇪🇸 Español — Guía de Contribución

¡Gracias por tu interés en contribuir a **SylverOptimizer**! Este documento detalla las reglas de flujo de trabajo, formato de commits y estándares técnicos para colaborar en el repositorio.

### 1. Flujo de Ramas (Git Workflow)

El repositorio cuenta con dos ramas principales:
- **`estable`**: Rama de producción con versiones listas para el usuario final. **Nunca envíes commits ni Pull Requests directamente a `estable`.**
- **`desarrollo`**: Rama principal de integración donde se concentra todo el trabajo nuevo.

**Pasos para colaborar:**
1. Haz un *Fork* del repositorio o clónalo si eres colaborador directo.
2. Asegúrate de partir siempre desde la rama `desarrollo` actualizada:
   ```powershell
   git checkout desarrollo
   git pull origin desarrollo
   ```
3. Crea una rama descriptiva para tu cambio:
   - `feat/nombre-de-funcion` (nuevas características)
   - `fix/descripcion-del-bug` (corrección de errores)
   - `refactor/componente` (mejoras internas de código)
   - `docs/tema` (actualización de documentación)
   ```powershell
   git checkout -b feat/mi-nueva-mejora
   ```

### 2. Regla de Commits Bilingües (Inglés y Español)

Todos los mensajes de commit (`git commit`) **deben escribirse de forma bilingüe (Inglés y Español)** siguiendo el formato *Conventional Commits*:

```text
<tipo>: <resumen en inglés> / <resumen en español>

EN: <Explicación detallada en inglés de qué se cambió y por qué.>

ES: <Explicación detallada en español de qué se cambió y por qué.>
```

**Tipos permitidos (`<tipo>`):** `feat`, `fix`, `refactor`, `docs`, `style`, `perf`, `chore`, `ci`.

**Ejemplo real del proyecto:**
```text
feat: add custom refresh rate selector / agregar selector de frecuencia de actualización

EN: Add 0.5s, 1.0s, and 2.0s telemetry interval options in the Settings panel and persist the selection in settings.json.

ES: Agrega las opciones de intervalo de telemetría de 0.5s, 1.0s y 2.0s en el panel de Configuración y guarda la selección en settings.json.
```

### 3. Requisitos del Entorno de Desarrollo

**Para la versión de Windows (`windows/`):**
- **Sistema Operativo:** Windows 10 o Windows 11 (x64).
- **SDK:** [.NET 10.0 SDK](https://dotnet.microsoft.com/download/dotnet/10.0) (`net10.0-windows` con soporte para WPF).
- **IDE recomendado:** Visual Studio 2022/2026, JetBrains Rider o VS Code (con extensión C# Dev Kit).
- **Opcional (para generar instalador):** [Inno Setup 6](https://jrsoftware.org/isinfo.php) para compilar `windows/Installer/SylverOptimizer.iss`.

**Para la versión de Android (`android/`):**
- **JDK:** Java 17 o superior.
- **SDK:** Android SDK Platform 35 (`minSdk = 26`, `targetSdk = 35`) y Gradle 8.10.2.
- **IDE recomendado:** Android Studio Ladybug o superior (Kotlin 2.0 + Jetpack Compose).

### 4. Compilación y Pruebas Locales

Antes de abrir un Pull Request, verifica que la plataforma modificada compile sin errores en modo `Release`:

```powershell
# 🖥️ Compilar y probar versión Windows
cd windows
dotnet restore
dotnet build SylverOptimizer.csproj --configuration Release
dotnet run --configuration Release

# 📱 Compilar APK versión Android
cd ../android
gradle assembleRelease
```

### 5. Estándares de Código y Diseño

- **Formato:** Respeta las reglas definidas en [`.editorconfig`](./.editorconfig) (indentación de 4 espacios en C# y XAML, codificación UTF-8).
- **APIs Nativas (`P/Invoke`):** Cualquier llamada nueva a `kernel32.dll`, `psapi.dll`, `ntdll.dll` o `advapi32.dll` debe declararse en [`windows/Services/NativeMethods.cs`](./windows/Services/NativeMethods.cs) manejando adecuadamente permisos e impidiendo bloqueos en el hilo de interfaz gráfica (UI Thread).
- **Temas Dinámicos:** Si agregas controles visuales en [`windows/MainWindow.xaml`](./windows/MainWindow.xaml) o en [`android/`](./android), utiliza recursos dinámicos para que sean compatibles con los **8 temas de color** gestionados por [`windows/Services/AppSettingsService.cs`](./windows/Services/AppSettingsService.cs) y [`android/app/src/main/java/com/sylveroptimizer/android/services/AppSettingsService.kt`](./android/app/src/main/java/com/sylveroptimizer/android/services/AppSettingsService.kt).
- **Documentación Bilingüe:** Si tu cambio modifica la interfaz o añade una funcionalidad visible para el usuario, actualiza tanto la documentación en español ([`README.md`](./README.md), [`MANUAL_DE_USUARIO.md`](./MANUAL_DE_USUARIO.md)) como en inglés ([`README.en.md`](./README.en.md), [`USER_MANUAL.en.md`](./USER_MANUAL.en.md)).
- **Licencia (`GNU GPLv3`):** Al enviar una contribución a este repositorio, aceptas que tu código sea distribuido bajo los términos de la licencia **[GNU General Public License v3.0](./LICENSE)**.

---

## 🇺🇸 English — Contribution Guide

Thank you for your interest in contributing to **SylverOptimizer**! This document outlines our branching workflow, bilingual commit conventions, and technical standards.

### 1. Branching Workflow

The repository maintains two main branches:
- **`estable`**: Production branch containing stable releases. **Never push or open Pull Requests directly against `estable`.**
- **`desarrollo`**: Primary integration branch where all active development takes place.

**Steps to contribute:**
1. Fork the repository (or clone it directly if you have collaborator access).
2. Always branch off from an up-to-date `desarrollo` branch:
   ```powershell
   git checkout desarrollo
   git pull origin desarrollo
   ```
3. Create a descriptive branch for your work:
   - `feat/feature-name` (new features)
   - `fix/bug-description` (bug fixes)
   - `refactor/component-name` (code refactoring)
   - `docs/topic` (documentation updates)
   ```powershell
   git checkout -b feat/my-new-feature
   ```

### 2. Bilingual Commit Message Rule (English & Spanish)

All Git commit messages (`git commit`) **must be written bilingually in both English and Spanish** following the structure below:

```text
<type>: <English summary> / <Spanish summary>

EN: <Detailed explanation in English of what changed and why.>

ES: <Detailed explanation in Spanish of what changed and why.>
```

**Allowed types (`<type>`):** `feat`, `fix`, `refactor`, `docs`, `style`, `perf`, `chore`, `ci`.

### 3. Development Environment Requirements

**For the Windows Desktop version (`windows/`):**
- **Operating System:** Windows 10 or Windows 11 (x64).
- **SDK:** [.NET 10.0 SDK](https://dotnet.microsoft.com/download/dotnet/10.0) (`net10.0-windows` with WPF workload).
- **Recommended IDE:** Visual Studio 2022/2026, JetBrains Rider, or VS Code (with C# Dev Kit).
- **Optional (for building the installer):** [Inno Setup 6](https://jrsoftware.org/isinfo.php) to compile `windows/Installer/SylverOptimizer.iss`.

**For the Android Mobile version (`android/`):**
- **JDK:** Java 17 or newer.
- **SDK:** Android SDK Platform 35 (`minSdk = 26`, `targetSdk = 35`) and Gradle 8.10.2.
- **Recommended IDE:** Android Studio Ladybug or newer (Kotlin 2.0 + Jetpack Compose).

### 4. Building & Testing Locally

Before submitting a Pull Request, ensure the modified target builds cleanly in `Release` configuration:

```powershell
# 🖥️ Build & run Windows Desktop version
cd windows
dotnet restore
dotnet build SylverOptimizer.csproj --configuration Release
dotnet run --configuration Release

# 📱 Build Android Release APK
cd ../android
gradle assembleRelease
```

### 5. Coding & UI Standards

- **Formatting:** Follow the rules in [`.editorconfig`](./.editorconfig) (4-space indentation for C# and XAML, UTF-8 encoding).
- **Native Win32/NT APIs (`P/Invoke`):** Place all native signatures in [`windows/Services/NativeMethods.cs`](./windows/Services/NativeMethods.cs), handle privilege/access exceptions gracefully, and keep heavy operations off the WPF UI thread.
- **Dynamic Color Themes:** Any new UI elements in [`windows/MainWindow.xaml`](./windows/MainWindow.xaml) or [`android/`](./android) must adapt seamlessly to all **8 color themes** defined in [`windows/Services/AppSettingsService.cs`](./windows/Services/AppSettingsService.cs) and [`android/app/src/main/java/com/sylveroptimizer/android/services/AppSettingsService.kt`](./android/app/src/main/java/com/sylveroptimizer/android/services/AppSettingsService.kt).
- **Bilingual Documentation:** User-facing changes should be reflected in both the Spanish ([`README.md`](./README.md), [`MANUAL_DE_USUARIO.md`](./MANUAL_DE_USUARIO.md)) and English ([`README.en.md`](./README.en.md), [`USER_MANUAL.en.md`](./USER_MANUAL.en.md)) documentation.
- **License (`GNU GPLv3`):** By submitting a contribution to this repository, you agree that your work will be licensed under the **[GNU General Public License v3.0](./LICENSE)**.

