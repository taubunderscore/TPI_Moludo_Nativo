package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.repository.HabitosRepository

class SuscribirHabitoUseCase(
    private val habitosRepository: HabitosRepository
) {
    // El "Invoke" permite ejecutar la clase como si fuera una función
    suspend operator fun invoke(userId: String, plantilla: HabitoPlantilla) {
        // Acá podemos meter reglas de negocio futuras (ej: validar que no esté suscrito ya)
        habitosRepository.suscribirUsuarioAHabito(userId, plantilla)
    }
}