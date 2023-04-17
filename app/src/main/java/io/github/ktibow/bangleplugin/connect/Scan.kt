package io.github.ktibow.bangleplugin.connect

import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import io.github.ktibow.bangleplugin.DEVICE_KEY
import io.github.ktibow.bangleplugin.MainActivity
import io.github.ktibow.bangleplugin.dataStore
import io.github.ktibow.bangleplugin.ui.theme.BanglePluginTheme
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class Scan : ComponentActivity() {
  private val deviceManager: CompanionDeviceManager by lazy {
    getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
  }
  private val associationResult =
      registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        onAssociation(it)
      }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    scan()
    setContent {
      BanglePluginTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
          Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { scan() }, modifier = Modifier.fillMaxWidth()) {
                  Text("Scan for your Bangle.js")
                }
                Text(
                    "Nothing happening when you tap? Make sure location is on and your Bangle.js is visible")
              }
        }
      }
    }
  }

  private fun scan() {
    val deviceFilter =
        BluetoothDeviceFilter.Builder().setNamePattern(Pattern.compile("Bangle\\.js")).build()
    val associationRequest = AssociationRequest.Builder().addDeviceFilter(deviceFilter).build()
    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
      deviceManager.associate(
          associationRequest,
          { it.run() },
          object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
              Log.i("Scan", "Device found")
              associationResult.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            override fun onFailure(error: CharSequence?) {
              Log.w("Scan", error.toString())
            }
          })
    } else {
      deviceManager.associate(
          associationRequest,
          object : CompanionDeviceManager.Callback() {
            @Deprecated("Deprecated in Java")
            override fun onDeviceFound(intentSender: IntentSender) {
              Log.i("Scan", "Device found")
              associationResult.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            override fun onFailure(error: CharSequence?) {
              Log.w("Scan", error.toString())
            }
          },
          null)
    }
  }
  @Suppress("DEPRECATION")
  private fun onAssociation(result: ActivityResult) {
    if (result.resultCode != RESULT_OK) return
    val device: BluetoothDevice? =
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU)
            result.data?.getParcelableExtra(
                CompanionDeviceManager.EXTRA_ASSOCIATION, BluetoothDevice::class.java)
        else result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
    if (device == null) {
      Log.w("Scan", "Device is null")
      return
    }
    Log.i("Scan", "Device associated: ${device.address}")
    lifecycleScope.launch {
      dataStore.edit {
        it[DEVICE_KEY] = device.address
        val intent = Intent(this@Scan, MainActivity::class.java)
        startActivity(intent)
        finish()
      }
    }
  }
  //  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  //    if (requestCode != 0) return super.onActivityResult(requestCode, resultCode, data)
  //    Log.i("Scan", "Device association returned $resultCode")
  //    when (resultCode) {
  //      Activity.RESULT_OK -> {
  //        val intent = Intent(this@Scan, MainActivity::class.java)
  //        startActivity(intent)
  //        finish()
  //      }
  //    }
  //  }
}
