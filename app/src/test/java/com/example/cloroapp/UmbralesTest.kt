package com.example.cloroapp

import com.example.cloroapp.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UmbralesTest {
    private val u = Umbrales()

    @Test fun normal_entre_3_y_5_inclusive() {
        listOf(3.0, 4.0, 5.0).forEach { assertEquals(EstadoCloro.NORMAL, u.clasificar(it)) }
    }

    @Test fun advertencia_bajo_y_alto() {
        listOf(2.0, 2.99, 5.01, 6.0).forEach { assertEquals(EstadoCloro.ADVERTENCIA, u.clasificar(it)) }
    }

    @Test fun critico_bajo_2_y_sobre_6() {
        listOf(0.0, 1.99, 6.01, 9.0).forEach { assertEquals(EstadoCloro.CRITICO, u.clasificar(it)) }
    }

    @Test fun tipo_de_desviacion() {
        assertEquals(TipoAlerta.CLORO_INSUFICIENTE, u.tipoDesviacion(2.5))
        assertEquals(TipoAlerta.CLORO_EXCESO, u.tipoDesviacion(5.5))
        assertNull(u.tipoDesviacion(4.0))
    }
}