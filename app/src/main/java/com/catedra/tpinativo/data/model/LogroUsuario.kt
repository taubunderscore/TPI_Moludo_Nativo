package com.catedra.tpinativo.data.model

import com.google.firebase.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// persisto los logros / hitos del usuario en firestore.
data class LogroUsuario(
    val id: String = "",
    val userId: String = "",
    val desafioId: String = "",
    val nombreDesafio: String = "",
    val fechaObtencion: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
)