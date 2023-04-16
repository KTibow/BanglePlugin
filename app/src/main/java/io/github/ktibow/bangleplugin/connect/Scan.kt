package io.github.ktibow.bangleplugin.connect

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import io.github.ktibow.bangleplugin.DEVICE_KEY
import io.github.ktibow.bangleplugin.MainActivity
import io.github.ktibow.bangleplugin.dataStore
import io.github.ktibow.bangleplugin.ui.theme.BanglePluginTheme
import kotlinx.coroutines.launch

class Scan : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      BanglePluginTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
          ScanScreen(data = dataStore, onChoose = { onChoose(it) })
        }
      }
    }
  }

  private fun onChoose(device: String) {
    lifecycleScope.launch {
      dataStore.edit {
        it[DEVICE_KEY] = device
        val intent = Intent(this@Scan, MainActivity::class.java)
        startActivity(intent)
        finish()
      }
    }
  }
}
