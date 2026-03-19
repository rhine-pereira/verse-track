package com.rhinepereira.versetrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rhinepereira.versetrack.data.*
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.SessionStatus

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PersonalNoteRepository
    private val dao: VerseDao

    val categories: StateFlow<List<PersonalNoteCategory>>

    init {
        val database = AppDatabase.getDatabase(application)
        dao = database.verseDao()
        repository = PersonalNoteRepository(application, dao)

        categories = SupabaseConfig.client.gotrue.sessionStatus
            .flatMapLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        repository.getAllCategories(status.session.user?.id ?: "")
                    }
                    else -> flowOf(emptyList())
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Seed predefined categories for the authenticated user if empty
        viewModelScope.launch {
            SupabaseConfig.client.gotrue.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    val userId = status.session.user?.id ?: ""
                    repository.getAllCategories(userId).first().let { current ->
                        if (current.isEmpty()) {
                            repository.insertCategory(PersonalNoteCategory(name = "CYP Talks", userId = userId))
                            repository.insertCategory(PersonalNoteCategory(name = "CGS Talks", userId = userId))
                            repository.insertCategory(PersonalNoteCategory(name = "Prophecies", userId = userId))
                        }
                    }
                }
            }
        }
    }

    fun getNotesForCategory(categoryId: String): Flow<List<PersonalNote>> = repository.getNotesForCategory(categoryId)

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(PersonalNoteCategory(name = name))
        }
    }

    fun addNote(categoryId: String, title: String, content: String, date: Long) {
        viewModelScope.launch {
            repository.insertNote(PersonalNote(categoryId = categoryId, title = title, content = content, date = date))
        }
    }

    fun updateNote(note: PersonalNote) {
        viewModelScope.launch {
            if (note.id.isBlank() || note.id == "0") { // Check if new
                 repository.insertNote(note.copy(id = java.util.UUID.randomUUID().toString()))
            } else {
                repository.updateNote(note)
            }
        }
    }

    fun deleteNote(note: PersonalNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
