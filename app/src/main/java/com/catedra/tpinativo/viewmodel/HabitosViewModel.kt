package com.catedra.tpinativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Representa el estado de la pantalla
data class HabitosUiState(
    val habitos: List<HabitoSuscrito> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val ultimoDesafioLogrado: String? = null // Guarda el nombre del desafío si acaba de ganar uno
)

class HabitosViewModel(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    // Carga los hábitos del usuario usando el repositorio
    fun cargarHabitos(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val lista = habitosRepository.obtenerSuscripcionesUsuario(userId)
                _uiState.update { it.copy(habitos = lista, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, cargando = false) }
            }
        }
    }

    // Se ejecuta al tocar el checkbox, delegando la lógica al caso de uso modular
    fun alternarEstadoHabito(habito: HabitoSuscrito) {
        viewModelScope.launch {
            try {
                // 1. Ejecutamos el caso de uso (guarda en DB, calcula %, verifica meta)
                val resultado = gestionarProgresoHabitoUseCase.ejecutar(habito)

                // 2. Actualizamos la lista local en memoria para que la UI cambie al instante
                _uiState.update { estadoActual ->
                    val listaActualizada = estadoActual.habitos.map { item ->
                        if (item.id == resultado.habitoActualizado.id) resultado.habitoActualizado else item
                    }
                    estadoActual.copy(
                        habitos = listaActualizada,
                        // Si se desbloqueó un desafío, guardamos el nombre para avisarle a la UI
                        ultimoDesafioLogrado = if (resultado.desafioDesbloqueado) resultado.nombreDesafio else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el estado") }
            }
        }
    }

    // Función para limpiar el cartel de festejo una vez mostrado
    fun resetearAlertaDesafio() {
        _uiState.update { it.copy(ultimoDesafioLogrado = null) }
    }
}
// 🚀 AGREGÁ ESTA CLASE FACTORY AL FINAL DEL ARCHIVO (Afuera de la clase principal):
class HabitosViewModelFactory(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java)) {
            return HabitosViewModel(habitosRepository, gestionarProgresoHabitoUseCase) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}