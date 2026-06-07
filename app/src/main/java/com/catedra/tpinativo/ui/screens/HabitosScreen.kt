package com.catedra.tpinativo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.catedra.tpinativo.viewmodel.HabitosViewModel

@Composable
fun HabitosScreen(
    viewModel: HabitosViewModel,
    userId: String,
    onVerDetalle: (String) -> Unit
) {
    val context  = LocalContext.current
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val desafios by viewModel.desafiosCatalogo.collectAsStateWithLifecycle()

    // ── Permiso de ubicación ──────────────────────────────────────────────────
    // Estado: ¿el permiso ya fue otorgado cuando el Composable carga?
    var permisoUbicacionOtorgado by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Launcher que pide el permiso al usuario y actualiza el estado
    val launcherPermisoUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoUbicacionOtorgado = concedido
        if (!concedido) {
            android.util.Log.w("HabitosScreen", "Permiso de ubicación denegado por el usuario")
        }
    }

    // Pedimos el permiso la primera vez que se muestra la pantalla (si todavía no fue otorgado)
    LaunchedEffect(Unit) {
        if (!permisoUbicacionOtorgado) {
            launcherPermisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    LaunchedEffect(userId) {
        viewModel.cargarHabitos(userId)
        viewModel.cargarDesafios()
        viewModel.cargarFotoPerfil(userId)
    }

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

    HabitosContent(
        habitos           = uiState.habitos,
        cargando          = uiState.cargando,
        error             = uiState.error,
        estaCumplidoHoyFn = { uiState.estaCumplidoHoy(it) },
        desafiosCatalogo  = desafios,
        onVerDetalle      = onVerDetalle,
        onAlternarEstado  = { viewModel.alternarEstadoHabito(it) },
        fotoPerfilUrl     = uiState.fotoPerfilUrl
    )
}

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
    fotoPerfilUrl: String? = null
) {
    var filtroSeleccionado by remember { mutableStateOf("TODOS") }
    var textoBuscado       by remember { mutableStateOf("") }

    val habitosFiltrados = habitos.filter { uh ->
        val cumpleTexto = uh.nombreCache.contains(textoBuscado, ignoreCase = true)
        val cumplidoHoy = estaCumplidoHoyFn(uh.id)
        val cumpleEstado = when (filtroSeleccionado) {
            "PENDIENTES" -> !cumplidoHoy
            "CUMPLIDOS"  -> cumplidoHoy
            else         -> true
        }
        cumpleTexto && cumpleEstado
    }

    Scaffold(
        topBar = {
            Surface(
                color          = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Mis Hábitos",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Foto de perfil en círculo
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
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
        }
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
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
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
                    contentPadding      = PaddingValues(bottom = 16.dp)
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
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = null,
                    tint               = colorEsquema.onSurfaceVariant,
                    modifier           = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text  = "Buscar hábito...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorEsquema.onSurfaceVariant
                        )
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
                Text(
                    text  = usuarioHabito.nombreCache,
                    style = MaterialTheme.typography.titleMedium
                )
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
            Checkbox(
                checked         = cumplidoHoy,
                onCheckedChange = { onAlternarEstado() }
            )
        }
    }
}