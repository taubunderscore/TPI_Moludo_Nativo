package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

/**
 * Da de baja un hábito individual:
 * desactiva el UsuarioHabito y elimina su historial de cumplimientos.
 */
class DarDeBajaHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val cumplimientosRepository: CumplimientosRepository
) {
    suspend operator fun invoke(userId: String, usuarioHabitoId: String) {
        habitosRepository.desactivarUsuarioHabito(usuarioHabitoId)
        cumplimientosRepository.eliminarCumplimientosDeHabito(userId, usuarioHabitoId)
    }
}
