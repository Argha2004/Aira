package com.aira.app.data.local

import android.content.ContentValues
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Opens old versions of the database (built from the exported schema files in app/schemas) and checks
 * that upgrading keeps the user's data and gives exactly the tables that Room expects.
 */
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private val dbName = "migration-test"

    private fun snapshotRow(withWeatherPending: Boolean) = ContentValues().apply {
        put("timestamp", 1_000L)
        put("latitude", 22.57)
        put("longitude", 88.36)
        put("temperature", 31.0)
        put("feelsLike", 37.0)
        put("humidity", 60)
        put("rainMm", 0.0)
        put("uvIndex", 6.0)
        put("windSpeed", 8.0)
        put("isOutdoor", 1)
        put("isMoving", 0)
        put("place", "OUTDOOR")
        put("movement", "STILL")
        if (withWeatherPending) put("weatherPending", 0)
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    @Test
    fun migrate2To3_keepsSnapshotsAndAddsAlertsAndTasks() {
        helper.createDatabase(dbName, 2).use { old ->
            old.insert("snapshots", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, snapshotRow(withWeatherPending = true))
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3)

        assertEquals(1, count(db, "snapshots"))
        assertEquals(0, count(db, "alerts"))
        assertEquals(0, count(db, "tasks"))

        // The new tables accept rows the way the app writes them.
        db.execSQL("INSERT INTO alerts (type, time, message, value, dismissed) VALUES ('HEAT', 1, 'Feels like 40', 40.0, 0)")
        db.execSQL(
            "INSERT INTO tasks (title, note, triggerType, dueTime, isOutdoor, repeat, status, snoozedUntil, " +
                "sourceAlertId, createdAt, completedAt) VALUES ('Drink water', '', 'HEAT', NULL, 0, 'ONCE', 'PENDING', NULL, 1, 1, NULL)",
        )
        assertEquals(1, count(db, "alerts"))
        assertEquals(1, count(db, "tasks"))
    }

    @Test
    fun migrate3To4_keepsDataAndAddsSavedLocations() {
        helper.createDatabase(dbName, 3).use { old ->
            old.insert("snapshots", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, snapshotRow(withWeatherPending = true))
            old.execSQL(
                "INSERT INTO tasks (title, note, triggerType, dueTime, isOutdoor, repeat, status, snoozedUntil, " +
                    "sourceAlertId, createdAt, completedAt) VALUES ('Drink water', '', 'HEAT', NULL, 0, 'ONCE', 'PENDING', NULL, NULL, 1, NULL)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        assertEquals(1, count(db, "snapshots"))
        assertEquals(1, count(db, "tasks"))
        assertEquals(0, count(db, "saved_locations"))
        db.execSQL("INSERT INTO saved_locations (name, latitude, longitude, createdAt) VALUES ('College', 22.6, 88.4, 1)")
        assertEquals(1, count(db, "saved_locations"))
    }

    @Test
    fun migrateFrom1AllTheWayTo3_keepsTheSnapshot() {
        helper.createDatabase(dbName, 1).use { old ->
            old.insert("snapshots", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, snapshotRow(withWeatherPending = false))
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        assertEquals(1, count(db, "snapshots"))
        // The column added in version 2 gets its default for the old row.
        db.query("SELECT weatherPending FROM snapshots").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }
}
