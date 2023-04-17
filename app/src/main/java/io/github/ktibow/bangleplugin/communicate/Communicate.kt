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
  val isConnected
    get() =
        AppState.watchConnection != null &&
            AppState.watchConnection!!.state.value == State.Connected
  fun connect(device: String) = launch {
    if (AppState.watchConnection == null || AppState.watchConnection!!.identifier != device)
        AppState.watchConnection = peripheral(device)
    AppState.watchConnection!!.connect()
  }

  fun disconnect() {
    if (AppState.watchConnection == null) return
    launch { AppState.watchConnection!!.disconnect() }
  }

  private var xoff = false // if you want to implement xoff stuff send a pr
  fun streamLines(): Flow<String> {
    if (AppState.watchConnection == null) throw IllegalStateException("No connection")
    return flow {
      AppState.watchConnection!!.observe(
              characteristicOf(
                  "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                  "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
              ),
          )
          .fold("") { accumulator, chunk ->
            val totalData =
                accumulator +
                    chunk.toString(Charsets.UTF_8).filter {
                      when (it.code) {
                        17 -> {
                          xoff = false
                          false
                        }
                        19 -> {
                          xoff = true
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
    if (AppState.watchConnection == null) throw IllegalStateException("No connection")
    val chunks = data.chunked(20)
    for (chunk in chunks) {
      AppState.watchConnection!!.write(
          characteristicOf(
              "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
              "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
          ),
          chunk.encodeToByteArray(),
      )
    }
  }
}
