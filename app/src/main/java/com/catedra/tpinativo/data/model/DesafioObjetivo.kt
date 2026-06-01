package com.catedra.tpinativo.data.model
//Este archivo define cuántas veces hay que cumplir la plantilla para destrabar el logro.
data class DesafioObjetivo(
    val id: String = "",
    val plantillaId: String = "", // Atado al ID de HabitoPlantilla
    val nombreDesafio: String = "",
    val metaObjetivo: Int = 0, // Cantidad de tildes requeridas (ej: 30)
    val habitosRequeridos: List<String> = emptyList(),
    val descripcion: String = "" //  Agregamos esta línea y Firebase hace el resto
)
