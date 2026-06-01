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

    // 2. Trae la lista de hábitos del usuario (FILTRANDO para ignorar el registro de desafío)
    suspend fun obtenerSuscripcionesUsuario(userId: String): List<HabitoSuscrito> {
        return try {
            db.collection("usuarios_suscripciones")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .filter { doc -> !doc.contains("tipo") } // 🚀 FILTRO CLAVE: Si tiene el campo "tipo" es un desafío, lo dejamos afuera del Home
                .mapNotNull { doc -> doc.toObject(HabitoSuscrito::class.java) }
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

    // 5. Crea un nuevo registro en las suscripciones del usuario y devuelve su ID físico
    suspend fun suscribirUsuarioAHabito(userId: String, plantilla: HabitoPlantilla): String {
        return try {
            val nuevaSubRef = db.collection("usuarios_suscripciones").document()

            val nuevaSuscripcion = hashMapOf(
                "id" to nuevaSubRef.id,
                "plantillaId" to plantilla.id,
                "userId" to userId,
                "nombre" to plantilla.nombre,
                "categoria" to plantilla.categoria,
                "frecuencia" to plantilla.frecuencia.name, // Guardamos el String puro del Enum
                "fechasCumplidas" to emptyList<String>()
            )

            nuevaSubRef.set(nuevaSuscripcion).await()

            // 🚀 CAMINO EXITOSO: Devolvemos el ID físico recién generado
            nuevaSubRef.id

        } catch (e: Exception) {
            // 🚀 CAMINO DE FALLO: Si explota, devolvemos un string vacío para no romper la app
            android.util.Log.e("DEBUG_REPO", "Error al suscribir hábito: ${e.localizedMessage}")
            ""
        }
    }

    /**
     * Busca una plantilla específica por su ID único en la colección habitos_plantillas.
     * Sirve para clonar los datos del hábito al suscribirse desde un desafío.
     */
    suspend fun obtenerPlantillaPorId(plantillaId: String): HabitoPlantilla? {
        return try {
            // Buscamos el documento cuyo campo interno "id" sea igual al que pasamos
            val snapshot = db.collection("habitos_plantillas")
                .whereEqualTo("id", plantillaId) // 🚀 Busca por el campo interno, no por el ID del doc
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val document = snapshot.documents.first()
                HabitoPlantilla(
                    id = document.getString("id") ?: "",
                    nombre = document.getString("nombre") ?: "",
                    categoria = document.getString("categoria") ?: "",
                    grupos = document.get("grupos") as? List<String> ?: emptyList(),
                    frecuencia = try {
                        TipoFrecuencia.valueOf(document.getString("frecuencia") ?: "DIARIO")
                    } catch (e: Exception) { TipoFrecuencia.DIARIO },
                    diasConfigurados = document.get("diasConfigurados") as? List<Int> ?: emptyList()
                )
            } else {
                android.util.Log.w("DEBUG_DESAFIO", "No se encontró ningún documento con el campo id = $plantillaId")
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}