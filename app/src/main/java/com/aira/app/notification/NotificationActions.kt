package com.aira.app.notification

import com.aira.app.domain.model.AlertType

/** The names used by notification buttons to tell [TaskActionReceiver] what to do. */
object NotificationActions {
    const val ADD_TASK = "com.aira.app.action.ADD_TASK"
    const val DISMISS_ALERT = "com.aira.app.action.DISMISS_ALERT"
    const val TASK_DONE = "com.aira.app.action.TASK_DONE"
    const val TASK_SNOOZE = "com.aira.app.action.TASK_SNOOZE"
    const val TASK_MOVE = "com.aira.app.action.TASK_MOVE"
    const val TASK_KEEP = "com.aira.app.action.TASK_KEEP"

    const val EXTRA_ALERT_ID = "alert_id"
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_NEW_DUE_TIME = "new_due_time"
    const val EXTRA_NOTIFICATION_ID = "notification_id"

    // One notification slot per alert type, and one per task, so a new one replaces the old one.
    fun alertNotificationId(type: AlertType) = 1000 + type.ordinal
    fun taskNotificationId(taskId: Long) = 2000 + (taskId % 1000).toInt()
    fun planNotificationId(taskId: Long) = 3000 + (taskId % 1000).toInt()
}
