@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.cloroapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cloroapp.R
import com.example.cloroapp.model.*
import com.example.cloroapp.navigation.Route
import com.example.cloroapp.viewmodel.CloroViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val teal=Color(0xFF126F66)
fun date(t:Long)=if(t==0L)"Sin sincronización" else SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.forLanguageTag("es-CL")).format(Date(t))
fun levelColor(l:Level)=when(l) { Level.NORMAL->Color(0xFF167449);Level.WARNING->Color(0xFF986000);Level.CRITICAL->Color(0xFFB82E3A);else->Color.Gray }
@Composable fun CloroApp(vm:CloroViewModel,width:WindowWidthSizeClass,alertRequest:Int,requestPermission:()->Unit) {
 MaterialTheme(colorScheme=lightColorScheme(primary=teal,secondary=Color(0xFF4F647B),background=Color(0xFFF4F7F8))) {
  val owner=LocalLifecycleOwner.current
  var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
  DisposableEffect(owner) { val observer=LifecycleEventObserver { _,_ -> foreground=owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) };owner.lifecycle.addObserver(observer);onDispose { owner.lifecycle.removeObserver(observer) } }
  val nav=rememberNavController();val scope=rememberCoroutineScope();val drawer=rememberDrawerState(DrawerValue.Closed)
  val entry by nav.currentBackStackEntryAsState();val route=entry?.destination?.route
  val destinations=listOf(Route.Home,Route.Points,Route.Alerts)+(if(vm.role=="Jefatura")listOf(Route.Reports) else emptyList())+Route.Settings
  var showSettings by remember { mutableStateOf(false) }
  LaunchedEffect(alertRequest,vm.user?.id) { if(vm.user!=null)nav.navigate(if(alertRequest>0)Route.Alerts.path else Route.Home.path) { popUpTo(Route.Home.path);launchSingleTop=true } }
  LaunchedEffect(vm.user?.id,vm.config,vm.offline,foreground) { if(vm.user!=null && foreground) { while(true) { delay(vm.config.int("refreshSeconds")*1000L);vm.automaticRefresh() } } }
  fun go(path: String) {
   nav.navigate(path) {
    popUpTo(Route.Home.path) { saveState = false }
    launchSingleTop = true
    restoreState = false
   }
  }
  if(vm.user==null) { LoginScreen(vm);return@MaterialTheme }
  ModalNavigationDrawer(drawerState=drawer,drawerContent={ ModalDrawerSheet {
   Spacer(Modifier.height(20.dp));Text("CloroApp",Modifier.padding(20.dp),style=MaterialTheme.typography.titleLarge)
   destinations.forEach { d -> NavigationDrawerItem(label={Text(d.title)},selected=route==d.path,onClick={go(d.path);scope.launch { drawer.close() }}) }
   TextButton(onClick=vm::logout) { Text("Cerrar sesión") }
  } }) {
   Scaffold(topBar={TopAppBar(title={Text("CloroApp · ${vm.role}")},navigationIcon={TextButton(onClick={scope.launch {drawer.open()}}){Text("Menú")}},actions={TextButton(onClick=vm::refresh,enabled=!vm.busy,modifier=Modifier.heightIn(min=48.dp)){Text("Actualizar")}})},
    bottomBar={if(width==WindowWidthSizeClass.Compact)NavigationBar { destinations.forEach { d -> NavigationBarItem(selected=route==d.path,onClick={go(d.path)},icon={PageIcon(d)},label={Text(if(d==Route.Settings)"Ajustes" else d.title)}) } }}) { padding ->
    Row(Modifier.padding(padding).fillMaxSize()) {
     if(width!=WindowWidthSizeClass.Compact)NavigationRail { destinations.forEach { d -> NavigationRailItem(selected=route==d.path,onClick={go(d.path)},icon={PageIcon(d)},label={Text(if(d==Route.Settings)"Ajustes" else d.title)}) } }
     Column(Modifier.weight(1f)) {
      Text("Demostración · Datos de ejemplo",Modifier.padding(horizontal=16.dp),style=MaterialTheme.typography.labelSmall)
      Text("${if(vm.offline)"Sin conexión" else if(vm.demo)"Guardado en este teléfono" else "Datos compartidos"} · ${date(vm.data.synced)}",Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.labelSmall)
      if(vm.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
      if(vm.message.isNotBlank())Text(vm.message,Modifier.padding(horizontal=16.dp,vertical=4.dp),style=MaterialTheme.typography.bodySmall,color=if(vm.isError) MaterialTheme.colorScheme.error else teal)
      NavHost(navController=nav,startDestination=Route.Home.path,modifier=Modifier.weight(1f)) {
       composable(Route.Home.path) { Home(vm,width) { go(Route.Detail.forPoint(it)) } }
       composable(Route.Points.path) { Points(vm) { go(Route.Detail.forPoint(it)) } }
       composable(Route.Alerts.path) { Alerts(vm) { go(Route.Detail.forPoint(it)) } }
       composable(Route.Reports.path) { ReportsScreen(vm) }
       composable(Route.Settings.path) { Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) { Settings(vm,requestPermission) } }
       composable(Route.Detail.path) { e -> val id=e.arguments?.getString("id") ?: "";key(id) { Detail(vm,id) { nav.popBackStack() } } }
      }
     }
    }
   }
  }
 }
}
@Composable fun Brand() { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) { Image(painterResource(R.drawable.ic_water),contentDescription="Gota de agua",modifier=Modifier.size(52.dp));Column { Text("CloroApp",style=MaterialTheme.typography.headlineLarge,color=teal);Text("Monitoreo de agua · Caso 2") } } }
@Composable fun Stat(title:String,value:String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(value,style=MaterialTheme.typography.headlineMedium,color=teal);Text(title) } } }
@Composable fun Home(vm:CloroViewModel,width:WindowWidthSizeClass,onOpen:(String)->Unit) {
 val points=vm.visiblePoints();val active=vm.data.incidents.count { !it.closed && points.any { p->p.id==it.point } }
 val context=LocalContext.current
 var exportText by remember { mutableStateOf("") }
 val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if(uri!=null)context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(exportText) } }
 Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Brand();Text("Hola, ${vm.user?.name}");Text("Estado de mis granjas",style=MaterialTheme.typography.titleLarge)
  if(width==WindowWidthSizeClass.Compact) { Stat("Puntos asignados",points.size.toString());Stat("Desviaciones abiertas",active.toString()) }
  else Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { Column(Modifier.weight(1f)){Stat("Puntos asignados",points.size.toString())};Column(Modifier.weight(1f)){Stat("Desviaciones abiertas",active.toString())};if(width==WindowWidthSizeClass.Expanded)Column(Modifier.weight(1f)){Stat("Registros pendientes",vm.pending.toString())} }
  if(points.isEmpty())Text("No hay información disponible. Actualice con conexión o cargue el simulador.")
  if(vm.config.enabled("showComparison"))points.groupBy { it.farmName }.forEach { (farm,list)->Card { Column(Modifier.padding(16.dp)) { Text(farm,style=MaterialTheme.typography.titleMedium);Text("${list.count { vm.level(it.id)==Level.NORMAL }} normales · ${list.count { vm.level(it.id)==Level.WARNING }} advertencias · ${list.count { vm.level(it.id)==Level.CRITICAL }} críticos") } } }
  points.filter { vm.level(it.id)!=Level.NORMAL }.sortedByDescending { vm.level(it.id)==Level.CRITICAL }.forEach { PointCard(it,vm,onOpen) }
  if(vm.can("export") && vm.config.enabled("csvExport"))Button(onClick={exportText=vm.csv();exporter.launch("mediciones_demo.csv")}) { Text("Exportar mediciones CSV") }
 }
}
@Composable fun PointCard(p:Point,vm:CloroViewModel,onOpen:(String)->Unit) {
 val reading=vm.latest(p.id);val level=vm.level(p.id)
 Card(Modifier.fillMaxWidth().clickable { onOpen(p.id) }) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Text(p.name,style=MaterialTheme.typography.titleMedium);Text("${p.farmName} · ${p.barn.ifBlank { "Sin galpón" }} · ${p.id}",style=MaterialTheme.typography.bodySmall)
  Text(if(reading==null)"Sin mediciones" else "${vm.config.format(reading.value)} ${vm.config.json.getString("unit")}",style=MaterialTheme.typography.headlineMedium)
  Text(level.label+if(level!=Level.NORMAL)" · ${vm.direction(p.id)}" else "",color=levelColor(level));Text("${date(reading?.time ?: 0)}${if(vm.stale(p.id))" · Dato desactualizado" else ""}",style=MaterialTheme.typography.bodySmall)
 } }
}
@Composable fun Points(vm:CloroViewModel,onOpen:(String)->Unit) {
 var farm by rememberSaveable { mutableStateOf("Todas") };val points=vm.visiblePoints()
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Text("Granjas y puntos",style=MaterialTheme.typography.headlineSmall);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { (listOf("Todas")+points.map { it.farmName }.distinct()).forEach { name -> FilterChip(selected=farm==name,onClick={farm=name},label={Text(name)}) } } }
  items(points.filter { farm=="Todas" || it.farmName==farm },key={it.id}) { PointCard(it,vm,onOpen) }
  if(points.isEmpty())item { Text("No hay puntos guardados para este usuario.") }
 }
}
@Composable fun Alerts(vm:CloroViewModel,onOpen:(String)->Unit) {
 var history by rememberSaveable { mutableStateOf(false) };val ids=vm.visiblePoints().map { it.id }
 val list=vm.data.incidents.filter { it.point in ids && (history || !it.closed) }.sortedByDescending { it.opened }
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { Text("Alertas y desviaciones",style=MaterialTheme.typography.headlineSmall);Row(verticalAlignment=Alignment.CenterVertically){Switch(history,{history=it});Text("Incluir cerradas")};Text("Las críticas requieren atención prioritaria. Se muestra la antigüedad; no hay escalamiento automático.",style=MaterialTheme.typography.bodySmall) }
  if(list.isEmpty())item { Text("No hay alertas para mostrar.") }
  items(list,key={it.id}) { i -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("${i.point} · ${if(i.level=="CRITICAL")"Crítico" else "Advertencia"} · ${i.direction}",style=MaterialTheme.typography.titleMedium);Text("${date(i.opened)} · ${if(i.closed)"Cerrada por ${i.closedBy} el ${date(i.closedAt)}" else if(i.acknowledged)"Reconocida" else "Pendiente"}");if(!i.closed)Text("${((vm.now-i.opened)/60000).coerceAtLeast(0)} min de antigüedad")
   if(!i.closed && vm.now-i.opened>vm.config.int("escalationMinutes")*60000L)Text("Requiere atención del supervisor",color=Color(0xFFB82E3A))
   if(!i.closed && vm.level(i.point)==Level.NORMAL)Text("Lectura normal; pendiente de cierre y seguimiento.")
   TextButton(onClick={onOpen(i.point)}){Text("Ver punto y registrar seguimiento")}
   if(!i.closed && vm.can("ack"))OutlinedButton(onClick={vm.updateIncident(i.id,false)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Reconocer")}
   if(!i.closed && vm.can("close"))OutlinedButton(onClick={vm.updateIncident(i.id,true)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Cerrar con validación")}
  } } }
 }
}
@Composable fun Detail(vm:CloroViewModel,id:String,back:()->Unit) {
 val point=vm.visiblePoints().find {it.id==id}
 if(point==null){Text("Punto inexistente o no asignado");return}
 var value by rememberSaveable(id){mutableStateOf("")};var extra by rememberSaveable(id){mutableStateOf("")};var action by rememberSaveable(id){mutableStateOf("")};var correction by rememberSaveable(id){mutableStateOf(false)};var reason by rememberSaveable(id){mutableStateOf("")};var original by rememberSaveable(id){mutableStateOf("")};var tab by rememberSaveable(id){mutableStateOf(if(vm.can("measure"))"Control" else "Histórico")}
 val active=vm.data.incidents.firstOrNull { it.point==id && !it.closed }
 val rows=vm.readings(id).filter { vm.now-it.time<=vm.historyDays*86400000L }
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item { TextButton(onClick=back){Text("Volver")};PointCard(point,vm,{});Text("${point.type} · ${point.location}",Modifier.padding(vertical=8.dp));Row(modifier=Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){(if(vm.can("measure"))listOf("Control","Seguimiento","Histórico") else listOf("Seguimiento","Histórico")).forEach { name->FilterChip(tab==name,{tab=name},label={Text(name)}) }} }
  item { Text(when(tab){"Control"->"Registra una nueva lectura de este punto.";"Seguimiento"->"Documenta lo realizado y verifica la solución de la alerta.";else->"Consulta lecturas anteriores y sus tendencias."},Modifier.padding(vertical=8.dp)) }
  if(tab=="Control" && (vm.can("measure") || vm.can("correct")))item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Registrar en ${point.id} · ${point.name}",style=MaterialTheme.typography.titleMedium)
   OutlinedTextField(value,{value=it},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),supportingText={Text("Ejemplo: 3,50 ppm · acepta coma o punto")},label={Text("Cloro libre · ${vm.config.json.getString("unit")}")},modifier=Modifier.fillMaxWidth(),singleLine=true)
   if(vm.config.json.getJSONArray("extraFields").length()>0)OutlinedTextField(extra,{extra=it},label={Text("Datos adicionales opcionales")},supportingText={Text(vm.config.json.getJSONArray("extraFields").strings().joinToString())},modifier=Modifier.fillMaxWidth())
   val hasLatest=vm.latest(id)!=null
   if(vm.can("correct"))Row(verticalAlignment=Alignment.CenterVertically){Checkbox(correction,{correction=it;original=if(it)vm.latest(id)?.id ?: "" else ""},enabled=hasLatest);Text(if(hasLatest)"Corregir última lectura conservando la original" else "Sin lecturas previas para corregir")}
   if(correction)OutlinedTextField(reason,{reason=it},label={Text("Motivo obligatorio de corrección")},modifier=Modifier.fillMaxWidth())
   Button(onClick={vm.saveReading(id,value,extra,if(correction)original else "",reason) { value="";extra="";reason="";correction=false;original="" }},enabled=!vm.busy && (vm.can("measure") || correction),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Guardar medición")}
  } } }
  if(tab=="Seguimiento")item { Card { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text(if(active!=null)"${active.direction} · seguimiento" else "Sin alerta abierta",style=MaterialTheme.typography.titleMedium)
   Text("1. Verificar la lectura. 2. Documentar una acción adecuada al tipo de desviación. 3. Registrar una medición normal. 4. Supervisor valida el cierre.")
   if(active!=null && vm.can("ack"))OutlinedButton(onClick={vm.updateIncident(active.id,false)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Reconocer alerta")}
   if(active!=null && vm.can("close"))OutlinedButton(onClick={vm.updateIncident(active.id,true)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Validar acción y cerrar")}
   if(active!=null && vm.can("action")) {
   OutlinedTextField(action,{action=it},label={Text("Acción realizada y resultado")},modifier=Modifier.fillMaxWidth())
   Button(onClick={vm.addAction(id,active.id,action) { action="" }},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Registrar acción")}
   TextButton(onClick={tab="Control"}){Text("Registrar medición de verificación ›")}
   }
  } } }
  if(tab=="Histórico") {
  item { Text("Histórico",style=MaterialTheme.typography.titleLarge);Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.horizontalScroll(rememberScrollState())) { listOf(1,7,vm.config.int("historyDays")).distinct().forEach { days ->FilterChip(selected=vm.historyDays==days,onClick={vm.historyDays=days},label={Text(if(days==1)"24 horas" else "$days días")}) } }
   if(rows.isNotEmpty()) { Text("Mín. ${vm.config.format(rows.minOf { it.value })} · Máx. ${vm.config.format(rows.maxOf { it.value })} · Prom. ${vm.config.format(rows.map {it.value}.average())}")
    if(vm.config.enabled("showChart"))HistoryChart(rows)
   } else Text("Sin lecturas en este período.")
  }
  items(rows.take(300),key={it.id}) { r -> ListItem(headlineContent={Text("${vm.config.format(r.value)} ${vm.config.json.getString("unit")} · ${when(r.status){"NORMAL"->"Normal";"WARNING"->"Advertencia";"CRITICAL"->"Crítico";else->"Sin estado"}}")},supportingContent={Text("${date(r.time)} · ${r.author}\n${r.extras}${if(r.replaces.isNotBlank())"\nCorrección de ${r.replaces} · Motivo: ${r.reason}" else ""}")}) }
  }
  if(tab=="Seguimiento") {
  if(vm.can("action")) item { PhotoEvidence(id) }
  item { Text("Acciones registradas",style=MaterialTheme.typography.titleLarge) }
  items(vm.data.actions.filter {it.point==id}.sortedByDescending {it.time},key={it.id}) { a -> ListItem(headlineContent={Text(a.text)},supportingContent={Text("${date(a.time)} · ${a.author}\n${a.resolution} · Medición ${a.reading}")}) }
  }
 }
}
@Composable fun HistoryChart(rows:List<Reading>) {
 val values=rows.take(60).reversed().map { it.value.toFloat() }
 Column { Canvas(Modifier.fillMaxWidth().height(110.dp).padding(12.dp)) {
  val max=(values.maxOrNull() ?: 1f).coerceAtLeast(1f)
  if(values.size==1) {
   drawCircle(teal,radius=8f,center=Offset(size.width/2,size.height*(1-values[0]/max)))
  } else {
   values.zipWithNext().forEachIndexed { i,(a,b)-> drawLine(teal,Offset(size.width*i/(values.size-1).coerceAtLeast(1),size.height*(1-a/max)),Offset(size.width*(i+1)/(values.size-1).coerceAtLeast(1),size.height*(1-b/max)),strokeWidth=4f) }
  }
 };Text("Tendencia: hasta 60 lecturas, de la más antigua a la reciente. Consulte valores exactos en la lista.",style=MaterialTheme.typography.labelSmall) }
}
@Composable fun Settings(vm:CloroViewModel,permission:()->Unit) {
 val thresholds=remember(vm.config) { mutableStateMapOf("criticalLow" to vm.config.number("criticalLow").toString(),"normalMin" to vm.config.number("normalMin").toString(),"normalMax" to vm.config.number("normalMax").toString(),"criticalHigh" to vm.config.number("criticalHigh").toString()) }
 var rangeError by remember { mutableStateOf("") }
 var text by remember(vm.config) { mutableStateOf(vm.config.json.toString(2)) };var url by remember {mutableStateOf(vm.api)};var demo by remember {mutableStateOf(vm.demo)}
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("Configuración",style=MaterialTheme.typography.headlineSmall)
  Text("Notificaciones",style=MaterialTheme.typography.titleMedium)
  OutlinedButton(onClick=permission,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Activar notificaciones")}
  Text("Mediciones por sincronizar: ${vm.pending}")
  if(vm.role=="Operario") { Text("La conexión y los rangos los administra el supervisor.");return@Column }
  Text("Conexión y almacenamiento",style=MaterialTheme.typography.titleMedium)
  Row(verticalAlignment=Alignment.CenterVertically){Switch(vm.offline,vm::toggleOfflineMode);Text("Simular sin conexión")}
  Row(verticalAlignment=Alignment.CenterVertically){Switch(demo,{demo=it});Text("Usar simulador local")}
  OutlinedTextField(url,{url=it},label={Text("URL API simulada")},modifier=Modifier.fillMaxWidth())
  Button(onClick={vm.setSource(demo,url)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Guardar origen")}
  Text("Mediciones por sincronizar: ${vm.pending}. En modo local se guardan solo en este celular. El servidor incluido permite probar consumo HTTP y compartir registros.")

  Text("Normal: ${vm.config.number("normalMin")}–${vm.config.number("normalMax")}. Crítico: menor que ${vm.config.number("criticalLow")} o mayor que ${vm.config.number("criticalHigh")}. Los restantes son advertencia. ")
  if(vm.can("close")) {
   Text("Datos de demostración",style=MaterialTheme.typography.titleMedium)
   Button(onClick=vm::generate,enabled=!vm.busy && !vm.offline){Text("Generar nueva ronda simulada")}
   if(vm.demo)Row(verticalAlignment=Alignment.CenterVertically){Switch(vm.autoSimulation,{vm.autoSimulation=it});Text("Simular cada 5 minutos con app abierta",Modifier.weight(1f))}
  }
  Text("Actualizar consulta datos existentes. Generar una ronda sí registra nuevas mediciones. Alertas locales con la app visible; sin push.")
  if(!vm.can("close") || !vm.demo)return@Column
  Text("Editar rangos fácilmente",style=MaterialTheme.typography.titleMedium)
  listOf("criticalLow" to "Crítico si es menor que", "normalMin" to "Normal desde", "normalMax" to "Normal hasta", "criticalHigh" to "Crítico si es mayor que").forEach { (key,label) ->
   OutlinedTextField(thresholds[key] ?: "",{thresholds[key]=it},label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())
  }
  if(rangeError.isNotBlank())Text(rangeError,color=MaterialTheme.colorScheme.error)
  Button(onClick={
   val numbers=thresholds.mapValues { it.value.replace(',','.').toDoubleOrNull() }
   if(numbers.values.any { it==null || !it.isFinite() })rangeError="Ingrese cuatro números válidos"
   else { val updated=org.json.JSONObject(vm.config.json.toString());numbers.forEach { (key,value)->updated.put(key,value) };rangeError="";vm.saveConfig(updated.toString()) }
  },enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Guardar rangos")}
  var advanced by remember { mutableStateOf(false) }
  TextButton(onClick={advanced=!advanced}){Text(if(advanced)"Ocultar opciones avanzadas" else "Opciones avanzadas por punto")}
  if(advanced) {
  OutlinedTextField(text,{text=it},label={Text("Reglas editables JSON")},modifier=Modifier.fillMaxWidth().heightIn(min=250.dp,max=500.dp))
  Button(onClick={vm.saveConfig(text)},enabled=!vm.busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Validar y guardar reglas")}
  TextButton(onClick={text=vm.defaultConfig()}){Text("Cargar valores originales en el editor")}
  Text("pointRanges permite definir los cuatro límites por código de punto. Los valores generales se usan donde no hay excepción.",style=MaterialTheme.typography.bodySmall)
  }
 }
}
@Preview(widthDp=360,heightDp=240,showBackground=true) @Composable fun CompactPreview(){MaterialTheme{Column(Modifier.padding(16.dp)){Brand();Stat("Puntos asignados","4")}}}
@Preview(widthDp=700,heightDp=240,showBackground=true) @Composable fun MediumPreview(){MaterialTheme{Row(Modifier.padding(16.dp)){Column(Modifier.weight(1f)){Brand()};Column(Modifier.weight(1f)){Stat("Alertas","2")}}}}
@Preview(widthDp=1000,heightDp=240,showBackground=true) @Composable fun ExpandedPreview(){MaterialTheme{Row(Modifier.padding(16.dp)){Column(Modifier.weight(1f)){Brand()};Column(Modifier.weight(1f)){Stat("Puntos","4")};Column(Modifier.weight(1f)){Stat("Alertas","2")}}}}
