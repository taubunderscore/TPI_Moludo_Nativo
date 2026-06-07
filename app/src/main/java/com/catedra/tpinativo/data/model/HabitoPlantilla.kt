package com.catedra.tpinativo.data.model

enum class TipoFrecuencia {
    DIARIO, SEMANAL, MENSUAL
}

data class HabitoPlantilla(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val grupos: List<String> = emptyList(),
    val frecuencia: TipoFrecuencia = TipoFrecuencia.DIARIO,
    val diasConfigurados: List<Int> = emptyList(),
)
