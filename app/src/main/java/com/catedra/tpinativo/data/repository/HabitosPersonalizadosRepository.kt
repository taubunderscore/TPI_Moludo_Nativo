package com.catedra.tpinativo.data.repository

import android.util.Log
import com.catedra.tpinativo.data.model.CategoriaHabito
import com.catedra.tpinativo.data.model.HabitoPersonalizado
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitosPersonalizadosRepository {

    private val db         = FirebaseFirestore.getInstance()
    private val coleccion  = db.collection("habitos_personalizados")
    private val TAG        = "HabPersonalizadosRepo"

    // ─── Lectura ─────────────────────────────────────────────────────────────

    /** Devuelve todos los hábitos personalizados activos del usuario. */
    suspend fun obtenerHabitosDeUsuario(userId: String): List<HabitoPersonalizado> {
        return try {
            coleccion
                .whereEqualTo("userId", userId)
                .whereEqualTo("activo", true)
                .get().await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(HabitoPersonalizado::class.java)?.copy(
                        id        = doc.id,
                        categoria = runCatching {
                            CategoriaHabito.valueOf(
                                doc.getString("categoria") ?: "SALUD"
                            )
                        }.getOrDefault(CategoriaHabito.SALUD)
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "obtenerHabitosDeUsuario: ${e.localizedMessage}")
            emptyList()
        }
    }

    // ─── Alta ────────────────────────────────────────────────────────────────

    /**
     * Crea un nuevo HabitoPersonalizado en Firestore.
     * @return el doc.id generado, o "" si falló.
     */
    suspend fun crear(
        userId: String,
        nombre: String,
        detalle: String,
        categoria: CategoriaHabito,
        horaRecordatorio: String
    ): String {
        return try {
            val ref = coleccion.document()
            val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val doc = hashMapOf(
                "id"               to ref.id,
                "userId"           to userId,
                "nombre"           to nombre,
                "detalle"          to detalle,
                "categoria"        to categoria.name,
                "horaRecordatorio" to horaRecordatorio,
                "activo"           to true,
                "fechaCreacion"    to hoy
            )
            ref.set(doc).await()
            Log.d(TAG, "Hábito personalizado creado: ${ref.id}")
            ref.id
        } catch (e: Exception) {
            Log.e(TAG, "crear: ${e.localizedMessage}")
            ""
        }
    }

    // ─── Baja lógica ─────────────────────────────────────────────────────────

    /** Marca el hábito como inactivo (no lo borra, preserva historial). */
    suspend fun desactivar(habitoId: String) {
        try {
            coleccion.document(habitoId).update("activo", false).await()
            Log.d(TAG, "Hábito personalizado desactivado: $habitoId")
        } catch (e: Exception) {
            Log.e(TAG, "desactivar: ${e.localizedMessage}")
        }
    }
    suspend fun editar(
        habitoId: String,
        nombre: String,
        detalle: String,
        categoria: CategoriaHabito,
        horaRecordatorio: String
    ) {
        try {
            android.util.Log.d("EDITAR_REPO", "Editando doc: $habitoId")  // ← acá

            coleccion.document(habitoId)
                .update(
                    mapOf(
                        "nombre"           to nombre,
                        "detalle"          to detalle,
                        "categoria"        to categoria.name,
                        "horaRecordatorio" to horaRecordatorio
                    )
                ).await()

            // También actualizar el cache en usuario_habitos
            val db = FirebaseFirestore.getInstance()
            val docs = db.collection("usuario_habitos")
                .whereEqualTo("habitoId", habitoId)
                .get().await()

            docs.documents.forEach { doc ->
                doc.reference.update(
                    mapOf(
                        "nombreCache"    to nombre,
                        "categoriaCache" to categoria.display,
                        "horaRecordatorio" to horaRecordatorio
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "editar: ${e.localizedMessage}")
        }
    }
}
