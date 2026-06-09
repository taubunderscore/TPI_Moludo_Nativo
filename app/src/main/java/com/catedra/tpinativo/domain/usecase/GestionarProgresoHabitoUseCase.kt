package com.catedra.tpinativo.domain.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import com.catedra.tpinativo.data.model.Desafio
import com.catedra.tpinativo.data.model.TipoDesafio
import com.catedra.tpinativo.data.model.UsuarioHabito
import com.catedra.tpinativo.data.repository.CumplimientosRepository
import com.catedra.tpinativo.data.repository.DesafiosRepository
import com.catedra.tpinativo.data.repository.HabitosRepository
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class ResultadoProgreso(
    val marcadoComoHecho: Boolean,
    val fechasCumplidas: List<String>,
    val porcentajeAvance: Int = 0,
    val progresoBarra: Float = 0f,
    val desafioDesbloqueado: Boolean = false,
    val nombreDesafio: String? = null
)

class GestionarProgresoHabitoUseCase(
    private val habitosRepository: HabitosRepository,
    private val cumplimientosRepository: CumplimientosRepository,
    private val desafiosRepository: DesafiosRepository,
    private val context: Context? = null
) {
    suspend fun ejecutar(usuarioHabito: UsuarioHabito): ResultadoProgreso {
        val marcado = cumplimientosRepository.alternarCumplimientoHoy(
            userId = usuarioHabito.userId,
            usuarioHabitoId = usuarioHabito.id,
            habitoCatalogoId = usuarioHabito.habitoId
        )

        val fechas = cumplimientosRepository.obtenerFechasCumplidas(
            userId = usuarioHabito.userId,
            usuarioHabitoId = usuarioHabito.id
        )

        val desafioId = usuarioHabito.desafioId
            ?: return ResultadoProgreso(
                marcadoComoHecho = marcado,
                fechasCumplidas = fechas
            )

        val desafio = desafiosRepository.obtenerDesafioPorId(desafioId)
            ?: return ResultadoProgreso(marcadoComoHecho = marcado, fechasCumplidas = fechas)

        return when (desafio.tipo) {
            TipoDesafio.ACUMULACION -> evaluarAcumulacion(usuarioHabito, desafio, fechas, marcado)
            TipoDesafio.COMBO -> evaluarCombo(usuarioHabito, desafio, fechas, marcado)
        }
    }

    private suspend fun evaluarAcumulacion(
        usuarioHabito: UsuarioHabito,
        desafio: Desafio,
        fechas: List<String>,
        marcado: Boolean
    ): ResultadoProgreso {
        val progreso = (fechas.size.toFloat() / desafio.meta.toFloat()).coerceAtMost(1f)
        val porcentaje = (progreso * 100).toInt()
        val seCumplioMeta = fechas.size >= desafio.meta

        if (seCumplioMeta && marcado) {
            val (lat, lng) = obtenerUbicacion()
            desafiosRepository.marcarDesafioCompletado(
                userId = usuarioHabito.userId,
                desafio = desafio,
                latitud = lat,
                longitud = lng
            )
        }

        return ResultadoProgreso(
            marcadoComoHecho = marcado,
            fechasCumplidas = fechas,
            porcentajeAvance = porcentaje,
            progresoBarra = progreso,
            desafioDesbloqueado = seCumplioMeta && marcado,
            nombreDesafio = if (seCumplioMeta && marcado) desafio.nombre else null
        )
    }

    private suspend fun evaluarCombo(
        usuarioHabito: UsuarioHabito,
        desafio: Desafio,
        fechas: List<String>,
        marcado: Boolean
    ): ResultadoProgreso {
        if (!marcado) {
            return ResultadoProgreso(
                marcadoComoHecho = false,
                fechasCumplidas = fechas
            )
        }

        val todosLosHabitos = habitosRepository.obtenerHabitosUsuario(usuarioHabito.userId)
        val habitosDelDesafio = todosLosHabitos.filter { it.desafioId == desafio.id }

        val cumplioComboHoy = habitosDelDesafio.all { hermano ->
            cumplimientosRepository.estaCumplidoHoy(usuarioHabito.userId, hermano.id)
        }

        if (cumplioComboHoy) {
            val (lat, lng) = obtenerUbicacion()
            desafiosRepository.marcarDesafioCompletado(
                userId = usuarioHabito.userId,
                desafio = desafio,
                latitud = lat,
                longitud = lng
            )
        }

        return ResultadoProgreso(
            marcadoComoHecho = true,
            fechasCumplidas = fechas,
            porcentajeAvance = if (cumplioComboHoy) 100 else 0,
            progresoBarra = if (cumplioComboHoy) 1f else 0f,
            desafioDesbloqueado = cumplioComboHoy,
            nombreDesafio = if (cumplioComboHoy) desafio.nombre else null
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun obtenerUbicacion(): Pair<Double?, Double?> {
        val ctx = context ?: return Pair(null, null)
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hayProvider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!hayProvider) {
                android.util.Log.w("ProgresoUseCase", "Ningún provider de ubicación habilitado")
                return Pair(null, null)
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)

            val lastLoc = withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine { cont ->
                    fusedClient.lastLocation
                        .addOnSuccessListener { loc -> cont.resume(loc) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }

            if (lastLoc != null) {
                android.util.Log.d(
                    "ProgresoUseCase",
                    "Ubicación obtenida (lastLocation): ${lastLoc.latitude}, ${lastLoc.longitude}"
                )
                return Pair(lastLoc.latitude, lastLoc.longitude)
            }

            android.util.Log.d(
                "ProgresoUseCase",
                "lastLocation null, solicitando ubicación fresca..."
            )
            val freshLoc = withTimeoutOrNull(8_000L) {
                suspendCancellableCoroutine { cont ->
                    val request = LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        1000L
                    ).setMaxUpdates(1).build()

                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            cont.resume(result.lastLocation)
                        }
                    }

                    fusedClient.requestLocationUpdates(
                        request,
                        callback,
                        Looper.getMainLooper()
                    ).addOnFailureListener { cont.resume(null) }

                    cont.invokeOnCancellation {
                        fusedClient.removeLocationUpdates(callback)
                    }
                }
            }

            if (freshLoc != null) {
                Pair(freshLoc.latitude, freshLoc.longitude)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            android.util.Log.w(
                "ProgresoUseCase",
                "Error obteniendo ubicación: ${e.localizedMessage}"
            )
            Pair(null, null)
        }
    }
}
