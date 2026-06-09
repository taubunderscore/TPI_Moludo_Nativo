package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

/**
 * Da de baja un hábito individual:
 * desactiva el UsuarioHabito y elimina su historial de cumplimientos. si tiene alarma o sea la notificacion la cancela
 */
class DarDeBajaHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val cumplimientosRepository: CumplimientosRepository,
    private val context: android.content.Context  // ← nuevo
) {
    suspend operator fun invoke(userId: String, usuarioHabitoId: String) {
        habitosRepository.desactivarUsuarioHabito(usuarioHabitoId)
        cumplimientosRepository.eliminarCumplimientosDeHabito(userId, usuarioHabitoId)
        // ✅ Cancelar la alarma
        com.catedra.tpinativo.domain.service.NotificacionesService.cancelar(
            context  = context,
            habitoId = usuarioHabitoId
        )
    }
}