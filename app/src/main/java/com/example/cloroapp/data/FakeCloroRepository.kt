package com.example.cloroapp.data

import com.example.cloroapp.model.*
import kotlin.random.Random

/**
 * Implementación en memoria con datos ficticios (3 granjas x 4 puntos, 15 usuarios).
 * Sirve para que la UI avance sin esperar a Room ni a la API.
 * Eduardo la reemplaza por la implementación real; la UI no debería notar el cambio.
 *
 * Usuarios de prueba (clave "1234" para todos):
 *   operario1@cloro.test ... operario8@cloro.test
 *   supervisor1@cloro.test ... supervisor4@cloro.test
 *   jefatura1@cloro.test ... jefatura3@cloro.test
 */
class FakeCloroRepository(private val ahoraMs: () -> Long = { System.currentTimeMillis() }) : CloroRepository {

    private val rnd = Random(42)
    private val hora = 60L * 60 * 1000

    private val granjas = listOf(
        Granja(1, "Granja Los Aromos", "Melipilla"),
        Granja(2, "Granja El Roble", "Curacaví"),
        Granja(3, "Granja Santa Rosa", "María Pinto")
    )

    private val puntos: List<PuntoMedicion> = granjas.flatMap { g ->
        listOf(
            PuntoMedicion(g.id * 10 + 1, g.id, "Galpón 1", "G${g.id}-TQ1", TipoPunto.TANQUE),
            PuntoMedicion(g.id * 10 + 2, g.id, "Galpón 1", "G${g.id}-LD1", TipoPunto.LINEA_DISTRIBUCION),
            PuntoMedicion(g.id * 10 + 3, g.id, "Galpón 2", "G${g.id}-BD1", TipoPunto.BEBEDERO),
            PuntoMedicion(g.id * 10 + 4, g.id, "Galpón 2", "G${g.id}-BD2", TipoPunto.BEBEDERO)
        )
    }

    private val usuarios: List<Usuario> =
        (1..8).map { Usuario(it, "Operario $it", "operario$it@cloro.test", Rol.OPERARIO, listOf((it - 1) % 3 + 1)) } +
        (1..4).map { Usuario(8 + it, "Supervisor $it", "supervisor$it@cloro.test", Rol.SUPERVISOR,
            if (it == 4) listOf(1, 2, 3) else listOf(it)) } +
        (1..3).map { Usuario(12 + it, "Jefatura $it", "jefatura$it@cloro.test", Rol.JEFATURA, listOf(1, 2, 3)) }

    private val mediciones = mutableListOf<Medicion>()
    private val alertas = mutableListOf<Alerta>()
    private val acciones = mutableListOf<AccionCorrectiva>()
    private var nextId = 1L

    init {
        val ahora = ahoraMs()
        // 30 días de historial, una medición por hora por punto, alrededor de 4 ppm.
        for (p in puntos) {
            for (h in 30 * 24 downTo 1) {
                val valor = (4.0 + (rnd.nextDouble() - 0.5) * 1.6).redondear2()
                mediciones += nueva(p.id, valor, ahora - h * hora, OrigenMedicion.SIMULADA, null)
            }
        }
        // Tres puntos con desviaciones recientes para poder probar alertas.
        mediciones += nueva(12, 2.60, ahora - 10 * 60_000, OrigenMedicion.SIMULADA, null)  // advertencia, insuficiente
        mediciones += nueva(23, 6.80, ahora - 5 * 60_000, OrigenMedicion.SIMULADA, null)   // crítico, exceso
        mediciones += nueva(31, 1.40, ahora - 2 * 60_000, OrigenMedicion.SIMULADA, null)   // crítico, insuficiente
        // Alertas para toda medición no normal de las últimas 24 horas.
        mediciones.filter { it.estado != EstadoCloro.NORMAL && it.timestampMs >= ahora - 24 * hora }
            .forEach { generarAlerta(it) }
    }

