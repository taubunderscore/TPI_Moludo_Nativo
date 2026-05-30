package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.HabitoPlantilla
import com.catedra.tpinativo.data.model.HabitoSuscrito
import com.catedra.tpinativo.data.model.TipoFrecuencia
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

    // 4. Trae las plantillas del catálogo global según la categoría (CONVERSIÓN SEGURA)
    suspend fun obtenerPlantillasPorCategoria(categoria: String): List<HabitoPlantilla> {
        return try {
            val snapshot = db.collection("habitos_plantillas")
                .whereEqualTo("categoria", categoria)
                .get()
                .await()

            snapshot.documents.map { doc ->
                val frecuenciaTexto = doc.getString("frecuencia") ?: "DIARIO"

                HabitoPlantilla(
                    id = doc.id,
                    nombre = doc.getString("nombre") ?: "",
                    categoria = doc.getString("categoria") ?: "",
                    frecuencia = try {
                        TipoFrecuencia.valueOf(frecuenciaTexto.uppercase())
                    } catch (e: Exception) {
                        TipoFrecuencia.DIARIO
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 5. Crea un nuevo registro en las suscripciones del usuario en caliente
    suspend fun suscribirUsuarioAHabito(userId: String, plantilla: HabitoPlantilla) {
        try {
            val nuevaSubRef = db.collection("usuarios_suscripciones").document()

            val nuevaSuscripcion = hashMapOf(
                "id" to nuevaSubRef.id,
                "plantillaId" to plantilla.id,
                "userId" to userId,
                "nombre" to plantilla.nombre,
                "categoria" to plantilla.categoria,
                "frecuencia" to plantilla.frecuencia.name, // 🚀 Guardamos el String puro del Enum
                "fechasCumplidas" to emptyList<String>()
            )

            nuevaSubRef.set(nuevaSuscripcion).await()
        } catch (e: Exception) {
            // Manejo de error por si falla la inserción
        }
    }
}