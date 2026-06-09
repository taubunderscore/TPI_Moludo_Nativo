package com.catedra.tpinativo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.Habito
import com.catedra.tpinativo.data.model.UsuarioDesafio
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.DesafiosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.UserRepository
import com.catedra.tpinativo.domain.service.NotificacionesService
import com.catedra.tpinativo.domain.usecase.DarDeBajaDesafioUseCase
import com.catedra.tpinativo.domain.usecase.DarDeBajaHabitoUseCase
import com.catedra.tpinativo.domain.usecase.GestionarProgresoHabitoUseCase
import com.catedra.tpinativo.domain.usecase.ObtenerDesafiosCatalogoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirHabitoUseCase
import com.catedra.tpinativo.domain.usecase.SuscribirseADesafioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────

data class HabitosUiState(
    val habitos: List<UsuarioHabito> = emptyList(),
    val fechasPorHabito: Map<String, List<String>> = emptyMap(),
    val desafiosSuscritos: List<UsuarioDesafio> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val ultimoDesafioLogrado: String? = null,
    val mensajeInspirador: String? = null,
    // Foto de perfil del usuario logueado (URL de Cloudinary)
    val fotoPerfilUrl: String? = null,
    val mensajeHabitoCumplido: String? = null
) {
    private val hoy: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun estaCumplidoHoy(usuarioHabitoId: String): Boolean =
        fechasPorHabito[usuarioHabitoId]?.contains(hoy) == true

    fun fechasCumplidas(usuarioHabitoId: String): List<String> =
        fechasPorHabito[usuarioHabitoId] ?: emptyList()

    fun obtenerEstadoHabitoIndividual(catalogoHabitoId: String): Pair<String, Boolean> {
        val activo = habitos.find { it.habitoId == catalogoHabitoId && it.desafioId == null }
            ?: return Pair("Suscribir", false)
        return if (estaCumplidoHoy(activo.id))
            Pair("¡Completado! 💪", false)
        else
            Pair("Ya suscripto", true)
    }

    fun obtenerEstadoDesafio(desafioId: String): Pair<String, Boolean> {
        val registro = desafiosSuscritos.find { it.desafioId == desafioId }
            ?: return Pair("Unirse", false)

        val hijosIds = habitos.filter { it.desafioId == desafioId }.map { it.id }
        val todosHoy = hijosIds.isNotEmpty() && hijosIds.all { estaCumplidoHoy(it) }

        return if (todosHoy)
            Pair("¡Cumplido hoy! 🎉", false)
        else
            Pair("Ya suscripto", true)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class HabitosViewModel(
    private val habitosRepository: HabitosRepository,
    private val cumplimientosRepository: CumplimientosRepository,
    private val desafiosRepository: DesafiosRepository,
    private val gestionarProgresoUseCase: GestionarProgresoHabitoUseCase,
    private val suscribirHabitoUseCase: SuscribirHabitoUseCase,
    private val suscribirseADesafioUseCase: SuscribirseADesafioUseCase,
    private val darDeBajaHabitoUseCase: DarDeBajaHabitoUseCase,
    private val darDeBajaDesafioUseCase: DarDeBajaDesafioUseCase,
    private val obtenerDesafiosCatalogoUseCase: ObtenerDesafiosCatalogoUseCase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitosUiState())
    val uiState: StateFlow<HabitosUiState> = _uiState.asStateFlow()

    private val _catalogoHabitos = MutableStateFlow<List<Habito>>(emptyList())
    val catalogoHabitos: StateFlow<List<Habito>> = _catalogoHabitos.asStateFlow()

    private val _desafiosCatalogo = MutableStateFlow<List<Desafio>>(emptyList())
    val desafiosCatalogo: StateFlow<List<Desafio>> = _desafiosCatalogo.asStateFlow()

    // ─── Carga principal ─────────────────────────────────────────────────────

    fun cargarHabitos(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            try {
                val habitos           = habitosRepository.obtenerHabitosUsuario(userId)
                val desafiosSuscritos = desafiosRepository.obtenerDesafiosUsuario(userId)
                android.util.Log.d("CARGAR_HABITOS", "habitos=${habitos.size} | desafios=${desafiosSuscritos.size}")

                val fechasPorHabito = habitos.associate { uh ->
                    uh.id to cumplimientosRepository.obtenerFechasCumplidas(userId, uh.id)
                }

                _uiState.update {
                    it.copy(
                        habitos           = habitos,
                        fechasPorHabito   = fechasPorHabito,
                        desafiosSuscritos = desafiosSuscritos,
                        cargando          = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, cargando = false) }
            }
        }
    }

    /**
     * Carga la URL de la foto de perfil del usuario desde Firestore
     * y la expone en el UiState para que HabitosScreen la muestre.
     */
    fun cargarFotoPerfil(userId: String) {
        viewModelScope.launch {
            try {
                val datos = UserRepository().obtenerUsuario(userId)
                val url   = datos?.get("foto") as? String
                if (!url.isNullOrBlank()) {
                    _uiState.update { it.copy(fotoPerfilUrl = url) }
                }
            } catch (e: Exception) {
                android.util.Log.w("HabitosVM", "No se pudo cargar foto de perfil: ${e.localizedMessage}")
            }
        }
    }
    // listo todos los habitos cumplidos y desafios pormas que ya no esten suscriptos
    private val _todosHabitosUsuario = MutableStateFlow<List<UsuarioHabito>>(emptyList())
    val todosHabitosUsuario: StateFlow<List<UsuarioHabito>> = _todosHabitosUsuario.asStateFlow()

    private val _todosDesafiosUsuario = MutableStateFlow<List<UsuarioDesafio>>(emptyList())
    val todosDesafiosUsuario: StateFlow<List<UsuarioDesafio>> = _todosDesafiosUsuario.asStateFlow()

    private val _todasLasFechas = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val todasLasFechas: StateFlow<Map<String, List<String>>> = _todasLasFechas.asStateFlow()

    fun cargarDatosLogros(userId: String) {
        viewModelScope.launch {
            try {
                val habitos  = habitosRepository.obtenerTodosLosHabitosUsuario(userId)
                val desafios = desafiosRepository.obtenerTodosLosDesafiosUsuario(userId)
                val fechas   = habitos.associate { uh ->
                    uh.id to cumplimientosRepository.obtenerFechasCumplidas(userId, uh.id)
                }
                _todosHabitosUsuario.value  = habitos
                _todosDesafiosUsuario.value = desafios
                _todasLasFechas.value       = fechas
            } catch (e: Exception) {
                android.util.Log.e("HabitosVM", "cargarDatosLogros: ${e.localizedMessage}")
            }
        }
    }
    // ─── Alternar cumplimiento ───────────────────────────────────────────────

    fun alternarEstadoHabito(usuarioHabito: UsuarioHabito) {
        viewModelScope.launch {
            try {
                val resultado = gestionarProgresoUseCase.ejecutar(usuarioHabito)
                val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                _uiState.update { estado ->
                    val nuevasFechas = estado.fechasPorHabito.toMutableMap()
                    nuevasFechas[usuarioHabito.id] = resultado.fechasCumplidas
                    val seTildo = resultado.fechasCumplidas.contains(hoy)

                    estado.copy(
                        fechasPorHabito       = nuevasFechas,
                        ultimoDesafioLogrado  = if (resultado.desafioDesbloqueado) resultado.nombreDesafio else null,
                        mensajeHabitoCumplido = if (seTildo) "✅ ${usuarioHabito.nombreCache} cumplido hoy! 💪" else null
                    )
                }

                // ✅ Si se desbloqueó un desafío, refrescamos para ver el logro en Logros
                if (resultado.desafioDesbloqueado) {
                    cargarHabitos(usuarioHabito.userId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo actualizar el estado") }
            }
        }
    }
    fun resetearMensajeHabitoCumplido() {
        _uiState.update { it.copy(mensajeHabitoCumplido = null) }
    }
    // ─── Catálogo ────────────────────────────────────────────────────────────

    fun cargarCatalogoPorCategoria(categoria: String) {
        viewModelScope.launch {
            _catalogoHabitos.value = habitosRepository.obtenerHabitosPorCategoria(categoria)
        }
    }

    fun cargarTodosLosHabitosCatalogo() {
        viewModelScope.launch {
            _catalogoHabitos.value = habitosRepository.obtenerTodosLosHabitos()
        }
    }

    fun cargarDesafios() {
        viewModelScope.launch {
            _desafiosCatalogo.value = obtenerDesafiosCatalogoUseCase()
        }
    }

    // ─── Suscripciones ───────────────────────────────────────────────────────

    fun suscribirseAHabito(
        userId: String,
        habito: Habito,
        horaRecordatorio: String? = null  // ✅ nuevo
    ) {
        viewModelScope.launch {
            try {
                suscribirHabitoUseCase(userId, habito, horaRecordatorio)
                // ✅ Programar notificación si eligió hora
                if (horaRecordatorio != null) {
                    NotificacionesService.programar(
                        context          = context,
                        habitoId         = "${userId}_${habito.id}",
                        nombre           = habito.nombre,
                        detalle          = "Es hora de cumplir tu hábito 💪",
                        horaRecordatorio = horaRecordatorio
                    )
                }
                _uiState.update {
                    it.copy(mensajeInspirador = "¡Hábito activado! Pasito a pasito se llega lejos 🎯")
                }
                cargarHabitos(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo suscribir al hábito") }
            }
        }
    }

    fun suscribirseADesafio(userId: String, desafio: Desafio) {
        viewModelScope.launch {
            try {
                suscribirseADesafioUseCase(userId, desafio)
                _uiState.update {
                    it.copy(mensajeInspirador = "¡Te sumaste al desafío! ¡Dale con fuerza! 🔥")
                }
                cargarHabitos(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo suscribir al desafío") }
            }
        }
    }

    // ─── Bajas ───────────────────────────────────────────────────────────────

    fun darDeBajaHabito(userId: String, usuarioHabitoId: String) {
        viewModelScope.launch {
            try {
                darDeBajaHabitoUseCase(userId, usuarioHabitoId)
                cargarHabitos(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo dar de baja el hábito") }
            }
        }
    }

    fun darDeBajaDesafioCompleto(
        userId: String,
        desafioId: String,
        habitosHijosIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                darDeBajaDesafioUseCase(userId, desafioId, habitosHijosIds)
                cargarHabitos(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "No se pudo dar de baja el desafío") }
            }
        }
    }

    // ─── Reset helpers ───────────────────────────────────────────────────────

    fun resetearAlertaDesafio() {
        _uiState.update { it.copy(ultimoDesafioLogrado = null) }
    }

    fun resetearMensajeInspirador() {
        _uiState.update { it.copy(mensajeInspirador = null) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Factory
// ─────────────────────────────────────────────────────────────────────────────

class HabitosViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val habitosRepo       = HabitosRepository()
        val cumplimientosRepo = CumplimientosRepository()
        val desafiosRepo      = DesafiosRepository()

        // Use cases — le pasamos el context para geolocalización
        val gestionarProgreso  = GestionarProgresoHabitoUseCase(
            habitosRepository       = habitosRepo,
            cumplimientosRepository = cumplimientosRepo,
            desafiosRepository      = desafiosRepo,
            context                 = context        // ← nuevo
        )
        val suscribirHabito    = SuscribirHabitoUseCase(habitosRepo)
        val suscribirDesafio   = SuscribirseADesafioUseCase(habitosRepo, desafiosRepo)
        val bajaHabito         = DarDeBajaHabitoUseCase(habitosRepo, cumplimientosRepo,context = context)
        val bajaDesafio        = DarDeBajaDesafioUseCase(habitosRepo, desafiosRepo, cumplimientosRepo)
        val obtenerDesafios    = ObtenerDesafiosCatalogoUseCase(desafiosRepo)

        return HabitosViewModel(
            habitosRepository              = habitosRepo,
            cumplimientosRepository        = cumplimientosRepo,
            desafiosRepository             = desafiosRepo,
            gestionarProgresoUseCase       = gestionarProgreso,
            suscribirHabitoUseCase         = suscribirHabito,
            suscribirseADesafioUseCase     = suscribirDesafio,
            darDeBajaHabitoUseCase         = bajaHabito,
            darDeBajaDesafioUseCase        = bajaDesafio,
            obtenerDesafiosCatalogoUseCase = obtenerDesafios,
            context                        = context
        ) as T
    }
}