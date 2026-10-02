package com.aira.app.domain.model

/** A place the user saved on the map to check its weather on Home. Coordinates are rounded to 2 decimals. */
data class CustomLocation(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** A place found by the search box: its name and where it is (e.g. "Howrah", "West Bengal, India"). */
data class PlaceResult(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
)
