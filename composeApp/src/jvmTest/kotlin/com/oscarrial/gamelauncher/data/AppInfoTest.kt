package com.oscarrial.gamelauncher.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AppInfoTest {

    @Test
    fun `crear AppInfo con valores por defecto`() {
        val app = AppInfo(
            name = "Test App",
            path = "/usr/bin/test"
        )

        assertEquals("Test App", app.name)
        assertEquals("/usr/bin/test", app.path)
        assertEquals("❓", app.icon)
        assertEquals("", app.description)
        assertFalse(app.isCustom)
        assertEquals(null, app.iconBytes)
    }

    @Test
    fun `crear AppInfo personalizada con todos los campos`() {
        val iconBytes = byteArrayOf(0x01, 0x02, 0x03)
        val app = AppInfo(
            name = "Custom App",
            path = "C:\\Program Files\\Custom\\app.exe",
            icon = "🎮",
            description = "Aplicación personalizada",
            isCustom = true,
            iconBytes = iconBytes
        )

        assertEquals("Custom App", app.name)
        assertEquals("C:\\Program Files\\Custom\\app.exe", app.path)
        assertEquals("🎮", app.icon)
        assertEquals("Aplicación personalizada", app.description)
        assertTrue(app.isCustom)
        assertTrue(iconBytes.contentEquals(app.iconBytes))
    }

    @Test
    fun `equals devuelve true para apps idénticas`() {
        val iconBytes = byteArrayOf(0x01, 0x02, 0x03)

        val app1 = AppInfo(
            name = "App",
            path = "/usr/bin/app",
            icon = "🎮",
            description = "Test",
            isCustom = true,
            iconBytes = iconBytes
        )

        val app2 = AppInfo(
            name = "App",
            path = "/usr/bin/app",
            icon = "🎮",
            description = "Test",
            isCustom = true,
            iconBytes = iconBytes
        )

        assertEquals(app1, app2)
    }

    @Test
    fun `equals devuelve false para apps diferentes`() {
        val app1 = AppInfo(name = "App1", path = "/path1")
        val app2 = AppInfo(name = "App2", path = "/path2")

        assertNotEquals(app1, app2)
    }

    @Test
    fun `hashCode es consistente para apps iguales`() {
        val iconBytes = byteArrayOf(0x01, 0x02, 0x03)

        val app1 = AppInfo(
            name = "App",
            path = "/usr/bin/app",
            iconBytes = iconBytes
        )

        val app2 = AppInfo(
            name = "App",
            path = "/usr/bin/app",
            iconBytes = iconBytes
        )

        assertEquals(app1.hashCode(), app2.hashCode())
    }
}