    private fun nueva(
        puntoId: Int, valor: Double, ts: Long, origen: OrigenMedicion, usuarioId: Int?,
        corrigeId: Long? = null, motivo: String? = null
    ) = Medicion(
        id = nextId++, puntoId = puntoId, valorPpm = valor, timestampMs = ts,
        estado = ConfigCloro.umbrales.clasificar(valor), origen = origen, usuarioId = usuarioId,
        corrigeId = corrigeId, motivoCorreccion = motivo
    )

    private fun generarAlerta(m: Medicion) {
        val tipo = ConfigCloro.umbrales.tipoDesviacion(m.valorPpm) ?: return
        alertas += Alerta(nextId++, m.puntoId, m.id, tipo, m.estado, m.timestampMs)
    }

    override suspend fun login(correo: String, clave: String): Usuario? =
        if (clave == "1234") usuarios.firstOrNull { it.correo.equals(correo.trim(), ignoreCase = true) } else null

    override suspend fun getGranjas(usuario: Usuario): List<Granja> =
        granjas.filter { it.id in usuario.granjaIds }

    override suspend fun getPuntos(granjaId: Int): List<PuntoMedicion> =
        puntos.filter { it.granjaId == granjaId }

    override suspend fun getUltimaMedicion(puntoId: Int): Medicion? =
        mediciones.filter { it.puntoId == puntoId }.maxByOrNull { it.timestampMs }

    override suspend fun getHistorial(puntoId: Int, periodo: Periodo): List<Medicion> {
        val desde = ahoraMs() - periodo.horas * hora
        return mediciones.filter { it.puntoId == puntoId && it.timestampMs >= desde }
            .sortedByDescending { it.timestampMs }
    }

    override suspend fun getAlertas(soloAbiertas: Boolean): List<Alerta> =
        alertas.filter { !soloAbiertas || it.abierta }.sortedByDescending { it.creadaMs }

    override suspend fun getAcciones(alertaId: Long): List<AccionCorrectiva> =
        acciones.filter { it.alertaId == alertaId }.sortedBy { it.timestampMs }

    override suspend fun registrarMedicion(puntoId: Int, valorPpm: Double, usuarioId: Int): Medicion {
        val m = nueva(puntoId, valorPpm.redondear2(), ahoraMs(), OrigenMedicion.MANUAL, usuarioId)
        mediciones += m
        generarAlerta(m)
        return m
    }

    override suspend fun corregirMedicion(
        medicionId: Long, nuevoValorPpm: Double, motivo: String, usuarioId: Int
    ): Medicion {
        val original = mediciones.first { it.id == medicionId }
        val m = nueva(original.puntoId, nuevoValorPpm.redondear2(), ahoraMs(), OrigenMedicion.MANUAL,
            usuarioId, corrigeId = original.id, motivo = motivo)
        mediciones += m
        generarAlerta(m)
        return m
    }

    override suspend fun registrarAccion(alertaId: Long, descripcion: String, responsableId: Int): AccionCorrectiva {
        val alerta = alertas.first { it.id == alertaId }
        val a = AccionCorrectiva(nextId++, alertaId, alerta.puntoId, alerta.medicionId,
            descripcion, responsableId, ahoraMs())
        acciones += a
        return a
    }

    override suspend fun cerrarAlerta(alertaId: Long, supervisor: Usuario): Result<Unit> {
        if (supervisor.rol != Rol.SUPERVISOR) return Result.failure(IllegalStateException("Solo un supervisor puede cerrar alertas"))
        val idx = alertas.indexOfFirst { it.id == alertaId }
        if (idx < 0) return Result.failure(NoSuchElementException("Alerta no encontrada"))
        val alerta = alertas[idx]
        if (!alerta.abierta) return Result.failure(IllegalStateException("La alerta ya está cerrada"))
        if (acciones.none { it.alertaId == alertaId })
            return Result.failure(IllegalStateException("Falta registrar una acción correctiva"))
        val ultima = getUltimaMedicion(alerta.puntoId)
        if (ultima == null || ultima.estado != EstadoCloro.NORMAL)
            return Result.failure(IllegalStateException("La última medición del punto debe estar en rango normal"))
        alertas[idx] = alerta.copy(cerradaMs = ahoraMs(), cerradaPorId = supervisor.id)
        return Result.success(Unit)
    }
}
