package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.DesafioObjetivo //crear
import com.catedra.tpinativo.data.repository.DesafiosRepository //crear

class ObtenerDesafiosCatalogoUseCase(
    private val desafiosRepository: DesafiosRepository
) {
    // El operador invoke permite llamar al caso de uso como si fuera una función
    suspend operator fun invoke(): List<DesafioObjetivo> {
        return desafiosRepository.obtenerTodosLosDesafios()
    }
}