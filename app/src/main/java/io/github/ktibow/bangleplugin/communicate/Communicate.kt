package io.github.ktibow.bangleplugin.communicate

import com.juul.kable.State
import com.juul.kable.characteristicOf
import com.juul.kable.identifier
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch


object Communicate : CoroutineScope {
  override val coroutineContext = Dispatchers.Default
  val isConnected get() = BanglePluginApp.watchConnection != null && BanglePluginApp.watchConnection!!.state.value == State.Connected
  fun connect(device: String) {
    if (BanglePluginApp.watchConnection == null || BanglePluginApp.watchConnection!!.identifier != device) BanglePluginApp.watchConnection =
        peripheral(device)
    launch {
      BanglePluginApp.watchConnection!!.connect()
    }
  }

  fun disconnect() {
    if (BanglePluginApp.watchConnection == null) return
    launch {
      BanglePluginApp.watchConnection!!.disconnect()
    }
  }

  private var xoff = false
  private var xoffSince: Long = 0
  fun streamLines(): Flow<String> {
    if (BanglePluginApp.watchConnection == null) throw IllegalStateException("No connection")
    return flow {
      BanglePluginApp.watchConnection!!.observe(
          characteristicOf(
              "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
              "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
          ),
      ).fold("") { accumulator, chunk ->
        val totalData = accumulator + chunk.toString(Charsets.UTF_8).filter {
          when (it.code) {
            17 -> {
              xoff = false
              false
            }

            19 -> {
              xoff = true
              xoffSince = System.currentTimeMillis()
              false
            }

            else -> true
          }
        }
        for (line in totalData.split('\n').dropLast(1)) {
          emit(line)
        }
        totalData.split("\n").last()
      }
    }
  }

  suspend fun sendData(data: String) {
    if (BanglePluginApp.watchConnection == null) throw IllegalStateException("No connection")
    val chunks = data.chunked(20)
    for (chunk in chunks) {
      BanglePluginApp.watchConnection!!.write(
          characteristicOf(
              "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
              "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
          ),
          chunk.encodeToByteArray(),
      )
    }
  }
}
