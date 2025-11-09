package com.oscarrial.gamelauncher.system

import com.oscarrial.gamelauncher.data.AppInfo
import java.io.File

/**
 * Servicio encargado de escanear aplicaciones del sistema.
 * Soporta Windows y Linux.
 */
object AppScanner {

    private val USER_HOME = System.getProperty("user.home")

    // ==================== WINDOWS ====================

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
        // IDEs y Desarrollo
        "intellij" to listOf("idea64.exe", "idea.exe"),
        "androidstudio" to listOf("studio64.exe", "studio.exe"),
        "visualstudio" to listOf("devenv.exe"),
        "vscode" to listOf("code.exe"),
        "pycharm" to listOf("pycharm64.exe", "pycharm.exe"),
        "eclipse" to listOf("eclipse.exe"),
        "netbeans" to listOf("netbeans64.exe", "netbeans.exe"),

        // Navegadores
        "chrome" to listOf("chrome.exe"),
        "firefox" to listOf("firefox.exe"),
        "edge" to listOf("msedge.exe"),
        "brave" to listOf("brave.exe"),
        "opera" to listOf("opera.exe"),

        // Comunicación
        "discord" to listOf("discord.exe"),
        "slack" to listOf("slack.exe"),
        "teams" to listOf("teams.exe"),
        "telegram" to listOf("telegram.exe"),
        "whatsapp" to listOf("whatsapp.exe"),
        "zoom" to listOf("zoom.exe"),

        // Multimedia
        "spotify" to listOf("spotify.exe"),
        "vlc" to listOf("vlc.exe"),
        "obs" to listOf("obs64.exe", "obs.exe"),
        "audacity" to listOf("audacity.exe"),
        "itunes" to listOf("itunes.exe"),

        // Gaming
        "steam" to listOf("steam.exe"),
        "epicgames" to listOf("epicgameslauncher.exe"),
        "origin" to listOf("origin.exe"),
        "gog" to listOf("galaxyclient.exe"),
        "uplay" to listOf("upc.exe"),
        "battlenet" to listOf("battle.net.exe"),

        // Herramientas
        "notepad++" to listOf("notepad++.exe"),
        "7zip" to listOf("7zfm.exe"),
        "winrar" to listOf("winrar.exe"),
        "gimp" to listOf("gimp-2.10.exe", "gimp.exe"),
        "photoshop" to listOf("photoshop.exe"),
        "virtualbox" to listOf("virtualbox.exe"),
        "vmware" to listOf("vmware.exe"),
        "docker" to listOf("docker desktop.exe"),
        "postman" to listOf("postman.exe"),
        "filezilla" to listOf("filezilla.exe"),

        // Ofimática
        "word" to listOf("winword.exe"),
        "excel" to listOf("excel.exe"),
        "powerpoint" to listOf("powerpnt.exe"),
        "outlook" to listOf("outlook.exe"),
        "onenote" to listOf("onenote.exe"),
        "notion" to listOf("notion.exe"),
        "obsidian" to listOf("obsidian.exe"),

