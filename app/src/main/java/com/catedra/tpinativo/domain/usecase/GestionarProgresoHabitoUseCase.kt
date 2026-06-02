package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.data.repository.LogrosRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        android.util.Log.d("DEBUG_COMBO", "1. Gatillado por hábito: ${habito.nombre} (plantillaId: ${habito.plantillaId})")

        val desafioSnapshot = db.collection("desafios_objetivos")
            .whereArrayContains("habitosRequeridos", habito.plantillaId)
            .get()
            .await()

        if (desafioSnapshot.isEmpty) {
            android.util.Log.w("DEBUG_COMBO", "Este hábito no pertenece a ningún desafío en la DB")
            return ResultadoProgreso(habitoActualizado = habitoActualizado)
        }

        val docDesafio = desafioSnapshot.documents.first()
        val desafioId = docDesafio.id
        val nombreDesafio = docDesafio.getString("nombreDesafio") ?: "Desafío Completado"
        val habitosRequeridos = docDesafio.get("habitosRequeridos") as? List<String> ?: emptyList()
        val metaObjetivo = docDesafio.getLong("metaObjetivo")?.toInt() ?: 1

        val totalDiasCumplidos = fechasActualizadas.size.toFloat()
        val progresoFloat = (totalDiasCumplidos / metaObjetivo.toFloat()).coerceAtMost(1.0f)
        val porcentajeEntero = (progresoFloat * 100).toInt()

        val hoyStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val estaTildadoHoy = fechasActualizadas.contains(hoyStr)

        // Reconstruimos el objeto desafío para pasarlo al repository
        val desafioObj = DesafioObjetivo(
            id = desafioId,
            nombreDesafio = nombreDesafio,
            habitosRequeridos = habitosRequeridos,
            metaObjetivo = metaObjetivo
        )

        android.util.Log.d("DEBUG_COMBO", "2. Desafío: $nombreDesafio. Pide: $habitosRequeridos. ¿Tildado hoy?: $estaTildadoHoy")

        if (habitosRequeridos.size > 1) {
            // ---------------------------------------------------------------
            // Caso A: Desafío tipo Combo (múltiples hábitos)
            // ---------------------------------------------------------------
            if (estaTildadoHoy) {
                val suscripcionDesafio = db.collection("usuarios_suscripciones")
                    .whereEqualTo("userId", habito.userId)
                    .whereEqualTo("desafioId", desafioId)
                    .whereEqualTo("tipo", "DESAFIO")
                    .whereEqualTo("completado", false)
                    .get()
                    .await()

                if (!suscripcionDesafio.isEmpty) {
                    val docSuscripcion = suscripcionDesafio.documents.first()
                    val hijosAsociados = docSuscripcion.get("suscripcionesHabitosHijos") as? List<String> ?: emptyList()
                    val habitosDelUsuario = habitosRepository.obtenerSuscripcionesUsuario(habito.userId)

                    val cumplioComboHoy = hijosAsociados.all { idDocFisico ->
                        habitosDelUsuario.any { h ->
                            h.id == idDocFisico && h.fechasCumplidas.contains(hoyStr)
                        }
                    }

                    if (cumplioComboHoy) {
                        db.collection("usuarios_suscripciones")
                            .document(docSuscripcion.id)
                            .update("completado", true)
                            .await()

                        // ✅ FIX 2: Usamos logrosRepository que ya tiene el chequeo de duplicados
                        logrosRepository.registrarLogroGanado(habito.userId, desafioObj)

                        android.util.Log.d("DEBUG_COMBO", "🏆 ¡LOGRO GRABADO!")

                        return ResultadoProgreso(
                            habitoActualizado = habitoActualizado,
                            porcentajeAvance = 100,
                            progresoBarra = 1.0f,
                            desafioDesbloqueado = true,
                            nombreDesafio = nombreDesafio
                        )
                    }
                } else {
                    android.util.Log.w("DEBUG_COMBO", "No se encontró la suscripción activa para desafioId='$desafioId'")
                }
            }
        } else {
            // ---------------------------------------------------------------
            // Caso B: Desafío clásico de un solo hábito por acumulación
            // ---------------------------------------------------------------
            val seCumplioMeta = fechasActualizadas.size >= metaObjetivo
            if (seCumplioMeta) {
                // ✅ FIX 3: Usamos logrosRepository que ya tiene el chequeo de duplicados
                // Antes se escribía directo a Firestore sin verificar si ya existía el logro
                logrosRepository.registrarLogroGanado(habito.userId, desafioObj)

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