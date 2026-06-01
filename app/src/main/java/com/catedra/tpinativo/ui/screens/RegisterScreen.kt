package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
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

        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMensaje!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                // Validación básica
                when {
                    nombre.isBlank() -> errorMensaje = "El nombre es obligatorio"
                    edad.isBlank() || edad.toIntOrNull() == null -> errorMensaje = "Ingresá una edad válida"
                    email.isBlank() -> errorMensaje = "El correo es obligatorio"
                    password.length < 6 -> errorMensaje = "La contraseña debe tener al menos 6 caracteres"
                    else -> {
                        isLoading = true
                        errorMensaje = null
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                isLoading = false
                                onRegistroExitoso()
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMensaje = when {
                                    exception.message?.contains("already in use") == true -> "Ya existe una cuenta con ese email"
                                    exception.message?.contains("badly formatted") == true -> "El formato del email no es válido"
                                    else -> "Error al registrarse: ${exception.message}"
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