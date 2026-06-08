package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.repository.DesafiosRepository

/**
 * Devuelve el catálogo completo de desafíos disponibles.
 */
class ObtenerDesafiosCatalogoUseCase(
    private val desafiosRepository: DesafiosRepository
) {
    suspend operator fun invoke(): List<Desafio> =
        desafiosRepository.obtenerTodosLosDesafios()
}
