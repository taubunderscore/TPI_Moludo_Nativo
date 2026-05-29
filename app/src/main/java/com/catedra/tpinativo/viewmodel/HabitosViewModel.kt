package com.catedra.tpinativo.viewmodel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.ViewModel
import com.catedra.tpinativo.data.Habito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estructura de estado unificada para la UI (como en el Lab 2B)
data class HabitosUiState(
    val habitos: List<Habito> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class HabitosViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    fun iniciarSesion(email: String, password: String, onResultado: (Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Por favor, completa todos los campos"
            onResultado(false)
            return
        }

        _loginError.value = null // Limpiamos errores previos

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    // ¡Login exitoso en los servidores de Google!
                    onResultado(true)
                } else {
                    // Si falló (contraseña mal, usuario no existe, etc.)
                    _loginError.value = tarea.exception?.localizedMessage ?: "Error de autenticación"
                    onResultado(false)
                }
            }
    }

    fun limpiarError() {
        _loginError.value = null
    }

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow() //lo expongo protegido por seguridad

    // LEER: Escucha los datos de la colección en tiempo real filtrados por usuario (RF2)
    fun cargarHabitos(userId: String) {
        _uiState.value = _uiState.value.copy(cargando = true)

        db.collection("habitos")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = HabitosUiState(cargando = false, error = e.message)
                    return@addSnapshotListener
                }

                val lista = snapshot?.toObjects(Habito::class.java) ?: emptyList()
                _uiState.value = HabitosUiState(habitos = lista, cargando = false)
            }
    }

    // ACTUALIZAR: Alterna el checkbox y actualiza Firestore de forma remota (RF4)
    fun alternarEstadoHabito(habitoId: String, estadoActual: Boolean) {
        db.collection("habitos").document(habitoId)
            .update("cumplido", !estadoActual)
    }
}