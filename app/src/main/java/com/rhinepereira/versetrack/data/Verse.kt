package com.rhinepereira.versetrack.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val theme: String,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Serializable
@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Verse(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("note_id")
    val noteId: String,
    val reference: String,
    val content: String,
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

data class NoteWithVerses(
    @Embedded val note: Note,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId"
    )
    val verses: List<Verse>
)
