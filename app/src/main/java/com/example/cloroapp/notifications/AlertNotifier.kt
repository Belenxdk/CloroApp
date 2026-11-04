package com.example.cloroapp.notifications
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cloroapp.MainActivity
import com.example.cloroapp.R
class AlertNotifier(private val context:Context) {
 fun show(id:String,title:String,text:String) {
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  val manager=context.getSystemService(NotificationManager::class.java)
  if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(NotificationChannel("cloro_alerts","Alertas de cloro demo",NotificationManager.IMPORTANCE_HIGH))
  val intent=Intent(context,MainActivity::class.java).putExtra("open_alerts",true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
  val pending=PendingIntent.getActivity(context,1,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  NotificationManagerCompat.from(context).notify(id.hashCode(),NotificationCompat.Builder(context,"cloro_alerts").setSmallIcon(R.drawable.ic_water).setContentTitle(title).setContentText(text).setContentIntent(pending).setAutoCancel(true).build())
 }
}
