package com.bekindremind.app.domain

import com.bekindremind.app.data.TrafficModel

data class RoutePoint(val lat: Double, val lng: Double)

interface EtaProvider {
    suspend fun etaMinutes(
        origin: RoutePoint,
        destination: RoutePoint,
        trafficModel: TrafficModel
    ): Int
}
