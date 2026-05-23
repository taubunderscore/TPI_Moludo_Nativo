package com.catedra.tpinativo.data

data class Habito(
    val id: String = "",           // ID único autogenerado por Firestore
    val userId: String = "",       // ID del usuario de Firebase Auth (RF2 de seguridad)
    val nombre: String = "",       // Ej: "Beber agua", "Hacer ejercicio"
    val categoria: String = "",    // Ej: "Salud", "Físico"
    val cumplido: Boolean = false, // Estado del hábito
    val fecha: String = "",         // Fecha de registro (YYYY-MM-DD)
    val latitud: Double? = null,   // Opcionales para el RF5 (Geolocalización)
    val longitud: Double? = null   // Opcionales para el RF5 (Geolocalización)
)