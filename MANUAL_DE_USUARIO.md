# 📖 Manual de Usuario — WinOptimizer

> 🌐 **Idioma / Language:** **🇪🇸 Español** | [🇺🇸 English](./USER_MANUAL.en.md)

Bienvenido al manual oficial de usuario de **WinOptimizer**, una herramienta nativa de monitoreo de hardware en tiempo real, optimización de memoria RAM y limpieza de caché del sistema diseñada para **Windows 10 y Windows 11 (x64)**.

---

## 📑 Índice de Contenidos

1. [Introducción y Seguridad del Sistema](#1-introducción-y-seguridad-del-sistema)
2. [Instalación y Modos de Ejecución](#2-instalación-y-modos-de-ejecución)
3. [Barra Superior y Navegación Global](#3-barra-superior-y-navegación-global)
4. [Banner de Estado del Sistema y Optimización 1-Clic](#4-banner-de-estado-del-sistema-y-optimización-1-clic)
5. [Módulo de Memoria RAM Física y Caché](#5-módulo-de-memoria-ram-física-y-caché)
6. [Módulo de Procesador (CPU) y Caché del Sistema](#6-módulo-de-procesador-cpu-y-caché-del-sistema)
7. [Panel de Procesos con Mayor Uso de RAM](#7-panel-de-procesos-con-mayor-uso-de-ram)
8. [Registro de Optimización y Eventos (Log)](#8-registro-de-optimización-y-eventos-log)
9. [Sección de Configuración y Temas de Color](#9-sección-de-configuración-y-temas-de-color)
10. [Preguntas Frecuentes y Consejos de Uso (FAQ)](#10-preguntas-frecuentes-y-consejos-de-uso-faq)

---

## 1. Introducción y Seguridad del Sistema

**WinOptimizer** permite conocer en todo momento el estado de consumo de memoria y procesador de tu equipo, así como recuperar memoria RAM inactiva y espacio en disco ocupado por archivos temporales huérfanos.

### ¿Cómo funciona sin poner en riesgo tu PC?
A diferencia de programas antiguos que utilizaban trucos inestables (como saturar la memoria artificialmente para forzar el archivo de paginación), **WinOptimizer** se comunica directamente con el núcleo (*kernel*) de Windows mediante las APIs oficiales documentadas por Microsoft:
- **`EmptyWorkingSet` (`psapi.dll`)**: Ordena a Windows recortar únicamente las páginas de memoria que los procesos ya no están utilizando activamente, sin cerrar ninguna aplicación ni borrar datos no guardados.
- **`NtSetSystemInformation` (`ntdll.dll`)**: Vacía de forma nativa las listas de páginas en espera (*Standby List* y *Modified Page List*) del administrador de memoria de Windows.
- **Respeto de bloqueos NTFS**: Durante la limpieza de disco, cualquier archivo temporal que esté siendo utilizado por Windows o por un programa abierto es omitido automáticamente de forma segura.

---

## 2. Instalación y Modos de Ejecución

### Formas de abrir la aplicación
1. **Mediante el Instalador de Windows**: Ejecuta `Installer/Output/WinOptimizerPro_Setup_v1.0.0.exe` para instalar la aplicación con accesos directos en el Menú Inicio y Escritorio.
2. **Modo Portable (Sin instalación)**: Ejecuta directamente `publish/WinOptimizerPro.exe` (incluye el entorno de `.NET 10` integrado).
3. **Acceso Rápido de Desarrollo**: Haz doble clic sobre `Ejecutar_WinOptimizerPro.bat` en la raíz del proyecto.

### Modo Usuario Estándar vs. Modo Administrador

| Función | Usuario Estándar | Administrador (`🛡️`) |
| :--- | :---: | :---: |
| Monitoreo en tiempo real de RAM, CPU, hilos y procesos | ✅ Sí | ✅ Sí |
| Liberación de RAM de procesos de usuario (`Working Sets`) | ✅ Sí | ✅ Sí |
| Limpieza de carpeta temporal del usuario (`%TEMP%`) y miniaturas | ✅ Sí | ✅ Sí |
| Vaciado de caché de red DNS (`ipconfig /flushdns`) | ✅ Sí | ✅ Sí |
| **Purga profunda de Caché RAM del Kernel (`Standby List`)** | ❌ Requiere Admin | ✅ **Habilitado** |
| **Limpieza de `C:\Windows\Temp` y `C:\Windows\Prefetch`** | ⚠️ Parcial | ✅ **Habilitado** |

> **💡 Consejo:** Puedes cambiar al modo Administrador en cualquier momento haciendo clic en el botón **`🛡️ Elevar a Administrador (Standby List)`** ubicado en la esquina superior derecha de la ventana.

---

## 3. Barra Superior y Navegación Global

En la parte superior de la ventana encontrarás los controles globales de la aplicación:

1. **Título de la Aplicación**: Muestra el icono y el nombre oficial de **WinOptimizer** junto con su descripción.
2. **Selector de Vista (`📊 Panel Principal` | `⚙️ Configuración`)**: Permite alternar instantáneamente entre la pantalla de monitoreo/optimización y el panel de personalización y ajustes.
3. **Interruptor Rápido `Auto-liberar RAM (>85%)`**: Activa o desactiva con un solo clic la optimización automática en segundo plano cuando el uso de memoria RAM alcance el umbral configurado.
4. **Botón de Estado de Privilegios**:
   - Si se muestra **`🛡️ Elevar a Administrador`**, al pulsarlo se solicitará confirmación de Windows (UAC) para reiniciar la aplicación con todos los permisos del kernel habilitados.
   - Si ya estás ejecutando como Administrador, mostrará **`⚡ Modo Administrador Activo`**.

---

## 4. Banner de Estado del Sistema y Optimización 1-Clic

Debajo de la barra superior se encuentra el banner principal de diagnóstico y acción rápida:

### Indicador de Salud del Sistema
Un punto luminoso (semáforo) y un título dinámico evalúan la carga combinada de tu hardware cada segundo:
- 🟢 **Rendimiento Óptimo**: Uso de RAM menor al `68%` y CPU menor al `70%`.
- 🟡 **Carga Moderada**: Uso de RAM entre `68%` y `84%`, o uso de CPU entre `70%` y `89%`.
- 🔴 **Carga Elevada**: Uso de RAM igual o superior al `85%`, o uso de CPU igual o superior al `90%`.

Debajo del título se muestra siempre el resumen del último diagnóstico o acción ejecutada.

### Contadores Acumulados de Sesión
- **`RAM LIBERADA (SESIÓN)`**: Suma total de megabytes (`MB`) o gigabytes (`GB`) de memoria RAM y caché *Standby* recuperados desde que abriste la aplicación.
- **`CACHÉ LIMPIADA (SESIÓN)`**: Suma total de espacio en disco (`MB` / `GB`) recuperado al eliminar archivos temporales durante la sesión actual.

### Botón `🚀 OPTIMIZAR TODO (1-CLIC)`
Es la acción principal de mantenimiento integral. Al presionarlo, ejecuta en secuencia y de forma asíncrona:
1. **Recorte de *Working Sets* de RAM** en todos los procesos abiertos.
2. **Purga de *Standby List* del kernel** (si la aplicación corre como Administrador y la opción está activa en Configuración).
3. **Limpieza de caché temporal en disco** (`%TEMP%`, `Windows\Temp`, `Prefetch`, miniaturas inactivas) y **vaciado de caché DNS**.
4. **Actualización automática** de las métricas, lista de procesos y contadores de sesión.

---

## 5. Módulo de Memoria RAM Física y Caché

Ubicado en la tarjeta central izquierda del **Panel Principal**, ofrece telemetría detallada y controles específicos para la memoria del sistema:

### Elementos Visuales
- **Cabecera**: Muestra la cantidad total de memoria física instalada (ej. `16.0 GB Totales`).
- **Medidor Circular de RAM**: Indica el porcentaje exacto de memoria física en uso y la relación `GB usados / GB totales`. Su color cambia automáticamente a ámbar o rojo si la memoria se satura.
- **Gráfico de Historial RAM (60s)**: Curva en tiempo real que grafica la evolución del consumo de memoria durante el último minuto.

### Cuadrícula de Detalles de Memoria
1. **En Uso**: Cantidad exacta de RAM física ocupada actualmente por el sistema operativo y tus aplicaciones.
2. **Disponible**: Memoria RAM lista para ser utilizada de inmediato por cualquier programa.
3. **Caché / Standby**: Memoria utilizada por el sistema como caché de archivos y páginas en espera.
4. **Commit (Pag.)**: Memoria virtual total comprometida por el sistema (suma de RAM física + archivo de paginación en disco) respecto al límite total de *Commit*.

### Botones de Acción de Memoria
- **`🧹 Liberar RAM (Working Sets)`**: Recorre todos los procesos de usuario y del sistema accesibles para liberar las páginas de memoria inactivas. Es seguro ejecutarlo en cualquier momento, incluso mientras trabajas o juegas.
- **`⚡ Purgar Caché RAM (Standby)`**: Vacía las listas de memoria modificada y en espera (*Standby List*) del kernel de Windows 11. Es especialmente útil antes de iniciar un videojuego exigente o renderizar video, cuando Windows ha acumulado varios gigabytes en caché en espera. *(Requiere permisos de Administrador).*

---

## 6. Módulo de Procesador (CPU) y Caché del Sistema

Ubicado en la tarjeta central derecha del **Panel Principal**, combina el monitoreo del procesador con las herramientas de limpieza de almacenamiento y red:

### Elementos Visuales
- **Cabecera y Botón de Información (`ⓘ`)**: Incluye un botón compacto **`ⓘ`** a la derecha del título que, al hacer clic, despliega una tarjeta emergente con el nombre completo de tu procesador (leído desde el registro de hardware de Windows), el número de núcleos lógicos y la arquitectura.
- **Medidor Circular de CPU**: Porcentaje de uso global del procesador en tiempo real y número de núcleos lógicos detectados.
- **Gráfico de Historial CPU (60s)**: Evolución de la carga del procesador durante los últimos 60 segundos.

### Cuadrícula de Detalles de CPU y Caché
1. **Procesos**: Número total de procesos activos en ejecución en Windows.
2. **Hilos (Threads)**: Cantidad total de subprocesos activos en el sistema.
3. **Handles**: Total de identificadores de recursos abiertos por los procesos del sistema.
4. **Caché Temp/Pref.**: Tamaño total en megabytes (`MB`) ocupado actualmente por archivos temporales susceptibles de limpieza.

### Botones de Limpieza del Sistema
- **`🔍 Analizar Caché`**: Escanea las carpetas `%TEMP%`, `C:\Windows\Temp`, `C:\Windows\Prefetch` y la caché de miniaturas del Explorador (`thumbcache_*.db`) para calcular cuántos archivos temporales existen y cuánto espacio ocupan, **sin borrar ningún archivo**.
- **`🗑️ Limpiar Caché Temporal + Prefetch + DNS`**:
  - Elimina todos los archivos temporales huérfanos o inactivos en las rutas anteriores.
  - Omite silenciosamente cualquier archivo que esté bloqueado por una aplicación abierta.
  - Ejecuta `ipconfig /flushdns` (si está habilitado en Configuración) para limpiar la caché de resolución de nombres de dominio de internet.

---

## 7. Panel de Procesos con Mayor Uso de RAM

En la esquina inferior izquierda se muestra el listado en vivo de los **12 procesos que más memoria RAM física están consumiendo** en este instante:

- **Información por fila**:
  - **Nombre del proceso** (ej. `chrome`, `Code`, `explorer`).
  - **Identificador de proceso (`PID`)**.
  - **Consumo actual de RAM (`MB`)**.
- **Botón individual `Liberar`**: Si notas que una aplicación específica está acaparando demasiada memoria pero no deseas optimizar todo el sistema, haz clic en el botón **`Liberar`** de esa fila para recortar únicamente el *Working Set* de ese proceso.
- **Botón `🔄 Actualizar`**: La lista se refresca automáticamente cada 4 ciclos del monitor, pero puedes pulsar este botón para forzar una actualización inmediata.

---

## 8. Registro de Optimización y Eventos (Log)

En la esquina inferior derecha se encuentra la consola de eventos en tiempo real:

- Registra con marca de hora exacta (`[HH:mm:ss]`) cada acción realizada:
  - Inicio de la aplicación y estado de privilegios (Usuario o Administrador).
  - Cantidad de RAM liberada y número de procesos optimizados vs. procesos protegidos omitidos.
  - Resultados de escaneo y limpieza de caché en disco (archivos eliminados, archivos en uso omitidos y estado de la caché DNS).
  - Disparos automáticos del modo **Auto-Optimización**.
- **Botón `Limpiar Log`**: Vacía el historial visual de la consola de eventos.

---

## 9. Sección de Configuración y Temas de Color

Al hacer clic en el botón **`⚙️ Configuración`** de la barra superior, accederás al panel de personalización avanzada. Todos los cambios que realices aquí **se guardan automáticamente**.

### 🎨 1. Temas de Color y Apariencia Visual
Puedes cambiar la paleta de colores completa de la aplicación en tiempo real haciendo clic sobre cualquiera de las **8 tarjetas de tema**:

1. **Esmeralda Cyber *(Predeterminado)***: Estilo clásico oscuro inspirado en AVG Cleaner con acentos en verde neón (`#00E676`) y azul cian (`#00B0FF`).
2. **Océano Ártico *(Azul / Cian)***: Fondo azul marino profundo con acentos en turquesa eléctrico y azul zafiro.
3. **Amatista Neón *(Púrpura / Synth)***: Estética *synthwave* en tonos violeta oscuro con acentos púrpura neón y magenta.
4. **Carmesí Gaming *(Rojo / Gaming)***: Alto contraste en negro obsidiana con acentos en rojo rubí y naranja brillante.
5. **Solar Dorado *(Ámbar / Oro)***: Carbón titanio elegante con acentos en oro cálido y ámbar.
6. **Modo Claro Cristal *(Modo Claro)***: Interfaz luminosa estilo *Windows 11 Light* con tarjetas blancas, bordes suaves y acentos en verde esmeralda y azul real.
7. **Gris Oscuro *(Gris / Grafito)***: Tonos grafito y pizarra neutros con acentos en plata brillante y acero.
8. **Gris Claro *(Gris / Plata)***: Interfaz en tonos gris perla y aluminio suave diseñada para reducir el cansancio visual.

### 🧠 2. Automatización y Optimización de RAM
- **Liberar RAM automáticamente cuando supere el umbral**: Activa la vigilancia automática en segundo plano. Incluye un tiempo de enfriamiento (*cooldown*) de **60 segundos** entre limpiezas automáticas para no sobrecargar el procesador.
- **Umbral de disparo de uso de RAM (`Slider` de `65%` a `95%`)**:
  - `65% – 75%` (*Agresivo*): Mantiene el consumo de RAM lo más bajo posible en equipos con poca memoria.
  - `85%` (*Recomendado*): Equilibrio ideal para evitar que Windows empiece a usar intensivamente el disco de paginación.
  - `90% – 95%` (*Conservador*): Solo interviene en situaciones de saturación crítica.
- **Purgar Standby List al usar 'Optimizar Todo (1-Clic)'**: Permite elegir si el botón de 1-Clic también debe vaciar la caché *Standby* del kernel cuando se ejecuta como Administrador.

### ⚡ 3. Caché del Sistema y Monitoreo en Tiempo Real
- **Vaciar caché DNS (`ipconfig /flushdns`) al limpiar caché**: Activa o desactiva la limpieza de caché de red cuando limpias los archivos temporales.
- **Frecuencia de actualización de gráficos y telemetría**:
  - **`⚡ 0.5s (Rápido)`**: Actualiza medidores y gráficos dos veces por segundo para diagnóstico de alta precisión.
  - **`⏱️ 1.0s (Estándar)`**: Frecuencia predeterminada recomendada.
  - **`🍃 2.0s (Ahorro)`**: Reduce las lecturas a una vez cada 2 segundos para minimizar el uso de batería en portátiles.

### 💾 4. Persistencia y Botón `🔄 Restaurar Predeterminados`
- Tu configuración se almacena automáticamente en formato JSON dentro de tu carpeta de usuario local:
  `%LocalAppData%\WinOptimizerPro\settings.json`
- Si deseas volver al tema **Esmeralda Cyber** y a los valores de fábrica (`85%` de umbral, intervalo de `1.0s`, DNS y Standby activos), pulsa el botón **`🔄 Restaurar Predeterminados`** en la parte superior de Configuración.

---

## 10. Preguntas Frecuentes y Consejos de Uso (FAQ)

### ❓ ¿Se cerrará algún programa o perderé mis documentos abiertos al pulsar "Liberar RAM"?
**No.** La función `EmptyWorkingSet` nunca cierra procesos ni borra información activa. Únicamente le indica al administrador de memoria de Windows que mueva las páginas de memoria que llevan tiempo sin usarse fuera del conjunto de trabajo inmediato, dejando esa RAM física libre para las aplicaciones que sí estás utilizando ahora.

### ❓ ¿Por qué al limpiar la caché aparece que algunos archivos "en uso" fueron omitidos?
Es el comportamiento normal y seguro de Windows. Aplicaciones abiertas (como tu navegador, Discord, Visual Studio o servicios de Windows) mantienen abiertos ciertos archivos temporales mientras funcionan. **WinOptimizer** respeta esos bloqueos para garantizar que ningún programa abierto sufra errores.

### ❓ ¿Cuándo conviene usar "Purgar Caché RAM (Standby)"?
Windows 11 suele guardar en la *Standby List* archivos de programas que ya cerraste por si vuelves a abrirlos. En ocasiones, esta caché no se libera lo suficientemente rápido al abrir un juego pesado o software de edición, provocando pequeños tirones (*stuttering*). Purgar la *Standby List* como Administrador antes de iniciar una tarea pesada deja toda esa memoria inmediatamente libre.

