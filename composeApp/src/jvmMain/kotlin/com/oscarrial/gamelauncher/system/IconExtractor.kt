package com.oscarrial.gamelauncher.system

import sun.awt.shell.ShellFolder
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.filechooser.FileSystemView
import java.awt.Graphics2D
import java.awt.RenderingHints
//import sun.awt.shell.ShellFolder

/**
 * Extrae iconos de alta calidad de ejecutables de Windows (.exe)
 * Utiliza múltiples estrategias para obtener la mejor resolución posible
 */
object IconExtractor {

    /**
     * Extrae el icono de mayor calidad posible de un ejecutable de Windows.
     *
     * Estrategia multi-nivel:
     * 1. Intenta extraer el icono de 256x256 usando ShellFolder (solo Windows)
     * 2. Si falla, usa FileSystemView (16x16 o 32x32)
     * 3. Escala con máxima calidad usando interpolación bicúbica
     *
     * @param exePath Ruta completa al archivo .exe
     * @param size Tamaño deseado del icono (recomendado: 128)
     * @return BufferedImage con el icono de alta calidad, o null si falla
     */
    fun extractIcon(exePath: String, size: Int = 128): BufferedImage? {
        return try {
            val file = File(exePath)
            if (!file.exists() || !file.name.endsWith(".exe", ignoreCase = true)) {
                return null
            }

            // Estrategia 1: Intentar extraer icono de alta resolución con ShellFolder
            val highResIcon = extractHighResolutionIcon(file)

            if (highResIcon != null && highResIcon.width >= 48) {
                // Tenemos un icono de buena calidad, escalarlo al tamaño deseado
                return scaleImageWithQuality(highResIcon, size, size)
            }

            // Estrategia 2: Usar FileSystemView como fallback
            val systemIcon = FileSystemView.getFileSystemView().getSystemIcon(file)
            val iconImage = when (systemIcon) {
                is ImageIcon -> systemIcon.image
                else -> iconToImage(systemIcon)
            }

            // Escalar con máxima calidad
            val targetSize = (size * 0.4).toInt().coerceAtLeast(20)
            scaleImageWithQuality(iconImage, targetSize, targetSize)
            //scaleImageWithQuality(iconImage, size, size)

        } catch (e: Exception) {
            println("Error extrayendo icono de $exePath: ${e.message}")
            null
        }
    }

    private fun extractHighResolutionIcon(file: File): BufferedImage? {
        return try {
            val icon = FileSystemView.getFileSystemView().getSystemIcon(file)
            val image = when (icon) {
                is ImageIcon -> icon.image
                else -> iconToImage(icon)
            }
            imageToBufferedImage(image)
        } catch (e: Exception) {
            null
        }
    }


    /**
     * Convierte un Icon de Swing a Image
     */
    private fun iconToImage(icon: javax.swing.Icon): Image {
        val bufferedImage = BufferedImage(
            icon.iconWidth,
            icon.iconHeight,
            BufferedImage.TYPE_INT_ARGB
        )
        val g = bufferedImage.createGraphics()

        // Aplicar hints de calidad antes de renderizar
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        icon.paintIcon(null, g, 0, 0)
        g.dispose()

        return bufferedImage
    }

    /**
     * Convierte un Image a BufferedImage
     */
    private fun imageToBufferedImage(image: Image): BufferedImage {
        if (image is BufferedImage) {
            return image
        }

        val width = image.getWidth(null)
        val height = image.getHeight(null)

        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = bufferedImage.createGraphics()

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        g.drawImage(image, 0, 0, null)
        g.dispose()

        return bufferedImage
    }

    /**
     * Escala una imagen con la máxima calidad posible usando interpolación bicúbica
     * y múltiples renderizado hints.
     *
     * Utiliza una técnica de escalado progresivo para minimizar la pixelación:
     * - Si la imagen es más pequeña que el tamaño deseado, la escala directamente
     * - Si es mucho más grande, hace múltiples escalados intermedios para mejor calidad
     */
    private fun scaleImageWithQuality(source: Image, targetWidth: Int, targetHeight: Int): BufferedImage {
        val sourceWidth = source.getWidth(null)
        val sourceHeight = source.getHeight(null)

        // Si el tamaño ya es correcto, solo convertir a BufferedImage
        if (sourceWidth == targetWidth && sourceHeight == targetHeight) {
            return imageToBufferedImage(source)
        }

        // Crear la imagen final
        val scaledImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d: Graphics2D = scaledImage.createGraphics()

        // Configurar todos los hints de máxima calidad
        g2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        g2d.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_ON
        )

        // Escalado progresivo si es necesario (reduce artefactos)
        if (sourceWidth > targetWidth * 2 || sourceHeight > targetHeight * 2) {
            // Imagen muy grande, usar escalado progresivo
            var currentImage = imageToBufferedImage(source)
            var currentWidth = sourceWidth
            var currentHeight = sourceHeight

            // Escalar en pasos del 50% hasta acercarnos al tamaño objetivo
            while (currentWidth / 2 >= targetWidth || currentHeight / 2 >= targetHeight) {
                currentWidth /= 2
                currentHeight /= 2

                val tempImage = BufferedImage(currentWidth, currentHeight, BufferedImage.TYPE_INT_ARGB)
                val tempG2d = tempImage.createGraphics()

                tempG2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                tempG2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                tempG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                tempG2d.drawImage(currentImage, 0, 0, currentWidth, currentHeight, null)
                tempG2d.dispose()

                currentImage = tempImage
            }

            // Escalado final al tamaño exacto
            g2d.drawImage(currentImage, 0, 0, targetWidth, targetHeight, null)
        } else {
            // Escalado directo para imágenes pequeñas o cercanas al tamaño objetivo
            g2d.drawImage(source, 0, 0, targetWidth, targetHeight, null)
        }

        g2d.dispose()
        return scaledImage
    }

    /**
     * Extrae el icono como ByteArray para usar directamente en Compose.
     * @param exePath Ruta al ejecutable
     * @param size Tamaño objetivo (recomendado: 128 para balance calidad/rendimiento)
     * @return ByteArray con la imagen PNG, o null si falla
     */
    fun extractIconAsBytes(exePath: String, size: Int = 128): ByteArray? {
        val image = extractIcon(exePath, size) ?: return null

        return try {
            val outputStream = java.io.ByteArrayOutputStream()
            // Usar PNG con compresión para mejor calidad
            ImageIO.write(image, "png", outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            println("Error convirtiendo icono a bytes: ${e.message}")
            null
        }
    }
}