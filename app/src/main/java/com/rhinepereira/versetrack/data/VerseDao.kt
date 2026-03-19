package com.rhinepereira.versetrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotesWithVerses(userId: String): Flow<List<NoteWithVerses>>

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllNotes(userId: String): Flow<List<Note>>

    @Query("SELECT * FROM verses WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getVersesForNote(noteId: String): Flow<List<Verse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: Verse)

    @Update
    suspend fun updateNote(note: Note)

    @Update
    suspend fun updateVerse(verse: Verse)

    @Delete
    suspend fun deleteNote(note: Note)

    @Delete
    suspend fun deleteVerse(verse: Verse)

    @Query("SELECT * FROM verses WHERE isSynced = 0")
    suspend fun getUnsyncedVerses(): List<Verse>

    @Query("SELECT * FROM notes WHERE isSynced = 0")
    suspend fun getUnsyncedNotes(): List<Note>

    // Daily Records
    @Query("SELECT * FROM daily_records WHERE userId = :userId ORDER BY date DESC")
    fun getAllDailyRecords(userId: String): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    fun getRecordForDate(userId: String, startOfToday: Long, endOfToday: Long): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE userId = :userId AND date >= :startOfDay AND date < :endOfDay LIMIT 1")
    suspend fun getRecordForDateSync(userId: String, startOfDay: Long, endOfDay: Long): DailyRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyRecord(record: DailyRecord)

    @Update
    suspend fun updateDailyRecord(record: DailyRecord)

    @Query("SELECT * FROM daily_records WHERE isSynced = 0")
    suspend fun getUnsyncedDailyRecords(): List<DailyRecord>

    // Personal Notes
    @Query("SELECT * FROM personal_note_categories WHERE userId = :userId ORDER BY createdAt ASC")
    fun getAllCategories(userId: String): Flow<List<PersonalNoteCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: PersonalNoteCategory)

    @Query("SELECT * FROM personal_notes WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getNotesForCategory(categoryId: String): Flow<List<PersonalNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalNote(note: PersonalNote)

    @Update
    suspend fun updatePersonalNote(note: PersonalNote)

    @Delete
    suspend fun deletePersonalNote(note: PersonalNote)

    @Query("SELECT * FROM personal_note_categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<PersonalNoteCategory>

    @Query("SELECT * FROM personal_notes WHERE isSynced = 0")
    suspend fun getUnsyncedPersonalNotes(): List<PersonalNote>
}
