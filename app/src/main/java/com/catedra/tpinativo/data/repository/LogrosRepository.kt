package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.LogroUsuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LogrosRepository {
    private val db = FirebaseFirestore.getInstance()

    // Busca desafío por plantillaId
    suspend fun obtenerDesafioPorPlantilla(plantillaId: String): DesafioObjetivo? {
        return try {
            db.collection("desafios_objetivos")
                .whereEqualTo("plantillaId", plantillaId)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.let { doc ->
                    DesafioObjetivo(
                        id = doc.id, // ✅ doc.id
                        plantillaId = doc.getString("plantillaId") ?: "",
                        nombreDesafio = doc.getString("nombreDesafio") ?: "",
                        metaObjetivo = doc.getLong("metaObjetivo")?.toInt() ?: 1,
                        habitosRequeridos = doc.get("habitosRequeridos") as? List<String> ?: emptyList(),
                        descripcion = doc.getString("descripcion") ?: ""
                    )
                }
        } catch (e: Exception) {
            null
        }
    }

    // Graba la medalla — con chequeo de duplicado usando doc.id del desafío
    suspend fun registrarLogroGanado(userId: String, desafio: DesafioObjetivo) {
        try {
            // Chequeo de duplicado usando doc.id del desafío
            val yaExiste = db.collection("logros_usuarios")
                .whereEqualTo("userId", userId)
                .whereEqualTo("desafioId", desafio.id)
                .get()
                .await()
                .isEmpty.not()

            if (!yaExiste) {
                val nuevoDoc = db.collection("logros_usuarios").document()
                val logro = LogroUsuario(
                    id           = nuevoDoc.id, // ✅ doc.id
                    userId       = userId,
                    desafioId    = desafio.id,
                    nombreDesafio = desafio.nombreDesafio
                )
                nuevoDoc.set(logro).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("LogrosRepo", "Error registrando logro: ${e.localizedMessage}")
        }
    }

    // Trae los trofeos del usuario — usando doc.id
    suspend fun obtenerLogrosUsuario(userId: String): List<LogroUsuario> {
        return try {
            db.collection("logros_usuarios")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .map { doc ->
                    val fechaRaw = doc.get("fechaObtencion")
                    val fechaString = when (fechaRaw) {
                        is com.google.firebase.Timestamp -> {
                            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            formatter.format(fechaRaw.toDate())
                        }
                        is String -> fechaRaw
                        else -> "Reciente"
                    }
                    LogroUsuario(
                        id            = doc.id, // ✅ doc.id
                        userId        = doc.getString("userId") ?: "",
                        desafioId     = doc.getString("desafioId") ?: "",
                        nombreDesafio = doc.getString("nombreDesafio") ?: "Desafío Completado 🎉",
                        fechaObtencion = fechaString
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}