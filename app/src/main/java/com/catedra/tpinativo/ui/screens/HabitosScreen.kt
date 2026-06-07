package com.catedra.tpinativo.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.catedra.tpinativo.data.model.CategoriaHabito
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.catedra.tpinativo.viewmodel.HabitosPersonalizadosViewModel
import com.catedra.tpinativo.viewmodel.HabitosPersonalizadosViewModelFactory
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitosScreen(
    viewModel: HabitosViewModel,
    userId: String,
    onVerDetalle: (String) -> Unit
) {
    val context  = LocalContext.current
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val desafios by viewModel.desafiosCatalogo.collectAsStateWithLifecycle()

    // ── ViewModel de hábitos personalizados ──────────────────────────────────
    val personalizadosVM: HabitosPersonalizadosViewModel = viewModel(
        factory = HabitosPersonalizadosViewModelFactory(context)
    )
    val personalizadosState by personalizadosVM.uiState.collectAsStateWithLifecycle()

    // ── Control del bottom sheet ──────────────────────────────────────────────
    var mostrarSheetNuevoHabito by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Permiso de ubicación ──────────────────────────────────────────────────
    var permisoUbicacionOtorgado by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcherPermisoUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permisoUbicacionOtorgado = concedido }

    // ── Permiso de notificaciones (Android 13+) ───────────────────────────────
    var permisoNotificacionOtorgado by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val launcherPermisoNotificacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> permisoNotificacionOtorgado = concedido }

    LaunchedEffect(Unit) {
        if (!permisoUbicacionOtorgado)
            launcherPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!permisoNotificacionOtorgado && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            launcherPermisoNotificacion.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(userId) {
        viewModel.cargarHabitos(userId)
        viewModel.cargarDesafios()
        viewModel.cargarFotoPerfil(userId)
        personalizadosVM.cargar(userId)
    }

    // ── Diálogo de desafío logrado ────────────────────────────────────────────
    uiState.ultimoDesafioLogrado?.let { nombreDesafio ->
        AlertDialog(
            onDismissRequest = { viewModel.resetearAlertaDesafio() },
            confirmButton = {
                Button(onClick = { viewModel.resetearAlertaDesafio() }) { Text("¡Buenísimo!") }
            },
            title = { Text("🏆 ¡Desafío Completado!") },
            text  = { Text("Felicitaciones, desbloqueaste:\n\n\"$nombreDesafio\"") }
        )
    }

    // ── Snackbar de éxito al crear hábito personalizado ───────────────────────
    personalizadosState.exitoMensaje?.let { msg ->
        LaunchedEffect(msg) {
            personalizadosVM.resetearExito()
        }
    }

    // ── Bottom Sheet: formulario de nuevo hábito personalizado ────────────────
    if (mostrarSheetNuevoHabito) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheetNuevoHabito = false },
            sheetState       = sheetState
        ) {
            FormularioNuevoHabitoPersonalizado(
                cargando = personalizadosState.cargando,
                error    = personalizadosState.error,
                onCreate = { nombre, detalle, categoria, hora ->
                    personalizadosVM.crear(userId, nombre, detalle, categoria, hora)
                    mostrarSheetNuevoHabito = false
                },
                onDismiss = { mostrarSheetNuevoHabito = false }
            )
        }
    }

    HabitosContent(
        habitos                = uiState.habitos,
        cargando               = uiState.cargando,
        error                  = uiState.error,
        estaCumplidoHoyFn      = { uiState.estaCumplidoHoy(it) },
        desafiosCatalogo       = desafios,
        onVerDetalle           = onVerDetalle,
        onAlternarEstado       = { viewModel.alternarEstadoHabito(it) },
        fotoPerfilUrl          = uiState.fotoPerfilUrl,
        onNuevoHabitoPersonalizado = { mostrarSheetNuevoHabito = true }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  HabitosContent — agrega el FAB
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitosContent(
    habitos: List<UsuarioHabito>,
    cargando: Boolean,
    error: String?,
    estaCumplidoHoyFn: (String) -> Boolean,
    desafiosCatalogo: List<Desafio>,
    onVerDetalle: (String) -> Unit,
    onAlternarEstado: (UsuarioHabito) -> Unit,
    fotoPerfilUrl: String? = null,
    onNuevoHabitoPersonalizado: () -> Unit = {}
) {
    var filtroSeleccionado by remember { mutableStateOf("PENDIENTES") }
    var textoBuscado       by remember { mutableStateOf("") }

    val habitosFiltrados = habitos.filter { uh ->
        val cumpleTexto  = uh.nombreCache.contains(textoBuscado, ignoreCase = true)
        val cumplidoHoy  = estaCumplidoHoyFn(uh.id)
        val cumpleEstado = when (filtroSeleccionado) {
            "PENDIENTES" -> !cumplidoHoy
            "CUMPLIDOS"  -> cumplidoHoy
            else         -> true
        }
        cumpleTexto && cumpleEstado
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                // ── Imagen de fondo desde Cloudinary ─────────────────────────
                AsyncImage(
                    model              = "https://res.cloudinary.com/dyylor99b/image/upload/v1780845200/HD-wallpaper-autumn-park-sunlight-calm-autumn-leaves-bench-nature-trees-relaxing_c7cmeu.jpg",
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
                // ── Scrim oscuro para que el texto sea legible ────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f),
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f)
                                )
                            )
                        )
                )
                // ── Contenido: título + avatar ────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text  = "Mis Hábitos",
                            style = MaterialTheme.typography.headlineSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text  = "Construí tu mejor versión 🌱",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Avatar de perfil
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!fotoPerfilUrl.isNullOrBlank()) {
                            AsyncImage(
                                model              = fotoPerfilUrl,
                                contentDescription = "Foto de perfil",
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier           = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        // ── FAB abajo a la derecha ────────────────────────────────────────────
        floatingActionButton = {
            FloatingActionButton(
                onClick            = onNuevoHabitoPersonalizado,
                containerColor     = MaterialTheme.colorScheme.primary,
                contentColor       = MaterialTheme.colorScheme.onPrimary,
                shape              = CircleShape
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Nuevo hábito personalizado"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            SearchBarCompacta(
                value         = textoBuscado,
                onValueChange = { textoBuscado = it },
                modifier      = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "PENDIENTES", "CUMPLIDOS").forEach { filtro ->
                    FilterChip(
                        selected = filtroSeleccionado == filtro,
                        onClick  = { filtroSeleccionado = filtro },
                        label    = {
                            Text(
                                text  = filtro.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                habitosFiltrados.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No se encontraron hábitos.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> LazyColumn(
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding      = PaddingValues(bottom = 80.dp) // espacio para el FAB
                ) {
                    items(habitosFiltrados) { uh ->
                        val cumplidoHoy           = estaCumplidoHoyFn(uh.id)
                        val nombreDesafioAsociado = uh.desafioId?.let { desafioId ->
                            desafiosCatalogo.find { it.id == desafioId }?.nombre
                        }
                        HabitoCard(
                            usuarioHabito         = uh,
                            cumplidoHoy           = cumplidoHoy,
                            nombreDesafioAsociado = nombreDesafioAsociado,
                            onVerDetalle          = { onVerDetalle(uh.id) },
                            onAlternarEstado      = { onAlternarEstado(uh) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Formulario de nuevo hábito personalizado (dentro del BottomSheet)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FormularioNuevoHabitoPersonalizado(
    cargando: Boolean,
    error: String?,
    onCreate: (nombre: String, detalle: String, categoria: CategoriaHabito, hora: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre    by remember { mutableStateOf("") }
    var detalle   by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CategoriaHabito.SALUD) }
    var hora      by remember { mutableStateOf("") }
    var errorLocal by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text  = "Nuevo hábito personalizado",
            style = MaterialTheme.typography.titleLarge
        )

        // Nombre
        OutlinedTextField(
            value         = nombre,
            onValueChange = { nombre = it },
            label         = { Text("Nombre *") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )

        // Detalle
        OutlinedTextField(
            value         = detalle,
            onValueChange = { detalle = it },
            label         = { Text("Detalle (opcional)") },
            minLines      = 2,
            maxLines      = 4,
            modifier      = Modifier.fillMaxWidth()
        )

        // Categoría
        Text(
            text  = "Categoría",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoriaHabito.entries.forEach { cat ->
                FilterChip(
                    selected = categoria == cat,
                    onClick  = { categoria = cat },
                    label    = { Text(cat.display, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Hora de recordatorio — picker nativo
        val context = LocalContext.current
        OutlinedTextField(
            value         = if (hora.isEmpty()) "" else hora,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Hora de recordatorio *") },
            placeholder   = { Text("Seleccioná una hora") },
            trailingIcon  = {
                IconButton(onClick = {
                    val calendar = java.util.Calendar.getInstance()
                    val horaActual    = if (hora.isNotEmpty()) hora.split(":")[0].toIntOrNull() ?: calendar.get(java.util.Calendar.HOUR_OF_DAY) else calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minutosActual = if (hora.isNotEmpty()) hora.split(":").getOrNull(1)?.toIntOrNull() ?: calendar.get(java.util.Calendar.MINUTE) else calendar.get(java.util.Calendar.MINUTE)
                    TimePickerDialog(
                        context,
                        { _, h, m -> hora = "%02d:%02d".format(h, m) },
                        horaActual,
                        minutosActual,
                        true   // formato 24h
                    ).show()
                }) {
                    Icon(
                        imageVector        = Icons.Default.AccessTime,
                        contentDescription = "Seleccionar hora"
                    )
                }
            },
            singleLine = true,
            modifier   = Modifier
                .fillMaxWidth()
                .clickable {
                    val calendar = java.util.Calendar.getInstance()
                    val horaActual    = if (hora.isNotEmpty()) hora.split(":")[0].toIntOrNull() ?: calendar.get(java.util.Calendar.HOUR_OF_DAY) else calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minutosActual = if (hora.isNotEmpty()) hora.split(":").getOrNull(1)?.toIntOrNull() ?: calendar.get(java.util.Calendar.MINUTE) else calendar.get(java.util.Calendar.MINUTE)
                    TimePickerDialog(
                        context,
                        { _, h, m -> hora = "%02d:%02d".format(h, m) },
                        horaActual,
                        minutosActual,
                        true
                    ).show()
                }
        )

        // Errores
        val mensajeError = errorLocal ?: error
        if (mensajeError != null) {
            Text(
                text  = mensajeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Botones
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Cancelar") }

            Button(
                onClick = {
                    errorLocal = null
                    when {
                        nombre.isBlank() -> errorLocal = "El nombre es obligatorio"
                        hora.isEmpty()   -> errorLocal = "Seleccioná una hora de recordatorio"
                        else -> onCreate(nombre, detalle, categoria, hora)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled  = !cargando
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Crear")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Composables reutilizados (sin cambios)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SearchBarCompacta(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorEsquema = MaterialTheme.colorScheme
    BasicTextField(
        value         = value,
        onValueChange = onValueChange,
        singleLine    = true,
        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = colorEsquema.onSurface),
        cursorBrush   = SolidColor(colorEsquema.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorEsquema.surfaceVariant)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = colorEsquema.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Buscar hábito...", style = MaterialTheme.typography.bodyMedium, color = colorEsquema.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun HabitoCard(
    usuarioHabito: UsuarioHabito,
    cumplidoHoy: Boolean,
    nombreDesafioAsociado: String?,
    onVerDetalle: () -> Unit,
    onAlternarEstado: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onVerDetalle() },
        shape    = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier              = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = usuarioHabito.nombreCache, style = MaterialTheme.typography.titleMedium)
                Text(
                    text  = "${usuarioHabito.categoriaCache} · ${usuarioHabito.frecuenciaCache}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (nombreDesafioAsociado != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape    = MaterialTheme.shapes.small,
                        color    = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text     = "🏆 $nombreDesafioAsociado",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Checkbox(checked = cumplidoHoy, onCheckedChange = { onAlternarEstado() })
        }
    }
}