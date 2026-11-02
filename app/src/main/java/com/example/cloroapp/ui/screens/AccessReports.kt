package com.example.cloroapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cloroapp.viewmodel.CloroViewModel

@Composable fun LoginScreen(vm:CloroViewModel) {
 var email by rememberSaveable { mutableStateOf("") };var password by remember { mutableStateOf("") }
 var help by rememberSaveable { mutableStateOf(false) };var connection by rememberSaveable { mutableStateOf(false) }
 var local by rememberSaveable { mutableStateOf(vm.demo) };var url by rememberSaveable { mutableStateOf(vm.api) }
 Box(Modifier.fillMaxSize().background(Color(0xFFF2F7F4)),contentAlignment=Alignment.Center) {
  Column(Modifier.widthIn(max=480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Brand();Text("El agua bajo control.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold,color=Color(0xFF173B37))
   Text("Consulta tus puntos, registra controles y sigue cada alerta hasta su resolución.")
   Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(24.dp)) {
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text("Iniciar sesión",style=MaterialTheme.typography.titleLarge)
     OutlinedTextField(email,{email=it},label={Text("Correo electrónico")},singleLine=true,modifier=Modifier.fillMaxWidth())
     OutlinedTextField(password,{password=it},label={Text("Contraseña")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
     Button(onClick={vm.login(email,password)},enabled=!vm.busy && email.isNotBlank() && password.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(vm.busy)"Ingresando…" else "Ingresar")}
     if(vm.message.isNotBlank())Text(vm.message,color=MaterialTheme.colorScheme.error)
    }
   }
   TextButton(onClick={help=!help}){Text("Ver cuentas de demostración")}
   if(help)Text("Contraseña: Cloro2026!\noperario1@cloro.demo\nsupervisor1@cloro.demo\njefatura1@cloro.demo\n9 operarios, 3 supervisores y 3 jefaturas numerados.")
   TextButton(onClick={connection=!connection}){Text("Conexión: ${if(vm.demo)"demostración local" else "API académica"}")}
   if(connection) {
    Row(verticalAlignment=Alignment.CenterVertically){Switch(local,{local=it});Text("Usar demostración local")}
    if(!local)OutlinedTextField(url,{url=it},label={Text("URL del servidor")},modifier=Modifier.fillMaxWidth())
    Button(onClick={vm.setSource(local,url)},enabled=!vm.busy){Text("Guardar conexión")}
    Text("La demo funciona sin servidor. La API permite compartir registros entre celulares.",style=MaterialTheme.typography.bodySmall)
   }
   Text("Prototipo académico · Datos ficticios · DSY1105",style=MaterialTheme.typography.labelSmall)
  }
 }
}

@Composable fun ReportsScreen(vm:CloroViewModel) {
 if(!vm.can("export")){Text("Reportes disponibles para jefatura");return}
 var farm by rememberSaveable { mutableStateOf("") };var point by rememberSaveable { mutableStateOf("") };var days by rememberSaveable { mutableStateOf(7) };var kind by rememberSaveable { mutableStateOf("Mediciones") }
 var csv by remember { mutableStateOf("") };var result by remember { mutableStateOf("") }
 val context=LocalContext.current
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->if(uri!=null)result=runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(csv) } ?: error("Archivo no disponible");"Reporte guardado" }.getOrElse { "No se pudo guardar: ${it.message}" } }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Text("Reportes e indicadores",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Text("Compara puntos y revisa su seguimiento")
  var filters by rememberSaveable { mutableStateOf(false) }
  OutlinedButton(onClick={filters=!filters},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(if(filters)"Ocultar filtros" else "Cambiar filtros · $days días")}
  if(filters) {
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilterChip(farm.isBlank(),{farm="";point=""},label={Text("Todas las granjas")})
   vm.visiblePoints().distinctBy { it.farm }.forEach { p->FilterChip(farm==p.farm,{farm=p.farm;point=""},label={Text(p.farmName)}) }
  }
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   FilterChip(point.isBlank(),{point=""},label={Text("Todos los puntos")})
   vm.visiblePoints().filter { farm.isBlank() || it.farm==farm }.forEach { p->FilterChip(point==p.id,{point=p.id},label={Text(p.id)}) }
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(1,7,30).forEach { d->FilterChip(days==d,{days=d},label={Text(if(d==1)"24 horas" else "$d días")}) }}
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Mediciones","Alertas","Acciones").forEach { k->FilterChip(kind==k,{kind=k},label={Text(k)}) }}
  }
  Text("$kind · ${if(farm.isBlank())"Todas las granjas" else farm} · ${if(point.isBlank())"Todos los puntos" else point} · $days días",style=MaterialTheme.typography.bodyMedium)
  val points=vm.visiblePoints().filter { (farm.isBlank() || it.farm==farm) && (point.isBlank() || it.id==point) };val ids=points.map { it.id };val cutoff=vm.now-days*86400000L
  Button(onClick={csv=vm.csv(kind,farm,point,days);exporter.launch("cloro_${kind.lowercase()}_${days}dias.csv")},modifier=Modifier.fillMaxWidth()){Text("Exportar $kind · CSV para Excel")}
  if(result.isNotBlank())Text(result)
  when(kind) {
   "Mediciones"->points.forEach { p->
    val rows=vm.readings(p.id).filter { it.time>=cutoff }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text("${p.id} · ${p.name}",fontWeight=FontWeight.Bold);Text(p.farmName)
     Text("Actual: ${vm.latest(p.id)?.let { vm.config.format(it.value) } ?: "—"} ppm")
     if(rows.isNotEmpty()) {
      Text("Promedio ${vm.config.format(rows.map { it.value }.average())} · Mínimo ${vm.config.format(rows.minOf { it.value })} · Máximo ${vm.config.format(rows.maxOf { it.value })}")
      if(point.isNotBlank())HistoryChart(rows)
     } else Text("Sin lecturas en el período")
    } }
   }
   "Alertas"->{val list=vm.data.incidents.filter { it.point in ids && it.opened>=cutoff };if(list.isEmpty())Text("Sin alertas en el período");list.forEach { i->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("${i.point} · ${i.direction}",fontWeight=FontWeight.Bold);Text("${if(i.closed)"Cerrada" else "Abierta"} · ${date(i.opened)}");if(i.closed)Text("Cierre ${date(i.closedAt)} · ${i.closedBy}")}} }}
   else->{val list=vm.data.actions.filter { it.point in ids && it.time>=cutoff };if(list.isEmpty())Text("Sin acciones en el período");list.forEach { a->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("${a.point} · ${a.resolution}",fontWeight=FontWeight.Bold);Text(a.text);Text("${a.author} · ${date(a.time)}")}} }}
  }
 }
}
