package com.catedra.tpinativo.data.model

import com.google.firebase.Timestamp
// persisto los logros / hitos del usuario en firestore.
data class LogroUsuario(
    val id: String = "",
    val userId: String = "",
    val desafioId: String = "",
    val nombreDesafio: String = "",
    val fechaObtencion: Timestamp = Timestamp.now()
)