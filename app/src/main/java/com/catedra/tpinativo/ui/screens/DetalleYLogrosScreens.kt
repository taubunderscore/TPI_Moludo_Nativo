package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.UsuarioDesafio
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
//  DetalleHabitoScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DetalleHabitoScreen(
    habitoId: String,
    viewModel: HabitosViewModel,
    onVolver: () -> Unit,
    onEditar: (() -> Unit)? = null


) {
    val uiState          by viewModel.uiState.collectAsStateWithLifecycle()
    val desafiosCatalogo by viewModel.desafiosCatalogo.collectAsStateWithLifecycle()

    val usuarioHabito   = uiState.habitos.find { it.id == habitoId }
    val cumplidoHoy     = usuarioHabito?.let { uiState.estaCumplidoHoy(it.id) } ?: false
    val fechasCumplidas = usuarioHabito?.let { uiState.fechasCumplidas(it.id) } ?: emptyList()

    val desafioAsociado = usuarioHabito?.desafioId?.let { desafioId ->
        desafiosCatalogo.find { it.id == desafioId }
    }

    val habitosDelDesafio = if (desafioAsociado != null)
        uiState.habitos.filter { it.desafioId == desafioAsociado.id }
    else
        emptyList()

    DetalleHabitoContent(
        usuarioHabito     = usuarioHabito,
        cumplidoHoy       = cumplidoHoy,
        fechasCumplidas   = fechasCumplidas,
        desafioAsociado   = desafioAsociado,
        habitosDelDesafio = habitosDelDesafio,
        onVolver          = onVolver,
        onEditar = onEditar,
        onDarDeBajaSimple = {
            usuarioHabito?.let { viewModel.darDeBajaHabito(it.userId, it.id) }
            onVolver()
        },

        onDarDeBajaCascada = {
            if (desafioAsociado != null && usuarioHabito != null) {
                viewModel.darDeBajaDesafioCompleto(
                    userId          = usuarioHabito.userId,
                    desafioId       = desafioAsociado.id,
                    habitosHijosIds = habitosDelDesafio.map { it.id }
                )
            }
            onVolver()
        }
    )
}

