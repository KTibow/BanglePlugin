package io.github.ktibow.bangleplugin.communicate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.juul.kable.State
import io.github.ktibow.bangleplugin.DEVICE_KEY
import io.github.ktibow.bangleplugin.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class Receiver : BroadcastReceiver() {
  private var accelJob: Job? = null
  private val startAccel =
      "\n!function(){" +
          "let e=0;" +
          "Bangle.on(\"accel\",a=>e=Math.max(e,a.mag))," +
          "this.accel_interval&&clearInterval(this.accel_interval)," +
          "this.accel_interval=setInterval(()=>{" +
          "console.log(\"[accel_max\",e.toFixed(6),\"]\"),e=0" +
          "},1e4)" +
          "}();\n"

  override fun onReceive(context: Context, intent: Intent) {
    Log.d("Receiver", "onReceive: " + intent.action)
    when (intent.action) {
      "com.urbandroid.sleep.watch.CHECK_CONNECTED" -> {
        if (Communicate.isConnected) {
          context.sendBroadcast(Intent("com.urbandroid.sleep.watch.CONFIRM_CONNECTED"))
        } else {
          goAsync {
            connect(context)
            context.sendBroadcast(Intent("com.urbandroid.sleep.watch.CONFIRM_CONNECTED"))
          }
        }
      }
      "com.urbandroid.sleep.watch.START_TRACKING" -> {
        goAsync {
          if (accelJob != null) accelJob!!.cancel()
          if (!Communicate.isConnected) connect(context)
          accelJob = launch {
            Log.i("Receiver", "Starting accel")
            Communicate.sendData(startAccel)
          }
          Communicate.streamLines().collect {
            Log.d("BLE Connection", it)
            val accel =
                Regex("\\[accel_max ([0-9\\.]+) \\]")
                    .findAll(it)
                    .firstOrNull()
                    ?.groups
                    ?.get(1)
                    ?.value
                    ?.toFloatOrNull()
                    ?: return@collect
            if (accelJob != null) accelJob!!.cancel()
            accelJob = launch { scheduleAccel() }
            AppState.batch += accel
            if (AppState.batch.size >= AppState.batchSize) {
              sendBatch(context)
              AppState.batch = arrayOf()
            }
          }
        }
      }
      "com.urbandroid.sleep.watch.HINT" -> {
        val repeatCount = intent.getIntExtra("REPEAT", 1)
        goAsync {
          if (!Communicate.isConnected) connect(context)
          Communicate.sendData(
              "\n!function(){" +
                  "let i=$repeatCount;" +
                  "!function n(){" +
                  "Bangle.buzz(300)" +
                  ".then(()=>{if(i>1)return new Promise(e=>setTimeout(e,300))})" +
                  ".then(()=>{--i>0&&n()})" +
                  "}()" +
                  "}();\n",
          )
        }
      }
      "com.urbandroid.sleep.watch.SET_BATCH_SIZE" -> {
        AppState.batchSize = intent.getLongExtra("SIZE", 12).toInt()
        Log.i("Receiver", "Batch size " + AppState.batchSize)
      }
      "com.urbandroid.sleep.watch.STOP_TRACKING" -> {
        goAsync {
          if (!Communicate.isConnected) {
            Log.i("Receiver", "Disconnected already before stopping")
            return@goAsync
          }
          Communicate.sendData(
              "\nif (this.accel_interval) clearInterval(this.accel_interval);\n",
          )
          Communicate.disconnect()
        }
      }
    }
  }

  private fun goAsync(
      context: CoroutineContext = EmptyCoroutineContext,
      block: suspend CoroutineScope.() -> Unit
  ) {
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob()).launch(context) {
      try {
        block()
      } finally {
        pendingResult.finish()
      }
    }
  }

  private suspend fun connect(context: Context) {
    val device = context.dataStore.data.map { it[DEVICE_KEY] }.first()
    if (device == null) {
      val startIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

      startIntent!!.flags =
          Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
              Intent.FLAG_ACTIVITY_NEW_TASK or
              Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
      context.startActivity(startIntent)
    } else {
      Communicate.connect(device).join()
    }
  }

  private fun sendBatch(context: Context) {
    val intent = Intent("com.urbandroid.sleep.watch.DATA_UPDATE")
    val _batch = AppState.batch
    val batch =
        _batch
            .sliceArray(IntRange(_batch.size - AppState.batchSize, _batch.size - 1))
            .toFloatArray()
    Log.d("Receiver", "Sending " + batch.joinToString(",") { it.toString() })
    intent.setPackage("com.urbandroid.sleep")
    intent.putExtra("MAX_RAW_DATA", batch)
    context.sendBroadcast(intent)
  }

  private suspend fun scheduleAccel() {
    delay(20000)
    if (AppState.watchConnection?.state?.value != State.Connected) return
    Log.i("Receiver", "Starting accel (Scheduled)")
    Communicate.sendData(startAccel)
  }
}
