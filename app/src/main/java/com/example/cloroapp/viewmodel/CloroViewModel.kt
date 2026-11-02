package com.example.cloroapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.*
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
 var data by mutableStateOf(Snapshot())
  private set
 var user by mutableStateOf<User?>(null)
  private set
 val role get()=user?.roles?.firstOrNull() ?: ""
 var message by mutableStateOf("")
  private set
 var isError by mutableStateOf(false)
  private set
 var busy by mutableStateOf(false)
  private set
 var offline by mutableStateOf(false)
  private set
 var demo by mutableStateOf(repo.demo)
  private set
 var api by mutableStateOf(repo.apiUrl)
  private set
 var pending by mutableStateOf(0)
  private set
 var now by mutableStateOf(System.currentTimeMillis())
  private set
 var historyDays by mutableStateOf(30)
 var autoSimulation by mutableStateOf(false)
 private var lastSimulation=0L
 init { viewModelScope.launch { while(true) { delay(1000);now=System.currentTimeMillis() } } }
 private fun operation(block:suspend ()->Unit) {
  if(busy)return
  viewModelScope.launch {
   busy=true
   try {
    block()
    isError=false
   } catch(e:Exception) {
    message=e.message ?: "No se pudo completar la operación"
    isError=true
   } finally {
    busy=false
   }
  }
 }
 private suspend fun loadRules() {
  val rules=withContext(Dispatchers.IO) { repo.remoteConfig() };val merged=JSONObject(config.json.toString())
  listOf("normalMin","normalMax","criticalLow","criticalHigh","pointRanges").forEach { if(rules.has(it)) merged.put(it,rules.get(it)) };config=CompanyConfig(merged)
 }
 fun login(email:String,password:String)=operation {
  val u=withContext(Dispatchers.IO){repo.authenticate(email,password,config)}
  if(!demo)loadRules()
  user=u;lastSimulation=System.currentTimeMillis();data=withContext(Dispatchers.IO){repo.load()}
  if(demo && data.points.isEmpty())commit(detect(CloroRepository.demoSnapshot(config)))
  pending=repo.pending().size
  if(!demo && !repo.offlineSession)sync()
  message=if(repo.offlineSession)"Acceso local verificado · sin red. Puedes registrar pendientes." else "Bienvenido/a, ${u.name}"
 }
 fun logout() { if(!busy) { user=null;repo.token="";data=Snapshot();autoSimulation=false;message="";isError=false } }
 fun can(permission:String)=user!=null && config.allowed(role,permission)
 fun visiblePoints()=data.points.filter { it.farm in (user?.farms ?: emptyList()) }
 fun readings(id:String)=data.readings.filter { it.point==id }.sortedByDescending { it.time }
 fun latest(id:String)=readings(id).firstOrNull()
 fun stale(id:String)=latest(id)?.let { now-it.time>config.int("staleMinutes")*60000L } ?: true
 fun level(id:String)=latest(id)?.let { config.level(it.value,id) } ?: Level.INVALID
 fun direction(id:String)=latest(id)?.let { config.direction(it.value,id) } ?: "Sin lectura"
 fun toggleOfflineMode(value:Boolean) { offline=value }
 private fun event(type:String,s:Snapshot)=JSONObject().put("id",UUID.randomUUID().toString()).put("type",type).put("payload",JSONObject(Codec.encode(s)))
 private suspend fun commit(s:Snapshot,e:JSONObject?=null) { withContext(Dispatchers.IO){repo.commit(s,e)};data=s;pending=repo.pending().size }
 private fun detect(s:Snapshot):Snapshot {
  val incidents=s.incidents.toMutableList()
  s.points.forEach { p->
   val r=s.readings.filter { it.point==p.id }.maxByOrNull { it.time }
   if(r!=null && config.level(r.value,p.id) in listOf(Level.WARNING,Level.CRITICAL)) {
    val old=incidents.indexOfFirst { it.point==p.id && !it.closed }
    val l=config.level(r.value,p.id).name;val d=config.direction(r.value,p.id)
    if(old<0)incidents.add(Incident("alert-${r.id}",p.id,r.time,l,reading=r.id,direction=d))
    else incidents[old]=incidents[old].copy(level=l,direction=d,reading=r.id)
   }
  }
  return s.copy(incidents=incidents)
 }
 private fun notifyChanges(before:Snapshot) {
  if(role !in listOf("Operario","Supervisor"))return
  val ids=visiblePoints().map { it.id }
  data.incidents.filter { !it.closed && it.point in ids }.forEach { i->
   val old=before.incidents.find { it.id==i.id }
   if(old==null || old.level!=i.level || old.direction!=i.direction)notifier.show(i.id,"${if(i.level=="CRITICAL")"Crítico" else "Advertencia"} · ${i.point}",i.direction)
  }
 }
 private suspend fun sync() {
  if(offline) { message="Sin conexión simulada. Registros conservados localmente.";return }
  if(demo) { message="Consulta actualizada. No se generaron mediciones nuevas.";return }
  val old=data;val remote=withContext(Dispatchers.IO){repo.flush();repo.fetch()}
  repo.offlineSession=false;loadRules();commit(remote);notifyChanges(old);message="Sincronización completada"
 }
 fun refresh()=operation { sync() }
 fun automaticRefresh() {
  if(user==null || busy || offline)return
  if(demo && autoSimulation && can("close") && now-lastSimulation>=300000L)generate()
  else if(!demo)refresh()
 }
 fun generate()=operation {
  require(can("close")) { "Solo el supervisor puede generar rondas" };require(!offline) { "Desactive la simulación sin conexión" }
  if(!demo) { withContext(Dispatchers.IO){repo.flush();repo.generateRemote()};sync() }
  else {
   val old=data;val stamp=System.currentTimeMillis()
   val rs=visiblePoints().map { p->val v=listOf(1.8,2.5,3.5,4.2,5.5,6.3).random();Reading(UUID.randomUUID().toString(),p.id,v,stamp,"Simulador",status=config.level(v,p.id).name,direction=config.direction(v,p.id),limits=config.limits(p.id)) }
   commit(detect(data.copy(readings=data.readings+rs,synced=stamp)));notifyChanges(old)
  }
  lastSimulation=now;message="Nueva ronda simulada registrada"
 }
 fun saveReading(point:String,value:String,extras:String,replaces:String="",reason:String="",done:()->Unit={})=operation {
  require(can(if(replaces.isBlank())"measure" else "correct")) { "Perfil sin permiso" };require(visiblePoints().any { it.id==point })
  val number=value.replace(',','.').toDoubleOrNull();require(number!=null && number.isFinite() && number>=0) { "Ingrese cloro válido, igual o mayor que cero" }
  if(replaces.isNotBlank()) { require(data.readings.any { it.id==replaces && it.point==point });require(reason.trim().length>=5) { "Explique el motivo de la corrección" } }
  val r=Reading(UUID.randomUUID().toString(),point,number,System.currentTimeMillis(),user!!.id,extras.trim(),replaces,reason.trim(),config.level(number,point).name,config.direction(number,point),config.limits(point))
  val old=data;commit(detect(data.copy(readings=data.readings+r)),event("reading",Snapshot(readings=listOf(r))));notifyChanges(old)
  message="Medición guardada en $point${if(!demo)" · pendiente de sincronización" else ""}";done()
 }
 fun addAction(point:String,incident:String,text:String,done:()->Unit={})=operation {
  require(can("action"));require(visiblePoints().any { it.id==point });require(text.trim().length>=5) { "Describa la verificación y acción realizada" }
  val i=data.incidents.find { it.id==incident && it.point==point && !it.closed } ?: error("Seleccione una alerta abierta")
  val a=Action(UUID.randomUUID().toString(),incident,point,text.trim(),user!!.id,System.currentTimeMillis(),i.reading,"Pendiente de validación")
  commit(data.copy(actions=data.actions+a),event("action",Snapshot(actions=listOf(a))));message="Acción guardada; falta validación del supervisor";done()
 }
 fun updateIncident(id:String,close:Boolean)=operation {
  require(can(if(close)"close" else "ack")) { "Solo supervisor puede validar" }
  val old=data.incidents.first { it.id==id };require(!old.closed && visiblePoints().any { it.id==old.point })
  if(close) {
   val a=data.actions.filter { it.incident==id }.maxByOrNull { it.time } ?: error("Registre una acción antes del cierre")
   require(level(old.point)==Level.NORMAL && !stale(old.point) && (latest(old.point)?.time ?: 0)>=a.time) { "Registre una medición normal reciente posterior a la acción" }
  }
  val i=old.copy(acknowledged=true,closed=close,closedBy=if(close)user!!.id else "",closedAt=if(close)System.currentTimeMillis() else 0)
  commit(data.copy(incidents=data.incidents.map { if(it.id==id)i else it },actions=data.actions.map { if(close && it.incident==id)it.copy(resolution="Validada por ${user!!.id}") else it }),event("incident",Snapshot(incidents=listOf(i))))
  message=if(close)"Alerta cerrada y acción validada" else "Alerta reconocida"
 }
 fun saveConfig(text:String)=operation {
  require(demo && can("close")) { "Las reglas API se administran en el servidor" }
  val parsed=CompanyConfig(JSONObject(text));parsed.validate();withContext(Dispatchers.IO){repo.saveConfig(text)};config=parsed;commit(detect(data));message="Reglas guardadas. El histórico conserva su clasificación original."
 }
 fun defaultConfig()=repo.defaults()
 fun setSource(isDemo:Boolean,url:String)=operation { repo.apiUrl=url;repo.demo=isDemo;api=repo.apiUrl;demo=isDemo;user=null;repo.token="";data=Snapshot();pending=0;autoSimulation=false;config=CompanyConfig(JSONObject(repo.configText()));message="Conexión guardada. Inicie sesión." }
 fun csv(kind:String="Mediciones",farm:String="",point:String="",days:Int=30):String {
  require(can("export"));val ids=visiblePoints().filter { (farm.isBlank() || it.farm==farm) && (point.isBlank() || it.id==point) }.map { it.id };val cutoff=now-days*86400000L
  val rows:List<List<Any>> = when(kind) {
   "Alertas"->listOf(listOf("id","punto","gravedad","tipo","apertura_epoch","cerrada","cierre_epoch","responsable"))+data.incidents.filter { it.point in ids && it.opened>=cutoff }.map { listOf(it.id,it.point,it.level,it.direction,it.opened,it.closed,it.closedAt,it.closedBy) }
   "Acciones"->listOf(listOf("id","punto","alerta","medicion","descripcion","autor","fecha_epoch","resolucion"))+data.actions.filter { it.point in ids && it.time>=cutoff }.map { listOf(it.id,it.point,it.incident,it.reading,it.text,it.author,it.time,it.resolution) }
   else->listOf(listOf("id","punto","cloro_ppm","fecha_epoch","autor","estado_original","tipo","corrige","motivo","limites"))+data.readings.filter { it.point in ids && it.time>=cutoff }.map { listOf(it.id,it.point,it.value,it.time,it.author,it.status,it.direction,it.replaces,it.reason,it.limits) }
  }
  fun cell(v:Any):String { val s=v.toString();val safe=if(s.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r'))"'"+s else s;return "\""+safe.replace("\"","\"\"")+"\"" }
  return "\uFEFF"+rows.joinToString("\n") { it.joinToString(";",transform=::cell) }
 }
}