@Composable
fun DetalleHabitoContent(
    usuarioHabito: UsuarioHabito?,
    cumplidoHoy: Boolean,
    fechasCumplidas: List<String>,
    desafioAsociado: Desafio?,
    habitosDelDesafio: List<UsuarioHabito>,
    onVolver: () -> Unit,
    onDarDeBajaSimple: () -> Unit,
    onDarDeBajaCascada: () -> Unit,
    onEditar: (() -> Unit)? = null  //
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    if (mostrarDialogo) {
        if (desafioAsociado != null) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("⚠️ Hábito con desafío asociado") },
                text  = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Este hábito pertenece al desafío:")
                        Text(
                            text  = "\"${desafioAsociado.nombre}\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Si lo das de baja se eliminarán también:")
                        habitosDelDesafio.forEach { Text("  •  ${it.nombreCache}") }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text  = "Esta acción no se puede deshacer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { mostrarDialogo = false; onDarDeBajaCascada() },
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Dar de baja todo") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("¿Dar de baja este hábito?") },
                text  = { Text("Se eliminará tu suscripción y el historial. Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = { mostrarDialogo = false; onDarDeBajaSimple() },
                        colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sí, dar de baja") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.Close, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detalle del Hábito", style = MaterialTheme.typography.headlineSmall)
                }
                if (onEditar != null && usuarioHabito?.esPersonalizado == true) {
                    IconButton(onClick = onEditar) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = { mostrarDialogo = true }, enabled = usuarioHabito != null) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Dar de baja",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)) {
            if (usuarioHabito != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(usuarioHabito.nombreCache, style = MaterialTheme.typography.headlineMedium)
                    Text("Categoría: ${usuarioHabito.categoriaCache}", style = MaterialTheme.typography.bodyLarge)
                    Text("Frecuencia: ${usuarioHabito.frecuenciaCache}", style = MaterialTheme.typography.bodyLarge)
                    Text("Días cumplidos: ${fechasCumplidas.size}", style = MaterialTheme.typography.bodyLarge)

                    if (desafioAsociado != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text     = "🏆 ${desafioAsociado.nombre}",
                                style    = MaterialTheme.typography.labelMedium,
                                color    = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado hoy: ", style = MaterialTheme.typography.bodyLarge)
                        AssistChip(
                            onClick = {},
                            label   = { Text(if (cumplidoHoy) "✅ CUMPLIDO" else "⏳ PENDIENTE") }
                        )
                    }
                }
            } else {
                Text("No se encontró la información del hábito.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LogrosScreen
// ─────────────────────────────────────────────────────────────────────────────

fun calcularRachaActual(fechasCumplidas: List<String>): Int {
    if (fechasCumplidas.isEmpty()) return 0
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val fechas = fechasCumplidas
        .mapNotNull { runCatching { LocalDate.parse(it, formatter) }.getOrNull() }
        .sortedDescending()

    var racha = 0
    var diaEsperado = LocalDate.now()
    for (fecha in fechas) {
        if (fecha == diaEsperado || fecha == diaEsperado.minusDays(1)) {
            racha++
            diaEsperado = fecha.minusDays(1)
        } else break
    }
    return racha
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogrosScreen(
    viewModel: HabitosViewModel,
    userId: String,
    onVerMapa: (lat: Double, lng: Double, nombre: String, fecha: String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargarHabitos(userId)
    }

    val desafiosCompletados = uiState.desafiosSuscritos.filter { it.completado }

    val habitosConRacha = uiState.habitos
        .filter { it.desafioId == null }
        .map { uh ->
            val fechas = uiState.fechasCumplidas(uh.id)
            uh to calcularRachaActual(fechas)
        }
        .filter { (_, racha) -> racha > 0 }
        .sortedByDescending { (_, racha) -> racha }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Logros y Medallas 🏆") }) }
    ) { innerPadding ->
        if (desafiosCompletados.isEmpty() && habitosConRacha.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = "¡Todavía no ganaste ninguna medalla!\nCompletá tus desafíos en el Home.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                if (desafiosCompletados.isNotEmpty()) {
                    item {
                        Text(
                            "Vitrina de Trofeos 🏅",
                            style    = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(desafiosCompletados) { ud ->
                        TrofeoCard(
                            usuarioDesafio = ud,
                            onVerMapa      = onVerMapa
                        )
                    }
                }

                if (habitosConRacha.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Rachas activas 🔥",
                            style    = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(habitosConRacha) { (uh, racha) ->
                        RachaCard(usuarioHabito = uh, racha = racha)
                    }
                }
            }
        }
    }
}

// ─── Cards ───────────────────────────────────────────────────────────────────

@Composable
fun TrofeoCard(
    usuarioDesafio: UsuarioDesafio,
    onVerMapa: (lat: Double, lng: Double, nombre: String, fecha: String?) -> Unit
) {
    val tieneUbicacion = usuarioDesafio.logroLatitud != null && usuarioDesafio.logroLongitud != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (tieneUbicacion) Modifier.clickable {
                    onVerMapa(
                        usuarioDesafio.logroLatitud!!,
                        usuarioDesafio.logroLongitud!!,
                        usuarioDesafio.nombreCache,
                        usuarioDesafio.fechaLogro
                    )
                } else Modifier
            ),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = Icons.Default.Star,
                contentDescription = "Medalla",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = usuarioDesafio.nombreCache,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (usuarioDesafio.fechaLogro != null) {
                    Text(
                        text  = "Ganado el: ${usuarioDesafio.fechaLogro}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                if (tieneUbicacion) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Default.LocationOn,
                            contentDescription = "Ver ubicación",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = "Toca para ver en mapa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RachaCard(
    usuarioHabito: UsuarioHabito,
    racha: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier          = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🔥",
                style    = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text  = usuarioHabito.nombreCache,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text  = "$racha día${if (racha != 1) "s" else ""} consecutivo${if (racha != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
