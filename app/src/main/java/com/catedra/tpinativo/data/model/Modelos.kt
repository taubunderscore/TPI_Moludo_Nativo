package com.catedra.tpinativo.data.model

// ─────────────────────────────────────────────────────────────────────────────
//  ENUMS
// ─────────────────────────────────────────────────────────────────────────────

enum class TipoFrecuencia { DIARIO, SEMANAL, MENSUAL }

enum class TipoDesafio { ACUMULACION, COMBO }

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: habitos
//  Catálogo global. Antes: habitos_plantillas
// ─────────────────────────────────────────────────────────────────────────────
data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",          // Físico, Salud, Productividad, Estudio
    val frecuencia: TipoFrecuencia = TipoFrecuencia.DIARIO,
    val diasConfigurados: List<Int> = emptyList() // [1,3,5] = Lun/Mie/Vie (solo para SEMANAL)
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: desafios
//  Antes: desafios_objetivos
// ─────────────────────────────────────────────────────────────────────────────
data class Desafio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val tipo: TipoDesafio = TipoDesafio.ACUMULACION,
    val habitosIds: List<String> = emptyList(), // IDs de habitos del catálogo
    val meta: Int = 0                           // días/veces requeridos para ACUMULACION
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: usuario_habitos
//  Relación M-N entre usuario y hábito activo. Un doc por par (userId, habitoId).
// ─────────────────────────────────────────────────────────────────────────────
data class UsuarioHabito(
    val id: String = "",                 // doc.id generado por Firestore
    val userId: String = "",
    val habitoId: String = "",
    val nombreCache: String = "",        // copia del nombre para mostrar sin extra fetch
    val categoriaCache: String = "",
    val frecuenciaCache: String = "DIARIO",
    val diasConfiguradosCache: List<Int> = emptyList(),
    val horaRecordatorio: String? = null,
    val fechaInicio: String = "",        // ISO yyyy-MM-dd
    val activo: Boolean = true,
    val desafioId: String? = null        // null = suscripción individual
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: usuario_desafios
//  Relación M-N entre usuario y desafío suscripto.
// ─────────────────────────────────────────────────────────────────────────────
data class UsuarioDesafio(
    val id: String = "",                 // doc.id = "${userId}_${desafioId}"
    val userId: String = "",
    val desafioId: String = "",
    val nombreCache: String = "",
    val fechaSuscripcion: String = "",   // ISO yyyy-MM-dd
    val completado: Boolean = false,
    val fechaLogro: String? = null,      // ISO yyyy-MM-dd cuando se completó
    val habitosHijosIds: List<String> = emptyList(), // IDs de UsuarioHabito creados para este desafío

    // ── Geolocalización del logro ─────────────────────────────────────────────
    // Se guardan al momento de marcar el desafío como completado.
    // Null si el usuario no otorgó permiso de ubicación o si el desafío no está completado.
    val logroLatitud: Double? = null,
    val logroLongitud: Double? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: historial_cumplimientos
//  Un documento por cada vez que el usuario marca un hábito como cumplido.
//  Índice compuesto requerido en Firestore: userId ASC + habitoId ASC + fecha ASC
// ─────────────────────────────────────────────────────────────────────────────
data class Cumplimiento(
    val id: String = "",                 // doc.id generado
    val userId: String = "",
    val habitoId: String = "",           // ID del UsuarioHabito (no del catálogo)
    val habitoCatalogoId: String = "",   // ID del Habito en catálogo (para stats globales)
    val fecha: String = "",              // ISO yyyy-MM-dd
    val nota: String? = null
)