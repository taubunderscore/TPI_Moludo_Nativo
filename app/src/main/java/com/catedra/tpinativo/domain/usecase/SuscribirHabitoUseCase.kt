package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.Habito
import com.catedra.tpinativo.data.repository.HabitosRepository

class SuscribirHabitoUseCase(
    private val habitosRepository: HabitosRepository
) {
    suspend operator fun invoke(
        userId: String,
        habito: Habito,
        horaRecordatorio: String? = null
    ) {
        val yaActivo = habitosRepository.obtenerHabitosUsuario(userId, incluirDesafios = false)
            .any { it.habitoId == habito.id }
        if (yaActivo) return
        habitosRepository.suscribirUsuarioAHabito(
            userId = userId,
            habito = habito,
            horaRecordatorio = horaRecordatorio
        )
    }
}