package com.catedra.tpinativo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.catedra.tpinativo.data.model.DesafioObjetivo
import kotlinx.coroutines.tasks.await


class DesafiosRepository {

    private val db = FirebaseFirestore.getInstance()

    // Trae los desafíos para la solapa "🏆 Desafíos"
    suspend fun obtenerTodosLosDesafios(): List<DesafioObjetivo> {
        return try {
            db.collection("desafios_objetivos")
                .get()
                .await()
                .toObjects(DesafioObjetivo::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }


}