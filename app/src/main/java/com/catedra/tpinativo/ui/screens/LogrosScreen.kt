package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogrosScreen(viewModel: HabitosViewModel, userId: String) {
    // Nos conectamos al StateFlow de logros que acabamos de acomodar en el ViewModel
    val logros by viewModel.logrosUsuario.collectAsState()

    // 🚀 EFECTO REACTIVO: Cada vez que el usuario abre esta pestaña,
    // se dispara la consulta asíncrona a Firestore para traer las medallas frescas.
    LaunchedEffect(Unit) {
        viewModel.cargarLogros(userId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Logros y Medallas 🏆") }) }
    ) { innerPadding ->
        if (logros.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¡Todavía no ganaste ninguna medalla!\nCompletá tus desafíos en el Home.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Vitrina de Trofeos",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(logros) { logro ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Ícono de la Medalla / Estrella
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Medalla",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Detalle del logro histórico obtenido
                            Column {
                                Text(
                                    text = logro.nombreDesafio,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Ganado el: ${logro.fechaObtencion}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}