package com.example.cloroapp.ui.utils
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.material3.windowsizeclass.*
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable fun windowWidth(activity:Activity)=calculateWindowSizeClass(activity).widthSizeClass
