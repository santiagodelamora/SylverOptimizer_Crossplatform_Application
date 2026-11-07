# 📖 Manual de Usuario — SylverOptimizer (Windows & Android)

> 🌐 **Idioma / Language:** **🇪🇸 Español** | [🇺🇸 English](./USER_MANUAL.en.md)

Bienvenido al manual oficial de usuario de **SylverOptimizer**, una suite nativa de monitoreo de hardware en tiempo real, optimización de memoria RAM y limpieza de caché del sistema diseñada para **Windows 10 / 11 (x64)** y **Android 8.0+**.

---

## 📑 Índice de Contenidos

1. [Introducción y Seguridad del Sistema](#1-introducción-y-seguridad-del-sistema)
2. [Instalación y Modos de Ejecución (Windows y Android)](#2-instalación-y-modos-de-ejecución-windows-y-android)
3. [Barra Superior y Navegación Global](#3-barra-superior-y-navegación-global)
4. [Banner de Estado del Sistema y Optimización 1-Clic](#4-banner-de-estado-del-sistema-y-optimización-1-clic)
5. [Módulo de Memoria RAM Física y Caché](#5-módulo-de-memoria-ram-física-y-caché)
6. [Módulo de Procesador (CPU / SoC) y Caché del Sistema](#6-módulo-de-procesador-cpu--soc-y-caché-del-sistema)
7. [Panel de Procesos / Apps con Mayor Uso de RAM](#7-panel-de-procesos--apps-con-mayor-uso-de-ram)
8. [Registro de Optimización y Eventos (Log)](#8-registro-de-optimización-y-eventos-log)
9. [Sección de Configuración y Temas de Color](#9-sección-de-configuración-y-temas-de-color)
10. [Preguntas Frecuentes y Consejos de Uso (FAQ)](#10-preguntas-frecuentes-y-consejos-de-uso-faq)

---

## 1. Introducción y Seguridad del Sistema

**SylverOptimizer** permite conocer en todo momento el estado de consumo de memoria y procesador de tu PC o dispositivo móvil, así como recuperar memoria RAM inactiva y espacio ocupado por archivos temporales huérfanos.

### ¿Cómo funciona sin poner en riesgo tu dispositivo?
A diferencia de programas antiguos que saturaban la memoria artificialmente, **SylverOptimizer** utiliza exclusivamente las APIs nativas oficiales de cada sistema operativo:

- **🖥️ En Windows (`Win32` y `NT Native API`):**
  - **`EmptyWorkingSet` (`psapi.dll`)**: Ordena a Windows recortar únicamente las páginas de memoria que los procesos ya no están utilizando activamente, sin cerrar ninguna aplicación ni borrar datos no guardados.
  - **`NtSetSystemInformation` (`ntdll.dll`)**: Vacía de forma nativa las listas de páginas en espera (*Standby List* y *Modified Page List*) del administrador de memoria de Windows.
  - **Respeto de bloqueos NTFS**: Durante la limpieza de disco, cualquier archivo temporal utilizado por Windows o un programa abierto es omitido automáticamente de forma segura.

- **📱 En Android (`ActivityManager`, `UsageStatsManager` y `/proc`):**
  - **`ActivityManager.killBackgroundProcesses`**: Detiene de forma segura los procesos secundarios y servicios en caché en segundo plano sin afectar al sistema ni borrar datos personales de tus aplicaciones.
  - **Telemetría Linux nativa (`/proc/meminfo` y `sysfs`)**: Lee directamente las métricas del kernel Android (*MemAvailable*, *Cached*, *Buffers*, *Swap / ZRAM*) y las frecuencias reales de cada núcleo del procesador sin requerir acceso *Root*.

---

## 2. Instalación y Modos de Ejecución (Windows y Android)

### 🖥️ En Windows 10 / 11 (x64)
1. **Mediante el Instalador de Windows**: Descarga `SylverOptimizer_Setup_v1.0.0.exe` desde *GitHub Releases* (o desde `windows/Installer/Output/`) para instalar la aplicación con accesos directos en el Menú Inicio y Escritorio.
2. **Modo Portable (Sin instalación)**: Ejecuta directamente `windows/publish/SylverOptimizer.exe` (incluye el entorno de `.NET 10` integrado).
3. **Acceso Rápido de Desarrollo**: Haz doble clic sobre `windows/Ejecutar_SylverOptimizer.bat`.

| Función en Windows | Usuario Estándar | Administrador (`🛡️`) |
| :--- | :---: | :---: |
| Monitoreo en tiempo real de RAM, CPU, hilos y procesos | ✅ Sí | ✅ Sí |
| Liberación de RAM de procesos de usuario (`Working Sets`) | ✅ Sí | ✅ Sí |
| Limpieza de carpeta temporal del usuario (`%TEMP%`) y miniaturas | ✅ Sí | ✅ Sí |
| Vaciado de caché de red DNS (`ipconfig /flushdns`) | ✅ Sí | ✅ Sí |
| **Purga profunda de Caché RAM del Kernel (`Standby List`)** | ❌ Requiere Admin | ✅ **Habilitado** |
| **Limpieza de `C:\Windows\Temp` y `C:\Windows\Prefetch`** | ⚠️ Parcial | ✅ **Habilitado** |

### 📱 En Android 8.0 o superior
1. **Instalación directa (APK)**: Descarga `SylverOptimizer_Android_v1.0.0.apk` desde *GitHub Releases* en tu teléfono o tablet Android y ábrelo para instalarlo (acepta la instalación desde el navegador/archivos si es la primera vez).
2. **Desde Android Studio**: Abre la carpeta `android/` en Android Studio y pulsa **Run (`▶`)** sobre un dispositivo físico o emulador.

| Función en Android | Modo Estándar | Con Acceso a Uso (`🛡️`) |
| :--- | :---: | :---: |
| Monitoreo en tiempo real de RAM, Caché Kernel y Swap / ZRAM | ✅ Sí | ✅ Sí |
| Monitoreo de frecuencias de CPU/SoC y temperatura de batería | ✅ Sí | ✅ Sí |
| Liberación global de RAM en 2º plano y limpieza de temporales | ✅ Sí | ✅ Sí |
| **Detección precisa de Apps recientes activas en segundo plano** | ⚠️ Básica | ✅ **Completa (`UsageStatsManager`)** |

> **💡 Consejo:** En Android puedes activar el permiso opcional tocando el botón **`🛡️ Acceso a Uso`** arriba a la derecha, lo que abrirá la pantalla oficial del sistema para autorizar a **SylverOptimizer**.

---

## 3. Barra Superior y Navegación Global

En la parte superior de la pantalla (tanto en Windows como en Android) encontrarás los controles globales:

1. **Título de la Aplicación**: Muestra el icono con degradado `⚡` y el nombre **SylverOptimizer**.
2. **Selector de Vista (`📊 Panel Principal` | `⚙️ Configuración`)**: Permite alternar instantáneamente entre la pantalla de monitoreo/optimización y el panel de personalización y temas.
3. **Interruptor Rápido `Auto-liberar RAM (>85%)` / `Auto-RAM (>85%)`**: Activa o desactiva con un solo toque la optimización automática cuando el uso de memoria alcance el umbral configurado.
4. **Botón de Privilegios / Permisos**:
   - **En Windows:** Muestra **`🛡️ Elevar a Administrador`** (solicita confirmación UAC para habilitar la purga de *Standby List*) o **`⚡ Modo Administrador Activo`**.
   - **En Android:** Muestra **`🛡️ Acceso a Uso`** (abre los ajustes de `PACKAGE_USAGE_STATS` para detectar con mayor exactitud qué apps están activas en segundo plano) o **`⚡ Uso Activo`**.

---

## 4. Banner de Estado del Sistema y Optimización 1-Clic

Debajo de la barra superior se encuentra el banner principal de diagnóstico y acción rápida:

### Indicador de Salud del Sistema
Un punto luminoso (semáforo) y un título dinámico evalúan la carga combinada de tu hardware en cada ciclo:
- 🟢 **Rendimiento Óptimo**: Uso de RAM menor al `68%` y CPU menor al `70%`.
- 🟡 **Carga Moderada**: Uso de RAM entre `68%` y `84%`, o uso de CPU entre `70%` y `89%`.
- 🔴 **Carga Elevada**: Uso de RAM igual o superior al `85%`, o uso de CPU igual o superior al `90%`.

### Contadores Acumulados de Sesión
- **`RAM LIBERADA (SESIÓN)`**: Suma total de `MB` o `GB` de memoria RAM y caché en espera recuperados desde que abriste la aplicación.
- **`CACHÉ LIMPIADA (SESIÓN)`**: Suma total de espacio en almacenamiento (`MB` / `GB`) recuperado al eliminar archivos temporales durante la sesión actual.

### Botón `🚀 OPTIMIZAR TODO (1-CLIC)`
Es la acción principal de mantenimiento integral. Ejecuta en secuencia y de forma asíncrona:
1. **Liberación de RAM** (*Working Sets* en Windows / procesos en 2º plano en Android).
2. **Purga de Caché de Memoria en Espera** (*Standby List* del kernel en Windows Admin / paquetes en caché en Android).
3. **Limpieza de caché temporal** (`%TEMP%`, `Prefetch` y DNS en Windows / directorios temporales y archivos `.tmp`/`.log` en Android).
4. **Actualización inmediata** de los medidores, lista de procesos/apps y contadores de sesión.

---

## 5. Módulo de Memoria RAM Física y Caché

Ofrece telemetría detallada y controles específicos para la memoria del dispositivo:

### Elementos Visuales
- **Cabecera**: Muestra la cantidad total de memoria física instalada (ej. `16.0 GB Totales` o `8.0 GB Totales`).
- **Medidor Circular de RAM**: Indica el porcentaje exacto de memoria física en uso y la relación `GB usados / GB totales`. Su color cambia automáticamente a ámbar o rojo bajo presión alta de memoria.
- **Gráfico de Historial RAM (60s)**: Curva de área en tiempo real que grafica la evolución del consumo de memoria durante el último minuto.

### Cuadrícula de Detalles de Memoria
1. **En Uso**: RAM física ocupada actualmente por el sistema operativo y las aplicaciones activas.
2. **Disponible**: Memoria RAM lista para asignarse de inmediato.
3. **Caché / Standby** *(Windows)* | **Caché Kernel** *(Android)*: Memoria utilizada por el sistema como caché de páginas y buffers (`Cached + Buffers` en `/proc/meminfo` en Android).
4. **Commit (Pag.)** *(Windows)* | **Swap / ZRAM** *(Android)*: Memoria virtual comprometida en el archivo de paginación de Windows o memoria comprimida de intercambio (*Swap / ZRAM*) en Android.

### Botones de Acción de Memoria
- **`🧹 Liberar RAM (Working Sets / 2º Plano)`**: Libera la memoria inactiva de procesos abiertos en Windows o detiene procesos secundarios en segundo plano en Android.
- **`⚡ Purgar Caché RAM`**: Vacía la *Standby List* del kernel en Windows (requiere Administrador) o purga las aplicaciones almacenadas en caché en espera en Android.

---

## 6. Módulo de Procesador (CPU / SoC) y Caché del Sistema

Combina el monitoreo en tiempo real del procesador con las herramientas de limpieza de almacenamiento:

### Elementos Visuales
- **Cabecera y Botón de Información (`ⓘ`)**: Al pulsar **`ⓘ`** se muestra el modelo completo del procesador o SoC (ej. Intel/AMD en Windows o Snapdragon/MediaTek/Tensor/Exynos en Android), número de núcleos lógicos, arquitectura (`x64` / `arm64-v8a`) y, en Android, la temperatura actual de la batería/dispositivo.
- **Medidor Circular de CPU**: Porcentaje de carga actual del procesador y número de núcleos lógicos.
- **Gráfico de Historial CPU (60s)**: Evolución de la carga de CPU durante los últimos 60 segundos.

### Cuadrícula de Detalles
- **En Windows**: **Procesos**, **Hilos (Threads)**, **Handles** y **Caché Temp/Pref.**.
- **En Android**: **Apps Activas**, **Hilos (Threads)**, **Temp. Batería (`°C`)** y **Caché Temp.**.

### Botones de Limpieza del Sistema
- **`🔍 Analizar Caché`**: Escanea las ubicaciones temporales y residuales para calcular cuántos archivos existen y cuánto espacio ocupan, **sin borrar ningún archivo**.
- **`🗑️ Limpiar Caché...`**:
  - **En Windows**: Elimina archivos temporales huérfanos en `%TEMP%`, `Windows\Temp`, `Prefetch`, miniaturas inactivas y ejecuta `ipconfig /flushdns`.
  - **En Android**: Limpia directorios de caché temporal accesibles y barre archivos residuales `.tmp` y `.log`.

---

## 7. Panel de Procesos / Apps con Mayor Uso de RAM

Muestra el listado dinámico de los **12 procesos (Windows) o aplicaciones activas (Android)** con mayor impacto en memoria:

- **Información por fila**:
  - **Windows**: Nombre del ejecutable, identificador `PID` y RAM física (`Working Set`) en `MB`.
  - **Android**: Nombre visible de la aplicación, nombre del paquete (`com.ejemplo.app`) e impacto en memoria en `MB`.
- **Botón individual `Liberar`**: Optimiza únicamente ese proceso o aplicación específica sin tocar el resto del sistema.
- **Botón `🔄 Actualizar`**: Fuerza el refresco inmediato de la lista (además de su actualización automática cada 4 ciclos del monitor).

---

## 8. Registro de Optimización y Eventos (Log)

Consola de eventos en vivo con marca de tiempo exacta (`[HH:mm:ss]`):

- Registra el inicio de la aplicación, estado de permisos, megabytes de RAM liberados, resultados de limpieza de caché y ejecuciones automáticas del modo **Auto-Optimización**.
- **Botón `Limpiar Log`**: Vacía el historial visual de la consola de eventos.

---

## 9. Sección de Configuración y Temas de Color

Al abrir **`⚙️ Configuración`**, todos los cambios se aplican al instante y **se guardan automáticamente**.

### 🎨 1. Los 8 Temas de Color (Idénticos en Windows y Android)
1. **Esmeralda Cyber *(Predeterminado)***: Oscuro profundo estilo AVG Cleaner con acentos en verde neón (`#00E676`) y azul cian (`#00B0FF`).
2. **Océano Ártico *(Azul / Cian)***: Azul marino profundo con acentos en turquesa eléctrico y azul zafiro.
3. **Amatista Neón *(Púrpura / Synth)***: Estética *synthwave* violeta oscuro con acentos púrpura neón y magenta.
4. **Carmesí Gaming *(Rojo / Gaming)***: Negro obsidiana de alto contraste con acentos en rojo rubí y naranja.
5. **Solar Dorado *(Ámbar / Oro)***: Carbón titanio con elegantes acentos en oro cálido y ámbar.
6. **Modo Claro Cristal *(Modo Claro)***: Interfaz luminosa con tarjetas blancas y acentos en verde esmeralda y azul real.
7. **Gris Oscuro *(Gris / Grafito)***: Tonos grafito y pizarra neutros con acentos plata brillante y acero.
8. **Gris Claro *(Gris / Plata)***: Tonos gris perla y aluminio suave diseñados para reducir el cansancio visual.

### 🧠 2. Automatización y Preferencias del Sistema
- **Liberar RAM automáticamente cuando supere el umbral**: Vigilancia automática con tiempo de enfriamiento (*cooldown*) de **60 segundos**.
- **Umbral de disparo de uso de RAM (`Slider` de `65%` a `95%`)**: `85%` recomendado por defecto.
- **Frecuencia de actualización de telemetría**: **`⚡ 0.5s`** (Rápido), **`⏱️ 1.0s`** (Estándar) o **`🍃 2.0s`** (Ahorro de energía/batería).
- **Persistencia local**:
  - **En Windows**: `%LocalAppData%\SylverOptimizer\settings.json`.
  - **En Android**: `SharedPreferences` privadas de la aplicación (`sylver_optimizer_settings`).
- **Botón `🔄 Restaurar Predeterminados` / `🔄 Restaurar`**: Restablece el tema *Esmeralda Cyber* y todos los valores de fábrica.

---

## 10. Preguntas Frecuentes y Consejos de Uso (FAQ)

### ❓ ¿Se cerrará algún programa o perderé mis datos al pulsar "Liberar RAM"?
- **En Windows:** **No.** `EmptyWorkingSet` nunca cierra aplicaciones abiertas ni borra documentos; solo mueve páginas inactivas fuera del conjunto de trabajo inmediato.
- **En Android:** `killBackgroundProcesses` únicamente detiene procesos secundarios en segundo plano; la aplicación activa en pantalla y tus datos guardados permanecen intactos.

### ❓ ¿Para qué sirve el botón "🛡️ Acceso a Uso" en Android?
Desde Android 5.1+, por privacidad el sistema restringe saber qué otras aplicaciones han estado activas recientemente a menos que el usuario autorice el permiso especial de **Acceso a datos de uso (`PACKAGE_USAGE_STATS`)**. Activarlo permite a **SylverOptimizer** identificar con mayor precisión las apps que siguen consumiendo recursos en segundo plano.

### ❓ ¿Qué significa "Swap / ZRAM" en la versión de Android?
La mayoría de los dispositivos Android modernos reservan un bloque comprimido de memoria llamado **ZRAM** (expansión de RAM / Swap) para comprimir páginas poco usadas. Mostrar `Swap / ZRAM` te permite ver qué tan exigida está la memoria virtual del dispositivo, cumpliendo el mismo rol diagnóstico que el indicador `Commit (Pag.)` en Windows.

### ❓ ¿Cuándo conviene usar "Purgar Caché RAM (Standby)" en Windows?
Antes de iniciar un videojuego exigente, máquina virtual o renderizado de video cuando Windows 11 ha acumulado varios gigabytes en la *Standby List*, dejándola inmediatamente libre (requiere ejecutar como Administrador).
