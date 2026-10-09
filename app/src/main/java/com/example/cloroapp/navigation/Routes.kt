package com.example.cloroapp.navigation
sealed class Route(val path:String,val title:String) {
 object Home:Route("home","Resumen")
 object Points:Route("points","Puntos")
 object Alerts:Route("alerts","Alertas")
 object Settings:Route("settings","Ajustes demo")
 object Detail:Route("detail/{id}","Detalle") { fun forPoint(id:String)="detail/$id" }
}
