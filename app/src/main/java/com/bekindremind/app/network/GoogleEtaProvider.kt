package com.bekindremind.app.network

import com.bekindremind.app.data.TrafficModel
import com.bekindremind.app.domain.EtaProvider
import com.bekindremind.app.domain.RoutePoint
import kotlin.math.ceil

class GoogleEtaProvider(
    private val api: DistanceMatrixApi,
    private val apiKey: String
) : EtaProvider {
    override suspend fun etaMinutes(
        origin: RoutePoint,
        destination: RoutePoint,
        trafficModel: TrafficModel
    ): Int {
        require(apiKey.isNotBlank()) { "MAPS_API_KEY is not configured." }

        val response = api.getDistanceMatrix(
            origins = "${origin.lat},${origin.lng}",
            destinations = "${destination.lat},${destination.lng}",
            trafficModel = trafficModel.toApiValue(),
            apiKey = apiKey
        )

        val element = response.rows.firstOrNull()?.elements?.firstOrNull()
            ?: error("Distance Matrix response had no route elements")

        val durationSeconds = element.durationInTraffic?.valueSeconds
            ?: element.duration?.valueSeconds
            ?: error("Distance Matrix response missing duration values")

        return ceil(durationSeconds / 60.0).toInt()
    }
}

private fun TrafficModel.toApiValue(): String {
    return when (this) {
        TrafficModel.BEST_GUESS -> "best_guess"
        TrafficModel.OPTIMISTIC -> "optimistic"
        TrafficModel.PESSIMISTIC -> "pessimistic"
    }
}
