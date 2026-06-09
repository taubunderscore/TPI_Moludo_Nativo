package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.DesafiosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

/**
 * Da de baja un desafío completo:
 * desactiva cada hábito hijo, limpia su historial de cumplimientos
 * y elimina el registro de suscripción al desafío.
 */
class DarDeBajaDesafioUseCase(
    private val habitosRepository: HabitosRepository,
    private val desafiosRepository: DesafiosRepository,
    private val cumplimientosRepository: CumplimientosRepository
) {
    suspend operator fun invoke(
        userId: String,
        desafioId: String,
        habitosHijosIds: List<String>
    ) {
        habitosHijosIds.forEach { hijoId ->
            habitosRepository.desactivarUsuarioHabito(hijoId)
        }
        desafiosRepository.desactivarSuscripcionDesafio(userId, desafioId)
    }
}
