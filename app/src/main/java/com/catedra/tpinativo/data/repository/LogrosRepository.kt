package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.LogroUsuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LogrosRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Busca si la plantilla de este hábito tiene un desafío/meta asociado
    suspend fun obtenerDesafioPorPlantilla(plantillaId: String): DesafioObjetivo? {
        return try {
            db.collection("desafios_objetivos")
                .whereEqualTo("plantillaId", plantillaId)
                .get()
                .await()
                .toObjects(DesafioObjetivo::class.java)
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    // 2. Graba la medalla ganada en la base de datos (Inmutable)
    suspend fun registrarLogroGanado(userId: String, desafio: DesafioObjetivo) {
        try {
            // Primero checkeamos que no exista ya para evitar duplicar medallas si vuelve a tildar
            val yaExiste = db.collection("logros_usuarios")
                .whereEqualTo("userId", userId)
                .whereEqualTo("desafioId", desafio.id)
                .get()
                .await()
                .isEmpty.not()

            if (!yaExiste) {
                val nuevoDoc = db.collection("logros_usuarios").document()

                // 🚀 CORREGIDO: No le pasamos fechaObtencion acá, deja que el modelo
                // use su valor por defecto que genera el String limpio "AAAA-MM-DD"
                val logro = LogroUsuario(
                    id = nuevoDoc.id,
                    userId = userId,
                    desafioId = desafio.id,
                    nombreDesafio = desafio.nombreDesafio
                )
                nuevoDoc.set(logro).await()
            }
        } catch (e: Exception) {
            // Logear error de persistencia
        }
    }

    // 3. Trae de Firebase todos los trofeos ganados por el usuario de Varela
    suspend fun obtenerLogrosUsuario(userId: String): List<LogroUsuario> {
        return try {
            val snapshot = db.collection("logros_usuarios")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snapshot.documents.map { doc ->
                // 🚀 Solución inteligente para la fecha:
                val fechaRaw = doc.get("fechaObtencion")
                val fechaString = when (fechaRaw) {
                    is com.google.firebase.Timestamp -> {
                        // Si es un Timestamp de Firebase (como tu registro viejo), lo pasamos a texto limpio
                        val date = fechaRaw.toDate()
                        val formatter =
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        formatter.format(date)
                    }

                    is String -> fechaRaw // Si ya es un String, pasa directo
                    else -> "Reciente"
                }

                LogroUsuario(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    desafioId = doc.getString("desafioId") ?: "",
                    nombreDesafio = doc.getString("nombreDesafio") ?: "Desafío Completado 🎉",
                    fechaObtencion = fechaString
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}