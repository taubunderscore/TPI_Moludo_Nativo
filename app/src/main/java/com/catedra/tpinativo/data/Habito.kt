package com.catedra.tpinativo.data

data class HabitoSuscrito(
    val id: String,
    val plantillaId: String?, // Será NULL si el hábito lo inventó el usuario de cero
    val userId: String,
    val nombre: String,
    val categoria: String,
    val frecuencia: String,          // "DIARIO", "SEMANAL"
    val diasConfigurados: List<Int>, // [1, 3, 5] si eligió días específicos
    val fechasCumplidas: List<String> = emptyList(),

    // LAS NOTIFICACIONES Y CREACIÓN:
    val esPersonalizado: Boolean = false, // True si lo creó él, False si vino del catálogo
    val horaRecordatorio: String? = null  // Ej: "18:45" (Null si no quiere alarmas)
)