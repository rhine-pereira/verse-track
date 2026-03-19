package com.rhinepereira.versetrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Entity(
    tableName = "daily_records",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @SerialName("date")
    val date: Long, // normalized to start of day
    
    @SerialName("read_today")
    val readToday: Boolean = false,
    @SerialName("what_read")
    val whatRead: String = "",
    @SerialName("total_read_time_minutes")
    val totalReadTimeMinutes: Int = 0,
    
    @SerialName("prayed_today")
    val prayedToday: Boolean = false,
    @SerialName("total_prayer_time_minutes")
    val totalPrayerTimeMinutes: Int = 0,
    @SerialName("prophecy")
    val prophecy: String = "",
    
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("is_synced")
    val isSynced: Boolean = false
)
