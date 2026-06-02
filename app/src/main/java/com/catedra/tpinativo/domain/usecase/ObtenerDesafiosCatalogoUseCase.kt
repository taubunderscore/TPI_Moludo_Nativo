package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.DesafioObjetivo //crear
import com.catedra.tpinativo.data.repository.DesafiosRepository //crear

class ObtenerDesafiosCatalogoUseCase(
    private val desafiosRepository: DesafiosRepository
) {    suspend operator fun invoke(): List<DesafioObjetivo> {
        return desafiosRepository.obtenerTodosLosDesafios()
    }
}