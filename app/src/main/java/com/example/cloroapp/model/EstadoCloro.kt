package com.example.cloroapp.model

import kotlin.math.round

enum class EstadoCloro { NORMAL, ADVERTENCIA, CRITICO }

/**
 * Umbrales de cloro libre en ppm. Valores por defecto según el cuestionario del caso:
 *  - Normal:      3 a 5
 *  - Advertencia: 2 a <3  o  >5 a 6
 *  - Crítico:     <2  o  >6
 * Son configurables porque el docente puede definir otros rangos.
 */
data class Umbrales(
    val criticoMin: Double = 2.0,
    val normalMin: Double = 3.0,
    val normalMax: Double = 5.0,
    val criticoMax: Double = 6.0
) {
    fun clasificar(valorPpm: Double): EstadoCloro = when {
        valorPpm < criticoMin || valorPpm > criticoMax -> EstadoCloro.CRITICO
        valorPpm in normalMin..normalMax -> EstadoCloro.NORMAL
        else -> EstadoCloro.ADVERTENCIA
    }

    /** null si el valor está dentro del rango normal. */
    fun tipoDesviacion(valorPpm: Double): TipoAlerta? = when {
        valorPpm < normalMin -> TipoAlerta.CLORO_INSUFICIENTE
        valorPpm > normalMax -> TipoAlerta.CLORO_EXCESO
        else -> null
    }
}

/** Umbrales activos de la app. Cambiar `umbrales` aplica a todas las clasificaciones nuevas. */
object ConfigCloro {
    var umbrales: Umbrales = Umbrales()
}

fun Double.redondear2(): Double = round(this * 100.0) / 100.0
