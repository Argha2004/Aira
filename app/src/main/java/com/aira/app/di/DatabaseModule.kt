package com.aira.app.di

import android.content.Context
import androidx.room.Room
import com.aira.app.data.local.AlertDao
import com.aira.app.data.local.AppDatabase
import com.aira.app.data.local.MIGRATION_2_3
import com.aira.app.data.local.MIGRATION_3_4
import com.aira.app.data.local.SavedLocationDao
import com.aira.app.data.local.TaskDao
import com.aira.app.data.local.CommuteDao
import com.aira.app.data.local.DiaryEventDao
import com.aira.app.data.local.MIGRATION_1_2
import com.aira.app.data.local.NoteDao
import com.aira.app.data.local.SnapshotDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun provideSnapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()
    @Provides fun provideDiaryEventDao(db: AppDatabase): DiaryEventDao = db.diaryEventDao()
    @Provides fun provideCommuteDao(db: AppDatabase): CommuteDao = db.commuteDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
    @Provides fun provideSavedLocationDao(db: AppDatabase): SavedLocationDao = db.savedLocationDao()
}
