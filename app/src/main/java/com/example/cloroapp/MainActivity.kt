package com.example.cloroapp
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cloroapp.ui.screens.CloroApp
import com.example.cloroapp.ui.utils.windowWidth
import com.example.cloroapp.viewmodel.CloroViewModel
class MainActivity:ComponentActivity() {
 private var alertRequest by mutableStateOf(0)
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  if(intent.getBooleanExtra("open_alerts",false))alertRequest++
  setContent { val vm:CloroViewModel=viewModel();CloroApp(vm,windowWidth(this),alertRequest) { if(Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS) } }
 }
 override fun onNewIntent(intent:Intent) { super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("open_alerts",false))alertRequest++ }
}
