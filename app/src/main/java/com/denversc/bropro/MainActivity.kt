package com.denversc.bropro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.denversc.bropro.ui.theme.BroProTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var pendingPrintText: String? = null
    private var pendingFontSize: Float? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pendingPrintText?.let {
                viewModel.printLabel(it, pendingFontSize)
                pendingPrintText = null
                pendingFontSize = null
            }
        } else {
            // Handle permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BroProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LabelPrinterApp(
                        viewModel = viewModel,
                        onPrintRequested = { text, fontSize ->
                            checkPermissionsAndPrint(text, fontSize)
                        }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndPrint(text: String, fontSize: Float?) {
        val permissionsNeeded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missingPermissions = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            viewModel.printLabel(text, fontSize)
        } else {
            pendingPrintText = text
            pendingFontSize = fontSize
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPrinterApp(
    viewModel: MainViewModel,
    onPrintRequested: (String, Float?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var customFontSize by remember { mutableStateOf<Float?>(null) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    val status by viewModel.status.collectAsState()
    val history by viewModel.history.collectAsState()

    val autoFontSize = remember(text) { viewModel.getAutoFontSize(text) }
    val currentFontSize = customFontSize ?: autoFontSize

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = currentFontSize,
            isAuto = customFontSize == null,
            onDismiss = { showFontSizeDialog = false },
            onSizeSelected = { size ->
                customFontSize = size
                showFontSizeDialog = false
            },
            onResetToAuto = {
                customFontSize = null
                showFontSizeDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Label Text") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onPrintRequested(text, customFontSize) },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        ) {
            Text("Print Label")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showFontSizeDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.TextFormat, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Font Size: ${currentFontSize.toInt()} pt${if (customFontSize == null) " (Auto)" else ""}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = if (status.contains("failed", ignoreCase = true)) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(history, key = { it.id }) { label ->
                HistoryItem(
                    label = label,
                    onClick = { text = label.text },
                    onDelete = { viewModel.deleteLabel(label.id) }
                )
            }
        }
    }
}

@Composable
fun FontSizeDialog(
    currentSize: Float,
    isAuto: Boolean,
    onDismiss: () -> Unit,
    onSizeSelected: (Float) -> Unit,
    onResetToAuto: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Font Size") },
        text = {
            Column {
                Text("Size: ${sliderValue.toInt()} pt")
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..60f,
                    steps = 58
                )
                if (!isAuto) {
                    TextButton(
                        onClick = onResetToAuto,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Reset to Auto-calculated")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSizeSelected(sliderValue) }) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HistoryItem(
    label: Label,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label.text,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
