package com.example.cloroapp.model

// Fechas como Long (epoch en milisegundos): java.time exige API 26 y el proyecto usa minSdk 24.

enum class Rol { OPERARIO, SUPERVISOR, JEFATURA }

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: Rol,
    /** Granjas a las que tiene acceso. La jefatura ve todas. */
    val granjaIds: List<Int>
)

data class Granja(
    val id: Int,
    val nombre: String,
    val ubicacion: String
)

enum class TipoPunto { TANQUE, LINEA_DISTRIBUCION, BEBEDERO }

data class PuntoMedicion(
    val id: Int,
    val granjaId: Int,
    val galpon: String,
    val codigo: String,
    val tipo: TipoPunto
)

enum class OrigenMedicion { SIMULADA, MANUAL }

data class Medicion(
    val id: Long,
    val puntoId: Int,
    /** Cloro libre en ppm (≈ mg/L), con 2 decimales. */
    val valorPpm: Double,
    val timestampMs: Long,
    val estado: EstadoCloro,
    val origen: OrigenMedicion = OrigenMedicion.SIMULADA,
    val usuarioId: Int? = null,
    /** true si se registró sin conexión y falta sincronizar con la API. */
    val pendienteSync: Boolean = false,
    /** Si no es null, esta medición corrige a la medición con ese id (la original nunca se edita). */
    val corrigeId: Long? = null,
    val motivoCorreccion: String? = null
)

enum class TipoAlerta { CLORO_INSUFICIENTE, CLORO_EXCESO }

data class Alerta(
    val id: Long,
    val puntoId: Int,
    val medicionId: Long,
    val tipo: TipoAlerta,
    /** ADVERTENCIA o CRITICO. */
    val gravedad: EstadoCloro,
    val creadaMs: Long,
    val cerradaMs: Long? = null,
    val cerradaPorId: Int? = null
) {
    val abierta: Boolean get() = cerradaMs == null
}

data class AccionCorrectiva(
    val id: Long,
    val alertaId: Long,
    val puntoId: Int,
    val medicionId: Long,
    val descripcion: String,
    val responsableId: Int,
    val timestampMs: Long,
    val resuelta: Boolean = false
)

enum class Periodo(val horas: Int) { H24(24), D7(24 * 7), D30(24 * 30) }
