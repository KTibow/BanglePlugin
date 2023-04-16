package io.github.ktibow.bangleplugin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import io.github.ktibow.bangleplugin.connect.Permission
import io.github.ktibow.bangleplugin.connect.Scan
import io.github.ktibow.bangleplugin.ui.theme.BanglePluginTheme
import io.github.ktibow.bangleplugin.ui.theme.Typography
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "config")
val DEVICE_KEY = stringPreferencesKey("device")

class MainActivity : ComponentActivity() {
  private var device: String? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      BanglePluginTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background,
        ) {
          device?.let {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              val name = it.replace(":".toRegex(), "").substring(8)
              Card(
                  modifier = Modifier
                      .fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(16.dp)) {
                  Text("Bangle.js $name", style = Typography.headlineMedium)
                  Text("Everything should be ready to track")
                }
              }
              Button(
                  onClick = { openScan() },
                  modifier = Modifier.fillMaxWidth(),
              ) {
                Text("Choose another Bangle")
              }
            }
          } ?: Column(
              modifier = Modifier
                      .fillMaxSize()
                      .padding(16.dp),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Hold on...")
            Image(
                painter = painterResource(id = R.drawable.watch_splash),
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Splash screen",
            )
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (ActivityCompat.checkSelfPermission(
          this, Manifest.permission.BLUETOOTH_CONNECT,
      ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
          this, Manifest.permission.BLUETOOTH_SCAN,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      val intent = Intent(
          this, Permission::class.java,
      )
      startActivity(intent);
      return;
    }
    lifecycleScope.launch {
      this@MainActivity.dataStore.data.map { it[DEVICE_KEY] }.collect {
        if (it == null) {
          openScan()
        } else {
          device = it
        }
      }
    }
  }

  private fun openScan() {
    val intent = Intent(this, Scan::class.java)
    startActivity(intent)
  }
}
