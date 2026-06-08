package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.data.model.TipoFrecuencia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordatorioTimePickerDialog(
    nombreHabito: String,
    frecuenciaDefault: TipoFrecuencia = TipoFrecuencia.DIARIO,
    onConfirmar: (hora: String?, frecuencia: TipoFrecuencia) -> Unit,
    onOmitir: (frecuencia: TipoFrecuencia) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )

    var frecuenciaSeleccionada by remember { mutableStateOf(frecuenciaDefault) }
    var configurarHora by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { onOmitir(frecuenciaSeleccionada) },
        title = { Text("Configurar hábito") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = nombreHabito,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ── Frecuencia ────────────────────────────────────
                Text(
                    text = "Frecuencia",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    TipoFrecuencia.values().forEach { frec ->
                        FilterChip(
                            selected = frecuenciaSeleccionada == frec,
                            onClick = { frecuenciaSeleccionada = frec },
                            label = { Text(frec.name) }
                        )
                    }
                }

                // ── Recordatorio opcional ─────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = configurarHora,
                        onCheckedChange = { configurarHora = it }
                    )
                    Text("Configurar recordatorio")
                }

                if (configurarHora) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TimePicker(state = timePickerState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hora = if (configurarHora) {
                        "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                    } else null
                    onConfirmar(hora, frecuenciaSeleccionada)
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onOmitir(frecuenciaSeleccionada) }) {
                Text("Cancelar")
            }
        }
    )
}