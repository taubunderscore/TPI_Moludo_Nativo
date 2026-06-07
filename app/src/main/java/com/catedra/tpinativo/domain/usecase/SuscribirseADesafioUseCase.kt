package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.repository.DesafiosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository

/**
 * Suscribe al usuario a un desafío del catálogo:
 * crea un UsuarioHabito hijo por cada hábito del desafío
 * y registra la suscripción en usuario_desafios.
 * Es idempotente: el repositorio ignora la operación si ya existe el documento.
 */
class SuscribirseADesafioUseCase(
    private val habitosRepository: HabitosRepository,
    private val desafiosRepository: DesafiosRepository
) {
    suspend operator fun invoke(userId: String, desafio: Desafio) {
        val habitosHijosIds = mutableListOf<String>()

        desafio.habitosIds.forEach { catalogoHabitoId ->
            val habito = habitosRepository.obtenerHabitoPorId(catalogoHabitoId) ?: return@forEach
            val idHijo = habitosRepository.suscribirUsuarioAHabito(
                userId    = userId,
                habito    = habito,
                desafioId = desafio.id
            )
            if (idHijo.isNotEmpty()) habitosHijosIds.add(idHijo)
        }

        desafiosRepository.suscribirUsuarioADesafio(userId, desafio, habitosHijosIds)
    }
}
