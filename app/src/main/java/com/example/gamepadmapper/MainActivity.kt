package com.example.gamepadmapper

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import com.example.gamepadmapper.data.MappingRepository
import com.example.gamepadmapper.model.InjectionEngine
import com.example.gamepadmapper.model.MappingConfig
import com.example.gamepadmapper.overlay.MappingOverlayService
import com.example.gamepadmapper.engine.ShizukuUserServiceConnection
import com.example.gamepadmapper.ui.theme.GamepadMapperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = MappingRepository(applicationContext)
        setContent {
            GamepadMapperTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EngineSelectionScreen(repository = repository)
                }
            }
        }
    }
}

@Composable
fun EngineSelectionScreen(repository: MappingRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by repository.config.collectAsState(initial = MappingConfig())
    val shizukuConnection = remember { ShizukuUserServiceConnection() }
    // Re-read availability on each composition; the provider may be started
    // after the activity is first displayed.
    val shizukuAvailable = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    LaunchedEffect(config.engine, shizukuAvailable) {
        if (config.engine == InjectionEngine.SHIZUKU && shizukuAvailable) {
            runCatching { shizukuConnection.bind() }
        } else {
            shizukuConnection.unbind()
        }
    }
    DisposableEffect(Unit) {
        onDispose { shizukuConnection.unbind() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gamepad Mapper") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose an input engine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Select how controller input should be translated into touch events.",
                style = MaterialTheme.typography.bodyMedium
            )

            EngineOption(
                title = "Touch engine",
                description = "Use the overlay and Android touch pipeline.",
                selected = config.engine == InjectionEngine.TOUCH,
                enabled = true,
                onSelect = { scope.launch { repository.setEngine(InjectionEngine.TOUCH) } }
            )
            EngineOption(
                title = "Shizuku User Service",
                description = if (shizukuAvailable) {
                    "Shizuku is available for the remote service."
                } else {
                    "Start Shizuku or Sui before selecting this engine."
                },
                selected = config.engine == InjectionEngine.SHIZUKU,
                enabled = shizukuAvailable,
                onSelect = { scope.launch { repository.setEngine(InjectionEngine.SHIZUKU) } }
            )

            if (!shizukuAvailable) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:rikka.shizuku")
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Shizuku settings")
                }
            }

            HorizontalDivider()
            Text("Mapping preferences", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating overlay", fontWeight = FontWeight.Medium)
                    Text("Keep the mapping surface available above games.")
                }
                Switch(
                    checked = config.overlayEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setOverlayEnabled(enabled) }
                    }
                )
            }

            Text("Analog sensitivity: ${"%.2f".format(config.sensitivity)}")
            Slider(
                value = config.sensitivity,
                onValueChange = { value ->
                    scope.launch { repository.setSensitivity(value) }
                },
                valueRange = 0.25f..2.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ready to map", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Selected engine: ${config.engine.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            if (Settings.canDrawOverlays(context)) {
                                context.startService(Intent(context, MappingOverlayService::class.java))
                            } else {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Start mapping overlay")
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled) onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = { if (enabled) onSelect() },
                enabled = enabled
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
