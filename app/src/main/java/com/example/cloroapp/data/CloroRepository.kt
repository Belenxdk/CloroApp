package com.example.cloroapp.data

import com.example.cloroapp.model.*

/**
 * Contrato entre la UI (Belén) y los datos (Eduardo).
 * La UI solo conoce esta interfaz; Eduardo la implementa con Room + API.
 * Mientras tanto se usa FakeCloroRepository.
 */
interface CloroRepository {

    /** null si el correo o la clave son incorrectos. */
    suspend fun login(correo: String, clave: String): Usuario?

    /** Granjas visibles para ese usuario (la jefatura ve todas). */
    suspend fun getGranjas(usuario: Usuario): List<Granja>

    suspend fun getPuntos(granjaId: Int): List<PuntoMedicion>

    suspend fun getUltimaMedicion(puntoId: Int): Medicion?

    /** Mediciones del período, de la más reciente a la más antigua. */
    suspend fun getHistorial(puntoId: Int, periodo: Periodo): List<Medicion>

    suspend fun getAlertas(soloAbiertas: Boolean = false): List<Alerta>

    suspend fun getAcciones(alertaId: Long): List<AccionCorrectiva>

    /** Registra una medición manual (genera alerta automática si sale de rango). */
    suspend fun registrarMedicion(puntoId: Int, valorPpm: Double, usuarioId: Int): Medicion

    /** Las correcciones se guardan como una medición nueva; la original no se modifica. */
    suspend fun corregirMedicion(medicionId: Long, nuevoValorPpm: Double, motivo: String, usuarioId: Int): Medicion

    suspend fun registrarAccion(alertaId: Long, descripcion: String, responsableId: Int): AccionCorrectiva

    /**
     * Solo un supervisor puede cerrar, y solo si hay una acción registrada
     * y la última medición del punto está en estado NORMAL.
     */
    suspend fun cerrarAlerta(alertaId: Long, supervisor: Usuario): Result<Unit>
}
