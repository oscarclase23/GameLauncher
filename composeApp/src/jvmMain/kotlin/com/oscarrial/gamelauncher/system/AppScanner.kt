package com.oscarrial.gamelauncher.system

import com.oscarrial.gamelauncher.data.AppInfo
import java.io.File

/**
 * Servicio encargado de escanear aplicaciones del sistema.
 * Soporta Windows y Linux (incluyendo Snap y Flatpak).
 */
// Objeto singleton que contiene toda la lógica para buscar aplicaciones instaladas.
object AppScanner {

    private val USER_HOME = System.getProperty("user.home")
    private const val DEFAULT_FALLBACK_ICON = "❓"

    // ==================== RUTAS DE WINDOWS ====================

    // Rutas comunes para directorios de archivos de programa en Windows.
    private val WINDOWS_PROGRAM_FILES = listOf(
        "C:\\Program Files",
        "C:\\Program Files (x86)",
    )

    // Rutas de datos de aplicación (AppData) donde se instalan muchas aplicaciones modernas.
    private val WINDOWS_APP_DATA_PATHS = listOf(
        "$USER_HOME\\AppData\\Local\\Programs",
        "$USER_HOME\\AppData\\Local",
        "$USER_HOME\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs"
    )

    // Rutas del sistema operativo Windows para aplicaciones básicas.
    private val WINDOWS_SYSTEM_PATHS = listOf(
        "C:\\Windows\\System32",
        "C:\\Windows"
    )

    // Combinación de todas las rutas de programa y AppData para el escaneo principal.
    private val ALL_WINDOWS_PATHS = WINDOWS_PROGRAM_FILES + WINDOWS_APP_DATA_PATHS

    // Mapa de aplicaciones conocidas con posibles nombres de ejecutables para un escaneo más preciso.
    private val KNOWN_APPS = mapOf(
        "intellij" to listOf("idea64.exe", "idea.exe"),
        "androidstudio" to listOf("studio64.exe", "studio.exe"),
        "visualstudio" to listOf("devenv.exe"),
        "vscode" to listOf("code.exe"),
        "pycharm" to listOf("pycharm64.exe", "pycharm.exe"),
        "chrome" to listOf("chrome.exe"),
        "firefox" to listOf("firefox.exe"),
        "edge" to listOf("msedge.exe"),
        "discord" to listOf("discord.exe"),
        "slack" to listOf("slack.exe"),
        "teams" to listOf("teams.exe"),
        "steam" to listOf("steam.exe"),
        "notepad++" to listOf("notepad++.exe"),
        "7zip" to listOf("7zfm.exe"),
        "winrar" to listOf("winrar.exe"),
        "gimp" to listOf("gimp-2.10.exe", "gimp.exe"),
        "photoshop" to listOf("photoshop.exe"),
        "word" to listOf("winword.exe"),
        "excel" to listOf("excel.exe"),
        "powerpoint" to listOf("powerpnt.exe"),
        "outlook" to listOf("outlook.exe"),
        "notion" to listOf("notion.exe")
    )

    // Mapa de aplicaciones de sistema básicas de Windows y sus ejecutables.
    private val SYSTEM_APPS = mapOf(
        "Calculadora" to "calc.exe",
        "Paint" to "mspaint.exe",
        "Notepad" to "notepad.exe",
        "Explorador" to "explorer.exe",
        "CMD" to "cmd.exe",
        "PowerShell" to "powershell.exe"
    )

    // Lista de nombres de ejecutables que deben ser ignorados (instaladores, desinstaladores, etc.).
    private val IGNORED_NAMES = setOf(
        "unins000.exe", "uninstall.exe", "uninst.exe", "setup.exe", "install.exe",
        "updater.exe", "update.exe", "launcher.exe", "helper.exe", "crashhandler.exe",
        "agent.exe", "service.exe", "daemon.exe", "background.exe",
        "autorun.exe", "runtime.exe", "redist.exe", "prerequisite.exe"
    ).map { it.lowercase() }.toSet()

