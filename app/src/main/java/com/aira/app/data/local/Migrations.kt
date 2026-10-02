package com.aira.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Version 4 adds the "saved_locations" table for the places picked on the map. Existing data is not touched. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_locations` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)",
        )
    }
}

/** Version 3 adds the "alerts" and "tasks" tables. Existing data is not touched. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `alerts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`time` INTEGER NOT NULL, " +
                "`message` TEXT NOT NULL, " +
                "`value` REAL, " +
                "`dismissed` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tasks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`note` TEXT NOT NULL, " +
                "`triggerType` TEXT NOT NULL, " +
                "`dueTime` INTEGER, " +
                "`isOutdoor` INTEGER NOT NULL, " +
                "`repeat` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`snoozedUntil` INTEGER, " +
                "`sourceAlertId` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`completedAt` INTEGER)",
        )
    }
}

/** Version 2 adds "weatherPending" so snapshots can be saved offline and get their weather later. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE snapshots ADD COLUMN weatherPending INTEGER NOT NULL DEFAULT 0")
    }
}
