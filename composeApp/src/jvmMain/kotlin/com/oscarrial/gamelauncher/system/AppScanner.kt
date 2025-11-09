package com.oscarrial.gamelauncher.system

import com.oscarrial.gamelauncher.data.AppInfo
import java.io.File

/**
 * Servicio encargado de escanear aplicaciones del sistema.
 * Soporta Windows y Linux.
 * @author Modificado para Linux, filtro de esenciales y corrección de sobrecarga.
 */
object AppScanner {

    private val USER_HOME = System.getProperty("user.home")
    private const val DEFAULT_FALLBACK_ICON = "🎮" // Icono único de fallback

    // ==================== RUTAS Y DATOS DE WINDOWS ====================

    private val WINDOWS_PROGRAM_FILES = listOf(
        "C:\\Program Files",
        "C:\\Program Files (x86)",
    )

    private val WINDOWS_APP_DATA_PATHS = listOf(
        "$USER_HOME\\AppData\\Local\\Programs",
        "$USER_HOME\\AppData\\Local",
        "$USER_HOME\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs"
    )

    private val WINDOWS_SYSTEM_PATHS = listOf(
        "C:\\Windows\\System32",
        "C:\\Windows"
    )

    private val ALL_WINDOWS_PATHS = WINDOWS_PROGRAM_FILES + WINDOWS_APP_DATA_PATHS

    private val KNOWN_APPS = mapOf(
        "intellij" to listOf("idea64.exe", "idea.exe"), "androidstudio" to listOf("studio64.exe", "studio.exe"),
        "visualstudio" to listOf("devenv.exe"), "vscode" to listOf("code.exe"), "pycharm" to listOf("pycharm64.exe", "pycharm.exe"),
        "chrome" to listOf("chrome.exe"), "firefox" to listOf("firefox.exe"), "edge" to listOf("msedge.exe"),
        "discord" to listOf("discord.exe"), "slack" to listOf("slack.exe"), "teams" to listOf("teams.exe"),
        "steam" to listOf("steam.exe"), "notepad++" to listOf("notepad++.exe"), "7zip" to listOf("7zfm.exe"),
        "winrar" to listOf("winrar.exe"), "gimp" to listOf("gimp-2.10.exe", "gimp.exe"), "photoshop" to listOf("photoshop.exe"),
        "word" to listOf("winword.exe"), "excel" to listOf("excel.exe"), "powerpoint" to listOf("powerpnt.exe"),
        "outlook" to listOf("outlook.exe"), "notion" to listOf("notion.exe")
    )

    private val SYSTEM_APPS = mapOf(
        "Calculadora" to "calc.exe", "Paint" to "mspaint.exe", "Notepad" to "notepad.exe",
        "Explorador" to "explorer.exe", "CMD" to "cmd.exe", "PowerShell" to "powershell.exe"
    )

    private val IGNORED_NAMES = setOf(
        "unins000.exe", "uninstall.exe", "uninst.exe", "setup.exe", "install.exe",
        "updater.exe", "update.exe", "launcher.exe", "helper.exe", "crashhandler.exe",
        "agent.exe", "service.exe", "daemon.exe", "background.exe",
        "autorun.exe", "runtime.exe", "redist.exe", "prerequisite.exe"
    ).map { it.lowercase() }.toSet()

    private val IGNORED_KEYWORDS = setOf(
        "uninstall", "setup", "install", "update", "updater", "crash", "redist",
        "helper", "maintenance", "prerequisite", "launcher", "bootstrapper"
    )

    private val IGNORED_FOLDERS = setOf(
        "common", "commonfiles", "shared", "lib", "libs", "library", "bin32",
        "data", "temp", "cache", "logs", "resources", "assets", "locales",
        "uninstall", "old", "backup", "system", "windows nt", "windowsapps"
    ).map { it.lowercase().replace(" ", "") }.toSet()


    // ==================== RUTAS Y NOMBRES DE LINUX ====================

    private val LINUX_APPLICATION_PATHS = listOf(
        "/usr/share/applications",
        "/usr/local/share/applications",
        "$USER_HOME/.local/share/applications",
    )

