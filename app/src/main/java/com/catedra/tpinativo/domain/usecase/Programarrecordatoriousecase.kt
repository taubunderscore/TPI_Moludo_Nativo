package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.repository.NotificacionRepository

class ProgramarRecordatorioUseCase(
    private val notificacionRepository: NotificacionRepository
) {
    operator fun invoke(habitoId: String, nombreHabito: String, horaRecordatorio: String?) {
        // Solo programamos si el usuario eligió una hora
        if (horaRecordatorio != null) {
            notificacionRepository.programarRecordatorio(habitoId, nombreHabito, horaRecordatorio)
        }
    }
}