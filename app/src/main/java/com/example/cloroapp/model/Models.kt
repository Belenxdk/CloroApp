package com.example.cloroapp.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class Level(val label: String) { NORMAL("Normal"), WARNING("Advertencia"), CRITICAL("Crítico"), INVALID("Sin lectura válida") }
data class Point(val id: String, val farm: String, val farmName: String, val barn: String, val name: String)
data class Reading(val id: String, val point: String, val value: Double, val time: Long, val author: String = "API simulada", val extras: String = "", val replaces: String = "")
data class Incident(val id: String, val point: String, val opened: Long, val level: String, val acknowledged: Boolean = false, val closed: Boolean = false, val closedBy: String = "")
data class Action(val id: String, val incident: String, val point: String, val text: String, val author: String, val time: Long)
data class User(val id: String, val name: String, val roles: List<String>, val farms: List<String>)
data class Snapshot(val points: List<Point> = emptyList(), val readings: List<Reading> = emptyList(), val incidents: List<Incident> = emptyList(), val actions: List<Action> = emptyList(), val synced: Long = 0)
fun JSONArray.strings() = (0 until length()).map { getString(it) }
fun <T> JSONArray.mapObjects(f: (JSONObject)->T) = (0 until length()).map { f(getJSONObject(it)) }
class CompanyConfig(val json: JSONObject) {
    fun number(key: String) = json.getDouble(key)
    fun int(key: String) = json.getInt(key)
    fun enabled(key: String) = json.optBoolean(key)
    fun range(point: String, key: String): Double = json.optJSONObject("pointRanges")?.optJSONObject(point)?.optDouble(key, number(key)) ?: number(key)
    fun level(value: Double, point: String): Level {
        if (!value.isFinite() || value < 0) return Level.INVALID
        if (value < range(point,"criticalLow") || value > range(point,"criticalHigh")) return Level.CRITICAL
        if (value < range(point,"normalMin") || value > range(point,"normalMax")) return Level.WARNING
        return Level.NORMAL
    }
    fun format(value: Double) = String.format(Locale.forLanguageTag("es-CL"),"%.${int("decimals")}f",value)
    val users: List<User> get() = json.getJSONArray("users").mapObjects { User(it.getString("id"),it.getString("name"),it.getJSONArray("roles").strings(),it.getJSONArray("farms").strings()) }
    fun allowed(role: String, permission: String) = json.getJSONObject("roles").optJSONArray(role)?.strings()?.contains(permission) == true
    fun validate() {
        fun checkRange(j: JSONObject) {
            val a=j.getDouble("criticalLow");val b=j.getDouble("normalMin");val c=j.getDouble("normalMax");val d=j.getDouble("criticalHigh")
            require(listOf(a,b,c,d).all { it.isFinite() } && 0<=a && a<b && b<c && c<d) { "Rangos: 0 ≤ crítico bajo < normal mínimo < normal máximo < crítico alto" }
        }
        checkRange(json)
        json.getJSONObject("pointRanges").let { ranges -> ranges.keys().forEach { key -> checkRange(ranges.getJSONObject(key)) } }
        require(int("decimals") in 0..4) { "Decimales: 0 a 4" }
        require(int("refreshSeconds") in 10..86400 && int("staleMinutes")>0 && int("escalationMinutes")>0) { "Intervalos inválidos" }
        require(int("historyDays")>0 && int("retentionDays")>=int("historyDays")) { "Retención debe cubrir el histórico" }
        require(json.getString("unit") in listOf("mg/L","ppm")) { "Unidad admitida: mg/L o ppm, sin conversión automática" }
        require(json.getString("analyte")=="Cloro libre") { "MVP mide cloro libre. Total/ambos requiere ampliar modelo y API." }
        require(json.getJSONArray("notifyRoles").strings().all { json.getJSONObject("roles").has(it) }) { "Destinatario de notificación inválido" }
        require(users.isNotEmpty() && users.map{it.id}.distinct().size==users.size) { "Usuarios vacíos o repetidos" }
        require(users.all { u -> u.roles.isNotEmpty() && u.roles.all { json.getJSONObject("roles").has(it) } }) { "Rol de usuario inexistente" }
        require(enabled("immutableMeasurements")) { "Se conserva auditoría: correcciones como nuevas lecturas" }
        val implemented=setOf("read","measure","action","ack","close","correct","export")
        json.getJSONObject("roles").let { roles -> roles.keys().forEach { r -> require(roles.getJSONArray(r).strings().all { it in implemented }) { "Permiso no implementado en $r" } } }
    }
}
object Codec {
 fun decode(text: String): Snapshot {
  val j=JSONObject(text)
  return Snapshot(
   j.optJSONArray("points")?.mapObjects { Point(it.getString("id"),it.getString("farm"),it.getString("farmName"),it.optString("barn"),it.getString("name")) } ?: emptyList(),
   j.optJSONArray("readings")?.mapObjects { Reading(it.getString("id"),it.getString("point"),it.getDouble("value"),it.getLong("time"),it.optString("author","API simulada"),it.optString("extras"),it.optString("replaces")) } ?: emptyList(),
   j.optJSONArray("incidents")?.mapObjects { Incident(it.getString("id"),it.getString("point"),it.getLong("opened"),it.getString("level"),it.optBoolean("acknowledged"),it.optBoolean("closed"),it.optString("closedBy")) } ?: emptyList(),
   j.optJSONArray("actions")?.mapObjects { Action(it.getString("id"),it.getString("incident"),it.getString("point"),it.getString("text"),it.getString("author"),it.getLong("time")) } ?: emptyList(),j.optLong("synced"))
 }
 fun encode(s: Snapshot): String = JSONObject().apply {
  put("synced",s.synced)
  put("points",JSONArray(s.points.map { JSONObject().put("id",it.id).put("farm",it.farm).put("farmName",it.farmName).put("barn",it.barn).put("name",it.name) }))
  put("readings",JSONArray(s.readings.map { JSONObject().put("id",it.id).put("point",it.point).put("value",it.value).put("time",it.time).put("author",it.author).put("extras",it.extras).put("replaces",it.replaces) }))
  put("incidents",JSONArray(s.incidents.map { JSONObject().put("id",it.id).put("point",it.point).put("opened",it.opened).put("level",it.level).put("acknowledged",it.acknowledged).put("closed",it.closed).put("closedBy",it.closedBy) }))
  put("actions",JSONArray(s.actions.map { JSONObject().put("id",it.id).put("incident",it.incident).put("point",it.point).put("text",it.text).put("author",it.author).put("time",it.time) }))
 }.toString()
}
