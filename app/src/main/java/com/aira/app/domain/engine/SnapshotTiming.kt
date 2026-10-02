package com.aira.app.domain.engine

import com.aira.app.domain.model.Snapshot

object SnapshotTiming {

    /**
     * How many minutes each snapshot stands for: the time until the next snapshot, but at most 60 (a long gap
     * means the app was not recording). The last snapshot of a day stands for the same time as the gap before
     * it (5 minutes when checking every 5 minutes, 30 when every 30), and for 30 minutes if it is the only one.
     * [snapshots] must be in time order.
     */
    fun minutes(snapshots: List<Snapshot>): List<Double> {
        val gaps = snapshots.zipWithNext { a, b -> minOf((b.timestamp - a.timestamp) / 60_000.0, Thresholds.MAX_SNAPSHOT_MINUTES) }
        val last = gaps.lastOrNull() ?: Thresholds.DEFAULT_SNAPSHOT_MINUTES
        return if (snapshots.isEmpty()) emptyList() else gaps + last
    }

    /** True when a snapshot was taken less than [Thresholds.MIN_SNAPSHOT_SPACING_MS] ago, so another is not needed yet. */
    fun tooSoon(lastSnapshotAt: Long?, now: Long): Boolean =
        lastSnapshotAt != null && now - lastSnapshotAt in 0 until Thresholds.MIN_SNAPSHOT_SPACING_MS
}
