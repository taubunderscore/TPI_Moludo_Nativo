package com.catedra.tpinativo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Crea el documento del usuario en la colección "usuarios".
     * El doc ID es el UID de Firebase Auth → siempre sabés quién es sin query adicional.
     */
    suspend fun crearUsuario(
        userId: String,
        nombre: String,
        email: String,
        edad: Int,
        intereses: List<String> = emptyList(),
        fotoUrl: String? = null        // URL de Cloudinary, puede ser null si no subió foto
    ) {
        val datos = hashMapOf(
            "userId"    to userId,
            "nombre"    to nombre,
            "email"     to email,
            "edad"      to edad,
            "intereses" to intereses,
            "foto"      to (fotoUrl ?: "")   // string vacío si no hay foto
        )
        db.collection("usuarios")
            .document(userId)
            .set(datos)
            .await()
    }

    /**
     * Actualiza solo el campo foto de un usuario ya existente.
     * Útil si querés permitir cambiar la foto desde el perfil después.
     */
    suspend fun actualizarFoto(userId: String, fotoUrl: String) {
        db.collection("usuarios")
            .document(userId)
            .update("foto", fotoUrl)
            .await()
    }

    /**
     * Obtiene los datos del usuario. Devuelve null si no existe.
     */
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
