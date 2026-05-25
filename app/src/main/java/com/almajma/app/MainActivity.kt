package com.almajma.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.almajma.app.ui.PlatformViewModel
import com.almajma.app.ui.screens.AlMajmaAppUi
import com.almajma.app.ui.theme.AlMajmaTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: PlatformViewModel = viewModel()
      val primaryColorHex by viewModel.primaryColor.collectAsStateWithLifecycle()
      val secondaryColorHex by viewModel.secondaryColor.collectAsStateWithLifecycle()

      val dynamicPrimary = try {
          Color(android.graphics.Color.parseColor(primaryColorHex))
      } catch (e: Exception) {
          null
      }
      val dynamicSecondary = try {
          Color(android.graphics.Color.parseColor(secondaryColorHex))
      } catch (e: Exception) {
          null
      }

      AlMajmaTheme(
          dynamicPrimary = dynamicPrimary,
          dynamicSecondary = dynamicSecondary
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AlMajmaAppUi(viewModel = viewModel)
        }
      }
    }
  }
}
