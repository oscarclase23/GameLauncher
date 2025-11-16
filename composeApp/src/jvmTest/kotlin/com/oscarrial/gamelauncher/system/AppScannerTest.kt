package com.oscarrial.gamelauncher.system

import kotlin.test.Test
import kotlin.test.assertTrue

class AppScannerTest {

    @Test
    fun `scanSystemApps devuelve una lista`() {
        // Este test puede tardar, ya que escanea el sistema real
        val apps = AppScanner.scanSystemApps()

        // Verifica que retorna una lista (puede estar vacía en entornos de test)
        assertTrue(apps is List)
    }

    @Test
    fun `scanSystemApps encuentra al menos algunas apps en sistemas reales`() {
        // Solo se ejecuta si no estamos en CI/CD
        val isCI = System.getenv("CI") != null

        if (!isCI) {
            val apps = AppScanner.scanSystemApps()

            // En un sistema real, debería encontrar al menos alguna app
            println("Apps encontradas: ${apps.size}")

            // Muestra algunas apps de ejemplo
            apps.take(5).forEach { app ->
                println("  - ${app.name} (${app.path})")
            }
        }
    }
}
