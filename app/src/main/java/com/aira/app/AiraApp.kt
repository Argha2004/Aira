package com.aira.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aira.app.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class. Hilt generates the dependency graph from here, and WorkManager is
 * configured to build workers through Hilt (the default initializer is disabled in the manifest).
 */
@HiltAndroidApp
class AiraApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel() // the "Weather alerts" channel must exist before any alert
        // The OpenStreetMap tile servers ask every app to identify itself; the map also keeps its settings here.
        org.osmdroid.config.Configuration.getInstance().apply {
            load(this@AiraApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            userAgentValue = packageName
        }
    }
}
