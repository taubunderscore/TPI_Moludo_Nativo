package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitosRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. Trae las plantillas globales filtradas por grupo (desafios, moda, etc)
    suspend fun obtenerPlantillasPorGrupo(grupo: String): List<HabitoPlantilla> {
        return try {
            db.collection("habitos_plantillas")
                .whereArrayContains("grupos", grupo)
                .get()
                .await()
                .toObjects(HabitoPlantilla::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 2. Trae la lista de hábitos a los que está suscrito el usuario de Varela
    suspend fun obtenerSuscripcionesUsuario(userId: String): List<HabitoSuscrito> {
        return try {
            db.collection("usuarios_suscripciones")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(HabitoSuscrito::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 3. LA CLAVE: Agrega o quita la fecha de hoy en el historial de Firestore
    suspend fun alternarFechaCumplimiento(habito: HabitoSuscrito): List<String> {
        val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nuevasFechas = habito.fechasCumplidas.toMutableList()

        if (nuevasFechas.contains(hoy)) {
            nuevasFechas.remove(hoy) // Destilda si ya existía
        } else {
            nuevasFechas.add(hoy) // Tilda si es la primera vez en el día
        }

        try {
            db.collection("usuarios_suscripciones")
                .document(habito.id)
                .update("fechasCumplidas", nuevasFechas)
                .await()
        } catch (e: Exception) {
            // Manejo de error si se cae la red
        }

        return nuevasFechas // Devolvemos la lista mutada para los cálculos del Caso de Uso
    }
}