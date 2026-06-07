package com.catedra.tpinativo.data.repository

import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.TipoDesafio
import com.catedra.tpinativo.data.model.UsuarioDesafio
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DesafiosRepository {
    private val db = FirebaseFirestore.getInstance()

    // ─── Catálogo global ─────────────────────────────────────────────────────

    suspend fun obtenerTodosLosDesafios(): List<Desafio> {
        return try {
            db.collection("desafios")
                .get().await()
                .documents
                .map { doc ->
                    val tipoStr = doc.getString("tipo") ?: "ACUMULACION"
                    Desafio(
                        id          = doc.id,
                        nombre      = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        tipo        = runCatching { TipoDesafio.valueOf(tipoStr.uppercase()) }
                            .getOrDefault(TipoDesafio.ACUMULACION),
                        habitosIds  = (doc.get("habitosIds") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        meta        = doc.getLong("meta")?.toInt() ?: 1
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "obtenerTodosLosDesafios: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun obtenerDesafioPorId(desafioId: String): Desafio? {
        return try {
            val doc = db.collection("desafios").document(desafioId).get().await()
            if (!doc.exists()) return null
            val tipoStr = doc.getString("tipo") ?: "ACUMULACION"
            Desafio(
                id          = doc.id,
                nombre      = doc.getString("nombre") ?: "",
                descripcion = doc.getString("descripcion") ?: "",
                tipo        = runCatching { TipoDesafio.valueOf(tipoStr.uppercase()) }
                    .getOrDefault(TipoDesafio.ACUMULACION),
                habitosIds  = (doc.get("habitosIds") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                meta        = doc.getLong("meta")?.toInt() ?: 1
            )
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "obtenerDesafioPorId: ${e.localizedMessage}")
            null
        }
    }

    // ─── Suscripciones usuario ────────────────────────────────────────────────

    suspend fun obtenerDesafiosUsuario(userId: String): List<UsuarioDesafio> {
        return try {
            db.collection("usuario_desafios")
                .whereEqualTo("userId", userId)
                .get().await()
                .documents
                .mapNotNull { doc ->
                    // Mapeamos manual para incluir los nuevos campos de geo
                    // (toObject() ignora campos no declarados con @get:Exclude, pero
                    //  los campos nullable con default null funcionan bien)
                    doc.toObject(UsuarioDesafio::class.java)?.copy(
                        id            = doc.id,
                        logroLatitud  = (doc.get("logroLatitud") as? Number)?.toDouble(),
                        logroLongitud = (doc.get("logroLongitud") as? Number)?.toDouble()
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "obtenerDesafiosUsuario: ${e.localizedMessage}")
            emptyList()
        }
    }

    /**
     * Crea el registro de suscripción del usuario al desafío.
     * Usa ID determinístico userId_desafioId para garantizar idempotencia.
     */
    suspend fun suscribirUsuarioADesafio(
        userId: String,
        desafio: Desafio,
        habitosHijosIds: List<String>
    ) {
        val docId = "${userId}_${desafio.id}"
        try {
            val yaExiste = db.collection("usuario_desafios")
                .document(docId).get().await().exists()
            if (yaExiste) return

            val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val doc = hashMapOf(
                "id"               to docId,
                "userId"           to userId,
                "desafioId"        to desafio.id,
                "nombreCache"      to desafio.nombre,
                "fechaSuscripcion" to hoy,
                "completado"       to false,
                "fechaLogro"       to null,
                "habitosHijosIds"  to habitosHijosIds,
                "logroLatitud"     to null,
                "logroLongitud"    to null
            )
            db.collection("usuario_desafios").document(docId).set(doc).await()
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "suscribirUsuarioADesafio: ${e.localizedMessage}")
        }
    }

    /**
     * Marca el desafío como completado, guarda la fecha del logro
     * y opcionalmente la geolocalización donde fue conseguido.
     */
    suspend fun marcarDesafioCompletado(
        userId: String,
        desafio: Desafio,
        latitud: Double? = null,
        longitud: Double? = null
    ) {
        val docId = "${userId}_${desafio.id}"
        try {
            val ref  = db.collection("usuario_desafios").document(docId)
            val snap = ref.get().await()
            if (!snap.exists() || snap.getBoolean("completado") == true) return

            val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val actualizaciones = mutableMapOf<String, Any?>(
                "completado"  to true,
                "fechaLogro"  to hoy,
                "logroLatitud"  to latitud,
                "logroLongitud" to longitud
            )
            ref.update(actualizaciones).await()
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "marcarDesafioCompletado: ${e.localizedMessage}")
        }
    }

    suspend fun eliminarSuscripcionDesafio(userId: String, desafioId: String) {
        try {
            db.collection("usuario_desafios")
                .document("${userId}_${desafioId}")
                .delete().await()
        } catch (e: Exception) {
            android.util.Log.e("DesafiosRepo", "eliminarSuscripcionDesafio: ${e.localizedMessage}")
        }
    }
}