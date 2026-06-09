package com.catedra.tpinativo.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.catedra.tpinativo.data.model.CategoriaHabito
import com.catedra.tpinativo.viewmodel.HabitosPersonalizadosViewModel
import com.catedra.tpinativo.viewmodel.HabitosPersonalizadosViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarHabitoScreen(
    userId: String,
    habitoId: String,
    nombreInicial: String,
    detalleInicial: String,
    categoriaInicial: String,
    horaInicial: String,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val personalizadosVM: HabitosPersonalizadosViewModel = viewModel(
        factory = HabitosPersonalizadosViewModelFactory(context)
    )
    val state by personalizadosVM.uiState.collectAsState()

    var nombre by remember { mutableStateOf(nombreInicial) }
    var detalle by remember { mutableStateOf(detalleInicial) }
    var categoria by remember {
        mutableStateOf(
            runCatching { CategoriaHabito.valueOf(categoriaInicial.uppercase()) }
                .getOrDefault(CategoriaHabito.SALUD)
        )
    }
    var hora by remember { mutableStateOf(horaInicial) }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.exitoMensaje) {
        if (state.exitoMensaje != null) {
            personalizadosVM.resetearExito()
            onVolver()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Hábito") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            OutlinedTextField(
                value = detalle,
                onValueChange = { detalle = it },
                label = { Text("Detalle (opcional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Categoría", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoriaHabito.entries.forEach { cat ->
                    FilterChip(
                        selected = categoria == cat,
                        onClick = { categoria = cat },
                        label = { Text(cat.display, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            OutlinedTextField(
                value = hora,
                onValueChange = {},
                readOnly = true,
                label = { Text("Hora de recordatorio *") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = java.util.Calendar.getInstance()
                        val h = hora.split(":").getOrNull(0)?.toIntOrNull()
                            ?: cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val m = hora.split(":").getOrNull(1)?.toIntOrNull()
                            ?: cal.get(java.util.Calendar.MINUTE)
                        TimePickerDialog(context, { _, hh, mm ->
                            hora = "%02d:%02d".format(hh, mm)
                        }, h, m, true).show()
                    }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Hora")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = java.util.Calendar.getInstance()
                        val h = hora.split(":").getOrNull(0)?.toIntOrNull()
                            ?: cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val m = hora.split(":").getOrNull(1)?.toIntOrNull()
                            ?: cal.get(java.util.Calendar.MINUTE)
                        TimePickerDialog(context, { _, hh, mm ->
                            hora = "%02d:%02d".format(hh, mm)
                        }, h, m, true).show()
                    }
            )

            val mensajeError = errorLocal ?: state.error
            if (mensajeError != null) {
                Text(
                    mensajeError, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    errorLocal = null
                    when {
                        nombre.isBlank() -> errorLocal = "El nombre es obligatorio"
                        hora.isEmpty() -> errorLocal = "Seleccioná una hora"
                        else -> {
                            android.util.Log.d(
                                "EDITAR",
                                "habitoId=$habitoId nombre=$nombre categoria=$categoria hora=$hora"
                            )

                            personalizadosVM.editar(
                                userId = userId,
                                habitoId = habitoId,
                                nombre = nombre,
                                detalle = detalle,
                                categoria = categoria,
                                horaRecordatorio = hora
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.cargando
            ) {
                if (state.cargando) CircularProgressIndicator(
                    Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                else Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
