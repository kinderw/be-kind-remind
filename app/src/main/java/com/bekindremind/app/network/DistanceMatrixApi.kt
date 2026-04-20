package com.bekindremind.app.network

import retrofit2.http.GET
import retrofit2.http.Query

interface DistanceMatrixApi {
    @GET("maps/api/distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("departure_time") departureTime: String = "now",
        @Query("traffic_model") trafficModel: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): DistanceMatrixResponse
}
