package com.aira.app.data.repository

import com.aira.app.data.local.SavedLocationDao
import com.aira.app.data.local.SavedLocationEntity
import com.aira.app.data.remote.GeocodingApi
import com.aira.app.data.remote.toPlaces
import com.aira.app.data.settings.SettingsStore
import com.aira.app.domain.engine.LocationRules
import com.aira.app.domain.model.CustomLocation
import com.aira.app.domain.model.PlaceResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The places saved on the map (at most [LocationRules.MAX_CUSTOM_LOCATIONS]), which one Home shows, and the
 * place search. The live (GPS) location is not stored here: "no place chosen" means live.
 */
@Singleton
class LocationsRepository @Inject constructor(
    private val dao: SavedLocationDao,
    private val settings: SettingsStore,
    private val geocoding: GeocodingApi,
) {
    val saved: Flow<List<CustomLocation>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** The id of the place Home shows, or null for the live location. */
    val selectedId: Flow<Long?> = settings.selectedLocationId

    /**
     * Saves a place (coordinates rounded to 2 decimals) and makes it the one Home shows.
     * Returns false, saving nothing, when [LocationRules.MAX_CUSTOM_LOCATIONS] places are saved already.
     */
    suspend fun add(name: String, latitude: Double, longitude: Double): Boolean {
        if (!LocationRules.canAdd(dao.count())) return false
        val id = dao.insert(
            SavedLocationEntity(
                name = name,
                latitude = LocationRules.round2(latitude),
                longitude = LocationRules.round2(longitude),
                createdAt = System.currentTimeMillis(),
            ),
        )
        settings.setSelectedLocationId(id)
        return true
    }

    /** Deletes a place; if Home was showing it, Home goes back to the live location. */
    suspend fun delete(id: Long) {
        dao.delete(id)
        if (settings.selectedLocationIdNow() == id) settings.setSelectedLocationId(null)
    }

    /** Shows [id] on Home, or the live location for null. */
    suspend fun select(id: Long?) = settings.setSelectedLocationId(id)

    suspend fun get(id: Long): CustomLocation? = dao.get(id)?.toDomain()

    /** Places whose name matches [query]; empty for fewer than 2 letters, or when offline. */
    suspend fun search(query: String): List<PlaceResult> {
        if (query.trim().length < 2) return emptyList()
        return try {
            geocoding.search(query.trim()).toPlaces()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun SavedLocationEntity.toDomain() = CustomLocation(id, name, latitude, longitude)
}
