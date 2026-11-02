package com.example.cloroapp.ui.screens
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import java.io.File
@Composable fun PhotoEvidence(point:String) {
 val context=LocalContext.current
 val folder=remember(point){File(context.filesDir,"evidence/$point").apply{mkdirs()}}
 var photos by remember(point){mutableStateOf(folder.listFiles { _, name -> name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) }?.sortedByDescending { it.name } ?: emptyList())}
 var status by remember(point){mutableStateOf("")}
 val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->
  if(bitmap!=null)runCatching {
   val file=File(folder,"${System.currentTimeMillis()}.jpg")
   file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)}
   photos=folder.listFiles { _, name -> name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) || name.endsWith(".png", true) }?.sortedByDescending { it.name } ?: emptyList();status="Foto guardada en $point"
  }.onFailure{status="No se pudo guardar la foto"}
 }
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Evidencia fotográfica",style=MaterialTheme.typography.titleMedium)
  Text("Fotos de referencia del punto. Se guardan solo en este teléfono, en resolución reducida; no se envían con las acciones.")
  Button(onClick={runCatching{camera.launch(null)}.onFailure{status="No hay una aplicación de cámara disponible"}},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("Abrir cámara")}
  if(status.isNotBlank())Text(status)
  photos.take(3).forEach{file->
   val bitmap=remember(file){BitmapFactory.decodeFile(file.absolutePath)}
   if(bitmap!=null)Image(bitmap.asImageBitmap(),"Evidencia de $point · ${date(file.nameWithoutExtension.toLongOrNull() ?: 0)}",Modifier.fillMaxWidth().height(180.dp))
  }
  if(photos.size>3)Text("Se muestran las 3 fotos más recientes de ${photos.size} guardadas.")
 } }
}
