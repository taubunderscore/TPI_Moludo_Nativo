package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.LogrosRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Estructura de respuesta para el ViewModel
data class ResultadoProgreso(
    val habitoActualizado: HabitoSuscrito,
    val porcentajeAvance: Int = 0,
    val progresoBarra: Float = 0.0f,
    val desafioDesbloqueado: Boolean = false,
    val nombreDesafio: String? = null
)

class GestionarProgresoHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val logrosRepository: LogrosRepository
) {
    private val db = FirebaseFirestore.getInstance()

    suspend fun ejecutar(habito: HabitoSuscrito): ResultadoProgreso {
        val fechasActualizadas = habitosRepository.alternarFechaCumplimiento(habito)
        val habitoActualizado = habito.copy(fechasCumplidas = fechasActualizadas)

        if (habito.plantillaId == null) {
            return ResultadoProgreso(habitoActualizado = habitoActualizado)
        }

        // 🔍 LOG 1: Entrada general
        android.util.Log.d("DEBUG_COMBO", "1. Gatillado por hábito: ${habito.nombre} (plantillaId: ${habito.plantillaId})")

        // Buscamos en el catálogo el desafío que contiene este hábito
        val desafioSnapshot = db.collection("desafios_objetivos")
            .whereArrayContains("habitosRequeridos", habito.plantillaId)
            .get()
            .await()

        if (desafioSnapshot.isEmpty) {
            android.util.Log.w("DEBUG_COMBO", "🛑 Freno: Este hábito no pertenece a ningún desafío en la DB")
            return ResultadoProgreso(habitoActualizado = habitoActualizado)
        }

        // 🛠️ RECONSTRUIMOS EL OBJETO DESAFÍO ORIGINAL
        val docDesafio = desafioSnapshot.documents.first()

        // Sacamos los datos limpios de la DB
        val desafioId = docDesafio.getString("id") ?: docDesafio.id
        val nombreDesafio = docDesafio.getString("nombreDesafio") ?: "Desafío Completado"
        val habitosRequeridos = docDesafio.get("habitosRequeridos") as? List<String> ?: emptyList()
        val metaObjetivo = docDesafio.getLong("metaObjetivo")?.toInt() ?: 1

        val totalDiasCumplidos = fechasActualizadas.size.toFloat()
        val progresoFloat = (totalDiasCumplidos / metaObjetivo.toFloat()).coerceAtMost(1.0f)
        val porcentajeEntero = (progresoFloat * 100).toInt()

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val estaTildadoHoy = fechasActualizadas.contains(hoyStr)

        // 🔍 LOG 2: Desafío encontrado
        android.util.Log.d("DEBUG_COMBO", "2. Encontró desafío: $nombreDesafio. Pide: $habitosRequeridos. ¿Está tildado hoy?: $estaTildadoHoy")

        if (habitosRequeridos.size > 1) {
            // -------------------------------------------------------------------------
            // Caso A: Es un Desafío tipo Combo (Múltiples hábitos)
            // -------------------------------------------------------------------------
            if (estaTildadoHoy) {
                android.util.Log.d("DEBUG_COMBO", "3. Buscando suscripción activa para el desafíoId: $desafioId")

                val suscripcionDesafio = db.collection("usuarios_suscripciones")
                    .whereEqualTo("userId", habito.userId)
                    .whereEqualTo("desafioId", desafioId)
                    .whereEqualTo("tipo", "DESAFIO")
                    .whereEqualTo("completado", false)
                    .get()
                    .await()

                if (!suscripcionDesafio.isEmpty) {
                    val docSuscripcion = suscripcionDesafio.documents.first()

                    // 🚀 LEEMOS LOS IDS DE LOS DOCUMENTOS ESPECÍFICOS DE ESTA SUSCRIPCIÓN
                    val hijosAsociados = docSuscripcion.get("suscripcionesHabitosHijos") as? List<String> ?: emptyList()

                    // Traemos las suscripciones de hábitos del usuario
                    val habitosDelUsuario = habitosRepository.obtenerSuscripcionesUsuario(habito.userId)

                    // 🔍 VALIDACIÓN INMUNE A DUPLICADOS:
                    // Verificamos que TODOS los documentos físicos guardados en el desafío estén tildados hoy
                    val cumplioComboHoy = hijosAsociados.all { idDocFisico ->
                        habitosDelUsuario.any { h ->
                            h.id == idDocFisico && h.fechasCumplidas.contains(hoyStr)
                        }
                    }

                    android.util.Log.d("DEBUG_COMBO", "Validando contra instancias físicas: $hijosAsociados. Resultado: $cumplioComboHoy")
                    if (cumplioComboHoy) {
                        // Marcamos el desafío como hecho
                        db.collection("usuarios_suscripciones")
                            .document(docSuscripcion.id)
                            .update("completado", true)
                            .await()

                        // Grabamos la insignia en logros_usuarios
                        val nuevoLogroRef = db.collection("logros_usuarios").document()
                        nuevoLogroRef.set(hashMapOf(
                            "id" to nuevoLogroRef.id,
                            "desafioId" to desafioId,
                            "nombreDesafio" to nombreDesafio,
                            "userId" to habito.userId,
                            "fechaObtencion" to hoyStr
                        )).await()

                        android.util.Log.d("DEBUG_COMBO", "🏆 ¡LOGRO GRABADO EN FIRESTORE!")

                        return ResultadoProgreso(
                            habitoActualizado = habitoActualizado,
                            porcentajeAvance = 100,
                            progresoBarra = 1.0f,
                            desafioDesbloqueado = true,
                            nombreDesafio = nombreDesafio
                        )
                    }
                } else {
                    android.util.Log.w("DEBUG_COMBO", "⚠️ Freno: No se encontró la suscripción testigo activa para desafioId='$desafioId'")
                }
            }
        } else {
            // -------------------------------------------------------------------------
            // Caso B: Es un desafío clásico de un solo hábito por acumulación
            // -------------------------------------------------------------------------
            val seCumplioMetaIndividual = fechasActualizadas.size >= metaObjetivo
            if (seCumplioMetaIndividual) {
                val nuevoLogroRef = db.collection("logros_usuarios").document()
                nuevoLogroRef.set(hashMapOf(
                    "id" to nuevoLogroRef.id,
                    "desafioId" to desafioId,
                    "nombreDesafio" to nombreDesafio,
                    "userId" to habito.userId,
                    "fechaObtencion" to hoyStr
                )).await()

                return ResultadoProgreso(
                    habitoActualizado = habitoActualizado,
                    porcentajeAvance = porcentajeEntero,
                    progresoBarra = progresoFloat,
                    desafioDesbloqueado = true,
                    nombreDesafio = nombreDesafio
                )
            }
        }

        return ResultadoProgreso(
            habitoActualizado = habitoActualizado,
            porcentajeAvance = porcentajeEntero,
            progresoBarra = progresoFloat,
            desafioDesbloqueado = false,
            nombreDesafio = nombreDesafio
        )
    }
}