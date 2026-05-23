package com.catedra.tpinativo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.Habito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estructura de estado unificada para la UI (como en el Lab 2B)
data class HabitosUiState(
    val habitos: List<Habito> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class HabitosViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

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