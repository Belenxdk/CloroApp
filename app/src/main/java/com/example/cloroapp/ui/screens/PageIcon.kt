package com.example.cloroapp.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import com.example.cloroapp.navigation.Route
@Composable fun PageIcon(route:Route) {
 val icon=ImageVector.Builder(route.title,30.dp,30.dp,24f,24f).apply {
 path(fill=SolidColor(Color(0xFF114C46))) {
 when(route) {
 Route.Home->{moveTo(2f,11f);lineTo(12f,2f);lineTo(22f,11f);lineTo(19f,11f);lineTo(19f,22f);lineTo(14f,22f);lineTo(14f,15f);lineTo(10f,15f);lineTo(10f,22f);lineTo(5f,22f);lineTo(5f,11f);close()}
 Route.Alerts->{moveTo(12f,2f);lineTo(23f,22f);lineTo(1f,22f);close()}
 Route.Reports->{moveTo(3f,22f);lineTo(3f,13f);lineTo(8f,13f);lineTo(8f,22f);close();moveTo(10f,22f);lineTo(10f,8f);lineTo(15f,8f);lineTo(15f,22f);close();moveTo(17f,22f);lineTo(17f,2f);lineTo(22f,2f);lineTo(22f,22f);close()}
 Route.Settings->{moveTo(2f,5f);lineTo(22f,5f);lineTo(22f,8f);lineTo(2f,8f);close();moveTo(2f,16f);lineTo(22f,16f);lineTo(22f,19f);lineTo(2f,19f);close();moveTo(6f,2f);lineTo(10f,2f);lineTo(10f,11f);lineTo(6f,11f);close();moveTo(14f,13f);lineTo(18f,13f);lineTo(18f,22f);lineTo(14f,22f);close()}
 else->{for(x in listOf(3f,14f))for(y in listOf(3f,14f)){moveTo(x,y);lineTo(x+7,y);lineTo(x+7,y+7);lineTo(x,y+7);close()}}
 }
 }
 if(route==Route.Alerts)path(fill=SolidColor(Color.White)){moveTo(11f,9f);lineTo(13f,9f);lineTo(13f,16f);lineTo(11f,16f);close();moveTo(11f,18f);lineTo(13f,18f);lineTo(13f,20f);lineTo(11f,20f);close()}
 }.build()
 Icon(icon,route.title,Modifier.size(30.dp),tint=Color.Unspecified)
}
