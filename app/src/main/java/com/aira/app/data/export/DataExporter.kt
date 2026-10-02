package com.aira.app.data.export

import android.content.Context
import android.net.Uri
import com.aira.app.data.repository.SnapshotRepository
import com.aira.app.domain.engine.CsvExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes all snapshots as CSV to a file the user picked with the system file dialog
 * (ACTION_CREATE_DOCUMENT), so no storage permission is needed.
 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val snapshots: SnapshotRepository,
) {
    /** Returns the number of snapshots written, or a failure. */
    suspend fun export(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val all = snapshots.getAll()
            val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Cannot open the file")
            Result.success(stream.bufferedWriter().use { CsvExporter.write(all, ZoneId.systemDefault(), it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
