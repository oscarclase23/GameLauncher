package com.oscarrial.gamelauncher.viewmodel

import com.oscarrial.gamelauncher.data.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LauncherViewModelTest {

    @Test
    fun `estado inicial del ViewModel`() {
        val viewModel = LauncherViewModel()

        // Verifica que la búsqueda está vacía al inicio
        assertEquals("", viewModel.searchQuery)

        // Verifica que hay información del OS
        assertTrue(viewModel.currentOSInfo.isNotEmpty())
    }

    @Test
    fun `updateSearchQuery actualiza la búsqueda`() {
        val viewModel = LauncherViewModel()

        viewModel.updateSearchQuery("test")
        assertEquals("test", viewModel.searchQuery)

        viewModel.updateSearchQuery("")
        assertEquals("", viewModel.searchQuery)
    }

    @Test
    fun `addApp añade aplicación personalizada`() {
        val viewModel = LauncherViewModel()

        // Esperar a que termine la carga inicial del escaneo
        Thread.sleep(1000)

        val initialCount = viewModel.apps.size

        val customApp = AppInfo(
            name = "Test App",
            path = "/usr/bin/test",
            isCustom = true
        )

        viewModel.addApp(customApp)

        assertEquals(initialCount + 1, viewModel.apps.size)
        assertTrue(viewModel.apps.contains(customApp))
    }

    @Test
    fun `addApp no añade duplicados`() {
        val viewModel = LauncherViewModel()

        // Esperar a que termine la carga inicial
        Thread.sleep(1000)

        val customApp = AppInfo(
            name = "Test App",
            path = "/usr/bin/test",
            isCustom = true
        )

        viewModel.addApp(customApp)
        val countAfterFirst = viewModel.apps.size

        // Intentar añadir la misma app
        viewModel.addApp(customApp)
        val countAfterSecond = viewModel.apps.size

        assertEquals(countAfterFirst, countAfterSecond)
    }

    @Test
    fun `removeApp elimina aplicación personalizada`() {
        val viewModel = LauncherViewModel()

        // Esperar a que termine la carga inicial
        Thread.sleep(1000)

        val customApp = AppInfo(
            name = "Test App",
            path = "/usr/bin/test",
            isCustom = true
        )

        viewModel.addApp(customApp)
        assertTrue(viewModel.apps.contains(customApp))

        viewModel.removeApp(customApp)
        assertFalse(viewModel.apps.contains(customApp))
    }

    @Test
    fun `filteredApps filtra correctamente`() {
        val viewModel = LauncherViewModel()

        // Esperar a que termine la carga inicial
        Thread.sleep(1000)

        // Añadir apps de prueba
        val app1 = AppInfo(name = "Firefox", path = "/usr/bin/firefox", isCustom = true)
        val app2 = AppInfo(name = "Chrome", path = "/usr/bin/chrome", isCustom = true)
        val app3 = AppInfo(name = "Code", path = "/usr/bin/code", isCustom = true)

        viewModel.addApp(app1)
        viewModel.addApp(app2)
        viewModel.addApp(app3)

        // Buscar "fire"
        viewModel.updateSearchQuery("fire")
        val filtered = viewModel.filteredApps

        assertTrue(filtered.any { it.name.contains("Fire", ignoreCase = true) })
        assertTrue(filtered.none { it.name == "Chrome" })
    }
}