package com.aira.app.domain.engine

/** Compares app versions such as "1.5.2" or a GitHub release tag such as "v1.6.0". */
object VersionCompare {
    /**
     * True when [latest] is a higher version than [current]. A leading "v" is ignored and the numbers are compared
     * one by one from the left ("1.10.0" is newer than "1.9.0"); a missing number counts as 0 ("1.6" equals "1.6.0").
     * Text that is not a version, or has no numbers at all, is never newer.
     */
    fun isNewer(current: String, latest: String): Boolean {
        val now = parts(current) ?: return false
        val next = parts(latest) ?: return false
        for (i in 0 until maxOf(now.size, next.size)) {
            val a = now.getOrElse(i) { 0 }
            val b = next.getOrElse(i) { 0 }
            if (b != a) return b > a
        }
        return false
    }

    /** "v1.5.2-beta" -> [1, 5, 2]; null when there are no numbers. */
    private fun parts(version: String): List<Int>? {
        val numbers = version.trim().removePrefix("v").removePrefix("V").substringBefore('-').split('.')
            .map { it.toIntOrNull() ?: return null }
        return numbers.takeIf { it.isNotEmpty() }
    }
}
