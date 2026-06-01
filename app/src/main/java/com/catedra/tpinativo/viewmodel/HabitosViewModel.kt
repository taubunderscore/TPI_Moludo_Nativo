package com.catedra.tpinativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.model.LogroUsuario
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.ObtenerDesafiosCatalogoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirseADesafioUseCase // 🚀 AGREGAMOS ESTA IMPORTACIÓN
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
    val ultimoDesafioLogrado: String? = null
)

class HabitosViewModel(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase,
    private val suscribirHabitoUseCase: SuscribirHabitoUseCase,
    // 🚀 INYECTAMOS LOS DOS CASOS DE USO NUEVOS EN EL CONSTRUCTOR:
    private val obtenerDesafiosCatalogoUseCase: ObtenerDesafiosCatalogoUseCase,
    private val suscribirseADesafioUseCase: SuscribirseADesafioUseCase
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

    // Se ejecuta al tocar el checkbox
    fun alternarEstadoHabito(habito: HabitoSuscrito) {
        viewModelScope.launch {
            try {
                val resultado = gestionarProgresoHabitoUseCase.ejecutar(habito)

                _uiState.update { estadoActual ->
                    val listaActualizada = estadoActual.habitos.map { item ->
                        if (item.id == resultado.habitoActualizado.id) resultado.habitoActualizado else item
                    }
                    estadoActual.copy(
                        habitos = listaActualizada,
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

    // 1. Declarar los flujos de estado para este nuevo feature del challenge
    private val _desafiosCatalogo = MutableStateFlow<List<DesafioObjetivo>>(emptyList())
    val desafiosCatalogo: StateFlow<List<DesafioObjetivo>> = _desafiosCatalogo.asStateFlow()

    // 2. Función para cargar el catálogo de desafíos
    fun cargarDesafios() {
        viewModelScope.launch {
            // Ahora sí te toma la variable porque está declarada arriba en el constructor
            val lista = obtenerDesafiosCatalogoUseCase()
            _desafiosCatalogo.value = lista
        }
    }

    // 3. Función para suscribirse (el click de "Unirse")
    fun suscribirseADesafio(userId: String, desafio: DesafioObjetivo) {
        viewModelScope.launch {
            suscribirseADesafioUseCase(userId, desafio)

            // 🔄 CORREGIDO: Llamamos a cargarHabitos que es el nombre real de tu método de arriba
            cargarHabitos(userId)
        }
    }
}

// ==========================================
// 🚀 LA FÁBRICA ACTUALIZADA PARA REPARTIR LOS NUEVOS CASOS DE USO:
class HabitosViewModelFactory(
    private val habitosRepository: HabitosRepository,
    private val gestionarProgresoHabitoUseCase: GestionarProgresoHabitoUseCase,
    private val suscribirHabitoUseCase: SuscribirHabitoUseCase,
    // Agregar acá también las dependencias para la Factory
    private val obtenerDesafiosCatalogoUseCase: ObtenerDesafiosCatalogoUseCase,
    private val suscribirseADesafioUseCase: SuscribirseADesafioUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitosViewModel::class.java)) {
            return HabitosViewModel(
                habitosRepository,
                gestionarProgresoHabitoUseCase,
                suscribirHabitoUseCase,
                obtenerDesafiosCatalogoUseCase, // 🚀 Pasamos el caso de uso
                suscribirseADesafioUseCase     // 🚀 Pasamos el caso de uso combo
            ) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida")
    }
}