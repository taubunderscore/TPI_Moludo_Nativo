package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.LogrosRepository

class GestionarProgresoHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val logrosRepository: LogrosRepository
) {

    suspend fun ejecutar(habito: HabitoSuscrito): ResultadoProgreso {
        // 1. Guardamos el tilde del día en Firestore y obtenemos el historial actualizado
        val fechasActualizadas = habitosRepository.alternarFechaCumplimiento(habito)

        // Clonamos el objeto con el nuevo historial para los cálculos siguientes
        val habitoActualizado = habito.copy(fechasCumplidas = fechasActualizadas)

        // Si el hábito es personalizado y no tiene plantilla global, no puede tener desafíos asociados
        if (habito.plantillaId == null) {
            return ResultadoProgreso(habitoActualizado = habitoActualizado)
        }

        // 2. Buscamos si este hábito está atado a algún desafío de la cátedra
        val desafio = logrosRepository.obtenerDesafioPorPlantilla(habito.plantillaId)
            ?: return ResultadoProgreso(habitoActualizado = habitoActualizado)

        // 3. LA CUENTITA: Calculamos el avance dinámico basado en el tamaño del historial
        val totalDiasCumplidos = fechasActualizadas.size.toFloat()
        val metaObjetivo = desafio.metaObjetivo.toFloat()

        val progresoFloat = (totalDiasCumplidos / metaObjetivo).coerceAtMost(1.0f)
        val porcentajeEntero = (progresoFloat * 100).toInt()

        // 4. VERIFICACIÓN REACTIVA: ¿Llegó a la meta en este preciso tilde?
        val seCumplioMeta = fechasActualizadas.size >= desafio.metaObjetivo

        if (seCumplioMeta) {
            // Grabamos a fuego el logro histórico en su propia colección
            logrosRepository.registrarLogroGanado(habito.userId, desafio)
        }

        // Devolvemos el paquete de datos masticado para que el ViewModel actualice la pantalla
        return ResultadoProgreso(
            habitoActualizado = habitoActualizado,
            porcentajeAvance = porcentajeEntero,
            progresoBarra = progresoFloat,
            desafioDesbloqueado = seCumplioMeta,
            nombreDesafio = desafio.nombreDesafio
        )
    }
}

// Data class auxiliar para empaquetar la respuesta del proceso
data class ResultadoProgreso(
    val habitoActualizado: HabitoSuscrito,
    val porcentajeAvance: Int = 0,
    val progresoBarra: Float = 0.0f,
    val desafioDesbloqueado: Boolean = false,
    val nombreDesafio: String? = null
)