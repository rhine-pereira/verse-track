package com.rhinepereira.versetrack.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rhinepereira.versetrack.data.AppDatabase
import com.rhinepereira.versetrack.data.SupabaseConfig
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.verseDao()

        // Get current user ID - skip sync if not authenticated
        val userId = SupabaseConfig.client.gotrue.currentUserOrNull()?.id
            ?: return@withContext Result.retry()

        try {
            // 1. Sync Notes (Themes)
            val unsyncedNotes = dao.getUnsyncedNotes()
            unsyncedNotes.forEach { note ->
                SupabaseConfig.client.postgrest["notes"].upsert(note.copy(userId = userId))
                dao.updateNote(note.copy(isSynced = true, userId = userId))
            }

            // 2. Sync Verses
            val unsyncedVerses = dao.getUnsyncedVerses()
            unsyncedVerses.forEach { verse ->
                SupabaseConfig.client.postgrest["verses"].upsert(verse.copy(userId = userId))
                dao.updateVerse(verse.copy(isSynced = true, userId = userId))
            }

            // 3. Sync Daily Records
            val unsyncedRecords = dao.getUnsyncedDailyRecords()
            unsyncedRecords.forEach { record ->
                SupabaseConfig.client.postgrest["daily_records"].upsert(record.copy(userId = userId))
                dao.updateDailyRecord(record.copy(isSynced = true, userId = userId))
            }

            // 4. Sync Personal Note Categories
            val unsyncedCategories = dao.getUnsyncedCategories()
            unsyncedCategories.forEach { category ->
                SupabaseConfig.client.postgrest["personal_note_categories"].upsert(category.copy(userId = userId))
                dao.insertCategory(category.copy(isSynced = true, userId = userId))
            }

            // 5. Sync Personal Notes
            val unsyncedPersonalNotes = dao.getUnsyncedPersonalNotes()
            unsyncedPersonalNotes.forEach { note ->
                SupabaseConfig.client.postgrest["personal_notes"].upsert(note.copy(userId = userId))
                dao.insertPersonalNote(note.copy(isSynced = true, userId = userId))
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