    // Lista de ejecutables/nombres esenciales para incluir las apps de sistema pedidas
    private val ESSENTIAL_LINUX_NAMES = listOf(
        "nautilus", "firefox", "terminal", "settings", "calculator",
        "clock", "calendar", "maps", "photos", "evince",
        "totem", "rhythmbox", "screenshot", "gedit", "monitor",
        "disks", "konsole", "xfce4-terminal", "tilix", "software", "update"
    ).map { it.lowercase() }


    // ==================== FUNCIÓN PÚBLICA PRINCIPAL ====================

    /**
     * Función principal que escanea aplicaciones según el sistema operativo detectado.
     */
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

    /**
     * Escanea todas las aplicaciones de Windows.
     */
    private fun scanWindowsApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Windows...")

        // 1. Aplicaciones del sistema (Calculadora, Paint, etc.)
        foundApps.addAll(scanWindowsSystemApps())

        // 2. Aplicaciones conocidas
        foundApps.addAll(scanKnownApps())

        // 3. Escanear carpetas normales
        for (basePath in ALL_WINDOWS_PATHS) {
            val baseDir = File(basePath)
            if (baseDir.exists() && baseDir.isDirectory) {
                baseDir.listFiles()?.forEach { entry ->
                    if (entry.isDirectory && !isIgnoredFolder(entry)) {
                        scanAppFolder(entry, foundApps, basePath in WINDOWS_PROGRAM_FILES)
                    }
                }
            }
        }

        println("✅ Escaneo completado. Se encontraron ${foundApps.size} aplicaciones.")

