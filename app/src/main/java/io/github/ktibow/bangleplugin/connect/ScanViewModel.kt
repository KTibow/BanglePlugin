package io.github.ktibow.bangleplugin.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Advertisement
import com.juul.kable.Scanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

sealed class ScanStatus {
  object Stopped : ScanStatus()
  object Scanning : ScanStatus()
  data class Failed(val message: String) : ScanStatus()
}

data class ScanUIState(
  val scanner: Scanner = Scanner(),
  val status: ScanStatus = ScanStatus.Stopped,
  val _found: HashMap<String, Advertisement> = hashMapOf(),
  val found: List<Advertisement> = emptyList(),
)

class ScanViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ScanUIState())
  val uiState: StateFlow<ScanUIState> = _uiState.asStateFlow()
  fun start() {
    _uiState.value = _uiState.value.copy(status = ScanStatus.Scanning)
    viewModelScope.launch {
      _uiState.value.scanner
          .advertisements
          .catch { cause ->
            _uiState.value = _uiState.value.copy(
                status = ScanStatus.Failed(
                    cause.message ?: "Unknown error",
                ),
            )
          }
          .onCompletion { cause ->
            if (cause is CancellationException) {
              _uiState.value = _uiState.value.copy(status = ScanStatus.Stopped)
            }
          }
          .collect { advertisement ->
            val updatedFound =
                _uiState.value._found.plus(Pair(advertisement.address, advertisement))
            val updatedFoundHM = HashMap<String, Advertisement>()
            updatedFoundHM.putAll(updatedFound)
            _uiState.value = _uiState.value.copy(
                _found = updatedFoundHM,
                found = updatedFound.values.toList(),
            )
          }
    }
  }

  fun stop() {
    viewModelScope.coroutineContext.cancelChildren()
  }

  fun clear() {
    _uiState.value = _uiState.value.copy(
        _found = hashMapOf(),
        found = emptyList(),
    )
  }
}
