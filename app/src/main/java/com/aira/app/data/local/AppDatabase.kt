package com.aira.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** The app's Room database. The schema is exported to app/schemas for migrations. */
@Database(
    entities = [
        SnapshotEntity::class, DiaryEventEntity::class, CommuteEntity::class, NoteEntity::class,
        AlertEntity::class, TaskEntity::class, SavedLocationEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snapshotDao(): SnapshotDao
    abstract fun diaryEventDao(): DiaryEventDao
    abstract fun commuteDao(): CommuteDao
    abstract fun noteDao(): NoteDao
    abstract fun alertDao(): AlertDao
    abstract fun taskDao(): TaskDao
    abstract fun savedLocationDao(): SavedLocationDao

    companion object {
        const val NAME = "aira.db"
    }
}
