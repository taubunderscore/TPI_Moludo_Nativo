package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorioTimePickerDialog(
    nombreHabito: String,
    onConfirmar: (hora: String) -> Unit,
    onOmitir: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onOmitir,
        title = { Text("¿Cuándo recordarte?") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Recordatorio para:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = nombreHabito,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ✅ TimePicker de Material 3
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Formateamos la hora como "HH:mm"
                    val hora = "%02d:%02d".format(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onConfirmar(hora)
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onOmitir) {
                Text("Sin recordatorio")
            }
        }
    )
}