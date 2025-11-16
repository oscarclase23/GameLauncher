# GameLauncher – Kotlin Multiplatform App Launcher

GameLauncher es una aplicación de escritorio multiplataforma desarrollada con **Kotlin Multiplatform (KMP)** y **Compose Multiplatform**, diseñada para **escorear, catalogar y lanzar aplicaciones** instaladas en Windows y Linux. Permite además añadir aplicaciones personalizadas, ofreciendo una interfaz moderna, rápida y unificada.

---

## 🚀 Funcionalidades Principales

### 🔍 Escaneo Automático

* Detecta aplicaciones instaladas en:

    * **Windows**: ejecutables `.exe` en rutas estándar.
    * **Linux**: archivos `.desktop`, incluyendo Snap y Flatpak.
* Filtra automáticamente archivos irrelevantes (ej. *uninstall.exe*).
* Procesamiento asíncrono con coroutines para evitar bloqueos.

### 🖼️ Extracción de Iconos en Alta Calidad

* Obtención del icono nativo de cada app.
* Escalado a **64×64px** con interpolación bicúbica.
* Soporte multiplataforma mediante lógica adaptada a Windows y Linux.

### 🎨 Interfaz Moderna con Compose

* Tema oscuro, diseño limpio y responsivo.
* Búsqueda en tiempo real.
* Scrollbar personalizado.
* Notificaciones (SnackBar) para acciones clave: añadir, eliminar, errores, etc.

### ➕ Gestión de Aplicaciones

* Añadir apps manualmente (`.exe`, `.desktop`, binarios).
* Eliminar apps personalizadas.
* Lanzamiento nativo:

    * Windows: `cmd.exe /c start`.
    * Linux: ejecución directa de binarios o resolución desde `.desktop`.

---

## 🧩 Arquitectura (MVVM)

* **View**: Composables en `AppLauncherScreen.kt`.
* **ViewModel**: `LauncherViewModel.kt`, manejo del estado y eventos UI.
* **Model**:

    * `AppInfo.kt` – datos de la aplicación
    * `AppScanner.kt` – detección y parseo
    * `IconExtractor.kt` – extracción de iconos
    * `PlatformService.kt` – detección del SO

Coroutines y `Dispatchers.IO` permiten una UI fluida incluso durante operaciones intensivas.

---

## 📗 Manual de Usuario

### ▶ Opción 1 — Ejecutar desde el IDE (IntelliJ IDEA)

```
git clone https://github.com/oscarclase23/GameLauncher.git
```

* Abrir en IntelliJ.
  * Ejecutar:
      * **Linux:**  
      ```
        ./gradlew :composeApp:run
    ```
     * **Windows:** 
     ```
        .\gradlew.bat :composeApp:run
      ```

### 💾 Opción 2 — Instaladores Nativos (Releases)

Descarga desde:
👉 [https://github.com/oscarclase23/GameLauncher/releases/tag/v1.0.1](https://github.com/oscarclase23/GameLauncher/releases/tag/v1.0.1)

| SO                    | Instalador |
| --------------------- | ---------- |
| Windows               | `.msi`     |
| Linux (Debian/Ubuntu) | `.deb`     |

---

## 🧪 Pruebas Realizadas

El proyecto cuenta con pruebas manuales que verifican:

* Detección del sistema operativo.
* Escaneo inicial en Windows y Linux.
* Visualización de iconos.
* Lanzamiento de aplicaciones.
* Añadir y eliminar apps personalizadas.
* Búsqueda en tiempo real.
* Limpieza de filtros y refresco de la lista.

Todas las pruebas se superaron satisfactoriamente.

---

## 🏁 Conclusiones

* El proyecto demuestra la capacidad real de **KMP + Compose** para desarrollar aplicaciones de escritorio funcionales y modernas.
* Se logró una arquitectura sólida (MVVM) y una UI reactiva.
* El sistema funciona correctamente en Windows y Linux, gestionando desde apps nativas hasta paquetes Snap/Flatpak.

### ❗ Dificultades Destacadas

1. **Extracción de iconos en Windows**
   Solución: Escalado propio de alta calidad con interpolación bicúbica.

2. **Parseo y ejecución en Linux (.desktop)**
   Solución: Resolución avanzada de rutas y comandos, búsqueda en `$PATH`.

3. **Operaciones de E/S pesadas**
   Solución: Uso de coroutines con `Dispatchers.IO`.

---

## 🔗 Repositorio

[https://github.com/oscarclase23/GameLauncher](https://github.com/oscarclase23/GameLauncher)

---