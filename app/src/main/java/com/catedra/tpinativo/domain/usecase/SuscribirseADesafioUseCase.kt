package com.catedra.tpinativo.domain.usecase

import com.catedra.tpinativo.data.model.DesafioObjetivo
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SuscribirseADesafioUseCase(
    private val habitosRepository: HabitosRepository,
) {
    private val db = FirebaseFirestore.getInstance()

    suspend operator fun invoke(userId: String, desafio: DesafioObjetivo) {
        try {
            // ✅ Verificar si ya existe la suscripción
            val docId = "${userId}_${desafio.id}"
            val yaExiste = db.collection("usuarios_suscripciones")
                .document(docId)
                .get()
                .await()
                .exists()

            if (yaExiste) {
                android.util.Log.w("DEBUG_SUSCRIPCION", "Ya suscripto al desafío ${desafio.id}")
                return
            }

            val listaIdsHijos = mutableListOf<String>()

            // 1. Insertamos los hábitos hijos marcados con el desafioId
            desafio.habitosRequeridos.forEach { plantillaId ->
                val plantilla = habitosRepository.obtenerPlantillaPorId(plantillaId)
                if (plantilla != null) {
                    // ✅ Pasamos el desafioId para que quede marcado en Firestore
                    val docIdGenerado = habitosRepository.suscribirUsuarioAHabito(
                        userId = userId,
                        plantilla = plantilla,
                        desafioId = desafio.id  // ← marca de origen
                    )
                    listaIdsHijos.add(docIdGenerado)
                }
            }

            // 2. Creamos el registro del desafío con los punteros a sus hijos
            val suscripcionDesafio = hashMapOf(
                "id"                        to docId,
                "userId"                    to userId,
                "desafioId"                 to desafio.id,
                "tipo"                      to "DESAFIO",
                "completado"                to false,
                "suscripcionesHabitosHijos" to listaIdsHijos
            )

            db.collection("usuarios_suscripciones")
                .document(docId)
                .set(suscripcionDesafio)
                .await()

        } catch (e: Exception) {
            android.util.Log.e("DEBUG_SUSCRIPCION", "Error: ${e.localizedMessage}")
        }
    }
}