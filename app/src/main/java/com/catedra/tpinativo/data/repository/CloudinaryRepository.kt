package com.catedra.tpinativo.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream

class CloudinaryRepository(private val context: Context) {

    private val CLOUD_NAME  = "dyylor99b"
    private val UPLOAD_PRESET = "Habit_Flow"   // preset sin firma (unsigned)

    private val client = OkHttpClient()

    /**
     * Sube la imagen al URI indicado y devuelve la URL segura de Cloudinary.
     * Retorna null si falla.
     */
    suspend fun subirFotoPerfil(imageUri: Uri, userId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream =
                    context.contentResolver.openInputStream(imageUri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()

                val mediaType = "image/jpeg".toMediaType()
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name     = "file",
                        filename = "perfil_$userId.jpg",
                        body     = bytes.toRequestBody(mediaType)
                    )
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .addFormDataPart("public_id", "usuarios/$userId/perfil")
                    // public_id fijo → si el usuario actualiza la foto, sobreescribe la anterior
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    android.util.Log.e("CloudinaryRepo", "Error HTTP ${response.code}: ${response.body?.string()}")
                    return@withContext null
                }

                val json = JSONObject(response.body?.string() ?: "")
                // secure_url es la URL HTTPS permanente de la imagen
                json.getString("secure_url")

            } catch (e: Exception) {
                android.util.Log.e("CloudinaryRepo", "subirFotoPerfil: ${e.localizedMessage}")
                null
            }
        }
}
