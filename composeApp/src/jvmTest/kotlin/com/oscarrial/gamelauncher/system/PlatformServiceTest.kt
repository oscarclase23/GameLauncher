package com.oscarrial.gamelauncher.system

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformServiceTest {

    @Test
    fun `getCurrentOS devuelve un sistema operativo válido`() {
        val os = PlatformService.getCurrentOS()

        // Verifica que retorna alguno de los valores del enum
        assertTrue(
            os in listOf(
                OperatingSystem.Windows,
                OperatingSystem.Linux,
                OperatingSystem.MacOS,
                OperatingSystem.Other
            )
        )
    }

    @Test
    fun `getOsNameWithVersion devuelve un nombre no vacío`() {
        val osInfo = PlatformService.getOsNameWithVersion()

        assertNotNull(osInfo)
        assertTrue(osInfo.isNotEmpty())

        // Verifica que contiene información útil
        assertTrue(
            osInfo.contains("Windows") ||
                    osInfo.contains("Linux") ||
                    osInfo.contains("macOS") ||
                    osInfo.contains("Unknown")
        )
    }

    @Test
    fun `getOsNameWithVersion contiene versión`() {
        val osInfo = PlatformService.getOsNameWithVersion()

        // Verifica que incluye información de versión con paréntesis
        assertTrue(osInfo.contains("(") && osInfo.contains(")"))
    }
}
