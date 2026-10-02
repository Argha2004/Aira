package com.aira.app.domain.engine

import com.aira.app.domain.model.AlertType

/** Ready-made task ideas for each alert. The first one is what the "Add task" button adds. */
object TaskSuggestions {

    fun forAlert(type: AlertType): List<String> = when (type) {
        AlertType.RAIN_SOON -> listOf("Carry an umbrella / raincoat", "Bring clothes inside", "Cover bike seat")
        AlertType.RAIN_NOW -> listOf("Find shelter", "Protect phone and bag")
        AlertType.HEAT -> listOf("Drink water", "Rest in shade", "Carry a water bottle")
        AlertType.STRONG_SUN -> listOf("Apply sunscreen", "Wear a cap")
        AlertType.PRESSURE_DROP -> listOf("Close windows", "Bring clothes inside")
    }
}
