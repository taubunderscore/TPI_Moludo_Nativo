package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.model.LogroUsuario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // manejo asincrono

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

    // 2. Graba  la medalla ganada en la base de datos (Inmutable)
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
                val logro = LogroUsuario(
                    id = nuevoDoc.id,
                    userId = userId,
                    desafioId = desafio.id,
                    nombreDesafio = desafio.nombreDesafio,
                    fechaObtencion = Timestamp.now()
                )
                nuevoDoc.set(logro).await()
            }
        } catch (e: Exception) {
            // Logear error de persistencia
        }
    }
}