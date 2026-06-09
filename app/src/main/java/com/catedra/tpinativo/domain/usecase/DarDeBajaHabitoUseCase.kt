package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

class DarDeBajaHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val cumplimientosRepository: CumplimientosRepository,
    private val context: android.content.Context
) {
    suspend operator fun invoke(userId: String, usuarioHabitoId: String) {
        habitosRepository.desactivarUsuarioHabito(usuarioHabitoId)
        cumplimientosRepository.eliminarCumplimientosDeHabito(userId, usuarioHabitoId)
        com.catedra.tpinativo.domain.service.NotificacionesService.cancelar(
            context = context,
            habitoId = usuarioHabitoId
        )
    }
}