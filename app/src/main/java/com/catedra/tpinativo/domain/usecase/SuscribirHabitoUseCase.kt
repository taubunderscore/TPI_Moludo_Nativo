package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.repository.HabitosRepository

class SuscribirHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val programarRecordatorioUseCase: ProgramarRecordatorioUseCase
) {
    suspend operator fun invoke(
        userId: String,
        plantilla: HabitoPlantilla,
        horaRecordatorio: String? = null
    ) {
        // 1. Suscribir y obtener el ID del documento creado
        val habitoId = habitosRepository.suscribirUsuarioAHabito(
            userId = userId,
            plantilla = plantilla,
            horaRecordatorio = horaRecordatorio
        )

        // 2. Programar notificación si el usuario eligió hora
        if (habitoId.isNotEmpty()) {
            programarRecordatorioUseCase(
                habitoId = habitoId,
                nombreHabito = plantilla.nombre,
                horaRecordatorio = horaRecordatorio
            )
        }
    }
}