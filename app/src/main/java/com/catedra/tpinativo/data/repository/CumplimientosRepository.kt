package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.Cumplimiento
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CumplimientosRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun alternarCumplimientoHoy(
        userId: String,
        usuarioHabitoId: String,
        habitoCatalogoId: String
    ): Boolean {
        val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        android.util.Log.d("CUMPLIMIENTO", "userId=$userId | habitoId=$usuarioHabitoId | hoy=$hoy")

        return try {
            val existing = db.collection("historial_cumplimientos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("habitoId", usuarioHabitoId)
                .whereEqualTo("fecha", hoy)
                .get().await()
            android.util.Log.d("CUMPLIMIENTO", "Docs existentes: ${existing.size()}")


            if (!existing.isEmpty) {
                // Ya estaba tildado → destildar
                existing.documents.forEach { it.reference.delete().await() }
                android.util.Log.d("CUMPLIMIENTO", "Destildado OK")

                false
            } else {
                // No estaba → tildar
                val ref = db.collection("historial_cumplimientos").document()
                val doc = hashMapOf(
                    "id" to ref.id,
                    "userId" to userId,
                    "habitoId" to usuarioHabitoId,
                    "habitoCatalogoId" to habitoCatalogoId,
                    "fecha" to hoy,
                    "nota" to null
                )
                ref.set(doc).await()
                android.util.Log.d("CUMPLIMIENTO", "Tildado OK — docId=${ref.id}")

                true
            }
        } catch (e: Exception) {
            android.util.Log.e("CUMPLIMIENTO", "Error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun obtenerFechasCumplidas(
        userId: String,
        usuarioHabitoId: String
    ): List<String> {
        return try {
            db.collection("historial_cumplimientos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("habitoId", usuarioHabitoId)
                .get().await()
                .documents
                .mapNotNull { it.getString("fecha") }
                .sorted()
        } catch (e: Exception) {
            android.util.Log.e("CumplimientosRepo", "obtenerFechasCumplidas: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun estaCumplidoHoy(
        userId: String,
        usuarioHabitoId: String
    ): Boolean {
        val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return try {
            !db.collection("historial_cumplimientos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("habitoId", usuarioHabitoId)
                .whereEqualTo("fecha", hoy)
                .get().await().isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun obtenerCumplimientosEnRango(
        userId: String,
        desde: String,
        hasta: String
    ): List<Cumplimiento> {
        return try {
            db.collection("historial_cumplimientos")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("fecha", desde)
                .whereLessThanOrEqualTo("fecha", hasta)
                .get().await()
                .documents
                .mapNotNull { it.toObject(Cumplimiento::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            android.util.Log.e(
                "CumplimientosRepo",
                "obtenerCumplimientosEnRango: ${e.localizedMessage}"
            )
            emptyList()
        }
    }

    suspend fun eliminarCumplimientosDeHabito(
        userId: String,
        usuarioHabitoId: String
    ) {
        try {
            val docs = db.collection("historial_cumplimientos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("habitoId", usuarioHabitoId)
                .get().await().documents

            val batch = db.batch()
            docs.forEach { batch.delete(it.reference) }
            if (docs.isNotEmpty()) batch.commit().await()
        } catch (e: Exception) {
            android.util.Log.e(
                "CumplimientosRepo",
                "eliminarCumplimientosDeHabito: ${e.localizedMessage}"
            )
        }
    }
}