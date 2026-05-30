package com.catedra.tpinativo.data.model

data class HabitoSuscrito(
    val id: String = "",
    val plantillaId: String? = null, // null si lo creó el usuario de cero
    val userId: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: String = "DIARIO",
    val diasConfigurados: List<Int> = emptyList(),
    val fechasCumplidas: List<String> = emptyList(), // Historial: ["2026-05-28", "2026-05-29"]
    val esPersonalizado: Boolean = false,
    val horaRecordatorio: String? = null // Ej: "08:30"
)