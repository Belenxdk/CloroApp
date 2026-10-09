package com.example.cloroapp.data

data class Granja(
    val id: Int,
    val nombre: String
)

data class PuntoMedicion(
    val id: Int,
    val granjaId: Int,
    val nombre: String
)

data class Medicion(
    val id: Int,
    val puntoId: Int,
    val nivelCloro: Double,
    val fechaHora: String,
    val estado: String
)

data class Alerta(
    val id: Int,
    val medicionId: Int,
    val descripcion: String,
    val registradaPor: String,
    val fechaHora: String
)

fun calcularEstado(nivel: Double): String = when{
    nivel < 0.3 || nivel > 2.0 -> "CRITICO"
    nivel < 0.5 || nivel > 1.5 -> "ADVERTENCIA"
    else -> "NORMAL"
}

class Modelos {


}