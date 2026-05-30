package com.catedra.tpinativo.data.model
//Este archivo define cuántas veces hay que cumplir la plantilla para destrabar el logro.
data class DesafioObjetivo(
    val id: String = "",
    val plantillaId: String = "", // Atado al ID de HabitoPlantilla
    val nombreDesafio: String = "",
    val descripcion: String = "",
    val metaObjetivo: Int = 0 // Cantidad de tildes requeridas (ej: 30)
)