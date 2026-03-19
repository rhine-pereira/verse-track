package com.rhinepereira.versetrack.data

import android.content.Context
import androidx.work.*
import com.rhinepereira.versetrack.sync.SyncWorker
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VerseRepository(private val context: Context, private val verseDao: VerseDao) {

    fun getAllNotesWithVerses(userId: String): Flow<List<NoteWithVerses>> = verseDao.getNotesWithVerses(userId)

    fun getVersesForNote(noteId: String): Flow<List<Verse>> = verseDao.getVersesForNote(noteId)

    private fun getCurrentUserId(): String {
        return SupabaseConfig.client.gotrue.currentUserOrNull()?.id ?: ""
    }

    suspend fun insertNote(note: Note) {
        verseDao.insertNote(note.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun insertVerse(verse: Verse) {
        verseDao.insertVerse(verse.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun updateVerse(verse: Verse) {
        verseDao.updateVerse(verse.copy(isSynced = false, userId = getCurrentUserId()))
        scheduleSync()
    }

    suspend fun deleteNote(note: Note) {
        verseDao.deleteNote(note)
        withContext(Dispatchers.IO) {
            try {
                SupabaseConfig.client.postgrest["notes"].delete {
                    filter { eq("id", note.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteVerse(verse: Verse) {
        verseDao.deleteVerse(verse)
        withContext(Dispatchers.IO) {
            try {
                SupabaseConfig.client.postgrest["verses"].delete {
                    filter { eq("id", verse.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchFromSupabase() = withContext(Dispatchers.IO) {
        try {
            // With RLS enabled, these queries automatically return only the current user's data
            
            // Fetch Notes
            val notes = SupabaseConfig.client.postgrest["notes"].select().decodeList<Note>()
            notes.forEach { note ->
                verseDao.insertNote(note.copy(isSynced = true))
            }

            // Fetch Verses
            val verses = SupabaseConfig.client.postgrest["verses"].select().decodeList<Verse>()
            verses.forEach { verse ->
                verseDao.insertVerse(verse.copy(isSynced = true))
            }
            
            // Fetch Daily Records
            val records = SupabaseConfig.client.postgrest["daily_records"].select().decodeList<DailyRecord>()
            records.forEach { record ->
                verseDao.insertDailyRecord(record.copy(isSynced = true))
            }

            // Fetch Categories
            val categories = SupabaseConfig.client.postgrest["personal_note_categories"].select().decodeList<PersonalNoteCategory>()
            categories.forEach { category ->
                verseDao.insertCategory(category.copy(isSynced = true))
            }

            // Fetch Personal Notes
            val personalNotes = SupabaseConfig.client.postgrest["personal_notes"].select().decodeList<PersonalNote>()
            personalNotes.forEach { personalNote ->
                verseDao.insertPersonalNote(personalNote.copy(isSynced = true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