        return foundApps
            .distinctBy { it.path.lowercase() }
            .groupBy { it.name }
            .mapValues { it.value.first() }
            .values
            .toList()
            .sortedBy { it.name }
    }

    /**
     * Escanea las aplicaciones internas de Windows (Calculadora, Paint, etc.)
     * RENOMBRADA a scanWindowsSystemApps para solucionar el error de sobrecarga.
     */
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

    private fun isIgnoredFolder(folder: File): Boolean {
        val folderName = folder.name.lowercase().replace(" ", "").replace("-", "")

        if (folderName.length <= 2) return true
        if (IGNORED_FOLDERS.any { folderName.contains(it) }) return true
        if (folderName.startsWith("microsoft") || folderName.startsWith("windows")) return true
        if (folderName.contains("uninstall") || folderName.contains("setup")) return true

        return false
    }

    private fun scanAppFolder(
        appFolder: File,
        foundApps: MutableList<AppInfo>,
        isProgramFiles: Boolean
    ) {
        val appFolderName = appFolder.name.lowercase().replace(" ", "").replace("-", "")

        val searchDirs = mutableListOf(appFolder)
        appFolder.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val dirName = dir.name.lowercase()
            when {
                dirName in setOf("bin", "app", "application") -> searchDirs.add(dir)
                dirName.startsWith("app-") && !isProgramFiles -> searchDirs.add(dir)
                dirName.matches(Regex("\\d+\\.\\d+.*")) -> searchDirs.add(dir)
            }
        }

        val candidates = searchDirs.flatMap { dir ->
            dir.listFiles()?.filter {
                it.isFile &&
                        it.name.endsWith(".exe", true) &&
                        !isIgnoredExecutable(it)
            } ?: emptyList()
        }

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

        mainExe?.let { file ->
            foundApps.add(createAppInfoFromWindowsFile(file, appFolder.name))
        }
    }

    private fun isIgnoredExecutable(file: File): Boolean {
        val fileName = file.name.lowercase()

        if (IGNORED_NAMES.contains(fileName)) return true

        val nameWithoutExt = file.nameWithoutExtension.lowercase()
        if (IGNORED_KEYWORDS.any { nameWithoutExt.contains(it) }) return true

        if (file.name.length > 50) return true

        return false
    }


    /**
     * Crea un AppInfo desde un archivo ejecutable de Windows.
     */
    private fun createAppInfoFromWindowsFile(file: File, folderName: String? = null): AppInfo {
        val name = (folderName ?: file.nameWithoutExtension)
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }

        println("  🎨 Extrayendo icono de: ${file.name}")
        val iconBytes = try {
            IconExtractor.extractIconAsBytes(file.absolutePath, size = 64) // TAMAÑO 64px
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

    /**
     * Escanea aplicaciones de Linux analizando archivos .desktop,
     * filtrando solo apps útiles con icono.
     */
    private fun scanLinuxApps(): List<AppInfo> {
        val foundApps = mutableListOf<AppInfo>()

        println("🔍 Iniciando escaneo de aplicaciones de Linux...")

        for (appPath in LINUX_APPLICATION_PATHS) {
            val appDir = File(appPath)
            if (!appDir.exists() || !appDir.isDirectory) continue

            println("📂 Escaneando: $appPath")

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

        return foundApps
            .distinctBy { it.path.lowercase() }
            .sortedBy { it.name }
    }

    /**
     * Parsea un archivo .desktop de Linux con filtros relajados y verificación de iconos.
     */
    private fun parseDesktopFile(desktopFile: File): AppInfo? {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null
        var comment: String? = null
        var isHidden = false
        var cleanExec: String? = null

        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()
            when {
                // ... (Lógica de parsing de las líneas) ...
                trimmedLine.startsWith("Name=") && name == null -> name = trimmedLine.substringAfter("Name=")
                trimmedLine.startsWith("Exec=") -> {
                    exec = trimmedLine.substringAfter("Exec=")
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

        if (name.isNullOrBlank() || cleanExec.isNullOrBlank() || isHidden) {
            return null
        }

        // ... (Lógica de filtrado de agentes y triviales) ...
        val normalizedExec = cleanExec!!.lowercase()
        val normalizedName = name!!.lowercase().replace(" ", "").replace("-", "")

        val isNotMainApp = normalizedExec.contains("handler", ignoreCase = true) ||
                normalizedExec.contains("agent", ignoreCase = true) ||
                normalizedExec.contains("daemon", ignoreCase = true) ||
                normalizedExec.contains("crash", ignoreCase = true)

        if (isNotMainApp) {
            if (!ESSENTIAL_LINUX_NAMES.any { normalizedExec.contains(it) || normalizedName.contains(it) }) {
                return null
            }
        }

        // CORRECCIÓN: Si resolveExecutablePath falla, es NULL y descartamos (return null)
        // Si resolveExecutablePath tiene éxito, la ruta es absoluta
        val executablePath = resolveExecutablePath(cleanExec!!)
        if (executablePath.isNullOrBlank()) {
            println("  🚫 No se pudo resolver la ruta absoluta para: $name. Descartando.")
            return null
        }

        // --- Extracción y Filtrado por Icono ---

        val iconBytes = if (!iconName.isNullOrBlank()) {
            IconExtractor.extractLinuxIcon(iconName!!)
        } else {
            null
        }

        // FILTRADO DE APPS SIN ICONO: Si no hay icono, ignorar la app
        if (iconBytes == null) {
            println("  🚫 Aplicación sin icono ignorada: $name")
            return null
        }

        println("  ✅ Aplicación encontrada: $name")

        return AppInfo(
            name = name!!,
            path = executablePath, // AHORA ESTA RUTA DEBE SER ABSOLUTA
            icon = DEFAULT_FALLBACK_ICON,
            description = comment ?: "Aplicación de Linux",
            iconBytes = iconBytes
        )
    }

    /**
     * Resuelve la ruta completa y canónica de un ejecutable de Linux buscando en el PATH.
     * Retorna NULL si no puede resolver una ruta ejecutable existente.
     */
    private fun resolveExecutablePath(command: String): String? {
        // Lista de ubicaciones a verificar, incluyendo el comando directo si es una ruta absoluta
        val searchFiles = mutableListOf<File>()

        if (command.startsWith("/")) {
            // Si es una ruta absoluta, se añade como primer candidato
            searchFiles.add(File(command))
        }

        // Buscar en el PATH si es solo un comando (ej: "vim")
        val pathEnv = System.getenv("PATH") ?: ""
        for (path in pathEnv.split(":")) {
            searchFiles.add(File(path, command))
        }

        for (file in searchFiles) {
            if (file.exists() && file.canExecute()) {
                return try {
                    // Intentar obtener la ruta canónica para resolver symlinks (como vim -> /usr/bin/vim.basic)
                    file.canonicalPath
                } catch (e: Exception) {
                    // Si falla (IOException al resolver), volvemos a la ruta absoluta
                    file.absolutePath
                }
            }
        }

        // Si no se pudo encontrar o resolver una ruta ejecutable existente, descartamos.
        return null
    }

    // ==================== HELPERS ====================

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}