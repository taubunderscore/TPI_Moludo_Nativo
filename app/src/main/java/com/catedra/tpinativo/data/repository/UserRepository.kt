package com.catedra.tpinativo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun crearUsuario(
        userId: String,
        nombre: String,
        email: String,
        edad: Int,
        intereses: List<String> = emptyList(),
        fotoUrl: String? = null
    ) {
        val datos = hashMapOf(
            "userId" to userId,
            "nombre" to nombre,
            "email" to email,
            "edad" to edad,
            "intereses" to intereses,
            "foto" to (fotoUrl ?: "")
        )
        db.collection("usuarios")
            .document(userId)
            .set(datos)
            .await()
    }

    suspend fun actualizarFoto(userId: String, fotoUrl: String) {
        db.collection("usuarios")
            .document(userId)
            .update("foto", fotoUrl)
            .await()
    }

    suspend fun obtenerUsuario(userId: String): Map<String, Any>? {
        return try {
            val doc = db.collection("usuarios").document(userId).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "obtenerUsuario: ${e.localizedMessage}")
            null
        }
    }
}
