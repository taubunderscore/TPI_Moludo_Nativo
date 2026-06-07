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

    // Plantillas globales filtradas por grupo
    suspend fun obtenerPlantillasPorGrupo(grupo: String): List<HabitoPlantilla> {
        return try {
            db.collection("habitos_plantillas")
                .whereArrayContains("grupos", grupo)
                .get()
                .await()
                .documents
                .map { doc -> doc.toHabitoPlantilla() } // ✅ extensión reutilizable
        } catch (e: Exception) {
            emptyList()
        }
    }
    suspend fun crearHabitoPersonalizado(
        userId: String,
        nombre: String,
        categoria: String,
        frecuencia: TipoFrecuencia,
        horaRecordatorio: String? = null,
        comentario: String? = null  // ✅ nuevo parámetro
    ): String {
        return try {
            val nuevaSubRef = db.collection("usuarios_suscripciones").document()
            val nuevoHabito = hashMapOf(
                "id"              to nuevaSubRef.id,
                "userId"          to userId,
                "nombre"          to nombre,
                "categoria"       to categoria,
                "frecuencia"      to frecuencia.name,
                "fechasCumplidas" to emptyList<String>(),
                "esPersonalizado" to true
            )
            if (horaRecordatorio != null) nuevoHabito["horaRecordatorio"] = horaRecordatorio
            if (comentario != null) nuevoHabito["comentario"] = comentario  // ✅

            nuevaSubRef.set(nuevoHabito).await()
            nuevaSubRef.id
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error creando hábito personal: ${e.localizedMessage}")
            ""
        }
    }


    //  Hábitos del usuario (sin registros de desafíos)
    suspend fun obtenerSuscripcionesUsuario(userId: String): List<HabitoSuscrito> {
        return try {
            db.collection("usuarios_suscripciones")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .filter { doc -> !doc.contains("tipo") }
                .mapNotNull { doc -> doc.toObject(HabitoSuscrito::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    //  Alterna la fecha de hoy en el historial
    suspend fun alternarFechaCumplimiento(habito: HabitoSuscrito): List<String> {
        val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nuevasFechas = habito.fechasCumplidas.toMutableList()

        if (nuevasFechas.contains(hoy)) nuevasFechas.remove(hoy)
        else nuevasFechas.add(hoy)

        try {
            db.collection("usuarios_suscripciones")
                .document(habito.id)
                .update("fechasCumplidas", nuevasFechas)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error alternando fecha: ${e.localizedMessage}")
        }

        return nuevasFechas
    }

    //  Plantillas del catálogo por categoría
    suspend fun obtenerPlantillasPorCategoria(categoria: String): List<HabitoPlantilla> {
        return try {
            db.collection("habitos_plantillas")
                .whereEqualTo("categoria", categoria)
                .get()
                .await()
                .documents
                .map { doc -> doc.toHabitoPlantilla() } // ✅ misma extensión
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Suscribe al usuario a un hábito y devuelve el ID físico generado
    suspend fun suscribirUsuarioAHabito(
        userId: String,
        plantilla: HabitoPlantilla,
        desafioId: String? = null,
        horaRecordatorio: String? = null
    ): String {
        return try {
            val nuevaSubRef = db.collection("usuarios_suscripciones").document()
            val nuevaSuscripcion = hashMapOf(
                "id"              to nuevaSubRef.id,
                "plantillaId"     to plantilla.id,
                "userId"          to userId,
                "nombre"          to plantilla.nombre,
                "categoria"       to plantilla.categoria,
                "frecuencia"      to plantilla.frecuencia.name,
                "fechasCumplidas" to emptyList<String>(),
                "esPersonalizado" to false  // ← siempre false, viene del catálogo
            )

            if (desafioId != null) nuevaSuscripcion["desafioId"] = desafioId
            if (horaRecordatorio != null) nuevaSuscripcion["horaRecordatorio"] = horaRecordatorio

            nuevaSubRef.set(nuevaSuscripcion).await()
            nuevaSubRef.id
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error suscribiendo: ${e.localizedMessage}")
            ""
        }
    }

    // Acceso directo por ID de documento — sin campo "id" en Firestore lo cambie porque era quilombo el doble id
    suspend fun obtenerPlantillaPorId(plantillaId: String): HabitoPlantilla? {
        return try {
            val doc = db.collection("habitos_plantillas")
                .document(plantillaId) //  acceso directo O(1) poresto lo cambie
                .get()
                .await()

            if (doc.exists()) doc.toHabitoPlantilla()
            else {
                android.util.Log.w("HabitosRepo", "No se encontró plantilla id=$plantillaId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error: ${e.localizedMessage}")
            null
        }
    }
    // Elimina una suscripción individual por ID de documento
    suspend fun eliminarSuscripcion(habitoId: String) {
        try {
            db.collection("usuarios_suscripciones")
                .document(habitoId)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error eliminando suscripción: ${e.localizedMessage}")
        }
    }

    // Trae solo los registros de tipo DESAFIO
    suspend fun obtenerSuscripcionesDesafios(userId: String): List<Map<String, Any>> {
        return try {
            db.collection("usuarios_suscripciones")
                .whereEqualTo("userId", userId)
                .whereEqualTo("tipo", "DESAFIO")
                .get()
                .await()
                .documents
                .map { doc -> doc.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Elimina el registro del desafío de usuarios_suscripciones
    suspend fun eliminarSuscripcionDesafio(userId: String, desafioId: String) {
        try {
            db.collection("usuarios_suscripciones")
                .document("${userId}_${desafioId}")
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "Error eliminando desafío: ${e.localizedMessage}")
        }
    }

}

//  Extensión privada — mapea doc → HabitoPlantilla usando doc.id
// Centraliza el mapeo en un solo lugar, sin duplicar código
private fun com.google.firebase.firestore.DocumentSnapshot.toHabitoPlantilla(): HabitoPlantilla {
    val frecuenciaTexto = getString("frecuencia") ?: "DIARIO"
    return HabitoPlantilla(
        id       = this.id, // ✅ doc.id — no necesita campo "id" en Firestore
        nombre   = getString("nombre") ?: "",
        categoria = getString("categoria") ?: "",
        grupos   = get("grupos") as? List<String> ?: emptyList(),
        frecuencia = try {
            TipoFrecuencia.valueOf(frecuenciaTexto.uppercase())
        } catch (e: Exception) { TipoFrecuencia.DIARIO },
        diasConfigurados = get("diasConfigurados") as? List<Int> ?: emptyList()
    )

}