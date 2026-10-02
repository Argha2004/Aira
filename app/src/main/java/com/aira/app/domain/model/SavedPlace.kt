package com.aira.app.domain.model

/** A place the user named, such as Home or College. */
data class SavedPlace(val label: String, val latitude: Double, val longitude: Double) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
}
