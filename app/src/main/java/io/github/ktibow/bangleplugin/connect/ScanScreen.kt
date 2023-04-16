package io.github.ktibow.bangleplugin.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juul.kable.Advertisement

@Composable
fun ScanScreen(
    data: DataStore<Preferences>,
    scanViewModel: ScanViewModel = viewModel(),
    onChoose: (String) -> Unit
) {
    val scanState by scanViewModel.uiState.collectAsState()
    LazyColumn(modifier = Modifier.padding(PaddingValues(0.dp, 16.dp))) {
        item {
            Column(
                    modifier = Modifier.padding(PaddingValues(16.dp, 0.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScanInfo(
                        status = scanState.status,
                        start = { scanViewModel.start() },
                        stop = { scanViewModel.stop() }, clear = { scanViewModel.clear() },
                )
            }
        }
        items(scanState.found.size) {
            val device = scanState.found[it]
            if (device.name != null && device.name!!.startsWith("Bangle.js")) {
                ScanDevice(
                        device = device,
                        chooseDevice = {
                            onChoose(device.address)
                        },
                )
            }
        }
    }
}

@Composable
fun ScanInfo(status: ScanStatus, start: () -> Unit, stop: () -> Unit, clear: () -> Unit) {
    when (status) {
        ScanStatus.Stopped -> {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Scan for your Bangle.js")
                Spacer(Modifier.weight(1f))
                IconButton(onClick = start) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Start")
                }
                IconButton(onClick = clear) {
                    Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear")
                }
            }
        }

        ScanStatus.Scanning -> {
            Row(
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Scanning")
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = stop) {
                    Icon(imageVector = Icons.Filled.Stop, contentDescription = "Stop")
                }
            }
        }

        is ScanStatus.Failed -> {
            Text(text = "Ran into an error")
            Text(text = status.message)
        }
    }
}

@Composable
fun ScanHeader(clear: () -> Unit) {
    Row(
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Choose your Bangle.js")
        Spacer(Modifier.weight(1f))
        IconButton(onClick = clear) {
            Icon(imageVector = Icons.Filled.ClearAll, contentDescription = "Clear")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDevice(device: Advertisement, chooseDevice: () -> Unit) {
    ListItem(
            headlineText = { Text(device.name ?: "Unknown") },
            supportingText = { Text(device.address) },
            modifier = Modifier.clickable { chooseDevice() },
    )
}
