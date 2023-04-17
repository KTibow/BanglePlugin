package io.github.ktibow.bangleplugin.connect

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import io.github.ktibow.bangleplugin.MainActivity
import io.github.ktibow.bangleplugin.ui.theme.BanglePluginTheme

class Permission : ComponentActivity() {
  private val permission =
      registerForActivityResult(RequestMultiplePermissions()) { granted ->
        val allGranted = granted.all { it.value }
        if (allGranted) {
          val intent =
              Intent(
                  this,
                  MainActivity::class.java,
              )
          startActivity(intent)
          finish()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    this.onBackPressedDispatcher.addCallback(this, false) {}
    setContent {
      BanglePluginTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
          Column(
              modifier = Modifier.padding(16.dp).statusBarsPadding(),
              verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Connect to Bangle")
                Button(
                    onClick = {
                      permission.launch(
                          arrayOf(
                              Manifest.permission.BLUETOOTH_CONNECT,
                              Manifest.permission.BLUETOOTH_SCAN,
                          ),
                      )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                  Text("Allow Bluetooth")
                }
              }
        }
      }
    }
  }
}
