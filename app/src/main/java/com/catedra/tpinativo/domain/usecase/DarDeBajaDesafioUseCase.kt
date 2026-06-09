package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.Cumplimiento
import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.DesafiosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

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
