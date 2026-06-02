package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.model.DesafioObjetivo
import androidx.compose.ui.tooling.preview.Preview
import com.catedra.tpinativo.ui.theme.TPINativoTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetalleHabitoScreenPreview() {
    TPINativoTheme {
        DetalleHabitoContent(
            habito = HabitoSuscrito(
                id = "habito_prueba_123",
                nombre = "Rutina de piernas y saltos al cajón",
                categoria = "Físico",
                userId = "user_varela_123"
            ),
            desafioAsociado = null,
            habitosDelDesafio = emptyList(),
            onVolver = {},
            onDarDeBajaSimple = {},
            onDarDeBajaCascada = {}
        )
    }
}

@Composable
fun DetalleHabitoScreen(
    habitoId: String,
    viewModel: HabitosViewModel,
    onVolver: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val desafios by viewModel.desafiosCatalogo.collectAsStateWithLifecycle()
    val habitoReal = uiState.habitos.find { it.id == habitoId }

    // ✅ Solo es hábito de desafío si tiene desafioId — no por plantillaId
    // Esto evita falsos positivos cuando el usuario se suscribe individualmente
    val desafioAsociado = habitoReal?.desafioId?.let { desafioId ->
        desafios.find { it.id == desafioId }
    }

    // ✅ Si tiene desafío, resolvemos los nombres de los otros hábitos hijos
    val habitosDelDesafio = desafioAsociado?.habitosRequeridos?.mapNotNull { plantillaId ->
        uiState.habitos.find { it.plantillaId == plantillaId }
    } ?: emptyList()

    DetalleHabitoContent(
        habito = habitoReal,
        desafioAsociado = desafioAsociado,
        habitosDelDesafio = habitosDelDesafio,
        onVolver = onVolver,
        onDarDeBajaSimple = {
            // Hábito individual — baja directa
            viewModel.darDeBajaHabito(habitoId = habitoId, userId = habitoReal?.userId ?: "")
            onVolver()
        },
        onDarDeBajaCascada = {
            // Hábito de desafío — baja en cascada de todo el desafío
            viewModel.darDeBajaDesafioCompleto(
                desafio = desafioAsociado!!,
                habitosHijos = habitosDelDesafio,
                userId = habitoReal?.userId ?: ""
            )
            onVolver()
        }
    )
}

@Composable
fun DetalleHabitoContent(
    habito: HabitoSuscrito?,
    desafioAsociado: DesafioObjetivo?,
    habitosDelDesafio: List<HabitoSuscrito>,
    onVolver: () -> Unit,
    onDarDeBajaSimple: () -> Unit,
    onDarDeBajaCascada: () -> Unit
) {
    val hoy = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    var mostrarDialogo by remember { mutableStateOf(false) }

    // ✅ Diálogo adaptativo según si tiene desafío o no
    if (mostrarDialogo) {
        if (desafioAsociado != null) {
            // Caso C — hábito atado a desafío
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("⚠️ Hábito con desafío asociado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Este hábito pertenece al desafío:")
                        Text(
                            text = "\"${desafioAsociado.nombreDesafio}\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Si lo das de baja se eliminarán también:")
                        habitosDelDesafio.forEach { h ->
                            Text("  •  ${h.nombre}")
                        }
                        Text("  •  El desafío completo")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Esta acción no se puede deshacer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogo = false
                            onDarDeBajaCascada()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Dar de baja todo")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mostrarDialogo = false }) {
                        Text("Cancelar")
                    }
                }
            )
        } else {
            // Caso simple — hábito individual
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("¿Dar de baja este hábito?") },
                text = {
                    Text("Se eliminará tu suscripción y todo el historial de cumplimiento. Esta acción no se puede deshacer.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            mostrarDialogo = false
                            onDarDeBajaSimple()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Sí, dar de baja")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mostrarDialogo = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.Close, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detalle del Hábito",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                IconButton(
                    onClick = { mostrarDialogo = true },
                    enabled = habito != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Dar de baja",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            if (habito != null) {
                val estaCumplidoHoy = habito.fechasCumplidas.contains(hoy)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = habito.nombre, style = MaterialTheme.typography.headlineMedium)
                    Text(text = "Categoría: ${habito.categoria}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Frecuencia: ${habito.frecuencia}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Días cumplidos: ${habito.fechasCumplidas.size}", style = MaterialTheme.typography.bodyLarge)

                    // ✅ Badge del desafío si está asociado
                    if (desafioAsociado != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "🏆 ${desafioAsociado.nombreDesafio}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Estado hoy: ", style = MaterialTheme.typography.bodyLarge)
                        AssistChip(
                            onClick = {},
                            label = { Text(if (estaCumplidoHoy) "✅ CUMPLIDO" else "⏳ PENDIENTE") }
                        )
                    }
                }
            } else {
                Text(
                    text = "No se encontró la información del hábito.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}