package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.model.TipoFrecuencia
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearHabitoScreen(
    viewModel: HabitosViewModel,
    userId: String,
    onVolver: () -> Unit,
    habitoEditar: HabitoSuscrito? = null  // ✅ null = crear, valor = editar
) {
    // ✅ Si estamos editando precargamos los valores, sino arrancamos vacío
    val esEdicion = habitoEditar != null

    var nombre by remember { mutableStateOf(habitoEditar?.nombre ?: "") }
    var comentario by remember { mutableStateOf(habitoEditar?.comentario ?: "") }
    var categoriaSeleccionada by remember {
        mutableStateOf(habitoEditar?.categoria ?: "Físico")
    }
    var frecuenciaSeleccionada by remember {
        mutableStateOf(
            habitoEditar?.frecuencia?.let {
                runCatching { TipoFrecuencia.valueOf(it) }.getOrDefault(TipoFrecuencia.DIARIO)
            } ?: TipoFrecuencia.DIARIO
        )
    }
    var mostrarTimePicker by remember { mutableStateOf(false) }
    var horaRecordatorio by remember { mutableStateOf(habitoEditar?.horaRecordatorio) }

    val categorias = listOf("Físico", "Estudio", "Salud", "Productividad")
    val frecuencias = listOf(TipoFrecuencia.DIARIO, TipoFrecuencia.SEMANAL, TipoFrecuencia.MENSUAL)

    if (mostrarTimePicker) {
        RecordatorioTimePickerDialog(
            nombreHabito = nombre.ifEmpty { "tu hábito" },
            frecuenciaDefault = frecuenciaSeleccionada,
            onConfirmar = { hora, _ ->
                horaRecordatorio = hora
                mostrarTimePicker = false
            },
            onOmitir = { _ ->
                horaRecordatorio = null
                mostrarTimePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // ✅ Título dinámico según modo
                title = { Text(if (esEdicion) "Editar Hábito" else "Nuevo Hábito") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Nombre ───────────────────────────────────────────
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del hábito") },
                placeholder = { Text("Ej: Meditar 10 minutos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Comentario ───────────────────────────────────────
            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = { Text("Nota personal (opcional)") },
                placeholder = { Text("Ej: Hacerlo antes de desayunar") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // ── Categoría ────────────────────────────────────────
            Text("Categoría", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categorias.forEach { cat ->
                    FilterChip(
                        selected = categoriaSeleccionada == cat,
                        onClick = { categoriaSeleccionada = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // ── Frecuencia ───────────────────────────────────────
            Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                frecuencias.forEach { frec ->
                    FilterChip(
                        selected = frecuenciaSeleccionada == frec,
                        onClick = { frecuenciaSeleccionada = frec },
                        label = { Text(frec.name) }
                    )
                }
            }

            // ── Recordatorio ─────────────────────────────────────
            Text("Recordatorio", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { mostrarTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (horaRecordatorio != null) "⏰ $horaRecordatorio"
                    else "⏰ Configurar hora de recordatorio"
                )
            }

            if (horaRecordatorio != null) {
                TextButton(onClick = { horaRecordatorio = null }) {
                    Text("Quitar recordatorio", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Botón Guardar / Guardar cambios ──────────────────
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        if (esEdicion) {
                            // ✅ Modo edición — actualizar hábito existente
                            viewModel.editarHabito(
                                habitoId = habitoEditar!!.id,
                                userId = userId,
                                nombre = nombre.trim(),
                                categoria = categoriaSeleccionada,
                                frecuencia = frecuenciaSeleccionada,
                                horaRecordatorio = horaRecordatorio,
                                comentario = comentario.trim().ifEmpty { null }
                            )
                        } else {
                            // ✅ Modo creación — nuevo hábito personalizado
                            viewModel.crearHabitoPersonalizado(
                                userId = userId,
                                nombre = nombre.trim(),
                                categoria = categoriaSeleccionada,
                                frecuencia = frecuenciaSeleccionada,
                                horaRecordatorio = horaRecordatorio,
                                comentario = comentario.trim().ifEmpty { null }
                            )
                        }
                        onVolver()
                    }
                },
                enabled = nombre.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                // ✅ Texto dinámico según modo
                Text(if (esEdicion) "Guardar cambios" else "Guardar hábito")
            }
        }
    }
}