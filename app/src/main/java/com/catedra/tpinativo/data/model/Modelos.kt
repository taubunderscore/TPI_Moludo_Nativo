package com.catedra.tpinativo.data.model

// ─────────────────────────────────────────────────────────────────────────────
//  ENUMS
// ─────────────────────────────────────────────────────────────────────────────

enum class TipoFrecuencia { DIARIO, SEMANAL, MENSUAL }

enum class TipoDesafio { ACUMULACION, COMBO }

enum class CategoriaHabito(val display: String) {
    FISICO("Físico"),
    ESTUDIO("Estudio"),
    SALUD("Salud"),
    PRODUCTIVIDAD("Productividad")
}

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: habitos
//  Catálogo global.
// ─────────────────────────────────────────────────────────────────────────────
data class Habito(
    val id: String = "",
    val nombre: String = "",
    val categoria: String = "",
    val frecuencia: TipoFrecuencia = TipoFrecuencia.DIARIO,
    val diasConfigurados: List<Int> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: desafios
// ─────────────────────────────────────────────────────────────────────────────
data class Desafio(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val tipo: TipoDesafio = TipoDesafio.ACUMULACION,
    val habitosIds: List<String> = emptyList(),
    val meta: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: usuario_habitos
// ─────────────────────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: usuario_desafios
// ─────────────────────────────────────────────────────────────────────────────
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
    val logroLongitud: Double? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: historial_cumplimientos
// ─────────────────────────────────────────────────────────────────────────────
data class Cumplimiento(
    val id: String = "",
    val userId: String = "",
    val habitoId: String = "",
    val habitoCatalogoId: String = "",
    val fecha: String = "",
    val nota: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
//  COLECCIÓN: habitos_personalizados
//  Hábitos creados por el propio usuario. Siempre DIARIO.
//  El campo horaRecordatorio dispara una notificación local (AlarmManager).
// ─────────────────────────────────────────────────────────────────────────────
data class HabitoPersonalizado(
    val id: String = "",                          // doc.id generado por Firestore
    val userId: String = "",
    val nombre: String = "",
    val detalle: String = "",
    val categoria: CategoriaHabito = CategoriaHabito.SALUD,
    val horaRecordatorio: String = "",            // formato "HH:mm", ej: "08:30"
    val activo: Boolean = true,
    val fechaCreacion: String = ""                // ISO yyyy-MM-dd
)
