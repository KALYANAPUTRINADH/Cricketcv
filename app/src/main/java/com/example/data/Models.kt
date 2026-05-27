package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "match_sessions")
data class MatchSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateTime: Long = System.currentTimeMillis(),
    val sourceVideoPath: String,
    val durationMs: Long = 0L,
    val isDemo: Boolean = false,
    val status: String = "IDLE" // "IDLE", "PROCESSING", "SEGMENTED"
) : Serializable

@Entity(tableName = "delivery_clips")
data class DeliveryClip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val overNumber: Int,
    val ballNumber: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val clipPath: String, // Trimmed video file path, or empty to play master
    val eventType: String, // "WICKET", "FOUR", "SIX", "DOT", "RUNS"
    val outcome: String, // e.g. "Clean Bowled", "Cover Drive for 4"
    val bowlerName: String,
    val batsmanName: String,
    val speedKph: Double = 0.0,
    val cameraAngle: String = "Main Broadcast",
    val aiAnalysis: String = "",
    val bowlerJersey: String = "",
    val batsmanJersey: String = "",
    val bowlerStyle: String = "",
    val batsmanStyle: String = "",
    val playerRecognitionDetails: String = "",
    val pitchLocation: String = "",
    val spinType: String = "",
    val trajectoryDetail: String = "",
    val shotType: String = "",
    val shotClassificationDetail: String = ""
) : Serializable {
    val ballLabel: String
        get() = "$overNumber.$ballNumber"
}
