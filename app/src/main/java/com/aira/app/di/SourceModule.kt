package com.aira.app.di

import com.aira.app.data.location.LocationProvider
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.data.repository.WeatherRepository
import com.aira.app.data.sensor.AndroidSensorSubscriber
import com.aira.app.data.sensor.SensorReader
import com.aira.app.data.sensor.SensorSubscriber
import com.aira.app.data.settings.SettingsStore
import com.aira.app.data.repository.AlertRepository
import com.aira.app.data.repository.TaskRepository
import com.aira.app.domain.usecase.AlertNotifier
import com.aira.app.domain.usecase.AlertSettingsSource
import com.aira.app.domain.usecase.AlertStore
import com.aira.app.domain.usecase.ForecastSource
import com.aira.app.domain.usecase.LocationSource
import com.aira.app.domain.usecase.ReminderScheduler
import com.aira.app.domain.usecase.TaskStore
import com.aira.app.domain.usecase.SensorSource
import com.aira.app.domain.usecase.SnapshotStore
import com.aira.app.domain.usecase.WeatherSource
import com.aira.app.notification.NotificationHelper
import com.aira.app.worker.TaskScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which real class to use for each interface the use case depends on. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SourceModule {
    @Binds abstract fun sensorSubscriber(impl: AndroidSensorSubscriber): SensorSubscriber
    @Binds abstract fun sensorSource(impl: SensorReader): SensorSource
    @Binds abstract fun alertSettings(impl: SettingsStore): AlertSettingsSource
    @Binds abstract fun alertNotifier(impl: NotificationHelper): AlertNotifier
    @Binds abstract fun locationSource(impl: LocationProvider): LocationSource
    @Binds abstract fun weatherSource(impl: WeatherRepository): WeatherSource
    @Binds abstract fun forecastSource(impl: WeatherRepository): ForecastSource
    @Binds abstract fun alertStore(impl: AlertRepository): AlertStore
    @Binds abstract fun taskStore(impl: TaskRepository): TaskStore
    @Binds abstract fun reminderScheduler(impl: TaskScheduler): ReminderScheduler
    @Binds abstract fun snapshotStore(impl: SnapshotRepository): SnapshotStore
}
