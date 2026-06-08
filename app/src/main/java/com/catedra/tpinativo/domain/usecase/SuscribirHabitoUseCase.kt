package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.model.TipoFrecuencia
import com.catedra.tpinativo.data.repository.HabitosRepository

class SuscribirHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val programarRecordatorioUseCase: ProgramarRecordatorioUseCase
) {
    suspend operator fun invoke(
        userId: String,
        plantilla: HabitoPlantilla,
        horaRecordatorio: String? = null,
        frecuencia: TipoFrecuencia = plantilla.frecuencia  // ← nuevo
    ) {
        val habitoId = habitosRepository.suscribirUsuarioAHabito(
            userId = userId,
            plantilla = plantilla,
            horaRecordatorio = horaRecordatorio,
            frecuenciaOverride = frecuencia  // ← nuevo
        )
        if (habitoId.isNotEmpty()) {
            programarRecordatorioUseCase(habitoId, plantilla.nombre, horaRecordatorio)
        }
    }
}