package com.aira.app.data.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Battery level in percent (null if unknown) and whether the phone is charging. */
data class BatteryStatus(val levelPercent: Int?, val charging: Boolean)

/** Reads the battery state. The battery broadcast is "sticky", so no receiver needs to stay registered. */
@Singleton
class BatteryMonitor @Inject constructor(@ApplicationContext private val context: Context) {

    fun read(): BatteryStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryStatus(levelPercent = null, charging = false)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0 ||
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null
        return BatteryStatus(percent, charging)
    }
}
