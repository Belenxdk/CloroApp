package com.example.cloroapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cloroapp.model.*
import com.example.cloroapp.repository.CloroRepository
import com.example.cloroapp.notifications.AlertNotifier
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.UUID

class CloroViewModel(app:Application):AndroidViewModel(app) {
 private val repo=CloroRepository(app)
 private val notifier=AlertNotifier(app)
 var config by mutableStateOf(CompanyConfig(JSONObject(repo.configText())))
  private set
 var data by mutableStateOf(repo.load())
  private set
 var user by mutableStateOf<User?>(null)
  private set
 var role by mutableStateOf("")
  private set
 var message by mutableStateOf("")
  private set
 var busy by mutableStateOf(false)
  private set
 var offline by mutableStateOf(false)
  private set
 var demo by mutableStateOf(repo.demo)
  private set
 var api by mutableStateOf(repo.apiUrl)
  private set
 var pending by mutableStateOf(repo.pending().size)
  private set
 var now by mutableStateOf(System.currentTimeMillis())
  private set
 var historyDays by mutableStateOf(30)
 init { viewModelScope.launch { while(true) { delay(1000);now=System.currentTimeMillis() } } }
 fun login(u:User,r:String) { user=u;role=r;historyDays=config.int("historyDays");if(data.points.isEmpty())refresh() }
 fun logout() { user=null;role="" }
 fun can(permission:String)=user!=null && config.allowed(role,permission)
 fun visiblePoints()=data.points.filter { it.farm in (user?.farms ?: emptyList()) && can("read") }
 fun readings(id:String)=data.readings.filter { it.point==id }.sortedByDescending { it.time }
 fun latest(id:String)=readings(id).firstOrNull()
 fun stale(id:String)=latest(id)?.let { now-it.time>config.int("staleMinutes")*60000L } ?: true
 fun level(id:String)=latest(id)?.let { config.level(it.value,id) } ?: Level.INVALID

