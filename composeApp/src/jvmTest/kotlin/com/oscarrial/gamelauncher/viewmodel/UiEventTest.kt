package com.oscarrial.gamelauncher.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UiEventTest {

    @Test
    fun `crear UiEvent de éxito`() {
        val event = UiEvent(
            message = "Aplicación añadida correctamente",
            isError = false
        )

        assertEquals("Aplicación añadida correctamente", event.message)
        assertFalse(event.isError)
    }

    @Test
    fun `crear UiEvent de error`() {
        val event = UiEvent(
            message = "Error al lanzar la aplicación",
            isError = true
        )

        assertEquals("Error al lanzar la aplicación", event.message)
        assertTrue(event.isError)
    }

    @Test
    fun `UiEvent usa valor por defecto para isError`() {
        val event = UiEvent(message = "Mensaje sin especificar tipo")

        assertFalse(event.isError) // Por defecto es false
    }
}