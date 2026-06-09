package com.catedra.tpinativo.data.model

enum class TipoFrecuencia { DIARIO, SEMANAL, MENSUAL }

enum class TipoDesafio { ACUMULACION, COMBO }

enum class CategoriaHabito(val display: String) {
    FISICO("Físico"),
    ESTUDIO("Estudio"),
    SALUD("Salud"),
    PRODUCTIVIDAD("Productividad")
}

data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: TipoFrecuencia = TipoFrecuencia.DIARIO,
    val diasConfigurados: List<Int> = emptyList()
)

data class Desafio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val tipo: TipoDesafio = TipoDesafio.ACUMULACION,
    val habitosIds: List<String> = emptyList(),
    val meta: Int = 0
)

data class UsuarioHabito(
    val id: String = "",
    val userId: String = "",
    val habitoId: String = "",
    val nombreCache: String = "",
    val categoriaCache: String = "",
    val frecuenciaCache: String = "DIARIO",
    val diasConfiguradosCache: List<Int> = emptyList(),
    val horaRecordatorio: String? = null,
    val fechaInicio: String = "",
    val activo: Boolean = true,
    val desafioId: String? = null,
    val esPersonalizado: Boolean = false
)

data class UsuarioDesafio(
    val id: String = "",
    val userId: String = "",
    val desafioId: String = "",
    val nombreCache: String = "",
    val fechaSuscripcion: String = "",
    val completado: Boolean = false,
    val fechaLogro: String? = null,
    val habitosHijosIds: List<String> = emptyList(),
    val logroLatitud: Double? = null,
    val logroLongitud: Double? = null,
    val activo: Boolean = true
)

data class Cumplimiento(
    val id: String = "",
    val userId: String = "",
    val habitoId: String = "",
    val habitoCatalogoId: String = "",
    val fecha: String = "",
    val nota: String? = null
)

data class HabitoPersonalizado(
    val id: String = "",
    val userId: String = "",
    val nombre: String = "",
    val detalle: String = "",
    val categoria: CategoriaHabito = CategoriaHabito.SALUD,
    val horaRecordatorio: String = "",
    val activo: Boolean = true,
    val fechaCreacion: String = ""
)
