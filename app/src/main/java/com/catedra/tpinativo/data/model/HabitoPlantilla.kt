package com.catedra.tpinativo.data.model
//Este es el molde para los desafíos semanales y hábitos de moda que ofrece la app.
enum class TipoFrecuencia {
    DIARIO, SEMANAL, MENSUAL
}

data class HabitoPlantilla(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "", // Físico, Salud, Productividad, Estudio
    val grupos: List<String> = emptyList(), // ["desafios", "moda"]
    val frecuencia: TipoFrecuencia = TipoFrecuencia.DIARIO,
    val diasConfigurados: List<Int> = emptyList() // [1, 3, 5] para Lun/Mie/Vie
)