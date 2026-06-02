package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DesafiosRepository {
    private val db = FirebaseFirestore.getInstance()

    // ✅ Usa doc.id en vez del campo "id" interno — no necesitás campo id en Firestore
    suspend fun obtenerTodosLosDesafios(): List<DesafioObjetivo> {
        return try {
            db.collection("desafios_objetivos")
                .get()
                .await()
                .documents
                .map { doc ->
                    DesafioObjetivo(
                        id = doc.id, // ✅ ID del documento directo
                        plantillaId = doc.getString("plantillaId") ?: "",
                        nombreDesafio = doc.getString("nombreDesafio") ?: "",
                        metaObjetivo = doc.getLong("metaObjetivo")?.toInt() ?: 1,
                        habitosRequeridos = doc.get("habitosRequeridos") as? List<String> ?: emptyList(),
                        descripcion = doc.getString("descripcion") ?: ""
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "Error: ${e.localizedMessage}")
            emptyList()
        }
    }
}