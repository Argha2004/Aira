package com.aira.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aira.app.domain.usecase.TaskActions
import com.aira.app.domain.usecase.runCatchingCancellable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the buttons on notifications: Add task, Dismiss, Done, Snooze 1 h, Move to a better time, Keep.
 * It works even when the app is closed: Android starts the app process for the broadcast, Hilt supplies
 * the dependencies, and the change is saved before the receiver finishes ([goAsync] keeps it alive).
 * A failure is logged and the notification is still removed; it never crashes the app.
 */
@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject lateinit var actions: TaskActions
    @Inject lateinit var notifications: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                runCatchingCancellable { handle(intent) }
                    .onFailure { Log.e(TAG, "Notification action ${intent.action} failed", it) }
                intent.getIntExtra(NotificationActions.EXTRA_NOTIFICATION_ID, -1)
                    .takeIf { it >= 0 }
                    ?.let { notifications.cancel(it) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(intent: Intent) {
        val alertId = intent.getLongExtra(NotificationActions.EXTRA_ALERT_ID, -1)
        val taskId = intent.getLongExtra(NotificationActions.EXTRA_TASK_ID, -1)
        when (intent.action) {
            NotificationActions.ADD_TASK -> actions.addFirstSuggestion(alertId)
            NotificationActions.DISMISS_ALERT -> actions.dismissAlert(alertId)
            NotificationActions.TASK_DONE -> actions.markDone(taskId)
            NotificationActions.TASK_SNOOZE -> actions.snooze(taskId)
            NotificationActions.TASK_MOVE -> {
                val newDue = intent.getLongExtra(NotificationActions.EXTRA_NEW_DUE_TIME, -1)
                if (newDue > 0) actions.moveTo(taskId, newDue)
            }
            NotificationActions.TASK_KEEP -> Unit // nothing to change; the notification just goes away
        }
    }

    private companion object {
        const val TAG = "TaskActionReceiver"
    }
}
