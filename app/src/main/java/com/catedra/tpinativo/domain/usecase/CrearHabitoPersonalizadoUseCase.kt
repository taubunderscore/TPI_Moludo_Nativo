package com.catedra.tpinativo.domain.usecase

import android.content.Context
import com.catedra.tpinativo.data.model.CategoriaHabito
import com.catedra.tpinativo.data.repository.HabitosPersonalizadosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.catedra.tpinativo.domain.service.NotificacionesService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CrearHabitoPersonalizadoUseCase(
    private val repository: HabitosPersonalizadosRepository,
    private val context: Context
) {
    private val db = FirebaseFirestore.getInstance()

    suspend operator fun invoke(
        userId: String,
        nombre: String,
        detalle: String,
        categoria: CategoriaHabito,
        horaRecordatorio: String
    ): Result<String> {

        if (nombre.isBlank())
            return Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
        if (!horaRecordatorio.matches(Regex("^\\d{2}:\\d{2}$")))
            return Result.failure(IllegalArgumentException("Hora inválida: usá formato HH:mm"))

        val idPersonalizado = repository.crear(
            userId = userId,
            nombre = nombre.trim(),
            detalle = detalle.trim(),
            categoria = categoria,
            horaRecordatorio = horaRecordatorio
        )
        if (idPersonalizado.isEmpty())
            return Result.failure(Exception("Error al guardar en Firestore"))

        crearUsuarioHabito(
            userId = userId,
            idPersonalizado = idPersonalizado,
            nombre = nombre.trim(),
            categoria = categoria,
            horaRecordatorio = horaRecordatorio
        )

        NotificacionesService.programar(
            context = context,
            habitoId = idPersonalizado,
            nombre = nombre.trim(),
            detalle = detalle.trim().ifBlank { "Es hora de cumplir tu hábito 💪" },
            horaRecordatorio = horaRecordatorio
        )

        return Result.success(idPersonalizado)
    }

    private suspend fun crearUsuarioHabito(
        userId: String,
        idPersonalizado: String,
        nombre: String,
        categoria: CategoriaHabito,
        horaRecordatorio: String
    ) {
        try {
            val ref = db.collection("usuario_habitos").document()
            val hoy = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val doc = hashMapOf(
                "id" to ref.id,
                "userId" to userId,
                "habitoId" to idPersonalizado,
                "nombreCache" to nombre,
                "categoriaCache" to categoria.display,
                "frecuenciaCache" to "DIARIO",
                "diasConfiguradosCache" to emptyList<Int>(),
                "horaRecordatorio" to horaRecordatorio,
                "fechaInicio" to hoy,
                "activo" to true,
                "desafioId" to null,
                "esPersonalizado" to true
            )
            ref.set(doc).await()
        } catch (e: Exception) {
            android.util.Log.e(
                "CrearHabitoUseCase",
                "Error creando UsuarioHabito: ${e.localizedMessage}"
            )
        }
    }
}