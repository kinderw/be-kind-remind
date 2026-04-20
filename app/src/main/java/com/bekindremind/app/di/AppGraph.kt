package com.bekindremind.app.di

import android.content.Context
import com.bekindremind.app.BuildConfig
import com.bekindremind.app.data.TaskRepository
import com.bekindremind.app.domain.EtaProvider
import com.bekindremind.app.network.DistanceMatrixApi
import com.bekindremind.app.network.GoogleEtaProvider
import com.bekindremind.app.notifications.NotificationHelper
import com.bekindremind.app.scheduling.SchedulingEngine
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppGraph(
    context: Context,
    val repository: TaskRepository
) {
    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val distanceMatrixApi = retrofit.create(DistanceMatrixApi::class.java)

    val etaProvider: EtaProvider = GoogleEtaProvider(distanceMatrixApi, BuildConfig.MAPS_API_KEY)

    val schedulingEngine = SchedulingEngine(context, repository, etaProvider)

    val notificationHelper = NotificationHelper(context)
}
