package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.Habito
import com.catedra.tpinativo.data.model.TipoFrecuencia
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HabitosRepository {
    private val db = FirebaseFirestore.getInstance()
    suspend fun obtenerHabitosPorCategoria(categoria: String): List<Habito> {
        return try {
            db.collection("habitos")
                .whereEqualTo("categoria", categoria)
                .get().await()
                .documents
                .map { it.toHabito() }
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "obtenerHabitosPorCategoria: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun obtenerTodosLosHabitos(): List<Habito> {
        return try {
            db.collection("habitos")
                .get().await()
                .documents
                .map { it.toHabito() }
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "obtenerTodosLosHabitos: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun obtenerTodosLosHabitosUsuario(userId: String): List<UsuarioHabito> {
        return try {
            db.collection("usuario_habitos")
                .whereEqualTo("userId", userId)
                .get().await().documents
                .mapNotNull { it.toObject(UsuarioHabito::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            android.util.Log.e(
                "HabitosRepo",
                "obtenerTodosLosHabitosUsuario: ${e.localizedMessage}"
            )
            emptyList()
        }
    }

    suspend fun obtenerHabitoPorId(habitoId: String): Habito? {
        return try {
            val doc = db.collection("habitos").document(habitoId).get().await()
            if (doc.exists()) doc.toHabito() else null
        } catch (e: Exception) {
            android.util.Log.e(
                "HabitosRepo",
                "obtenerHabitoPorId($habitoId): ${e.localizedMessage}"
            )
            null
        }
    }

    suspend fun obtenerHabitosUsuario(
        userId: String,
        incluirDesafios: Boolean = true
    ): List<UsuarioHabito> {
        return try {
            val query = db.collection("usuario_habitos")
                .whereEqualTo("userId", userId)
                .whereEqualTo("activo", true)
            query.get().await().documents
                .mapNotNull { it.toObject(UsuarioHabito::class.java)?.copy(id = it.id) }
                .filter { if (incluirDesafios) true else it.desafioId == null }
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "obtenerHabitosUsuario: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun suscribirUsuarioAHabito(
        userId: String,
        habito: Habito,
        desafioId: String? = null,
        horaRecordatorio: String? = null
    ): String {
        return try {
            val ref = db.collection("usuario_habitos").document()
            val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val doc = hashMapOf(
                "id" to ref.id,
                "userId" to userId,
                "habitoId" to habito.id,
                "nombreCache" to habito.nombre,
                "categoriaCache" to habito.categoria,
                "frecuenciaCache" to habito.frecuencia.name,
                "diasConfiguradosCache" to habito.diasConfigurados,
                "horaRecordatorio" to horaRecordatorio,
                "fechaInicio" to hoy,
                "activo" to true,
                "desafioId" to desafioId
            )
            ref.set(doc).await()
            ref.id
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "suscribirUsuarioAHabito: ${e.localizedMessage}")
            ""
        }
    }

    suspend fun desactivarUsuarioHabito(usuarioHabitoId: String) {
        try {
            db.collection("usuario_habitos")
                .document(usuarioHabitoId)
                .update("activo", false)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("HabitosRepo", "desactivarUsuarioHabito: ${e.localizedMessage}")
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toHabito(): Habito {
        val frecTexto = getString("frecuencia") ?: "DIARIO"
        return Habito(
            id = this.id,
            nombre = getString("nombre") ?: "",
            categoria = getString("categoria") ?: "",
            frecuencia = runCatching { TipoFrecuencia.valueOf(frecTexto.uppercase()) }
                .getOrDefault(TipoFrecuencia.DIARIO),
            diasConfigurados = (get("diasConfigurados") as? List<*>)
                ?.filterIsInstance<Long>()
                ?.map { it.toInt() } ?: emptyList()
        )
    }
}