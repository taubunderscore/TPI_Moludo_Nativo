package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.Cumplimiento
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Maneja la colección historial_cumplimientos.
 *
 * Estructura de cada documento:
 *   id, userId, habitoId (UsuarioHabito), habitoCatalogoId, fecha (yyyy-MM-dd), nota?
 *
 * Índice compuesto requerido en Firestore Console:
 *   Colección: historial_cumplimientos
 *   Campos: userId ASC, habitoId ASC, fecha ASC
 */
class CumplimientosRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Alterna el cumplimiento de hoy para un UsuarioHabito:
     * - Si ya existe el doc de hoy → lo borra (destilda)
     * - Si no existe → lo crea (tilda)
     * Devuelve true si quedó marcado, false si se desmarcó.
     */
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
                    "id"               to ref.id,
                    "userId"           to userId,
                    "habitoId"         to usuarioHabitoId,
                    "habitoCatalogoId" to habitoCatalogoId,
                    "fecha"            to hoy,
                    "nota"             to null
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

    /**
     * Devuelve todas las fechas cumplidas de un UsuarioHabito, ordenadas.
     * Útil para calcular rachas y progreso de desafíos.
     */
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

    /**
     * Verifica si un hábito ya fue cumplido hoy.
     */
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

    /**
     * Devuelve todos los cumplimientos de un usuario en un rango de fechas.
     * Útil para la pantalla de logros / estadísticas.
     */
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
            android.util.Log.e("CumplimientosRepo", "obtenerCumplimientosEnRango: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Elimina todos los cumplimientos de un UsuarioHabito (al darlo de baja).
     * Firestore no permite borrar colecciones enteras desde el cliente,
     * por eso hacemos batch manual.
     */
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
            android.util.Log.e("CumplimientosRepo", "eliminarCumplimientosDeHabito: ${e.localizedMessage}")
        }
    }
}