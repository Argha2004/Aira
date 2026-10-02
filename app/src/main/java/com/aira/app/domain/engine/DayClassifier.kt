package com.aira.app.domain.engine

enum class DayCondition { NO_DATA, SUNNY, RAINY, HOT, MOSTLY_INDOORS }

object DayClassifier {

    /**
     * The one icon a calendar day gets. No snapshots: NO_DATA. Otherwise the first that applies:
     * any rain encounter: RAINY; 60 or more minutes of heat: HOT; 30 or more minutes of strong sun: SUNNY;
     * anything else: MOSTLY_INDOORS.
     */
    fun classify(summary: DaySummary?): DayCondition = when {
        summary == null -> DayCondition.NO_DATA
        summary.stats.rainEncounters > 0 -> DayCondition.RAINY
        summary.stats.heatMinutes >= Thresholds.HOT_DAY_MINUTES -> DayCondition.HOT
        summary.stats.sunMinutes >= Thresholds.SUNNY_DAY_MINUTES -> DayCondition.SUNNY
        else -> DayCondition.MOSTLY_INDOORS
    }
}
