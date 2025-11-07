package com.oscarrial.gamelauncher.system

import java.awt.image.BufferedImage
import java.io.File
import javax.swing.filechooser.FileSystemView
import javax.swing.Icon
import java.awt.Graphics2D
import java.awt.RenderingHints

/**
 * Extrae el icono de un ejecutable de Windows (.exe)
 */
object IconExtractor {

    /**
     * Extrae el icono de un archivo ejecutable y lo devuelve como BufferedImage.
     * @param exePath Ruta completa al archivo .exe
     * @param size Tamaño deseado del icono (por defecto 48x48)
     * @return BufferedImage con el icono, o null si no se pudo extraer
     */
    fun extractIcon(exePath: String, size: Int = 48): BufferedImage? {
        return try {
            val file = File(exePath)
            if (!file.exists() || !file.name.endsWith(".exe", ignoreCase = true)) {
                return null
            }

            // Usar FileSystemView para obtener el icono del sistema
            val fileSystemView = FileSystemView.getFileSystemView()
            val icon: Icon = fileSystemView.getSystemIcon(file)

            // Convertir Icon a BufferedImage
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            val g2d: Graphics2D = image.createGraphics()

            // Aplicar antialiasing para mejor calidad
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Dibujar el icono escalado al tamaño deseado
            icon.paintIcon(null, g2d, 0, 0)
            g2d.dispose()

            image
        } catch (e: Exception) {
            println("Error extrayendo icono de $exePath: ${e.message}")
            null
        }
    }

    /**
     * Extrae el icono y lo guarda en un archivo temporal para su uso en Compose.
     * @return Ruta al archivo temporal con el icono, o null si falló
     */
    fun extractIconToTempFile(exePath: String, size: Int = 48): String? {
        val image = extractIcon(exePath, size) ?: return null

        return try {
            // Crear archivo temporal
            val tempFile = File.createTempFile("icon_", ".png")
            tempFile.deleteOnExit()

            // Guardar la imagen
            javax.imageio.ImageIO.write(image, "png", tempFile)
            tempFile.absolutePath
        } catch (e: Exception) {
            println("Error guardando icono temporal: ${e.message}")
            null
        }
    }

    /**
     * Extrae el icono como ByteArray para usarlo directamente en Compose
     */
    fun extractIconAsBytes(exePath: String, size: Int = 48): ByteArray? {
        val image = extractIcon(exePath, size) ?: return null

        return try {
            val outputStream = java.io.ByteArrayOutputStream()
            javax.imageio.ImageIO.write(image, "png", outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            println("Error convirtiendo icono a bytes: ${e.message}")
            null
        }
    }
}