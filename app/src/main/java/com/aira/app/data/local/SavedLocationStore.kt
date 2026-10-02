package com.aira.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Table "saved_locations": the places the user added on the map (at most 5). */
@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
)

@Dao
interface SavedLocationDao {
    @Insert
    suspend fun insert(location: SavedLocationEntity): Long

    @Query("SELECT * FROM saved_locations ORDER BY createdAt")
    fun observeAll(): Flow<List<SavedLocationEntity>>

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun count(): Int

    @Query("SELECT * FROM saved_locations WHERE id = :id")
    suspend fun get(id: Long): SavedLocationEntity?

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun delete(id: Long)
}
