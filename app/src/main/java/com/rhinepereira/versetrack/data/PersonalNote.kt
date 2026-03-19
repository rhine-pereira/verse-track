package com.rhinepereira.versetrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "personal_note_categories")
data class PersonalNoteCategory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("name")
    val name: String,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("is_synced")
    val isSynced: Boolean = false
)

@Serializable
@Entity(tableName = "personal_notes")
data class PersonalNote(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
    @SerialName("date")
    val date: Long, // normalized to start of day
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("is_synced")
    val isSynced: Boolean = false
)