        // Educativas
        "pseint" to listOf("pseint.exe"),
        "dia" to listOf("dia.exe"),
        "matlab" to listOf("matlab.exe")
    )

    private val SYSTEM_APPS = mapOf(
        "Calculadora" to "calc.exe",
        "Paint" to "mspaint.exe",
        "Notepad" to "notepad.exe",
        "WordPad" to "wordpad.exe",
        "Explorador" to "explorer.exe",
        "CMD" to "cmd.exe",
        "PowerShell" to "powershell.exe"
    )

    private val IGNORED_NAMES = setOf(
        "unins000.exe", "uninstall.exe", "uninst.exe", "setup.exe", "install.exe",
        "updater.exe", "update.exe", "launcher.exe", "helper.exe", "crashhandler.exe",
        "crashpad_handler.exe", "maintenance.exe", "maintenancetool.exe",
        "vc_redist.x64.exe", "vc_redist.x86.exe", "vcredist_x64.exe", "vcredist_x86.exe",
        "dxsetup.exe", "directx.exe", "physx.exe",
        "cefsharp.browsersubprocess.exe", "chromesetup.exe", "firefoxsetup.exe",
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

    // ==================== LINUX ====================

    private val LINUX_APPLICATION_PATHS = listOf(
        "/usr/share/applications",
        "/usr/local/share/applications",
        "$USER_HOME/.local/share/applications",
        "/var/lib/flatpak/exports/share/applications",
        "$USER_HOME/.local/share/flatpak/exports/share/applications"
    )

    // ==================== FUNCIÓN PÚBLICA PRINCIPAL ====================

    /**
     * Función principal que escanea aplicaciones según el sistema operativo detectado.
     * Esta es la función pública que debe ser llamada desde fuera del objeto.
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
        println("📂 Escaneando aplicaciones del sistema...")
        foundApps.addAll(scanWindowsSystemApps())

        // 2. Aplicaciones conocidas
        println("🎯 Escaneando aplicaciones conocidas...")
        foundApps.addAll(scanKnownApps())

        // 3. Escanear carpetas normales
        println("📁 Escaneando carpetas de programas...")
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
     * RENOMBRADA para evitar conflicto de sobrecarga.
     */
    private fun scanWindowsSystemApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()

        for ((name, exeName) in SYSTEM_APPS) {
            for (basePath in WINDOWS_SYSTEM_PATHS) {
                val exeFile = File(basePath, exeName)
                if (exeFile.exists()) {
                    apps.add(createAppInfoFromFile(exeFile, name))
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
                        return createAppInfoFromFile(foundExe, folder.name)
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
            foundApps.add(createAppInfoFromFile(file, appFolder.name))
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
    private fun createAppInfoFromFile(file: File, folderName: String? = null): AppInfo {
        val name = (folderName ?: file.nameWithoutExtension)
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.capitalize() }

        println("  🎨 Extrayendo icono de: ${file.name}")
        val iconBytes = try {
            IconExtractor.extractIconAsBytes(file.absolutePath, size = 128)
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
            icon = getIconForApp(name),
            description = "Aplicación de Windows",
            iconBytes = iconBytes
        )
    }

    private fun getIconForApp(name: String): String {
        val normalized = name.lowercase().replace(" ", "")

        return when {
            normalized.contains("intellij") || normalized.contains("idea") -> "💻"
            normalized.contains("visualstudio") || normalized.contains("studio") -> "💻"
            normalized.contains("vscode") || normalized.contains("code") -> "💻"
            normalized.contains("pycharm") -> "🐍"
            normalized.contains("android") -> "🤖"
            normalized.contains("chrome") -> "🌐"
            normalized.contains("firefox") -> "🦊"
            normalized.contains("edge") -> "🌐"
            normalized.contains("brave") -> "🦁"
            normalized.contains("discord") -> "💬"
            normalized.contains("slack") -> "💼"
            normalized.contains("teams") -> "👥"
            normalized.contains("telegram") -> "✈️"
            normalized.contains("whatsapp") -> "📱"
            normalized.contains("spotify") -> "🎵"
            normalized.contains("vlc") -> "🎬"
            normalized.contains("obs") -> "🎥"
            normalized.contains("steam") -> "🎮"
            normalized.contains("epic") -> "🎮"
            normalized.contains("origin") -> "🎮"
            normalized.contains("gog") -> "🎮"
            normalized.contains("notepad") -> "📝"
            normalized.contains("7zip") || normalized.contains("winrar") -> "📦"
            normalized.contains("gimp") || normalized.contains("photoshop") -> "🎨"
            normalized.contains("virtualbox") || normalized.contains("vmware") -> "🖥️"
            normalized.contains("calc") -> "🔢"
            normalized.contains("paint") -> "🎨"
            normalized.contains("cmd") || normalized.contains("powershell") -> "⌨️"
            normalized.contains("pseint") -> "📊"
            normalized.contains("dia") -> "📐"
            else -> "✨"
        }
    }

    // ==================== LINUX SCANNING ====================

    /**
     * Escanea aplicaciones de Linux analizando archivos .desktop
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
     * Parsea un archivo .desktop de Linux
     */
    private fun parseDesktopFile(desktopFile: File): AppInfo? {
        var name: String? = null
        var exec: String? = null
        var iconName: String? = null
        var comment: String? = null

        desktopFile.readLines().forEach { line ->
            val trimmedLine = line.trim()

            when {
                trimmedLine.startsWith("Name=") && name == null -> {
                    name = trimmedLine.substringAfter("Name=")
                }
                trimmedLine.startsWith("Exec=") -> {
                    exec = trimmedLine.substringAfter("Exec=")
                }
                trimmedLine.startsWith("Icon=") -> {
                    iconName = trimmedLine.substringAfter("Icon=")
                }
                trimmedLine.startsWith("Comment=") -> {
                    comment = trimmedLine.substringAfter("Comment=")
                }
            }
        }

        if (name.isNullOrBlank() || exec.isNullOrBlank()) {
            return null
        }

        val cleanExec = exec!!
            .replace(Regex("%[a-zA-Z]"), "")
            .trim()
            .split(" ")
            .firstOrNull() ?: return null

        val executablePath = resolveExecutablePath(cleanExec)
        if (executablePath == null || !File(executablePath).exists()) {
            return null
        }

        val iconBytes = if (!iconName.isNullOrBlank()) {
            IconExtractor.extractLinuxIcon(iconName!!)
        } else {
            null
        }

        println("  ✅ Aplicación encontrada: $name")

        return AppInfo(
            name = name!!,
            path = executablePath,
            icon = "🎮",
            description = comment ?: "Aplicación de Linux",
            iconBytes = iconBytes
        )
    }

    /**
     * Resuelve la ruta completa de un ejecutable de Linux
     */
    private fun resolveExecutablePath(command: String): String? {
        if (command.startsWith("/")) {
            return if (File(command).exists()) command else null
        }

        val pathEnv = System.getenv("PATH") ?: return null
        val paths = pathEnv.split(":")

        for (path in paths) {
            val executable = File(path, command)
            if (executable.exists() && executable.canExecute()) {
                return executable.absolutePath
            }
        }

        return null
    }

    // ==================== HELPERS ====================

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}