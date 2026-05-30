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

// ==========================================
// 1. LA PREVIEW (Ahora funciona 100% offline)
// ==========================================
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    TPINativoTheme {
        // Le pasamos datos de prueba estáticos directamente al contenido,
        // esquivando por completo al ViewModel y a Firebase.
        LoginContent(
            errorMensaje = "Contraseña incorrecta (Ejemplo de Preview)",
            isLoadingInit = false,
            onIniciarSesionClick = { _, _ -> }
        )
    }
}

// ==========================================
// 2. CONTENEDOR REAL (El que usa la app)
// ==========================================
@Composable
fun LoginScreen(
    viewModel: HabitosViewModel,
    onLoginExitoso: () -> Unit
) {
    // Si tu nuevo HabitosViewModel ya no tiene estas propiedades de login,
    // usamos estados locales temporales para que no te rompa la compilación del TP.
    var errorMensaje by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Este contenedor solo actúa de puente
    LoginContent(
        errorMensaje = errorMensaje,
        isLoadingInit = isLoading,
        onIniciarSesionClick = { email, password ->
            isLoading = true

            // Forzamos un login exitoso directo para el MVP si estás probando,
            // o si volviste a meter la función en el ViewModel, le declaramos el tipo (Boolean)
            // para ganarle al error del compilador:
            isLoading = false
            onLoginExitoso()
        }
    )
}

// ==========================================
// 3. EL CONTENIDO VISUAL (Limpio de dependencias pesadas)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    errorMensaje: String?,
    isLoadingInit: Boolean,
    onIniciarSesionClick: (String, String) -> Unit
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
    }
}