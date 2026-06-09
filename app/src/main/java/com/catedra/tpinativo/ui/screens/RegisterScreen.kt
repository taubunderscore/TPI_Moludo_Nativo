package com.catedra.tpinativo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.catedra.tpinativo.data.repository.CloudinaryRepository
import com.catedra.tpinativo.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var estadoSubida by remember { mutableStateOf("") }
    val interesesDisponibles = listOf("Físico", "Estudio", "Salud", "Productividad")
    val interesesSeleccionados = remember { mutableStateListOf<String>() }
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var mostrarDialogoFoto by remember { mutableStateOf(false) }

    val cameraUri: Uri = remember {
        val archivo = File(context.cacheDir, "foto_perfil_tmp.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",   // debe coincidir con tu AndroidManifest
            archivo
        )
    }

    val launcherCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { exito ->
        if (exito) fotoUri = cameraUri
    }

    val launcherGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) fotoUri = uri
    }

    val launcherPermisoCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) launcherCamara.launch(cameraUri)
        else errorMensaje = "Se necesita permiso de cámara para tomar la foto"
    }

    if (mostrarDialogoFoto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFoto = false },
            title = { Text("Foto de perfil") },
            text = { Text("¿De dónde querés tomar la foto?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoFoto = false
                    val permiso = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    )
                    if (permiso == PackageManager.PERMISSION_GRANTED) {
                        launcherCamara.launch(cameraUri)
                    } else {
                        launcherPermisoCamara.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoFoto = false
                    launcherGaleria.launch("image/*")
                }) { Text("Galería") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { mostrarDialogoFoto = true },
            contentAlignment = Alignment.Center
        ) {
            if (fotoUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(fotoUri),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = if (fotoUri != null) "Foto seleccionada ✓" else "Tocá para agregar foto",
            style = MaterialTheme.typography.bodySmall,
            color = if (fotoUri != null)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = edad,
            onValueChange = { edad = it },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirmar contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
            supportingText = {
                if (confirmPassword.isNotEmpty() && confirmPassword != password)
                    Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Intereses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Seleccioná los que quieras trabajar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interesesDisponibles.forEach { interes ->
                    val seleccionado = interes in interesesSeleccionados
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            if (seleccionado) interesesSeleccionados.remove(interes)
                            else interesesSeleccionados.add(interes)
                        },
                        label = { Text(interes, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMensaje!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (estadoSubida.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = estadoSubida,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                when {
                    nombre.isBlank() -> errorMensaje = "El nombre es obligatorio"
                    edad.isBlank() || edad.toIntOrNull() == null -> errorMensaje =
                        "Ingresá una edad válida"

                    email.isBlank() -> errorMensaje = "El correo es obligatorio"
                    password.length < 6 -> errorMensaje =
                        "La contraseña debe tener al menos 6 caracteres"

                    password != confirmPassword -> errorMensaje = "Las contraseñas no coinciden"
                    else -> {
                        isLoading = true
                        errorMensaje = null

                        FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { authResult ->
                                val uid = authResult.user?.uid ?: ""

                                scope.launch {
                                    try {
                                        var fotoUrl: String? = null
                                        if (fotoUri != null) {
                                            estadoSubida = "Subiendo foto…"
                                            fotoUrl = CloudinaryRepository(context)
                                                .subirFotoPerfil(fotoUri!!, uid)
                                            estadoSubida = if (fotoUrl != null)
                                                "Foto subida ✓" else "No se pudo subir la foto"
                                        }

                                        estadoSubida = "Guardando datos…"
                                        UserRepository().crearUsuario(
                                            userId = uid,
                                            nombre = nombre,
                                            email = email,
                                            edad = edad.toInt(),
                                            intereses = interesesSeleccionados.toList(),
                                            fotoUrl = fotoUrl
                                        )

                                        isLoading = false
                                        estadoSubida = ""
                                        onRegistroExitoso()

                                    } catch (e: Exception) {
                                        isLoading = false
                                        estadoSubida = ""
                                        errorMensaje =
                                            "Error al guardar los datos: ${e.localizedMessage}"
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMensaje = when {
                                    exception.message?.contains("already in use") == true ->
                                        "Ya existe una cuenta con ese email"

                                    exception.message?.contains("badly formatted") == true ->
                                        "El formato del email no es válido"

                                    else ->
                                        "Error al registrarse: ${exception.message}"
                                }
                            }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Registrarse", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onVolver) {
            Text("¿Ya tenés cuenta? Iniciá sesión")
        }
    }
}
