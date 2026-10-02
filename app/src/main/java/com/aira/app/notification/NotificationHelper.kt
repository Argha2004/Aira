package com.aira.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aira.app.MainActivity
import com.aira.app.R
import com.aira.app.domain.engine.AlertMessages
import com.aira.app.domain.engine.PlanIssue
import com.aira.app.domain.engine.PlanWarning
import com.aira.app.domain.engine.Temperature
import com.aira.app.domain.engine.TemperatureUnit
import com.aira.app.domain.model.WeatherAlert
import com.aira.app.domain.model.WeatherTask
import com.aira.app.domain.usecase.AlertNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.util.Objects
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the notification channels and shows the app's notifications:
 * weather alerts (with the tasks linked to them), task reminders, and outdoor plan warnings.
 * The buttons on them are handled by [TaskActionReceiver].
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlertNotifier {

    /** Notifications need a channel (minSdk is 26, so every phone we support has channels). Safe to call repeatedly. */
    fun createChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.channel_weather_alerts),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_weather_alerts_description) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASKS,
                context.getString(R.string.channel_task_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_task_reminders_description) },
        )
    }

    /**
     * A weather alert. The title is the headline; the body is the advice and, if there are any, the
     * user's tasks for this kind of alert. Buttons: Add task (adds the first suggestion) and Dismiss.
     */
    override fun show(alert: WeatherAlert, tasks: List<WeatherTask>, useFahrenheit: Boolean) {
        val unit = unitOf(useFahrenheit)
        val title = Temperature.convertText(alert.message, unit)
        val advice = AlertMessages.advice(alert.type)
        val notificationId = NotificationActions.alertNotificationId(alert.type)

        val summary = if (tasks.isEmpty()) {
            advice
        } else {
            "$advice ${context.getString(R.string.notif_your_tasks)} ${tasks.joinToString { it.title }}"
        }
        val inbox = NotificationCompat.InboxStyle().setBigContentTitle(title).addLine(advice)
        if (tasks.isNotEmpty()) {
            inbox.addLine(context.getString(R.string.notif_your_tasks))
            tasks.forEach { inbox.addLine("• ${it.title}") }
        }

        post(
            notificationId,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setContentTitle(title)
                .setContentText(summary)
                .setStyle(inbox)
                .addAction(
                    0,
                    context.getString(R.string.action_add_task),
                    broadcast(NotificationActions.ADD_TASK, alert.id, notificationId) {
                        putExtra(NotificationActions.EXTRA_ALERT_ID, alert.id)
                    },
                )
                .addAction(
                    0,
                    context.getString(R.string.action_dismiss),
                    broadcast(NotificationActions.DISMISS_ALERT, alert.id, notificationId) {
                        putExtra(NotificationActions.EXTRA_ALERT_ID, alert.id)
                    },
                ),
        )
    }

    /** A task has come due (or its snooze ended). Buttons: Done and Snooze 1 h. */
    fun showTaskDue(task: WeatherTask) {
        val notificationId = NotificationActions.taskNotificationId(task.id)
        post(
            notificationId,
            NotificationCompat.Builder(context, CHANNEL_TASKS)
                .setContentTitle(task.title)
                .setContentText(task.note.ifBlank { context.getString(R.string.notif_task_due) })
                .addAction(0, context.getString(R.string.action_done), taskBroadcast(NotificationActions.TASK_DONE, task.id, notificationId))
                .addAction(0, context.getString(R.string.action_snooze), taskBroadcast(NotificationActions.TASK_SNOOZE, task.id, notificationId)),
        )
    }

    /**
     * The weather looks bad for an outdoor task. "Rain likely at 5 PM for 'Go to market'. Better time: 2 PM?"
     * Buttons: Move to 2 PM (only if there is a better hour) and Keep.
     */
    fun showPlanWarning(task: WeatherTask, warning: PlanWarning, zone: ZoneId, useFahrenheit: Boolean) {
        val notificationId = NotificationActions.planNotificationId(task.id)
        val badHour = AlertMessages.hourText(warning.badHour.time, zone)
        val problem = when (warning.issue) {
            PlanIssue.RAIN -> context.getString(R.string.notif_plan_rain, badHour, task.title)
            PlanIssue.HEAT -> context.getString(
                R.string.notif_plan_heat, badHour,
                Temperature.format(warning.badHour.feelsLikeC, unitOf(useFahrenheit)), task.title,
            )
        }
        val better = warning.betterHour
        val betterText = better?.let { AlertMessages.hourText(it.time, zone) }
        val text = if (betterText != null) problem + context.getString(R.string.notif_plan_better, betterText) else problem

        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS)
            .setContentTitle(context.getString(R.string.notif_plan_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        if (better != null && task.dueTime != null) {
            // Keep the minutes of the original time: 5:30 PM moves to 2:30 PM, not 2:00 PM.
            val newDue = better.time + (task.dueTime - warning.badHour.time)
            builder.addAction(
                0,
                context.getString(R.string.action_move_to, betterText),
                broadcast(NotificationActions.TASK_MOVE, task.id, notificationId) {
                    putExtra(NotificationActions.EXTRA_TASK_ID, task.id)
                    putExtra(NotificationActions.EXTRA_NEW_DUE_TIME, newDue)
                },
            )
        }
        builder.addAction(0, context.getString(R.string.action_keep), taskBroadcast(NotificationActions.TASK_KEEP, task.id, notificationId))
        post(notificationId, builder)
    }

    fun cancel(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    // ---- helpers ----

    private fun unitOf(useFahrenheit: Boolean) = if (useFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS

    @SuppressLint("MissingPermission") // checked in canNotify()
    private fun post(notificationId: Int, builder: NotificationCompat.Builder) {
        if (!canNotify()) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun canNotify(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun taskBroadcast(action: String, taskId: Long, notificationId: Int): PendingIntent =
        broadcast(action, taskId, notificationId) { putExtra(NotificationActions.EXTRA_TASK_ID, taskId) }

    /** A button press that goes to [TaskActionReceiver]. Immutable, and unique per action and id. */
    private fun broadcast(action: String, id: Long, notificationId: Int, extras: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, TaskActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActions.EXTRA_NOTIFICATION_ID, notificationId)
            .apply(extras)
        return PendingIntent.getBroadcast(
            context,
            Objects.hash(action, id),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val CHANNEL_ALERTS = "weather_alerts"
        const val CHANNEL_TASKS = "task_reminders"
    }
}
