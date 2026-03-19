package com.rhinepereira.versetrack.data

import android.content.Context
import androidx.work.*
import com.rhinepereira.versetrack.sync.SyncWorker
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PersonalNoteRepository(private val context: Context, private val verseDao: VerseDao) {

    fun getAllCategories(userId: String): Flow<List<PersonalNoteCategory>> = verseDao.getAllCategories(userId)

    fun getNotesForCategory(categoryId: String): Flow<List<PersonalNote>> = verseDao.getNotesForCategory(categoryId)

    private fun getCurrentUserId(): String {
        return SupabaseConfig.client.gotrue.currentUserOrNull()?.id ?: ""
    }

    suspend fun insertCategory(category: PersonalNoteCategory) {
        verseDao.insertCategory(category.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun insertNote(note: PersonalNote) {
        verseDao.insertPersonalNote(note.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun updateNote(note: PersonalNote) {
        verseDao.insertPersonalNote(note.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun deleteNote(note: PersonalNote) {
        verseDao.deletePersonalNote(note)
        withContext(Dispatchers.IO) {
            try {
                SupabaseConfig.client.postgrest["personal_notes"].delete {
                    filter { eq("id", note.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "supabase_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