    // Palabras clave dentro de nombres de archivo que sugieren que un ejecutable debe ser ignorado.
    private val IGNORED_KEYWORDS = setOf(
        "uninstall", "setup", "install", "update", "updater", "crash", "redist",
        "helper", "maintenance", "prerequisite", "launcher", "bootstrapper"
    )

    // Nombres de carpetas comunes que deben ser excluidas del escaneo.
    private val IGNORED_FOLDERS = setOf(
        "common", "commonfiles", "shared", "lib", "libs", "library", "bin32",
        "data", "temp", "cache", "logs", "resources", "assets", "locales",
        "uninstall", "old", "backup", "system", "windows nt", "windowsapps"
    ).map { it.lowercase().replace(" ", "").replace("-", "") }.toSet()

    // ==================== RUTAS DE LINUX ====================

    // Rutas donde se encuentran los archivos .desktop (accesos directos a aplicaciones) en Linux.
    private val LINUX_APPLICATION_PATHS = listOf(
        // Rutas estándar
        "/usr/share/applications",
        "/usr/local/share/applications",
        "$USER_HOME/.local/share/applications",

        // Snap
        "/var/lib/snapd/desktop/applications",
        // Flatpak (Sistema)
        "/var/lib/flatpak/exports/share/applications",
        // Flatpak (Usuario)
        "$USER_HOME/.local/share/flatpak/exports/share/applications"
    )

    // Lista de nombres de ejecutables considerados esenciales que deben ser incluidos, incluso si el filtro inicial los podría descartar.
    private val ESSENTIAL_LINUX_NAMES = listOf(
        "nautilus", "firefox", "terminal", "settings", "calculator",
        "clock", "calendar", "maps", "photos", "evince",
        "totem", "rhythmbox", "screenshot", "gedit", "monitor",
        "disks", "konsole", "xfce4-terminal", "tilix", "software", "update",
        // ✅ AÑADIDO: Apps de desarrollo conocidas (para evitar filtros)
        "code", "vscode", "intellij", "androidstudio", "pycharm", "clion"
    ).map { it.lowercase() }

    // ==================== FUNCIÓN PRINCIPAL ====================

    // Función principal que determina el SO y delega el escaneo al método apropiado.
    fun scanSystemApps(): List<AppInfo> {
        val osName = System.getProperty("os.name", "").lowercase()

        return when {
            osName.contains("win") -> scanWindowsApps()
            osName.contains("nux") || osName.contains("nix") -> scanLinuxApps()
            else -> {
                println("⚠️ Sistema operativo no soportado: $osName")
                emptyList()
            }
        }
    }

    // ==================== WINDOWS SCANNING ====================

    // Lógica para escanear aplicaciones en Windows, combinando diferentes estrategias.
    private fun scanWindowsApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Windows...")

        // Escanea apps del sistema (ej: Notepad).
        foundApps.addAll(scanWindowsSystemApps())
        // Escanea apps conocidas con rutas específicas (ej: IDEs).
        foundApps.addAll(scanKnownApps())

