package com.catedra.tpinativo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.model.CategoriaHabito
import com.catedra.tpinativo.data.model.HabitoPersonalizado
import com.catedra.tpinativo.data.repository.HabitosPersonalizadosRepository
import com.catedra.tpinativo.domain.service.NotificacionesService
import com.catedra.tpinativo.domain.usecase.CrearHabitoPersonalizadoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────

data class HabitosPersonalizadosUiState(
    val habitos: List<HabitoPersonalizado> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val exitoMensaje: String? = null    // mensaje de confirmación tras crear
)

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class HabitosPersonalizadosViewModel(
    private val repository: HabitosPersonalizadosRepository,
    private val crearUseCase: CrearHabitoPersonalizadoUseCase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitosPersonalizadosUiState())
    val uiState: StateFlow<HabitosPersonalizadosUiState> = _uiState.asStateFlow()

    // ─── Carga ───────────────────────────────────────────────────────────────

    fun cargar(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val lista = repository.obtenerHabitosDeUsuario(userId)
                _uiState.update { it.copy(habitos = lista, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, cargando = false) }
            }
        }
    }

    // ─── Crear ───────────────────────────────────────────────────────────────

    fun crear(
        userId: String,
        nombre: String,
        detalle: String,
        categoria: CategoriaHabito,
        horaRecordatorio: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null, exitoMensaje = null) }
            val resultado = crearUseCase(userId, nombre, detalle, categoria, horaRecordatorio)
            resultado.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            cargando     = false,
                            exitoMensaje = "¡Hábito creado! Te recordamos a las $horaRecordatorio 🔔"
                        )
                    }
                    cargar(userId)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(cargando = false, error = e.message) }
                }
            )
        }
    }

    // ─── Baja ────────────────────────────────────────────────────────────────

    fun desactivar(userId: String, habitoId: String) {
        viewModelScope.launch {
            try {
                repository.desactivar(habitoId)
                NotificacionesService.cancelar(context, habitoId)
                cargar(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo eliminar el hábito") }
            }
        }
    }

    // ─── Reset helpers ───────────────────────────────────────────────────────

    fun resetearExito() = _uiState.update { it.copy(exitoMensaje = null) }
    fun resetearError() = _uiState.update { it.copy(error = null) }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Factory
// ─────────────────────────────────────────────────────────────────────────────

class HabitosPersonalizadosViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo      = HabitosPersonalizadosRepository()
        val useCase   = CrearHabitoPersonalizadoUseCase(repo, context.applicationContext)
        return HabitosPersonalizadosViewModel(repo, useCase, context.applicationContext) as T
    }
}
