package com.catedra.tpinativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.catedra.tpinativo.viewmodel.HabitosViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.catedra.tpinativo.ui.theme.TPINativoTheme

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    TPINativoTheme {
        LoginContent(
            errorMensaje = "Contraseña incorrecta (Ejemplo de Preview)",
            isLoadingInit = false,
            onIniciarSesionClick = { _, _ -> },
            onIrARegistro = {}
        )
    }
}

@Composable
fun LoginScreen(
    viewModel: HabitosViewModel,
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit
) {
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LoginContent(
        errorMensaje = errorMensaje,
        isLoadingInit = isLoading,
        onIniciarSesionClick = { email, password ->
            isLoading = true
            errorMensaje = null
            com.google.firebase.auth.FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    isLoading = false
                    onLoginExitoso()
                }
                .addOnFailureListener { exception ->
                    isLoading = false
                    errorMensaje = when {
                        exception.message?.contains("no user record") == true -> "No existe una cuenta con ese email"
                        exception.message?.contains("password is invalid") == true -> "Contraseña incorrecta"
                        exception.message?.contains("badly formatted") == true -> "El formato del email no es válido"
                        else -> "Error al iniciar sesión: ${exception.message}"
                    }
                }
        },
        onIrARegistro = onIrARegistro
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    errorMensaje: String?,
    isLoadingInit: Boolean,
    onIniciarSesionClick: (String, String) -> Unit,
    onIrARegistro: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mis Hábitos Diarios",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMensaje != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onIniciarSesionClick(email, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoadingInit
        ) {
            if (isLoadingInit) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onIrARegistro) {
            Text("¿No tenés cuenta? Registrate")
        }
    }
}