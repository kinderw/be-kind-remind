package com.bekindremind.app.network

import com.squareup.moshi.Json

data class DistanceMatrixResponse(
    @Json(name = "rows") val rows: List<Row> = emptyList(),
    @Json(name = "status") val status: String? = null
)

data class Row(
    @Json(name = "elements") val elements: List<Element> = emptyList()
)

data class Element(
    @Json(name = "status") val status: String? = null,
    @Json(name = "duration") val duration: DurationValue? = null,
    @Json(name = "duration_in_traffic") val durationInTraffic: DurationValue? = null
)

data class DurationValue(
    @Json(name = "value") val valueSeconds: Int = 0
)