        // Escaneo profundo de todos los directorios principales de instalación.
        for (basePath in ALL_WINDOWS_PATHS) {
            val baseDir = File(basePath)
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.forEach { entry ->
                    // Procesa cada subdirectorio si no está en la lista de carpetas ignoradas.
                    if (entry.isDirectory && !isIgnoredFolder(entry)) {
                        scanAppFolder(entry, foundApps, basePath in WINDOWS_PROGRAM_FILES)
                    }
                }
            }
        }

        println("✅ Escaneo completado. Se encontraron ${foundApps.size} aplicaciones.")

        // Limpia duplicados y ordena la lista final de aplicaciones.
        return foundApps
            .distinctBy { it.path.lowercase() }
            .groupBy { it.name }
            .mapValues { it.value.first() }
            .values
            .toList()
            .sortedBy { it.name }
    }

    // Busca las aplicaciones de sistema básicas de Windows.
    private fun scanWindowsSystemApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        for ((name, exeName) in SYSTEM_APPS) {
            for (basePath in WINDOWS_SYSTEM_PATHS) {
                val exeFile = File(basePath, exeName)
                if (exeFile.exists()) {
                    apps.add(createAppInfoFromWindowsFile(exeFile, name))
                    break
                }
            }
        }

        return apps
    }

    // Busca aplicaciones basándose en la lista predefinida de aplicaciones conocidas.
    private fun scanKnownApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        for ((appKey, exeNames) in KNOWN_APPS) {
            val foundApp = findKnownApp(appKey, exeNames)
            if (foundApp != null) {
                apps.add(foundApp)
            }
        }

        return apps
    }

    // Realiza una búsqueda heurística en carpetas para encontrar un ejecutable principal de una aplicación conocida.
    private fun findKnownApp(appKey: String, exeNames: List<String>): AppInfo? {
        for (basePath in ALL_WINDOWS_PATHS) {
            val baseDir = File(basePath)
            if (!baseDir.exists()) continue

            baseDir.listFiles()?.forEach { folder ->
                if (!folder.isDirectory) return@forEach

                val folderName = folder.name.lowercase().replace(" ", "").replace("-", "")
                if (folderName.contains(appKey) || appKey.contains(folderName)) {
                    val foundExe = searchInFolderRecursive(folder, exeNames, maxDepth = 3)
                    if (foundExe != null) {
                        return createAppInfoFromWindowsFile(foundExe, folder.name)
                    }
                }
            }
        }
        return null
    }

    // Búsqueda recursiva de un ejecutable específico dentro de una carpeta hasta una profundidad máxima.
    private fun searchInFolderRecursive(
        folder: File,
        exeNames: List<String>,
        maxDepth: Int,
        currentDepth: Int = 0
    ): File? {
        if (currentDepth > maxDepth) return null

        folder.listFiles()?.forEach { file ->
            if (file.isFile && exeNames.any { it.equals(file.name, ignoreCase = true) }) {
                return file
            }
            if (file.isDirectory && currentDepth < maxDepth) {
                val found = searchInFolderRecursive(file, exeNames, maxDepth, currentDepth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    // Verifica si una carpeta debe ser ignorada durante el escaneo.
    private fun isIgnoredFolder(folder: File): Boolean {
        val folderName = folder.name.lowercase().replace(" ", "").replace("-", "")

        if (folderName.length <= 2) return true
        if (IGNORED_FOLDERS.any { folderName.contains(it) }) return true
        if (folderName.startsWith("microsoft") || folderName.startsWith("windows")) return true
        if (folderName.contains("uninstall") || folderName.contains("setup")) return true

        return false
    }

    // Escanea una carpeta de aplicación para encontrar el ejecutable principal basándose en un sistema de puntuación.
    private fun scanAppFolder(
        appFolder: File,
        foundApps: MutableList<AppInfo>,
        isProgramFiles: Boolean
    ) {
        val appFolderName = appFolder.name.lowercase().replace(" ", "").replace("-", "")

        // Define directorios dentro de la carpeta a inspeccionar (ej: 'bin', 'app-x.x.x').
        val searchDirs = mutableListOf(appFolder)
        appFolder.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val dirName = dir.name.lowercase()
            when {
                dirName in setOf("bin", "app", "application") -> searchDirs.add(dir)
                dirName.startsWith("app-") && !isProgramFiles -> searchDirs.add(dir)
                dirName.matches(Regex("\\d+\\.\\d+.*")) -> searchDirs.add(dir)
            }
        }

        // Genera una lista de ejecutables candidatos, excluyendo los ignorados.
        val candidates = searchDirs.flatMap { dir ->
            dir.listFiles()?.filter {
                it.isFile &&
                        it.name.endsWith(".exe", true) &&
                        !isIgnoredExecutable(it)
            } ?: emptyList()
        }

        // Selecciona el ejecutable principal con el mejor nombre (más parecido al nombre de la carpeta).
        val mainExe = candidates.minByOrNull { file ->
            val exeName = file.nameWithoutExtension.lowercase().replace(" ", "").replace("-", "")

            val matchScore = when {
                exeName == appFolderName -> 0
                exeName.contains(appFolderName) || appFolderName.contains(exeName) -> 1
                else -> 10
            }

            val lengthScore = file.name.length / 10

            matchScore + lengthScore
        }

        // Crea el objeto AppInfo si se encuentra un ejecutable principal.
        mainExe?.let { file ->
            foundApps.add(createAppInfoFromWindowsFile(file, appFolder.name))
        }
    }

    // Verifica si un archivo ejecutable debe ser ignorado.
    private fun isIgnoredExecutable(file: File): Boolean {
        val fileName = file.name.lowercase()

        if (IGNORED_NAMES.contains(fileName)) return true

        val nameWithoutExt = file.nameWithoutExtension.lowercase()
        if (IGNORED_KEYWORDS.any { nameWithoutExt.contains(it) }) return true

        if (file.name.length > 50) return true

        return false
    }

    // Crea un objeto AppInfo para una aplicación de Windows, extrayendo el icono y formateando el nombre.
    private fun createAppInfoFromWindowsFile(file: File, folderName: String? = null): AppInfo {
        // Formatea el nombre a mayúsculas iniciales.
        val name = (folderName ?: file.nameWithoutExtension)
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }

        // Intenta extraer el icono del ejecutable.
        println("  🎨 Extrayendo icono de: ${file.name}")
        val iconBytes = try {
            // Se utiliza la lógica de IconExtractor para obtener los bytes del icono.
            IconExtractor.extractIconAsBytes(file.absolutePath, size = 64)
        } catch (e: Exception) {
            println("  ⚠️ Error extrayendo icono de ${file.name}: ${e.message}")
            null
        }

        if (iconBytes != null) {
            println("  ✅ Icono extraído correctamente (${iconBytes.size} bytes)")
        } else {
            println("  ⚠️ No se pudo extraer el icono, usando fallback")
        }

        return AppInfo(
            name = name,
            path = file.absolutePath,
            icon = DEFAULT_FALLBACK_ICON,
            description = "Aplicación de Windows",
            iconBytes = iconBytes
        )
    }

    // ==================== LINUX SCANNING ====================

    // Lógica para escanear aplicaciones en Linux buscando archivos .desktop.
    private fun scanLinuxApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Linux (incluyendo Snap/Flatpak)...")

        // Itera sobre las rutas comunes de archivos .desktop.
        for (appPath in LINUX_APPLICATION_PATHS) {
            val appDir = File(appPath)
            if (!appDir.exists() || !appDir.isDirectory) {
                println("  ⏭️ Ruta no encontrada: $appPath")
                continue
            }

            println("📂 Escaneando: $appPath")

            // Procesa cada archivo .desktop encontrado.
            appDir.listFiles()?.filter { it.name.endsWith(".desktop") }?.forEach { desktopFile ->
                try {
                    val appInfo = parseDesktopFile(desktopFile)
                    if (appInfo != null) {
                        foundApps.add(appInfo)
                    }
                } catch (e: Exception) {
                    println("  ⚠️ Error procesando ${desktopFile.name}: ${e.message}")
                }
            }
        }

        println("✅ Escaneo completado. Se encontraron ${foundApps.size} aplicaciones.")

        // Limpia duplicados y ordena la lista final.
        return foundApps
            .distinctBy { it.path.lowercase() }
            .sortedBy { it.name }
    }

    // Parsea un archivo .desktop para extraer los metadatos de la aplicación.
    private fun parseDesktopFile(desktopFile: File): AppInfo? {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null
        var comment: String? = null
        var isHidden = false
        var cleanExec: String? = null

        // Lee el archivo línea por línea para encontrar los campos clave.
        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("[Desktop Entry]") -> {}
                trimmedLine.startsWith("Name=") && name == null -> name = trimmedLine.substringAfter("Name=")
                trimmedLine.startsWith("Exec=") -> {
                    exec = trimmedLine.substringAfter("Exec=")
                    // Limpia el comando de ejecución de parámetros.
                    cleanExec = exec!!
                        .replace(Regex("%[a-zA-Z]"), "")
                        .trim()
                        .split(" ")
                        .firstOrNull()
                }
                trimmedLine.startsWith("Icon=") -> iconName = trimmedLine.substringAfter("Icon=")
                trimmedLine.startsWith("Comment=") -> comment = trimmedLine.substringAfter("Comment=")
                trimmedLine.startsWith("NoDisplay=") && trimmedLine.substringAfter("NoDisplay=").equals("true", ignoreCase = true) -> isHidden = true
                trimmedLine.startsWith("Hidden=") && trimmedLine.substringAfter("Hidden=").equals("true", ignoreCase = true) -> isHidden = true
            }
        }

        // Filtra archivos que no tienen nombre, ejecutable o están marcados como ocultos.
        if (name.isNullOrBlank() || cleanExec.isNullOrBlank() || isHidden) {
            return null
        }

        val normalizedExec = cleanExec!!.lowercase()
        val normalizedName = name!!.lowercase().replace(" ", "").replace("-", "")

        // 🔍 Evitar subprocesos irrelevantes (daemons, helpers, etc.)
        // Filtra comandos que parecen ser subprocesos o utilidades internas, a menos que sean esenciales.
        val isNotMainApp = normalizedExec.contains("handler") ||
                normalizedExec.contains("agent") ||
                normalizedExec.contains("daemon") ||
                normalizedExec.contains("crash")

        if (isNotMainApp && ESSENTIAL_LINUX_NAMES.none { normalizedExec.contains(it) || normalizedName.contains(it) }) {
            return null
        }

        // Resuelve la ruta del ejecutable (busca en PATH, rutas absolutas, etc.).
        val finalPath = resolveExecutablePath(cleanExec!!)


        if (finalPath.isNullOrBlank()) {
            println("  ⚠️ No se pudo resolver el ejecutable de $name (exec=$cleanExec)")
            return null
        }

        // 🎨 Cargar icono (mejorado)
        // Intenta cargar el icono utilizando el nombre del icono.
        val iconBytes = iconName?.let { IconExtractor.extractLinuxIcon(it) }

        if (iconBytes == null) {
            println("  ⚠️ Sin icono, usando fallback para: $name")
        } else {
            println("  ✅ Aplicación detectada: $name")
        }

        return AppInfo(
            name = name!!,
            path = finalPath,
            icon = DEFAULT_FALLBACK_ICON,
            description = comment ?: "Aplicación de Linux",
            iconBytes = iconBytes
        )
    }


    // Intenta encontrar la ruta absoluta de un ejecutable de Linux buscando en el PATH.
    private fun resolveExecutablePath(exec: String): String? {
        val file = File(exec)
        if (file.exists()) return file.absolutePath

        // 🔍 Si no empieza por /, buscamos en el PATH del sistema
        val pathDirs = System.getenv("PATH")?.split(":") ?: emptyList()
        for (dir in pathDirs) {
            val candidate = File(dir, exec)
            if (candidate.exists()) return candidate.absolutePath
        }

        // ⚙️ Si es un comando flatpak o snap, mantenemos el texto tal cual
        // Mantiene comandos especiales (ej: "flatpak run com.app.name") sin resolver a una ruta física.
        return when {
            exec.startsWith("flatpak") || exec.startsWith("snap") -> exec
            else -> null
        }
    }

    // Función de extensión para poner en mayúscula la primera letra de una cadena.
    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}