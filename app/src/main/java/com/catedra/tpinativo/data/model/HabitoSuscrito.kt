package com.catedra.tpinativo.data.model

data class HabitoSuscrito(
    val id: String = "",
    val plantillaId: String? = null,
    val userId: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: String = "DIARIO",
    val diasConfigurados: List<Int> = emptyList(),
    val fechasCumplidas: List<String> = emptyList(),
    val esPersonalizado: Boolean = false,
    val horaRecordatorio: String? = null,
    // ✅ null = suscripto individualmente, valor = viene de un desafío
    val desafioId: String? = null
)
