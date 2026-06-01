package com.catedra.tpinativo.domain.usecase
import com.catedra.tpinativo.data.repository.DesafiosRepository
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
            val listaIdsHijos = mutableListOf<String>()

            // 1. Insertamos primero los hábitos hijos y recolectamos sus IDs de documentos reales
            desafio.habitosRequeridos.forEach { plantillaId ->
                val plantilla = habitosRepository.obtenerPlantillaPorId(plantillaId)
                if (plantilla != null) {
                    val docIdGenerado = habitosRepository.suscribirUsuarioAHabito(userId, plantilla)
                    listaIdsHijos.add(docIdGenerado) // 👈 Guardamos el ID del documento físico
                }
            }

            // 2. Ahora creamos el registro del desafío con los punteros exactos a sus hijos
            val suscripcionDesafio = hashMapOf(
                "id" to "${userId}_${desafio.id}",
                "userId" to userId,
                "desafioId" to desafio.id,
                "tipo" to "DESAFIO",
                "completado" to false,
                "suscripcionesHabitosHijos" to listaIdsHijos // 🔗 Asociación cerrada
            )

            db.collection("usuarios_suscripciones")
                .document("${userId}_${desafio.id}")
                .set(suscripcionDesafio)
                .await()

        } catch (e: Exception) {
            android.util.Log.e("DEBUG_SUSCRIPCION", "Error: ${e.localizedMessage}")
        }
    }}