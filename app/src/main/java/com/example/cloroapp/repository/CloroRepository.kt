package com.example.cloroapp.repository

import android.content.Context
import com.example.cloroapp.model.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject
import org.json.JSONArray

class CloroRepository(context: Context) {
 private val prefs=context.getSharedPreferences("cloro_v2",Context.MODE_PRIVATE)
 private val defaults=context.assets.open("company_config.json").bufferedReader().use { it.readText() }
 var token=""
 var owner=""
 var offlineSession=false
 fun configText()=prefs.getString("config",defaults)!!
 fun saveConfig(text:String) { CompanyConfig(JSONObject(text)).validate();check(prefs.edit().putString("config",text).commit()) }
 fun defaults()=defaults
 private fun key()=if(demo)"local" else "http:$apiUrl:$owner"
 fun load():Snapshot=prefs.getString("snapshot:${key()}",null)?.let(Codec::decode) ?: Snapshot()
 fun save(s:Snapshot) { commit(s,null) }
 fun commit(s:Snapshot,event:JSONObject?) {
  val editor=prefs.edit().putString("snapshot:${key()}",Codec.encode(s))
  if(!demo && event!=null)editor.putString("queue:${key()}",JSONArray(pending()+event.toString()).toString())
  check(editor.commit()) { "No se pudo guardar localmente" }
 }
 var apiUrl:String
  get()=prefs.getString("api","http://10.0.2.2:8080")!!
  set(value) { require(value.startsWith("http://") || value.startsWith("https://"));prefs.edit().putString("api",value.trim().trimEnd('/')).apply() }
 var demo:Boolean
  get()=prefs.getBoolean("demo",true)
  set(value) { prefs.edit().putBoolean("demo",value).apply() }
 fun pending()=JSONArray(prefs.getString("queue:${key()}","[]")).strings()
 fun flush() { for(event in pending()) { request("/events","POST",event);check(prefs.edit().putString("queue:${key()}",JSONArray(pending().filterNot { it==event }).toString()).commit()) } }
 fun authenticate(email:String,password:String,config:CompanyConfig):User {
  val u=config.users.firstOrNull { it.email.equals(email.trim(),true) } ?: error("Correo o contraseña incorrectos")
  val hash=MessageDigest.getInstance("SHA-256").digest((u.id+":"+password).toByteArray()).joinToString(""){"%02x".format(it)}
  require(hash==u.passwordHash) { "Correo o contraseña incorrectos" }
  owner=u.id;offlineSession=false
  if(!demo)try {
   token=JSONObject(request("/login","POST",JSONObject().put("email",email.trim()).put("password",password).toString())).getString("token")
   check(prefs.edit().putBoolean("verified:${key()}",true).putString("token:${key()}",token).commit())
  } catch(e:java.io.IOException) {
   if(!prefs.getBoolean("verified:${key()}",false) || load().points.isEmpty())throw e
   token=prefs.getString("token:${key()}","")!!;offlineSession=true
  }
  return u
 }
 fun remoteConfig():JSONObject {
  if(offlineSession)return JSONObject(prefs.getString("rules:${key()}",defaults)!!)
  val text=request("/config");prefs.edit().putString("rules:${key()}",text).apply();return JSONObject(text)
 }
 fun fetch()=Codec.decode(request("/snapshot"))
 fun generateRemote() { request("/simulate","POST","{}") }
 private fun request(path:String,method:String="GET",body:String?=null):String {
  val c=URL(apiUrl+path).openConnection() as HttpURLConnection
  try {
   c.connectTimeout=7000;c.readTimeout=7000;c.requestMethod=method
   if(token.isNotBlank())c.setRequestProperty("Authorization","Bearer $token")
   if(body!=null) { c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.bufferedWriter().use { it.write(body) } }
   val code=c.responseCode
   val text=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
   check(code in 200..299) { runCatching { JSONObject(text).getString("error") }.getOrDefault("API respondió $code") }
   return text
  } finally { c.disconnect() }
 }
 companion object {
  fun demoSnapshot(config:CompanyConfig):Snapshot {
   val now=System.currentTimeMillis();val names=listOf("Tanque principal","Línea de distribución","Bebedero galpón 1","Bebedero galpón 2")
   val points=(0 until 12).map { i->Point("P%02d".format(i+1),"F${i/4+1}",listOf("Granja Norte","Granja Centro","Granja Sur")[i/4],if(i%4==3)"Galpón 2" else "Galpón 1",names[i%4],"Sector ${i/4+1} · acceso ${i%4+1}",listOf("Tanque","Línea","Bebedero","Bebedero")[i%4]) }
   val values=listOf(4.0,2.5,1.5,5.5,3.5,6.5,4.2,3.8,4.6,2.8,5.0,3.0)
   return Snapshot(points,points.flatMapIndexed { i,p-> ((0..23).map { it*3600000L }+(1..30).map { it*86400000L }).mapIndexed { n,age->
    val value=if(n==0)values[i] else 3.5+((i+n)%12)*.1
    Reading("seed-${p.id}-$n",p.id,value,now-age,"Simulador",status=config.level(value,p.id).name,direction=config.direction(value,p.id),limits=config.limits(p.id))
   } },synced=now)
  }
 }
}
