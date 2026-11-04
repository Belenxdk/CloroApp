package com.example.cloroapp.repository

import android.content.Context
import com.example.cloroapp.model.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class CloroRepository(context: Context) {
 private val prefs=context.getSharedPreferences("cloro_private",Context.MODE_PRIVATE)
 private val defaults=context.assets.open("company_config.json").bufferedReader().use { it.readText() }
 fun configText()=prefs.getString("config",defaults)!!
 fun saveConfig(text:String) { CompanyConfig(JSONObject(text)).validate();check(prefs.edit().putString("config",text).commit()) }
 fun defaults()=defaults
 fun load():Snapshot = prefs.getString("snapshot",null)?.let { Codec.decode(it) } ?: Snapshot()
 fun save(s:Snapshot) { check(prefs.edit().putString("snapshot",Codec.encode(s)).commit()) { "No se pudo guardar localmente" } }
 var apiUrl:String
  get()=prefs.getString("api","http://10.0.2.2:8080")!!
  set(value) { require(value.startsWith("http://") || value.startsWith("https://"));prefs.edit().putString("api",value.trimEnd('/')).apply() }
 var demo:Boolean
  get()=prefs.getBoolean("demo",true)
  set(v) { prefs.edit().putBoolean("demo",v).apply() }
 fun pending():List<String> = prefs.getString("pending","[]")!!.let { org.json.JSONArray(it).strings() }
 fun queue(event:JSONObject) { val all=pending()+event.toString();check(prefs.edit().putString("pending",org.json.JSONArray(all).toString()).commit()) }
 fun flush() { for(event in pending()) { request("/events","POST",event);check(prefs.edit().putString("pending",org.json.JSONArray(pending().filterNot { it==event }).toString()).commit()) } }
 fun fetch():Snapshot = if(demo) demoSnapshot() else Codec.decode(request("/snapshot"))
 private fun request(path:String,method:String="GET",body:String?=null):String {
  val c=URL(apiUrl+path).openConnection() as HttpURLConnection
  try {
   c.connectTimeout=5000;c.readTimeout=5000;c.requestMethod=method
   if(body!=null) { c.doOutput=true;c.setRequestProperty("Content-Type","application/json");c.outputStream.bufferedWriter().use { it.write(body) } }
   require(c.responseCode in 200..299) { "API respondió ${c.responseCode}" }
   return c.inputStream.bufferedReader().use { it.readText() }
  } finally { c.disconnect() }
 }
 companion object {
  fun demoSnapshot():Snapshot {
   val now=System.currentTimeMillis();val points=listOf(Point("P1","F1","Granja Norte Demo","Galpón 1","Tanque principal"),Point("P2","F1","Granja Norte Demo","Galpón 2","Línea de agua"),Point("P3","F2","Granja Sur Demo","Galpón 1","Bebedero"),Point("P4","F2","Granja Sur Demo","","Reserva exterior"))
   val values=listOf(.9,.3,2.3,1.1)
   return Snapshot(points,points.flatMapIndexed { i,p -> (0..12).map { h -> Reading("demo-${p.id}-${now-h*3600000}",p.id,(values[i]+ if(h==0) 0.0 else (h%3-1)*.08).coerceAtLeast(0.0),now-h*3600000) } },synced=now)
  }
 }
}
