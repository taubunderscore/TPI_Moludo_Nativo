package com.catedra.tpinativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.model.LogroUsuario
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Representa el estado de la pantalla principal (Home)
data class HabitosUiState(
    val habitos: List<HabitoSuscrito> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val ultimoDesafioLogrado: String? = null // Guarda el nombre del desafío si acaba de ganar uno
)

class HabitosViewModel(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase,
    // Recibimos el caso de uso para suscribirse
    private val suscribirHabitoUseCase: SuscribirHabitoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    // Estado reactivo para el catálogo de la pestaña Descubrir
    private val _plantillasCatalogo = MutableStateFlow<List<HabitoPlantilla>>(emptyList())
    val plantillasCatalogo: StateFlow<List<HabitoPlantilla>> = _plantillasCatalogo.asStateFlow()

    // Estado reactivo para la pestaña de Logros / Medallas
    private val _logrosUsuario = MutableStateFlow<List<LogroUsuario>>(emptyList())
    val logrosUsuario: StateFlow<List<LogroUsuario>> = _logrosUsuario.asStateFlow()

    // Instancia del repositorio de logros para leer las medallas
    private val logrosRepository = com.catedra.tpinativo.data.repository.LogrosRepository()

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

    // Carga las plantillas del catálogo global según la categoría elegida
    fun cargarCatalogoPorCategoria(categoria: String) {
        viewModelScope.launch {
            val lista = habitosRepository.obtenerPlantillasPorCategoria(categoria)
            _plantillasCatalogo.value = lista
        }
    }

    // Ejecuta la suscripción mediante su Caso de Uso y refresca el Home
    fun suscribirseAHabito(userId: String, plantilla: HabitoPlantilla) {
        viewModelScope.launch {
            try {
                suscribirHabitoUseCase(userId, plantilla)
                // Refrescamos automáticamente la lista del Home
                cargarHabitos(userId)
            } catch (e: Exception) {
                // Error pasivo por si falla la red
            }
        }
    }

    // Carga los logros y medallas del usuario de forma asíncrona
    fun cargarLogros(userId: String) {
        viewModelScope.launch {
            try {
                val lista = logrosRepository.obtenerLogrosUsuario(userId)
                _logrosUsuario.value = lista
            } catch (e: Exception) {
                // Error pasivo por si falla la red
            }
        }
    }

    // Función para limpiar el cartel de festejo una vez mostrado
    fun resetearAlertaDesafio() {
        _uiState.update { it.copy(ultimoDesafioLogrado = null) }
    }
} // ⬅️ ACÁ CIERRA LA CLASE PRINCIPAL HABITOSVIEWMODEL

// ==========================================
// 🚀 LA FÁBRICA QUEDA BIEN AFUERA DE LA CLASE:
class HabitosViewModelFactory(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase,
    private val suscribirHabitoUseCase: SuscribirHabitoUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java)) {
            return HabitosViewModel(
                habitosRepository,
                gestionarProgresoHabitoUseCase,
                suscribirHabitoUseCase
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}