 fun toggleOfflineMode(value: Boolean) { offline = value }
 private fun runOperation(block:suspend ()->Unit) {
  if(busy)return
  viewModelScope.launch { busy=true;try { block() } catch(e:Exception) { message=e.message ?: "No se pudo completar la operación" } finally { busy=false } }
 }
 private suspend fun persist(s:Snapshot) { withContext(Dispatchers.IO) { repo.save(s) };data=s }
 private suspend fun queue(type:String,payload:Snapshot) { withContext(Dispatchers.IO) { repo.queue(JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("payload",JSONObject(Codec.encode(payload)))) };pending=repo.pending().size }
 fun refresh()=runOperation {
  if(offline) { message="Sin conexión simulada. Se conserva la información local.";return@runOperation }
  val remote=withContext(Dispatchers.IO) { if(!demo)repo.flush();repo.fetch() }
  val cutoff=now-config.int("retentionDays")*86400000L
  val combined=(remote.readings+data.readings).distinctBy { it.id }.filter { it.time>=cutoff }
  val incidents=(remote.incidents+data.incidents).distinctBy { it.id }.toMutableList()
  val notifications=mutableListOf<Pair<Point,Incident>>()
  if(config.enabled("autoDeviation"))remote.points.forEach { p ->
   val r=combined.filter { it.point==p.id }.maxByOrNull { it.time }
   if(r!=null) {
    val level=config.level(r.value,p.id)
    if(level in listOf(Level.WARNING,Level.CRITICAL)) {
     val existing=incidents.indexOfFirst { it.point==p.id && !it.closed }
     if(existing<0) { val incident=Incident("incident-${p.id}-${r.id}",p.id,r.time,level.name);incidents.add(incident);notifications.add(p to incident) }
     else if(incidents[existing].level!=level.name) { val changed=incidents[existing].copy(level=level.name);incidents[existing]=changed;notifications.add(p to changed) }
    }
   }
  }
  val result=Snapshot(remote.points,combined,incidents,(remote.actions+data.actions).distinctBy { it.id },now)
  persist(result)
  // Alert creation and severity changes are propagated on the next explicit/automatic refresh.
  for((p,i) in notifications) {
   queue("incident",Snapshot(incidents=listOf(i)))
   if(p.farm in (user?.farms ?: emptyList()) && config.enabled("notificationsEnabled") && role in config.json.getJSONArray("notifyRoles").strings() && (i.level==Level.CRITICAL.name || config.enabled("notifyWarning")))notifier.show(i.id,"${i.level}: ${p.name}","Dato simulado fuera del rango configurado")
  }
  pending=repo.pending().size
  message=if(demo)"Datos ficticios actualizados; registros conservados en este dispositivo." else "API sincronizada. Última consulta correcta."
 }
 fun saveReading(point:String,value:String,extras:String,replaces:String="")=runOperation {
  require(can(if(replaces.isBlank())"measure" else "correct")) { "Perfil sin permiso" }
  require(visiblePoints().any { it.id==point })
  require(!offline || config.enabled("offlineWrites")) { "Registro offline deshabilitado" }
  val number=value.replace(',','.').toDoubleOrNull();require(number!=null && number.isFinite() && number>=0) { "Ingrese un valor de cloro no negativo" }
  val r=Reading(UUID.randomUUID().toString(),point,number,now,user!!.id,extras,replaces)
  val level=config.level(number,point)
  var incidents=data.incidents
  var changed:Incident?=null
  if(config.enabled("autoDeviation") && level in listOf(Level.WARNING,Level.CRITICAL)) {
   val old=incidents.firstOrNull { it.point==point && !it.closed }
   val updated=old?.copy(level=level.name) ?: Incident("incident-$point-${r.id}",point,now,level.name)
   changed=updated
   incidents=incidents.filterNot { it.id==updated.id }+updated
  }
  val payload=Snapshot(readings=listOf(r),incidents=listOfNotNull(changed))
  queue("reading",payload);persist(data.copy(readings=data.readings+r,incidents=incidents))
  if(changed!=null && config.enabled("notificationsEnabled") && role in config.json.getJSONArray("notifyRoles").strings() && (level==Level.CRITICAL || config.enabled("notifyWarning")))notifier.show(changed.id,"${level.label}: $point","Medición demo fuera de rango")
  message="Medición guardada. ${if(replaces.isNotBlank())"La original se conserva." else ""}"
 }
 fun addAction(point:String,incident:String,text:String)=runOperation {
  require(can("action"));require(visiblePoints().any { it.id==point });require(text.trim().length>=5) { "Describa la acción con al menos 5 caracteres" }
  require(!offline || config.enabled("offlineWrites")) { "Registro offline deshabilitado" }
  val a=Action(UUID.randomUUID().toString(),incident,point,text.trim(),user!!.id,now)
  queue("action",Snapshot(actions=listOf(a)));persist(data.copy(actions=data.actions+a));message="Acción guardada con autor y fecha."
 }
 fun updateIncident(id:String,close:Boolean)=runOperation {
  require(can(if(close)"close" else "ack")) { "Perfil sin permiso" }
  require(!offline || config.enabled("offlineWrites")) { "Cambios offline deshabilitados" }
  val old=data.incidents.first { it.id==id };require(visiblePoints().any { it.id==old.point })
  if(close) {
   require(!config.enabled("closeRequiresNormal") || (level(old.point)==Level.NORMAL && !stale(old.point))) { "Para cerrar se requiere una lectura normal reciente" }
   require(data.actions.any { it.incident==id }) { "Registre una acción antes del cierre" }
  }
  val changed=old.copy(acknowledged=true,closed=close,closedBy=if(close)user!!.id else "")
  queue("incident",Snapshot(incidents=listOf(changed)));persist(data.copy(incidents=data.incidents.map { if(it.id==id)changed else it }));message=if(close)"Desviación cerrada; su historial se conserva." else "Alerta reconocida."
 }
 fun saveConfig(text:String)=runOperation {
  val parsed=CompanyConfig(JSONObject(text));parsed.validate()
  withContext(Dispatchers.IO) { repo.saveConfig(text) };config=parsed;logout();message="Configuración guardada. Seleccione nuevamente el usuario demo."
 }
 fun defaultConfig()=repo.defaults()
 fun setSource(isDemo:Boolean,url:String)=runOperation { require(url.startsWith("http://") || url.startsWith("https://")) { "URL debe comenzar con http:// o https://" };repo.apiUrl=url;repo.demo=isDemo;api=repo.apiUrl;demo=isDemo;message="Origen guardado. Pulse actualizar para consultar." }
 fun csv():String {
  require(can("export") && config.enabled("csvExport"))
  val ids=visiblePoints().map { it.id }.toSet()
  fun cell(s:String)="\""+s.replace("\"","\"\"")+"\""
  return "id,punto,cloro,unidad,fecha_epoch,autor\n"+data.readings.filter { it.point in ids }.joinToString("\n") { listOf(it.id,it.point,it.value.toString(),config.json.getString("unit"),it.time.toString(),it.author).joinToString(",",transform=::cell) }
 }
}
