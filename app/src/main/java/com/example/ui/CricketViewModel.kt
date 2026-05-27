package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CricketRepository
import com.example.data.DeliveryClip
import com.example.data.MatchSession
import com.example.utils.AIVideoSegmenter
import com.example.utils.VideoTrimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CricketViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CricketRepository
    
    val allSessions: StateFlow<List<MatchSession>>

    private val _selectedSession = MutableStateFlow<MatchSession?>(null)
    val selectedSession: StateFlow<MatchSession?> = _selectedSession.asStateFlow()

    private val _sessionClips = MutableStateFlow<List<DeliveryClip>>(emptyList())
    val sessionClips: StateFlow<List<DeliveryClip>> = _sessionClips.asStateFlow()

    private val _activeClip = MutableStateFlow<DeliveryClip?>(null)
    val activeClip: StateFlow<DeliveryClip?> = _activeClip.asStateFlow()

    // Processing flags
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage: StateFlow<String> = _progressMessage.asStateFlow()

    private val _trimProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val trimProgress: StateFlow<Float> = _trimProgress.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CricketRepository(database)
        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed demo data if database is brand new, so app starts with content
        viewModelScope.launch {
            repository.seedDemoDataIfEmpty()
        }
    }

    fun selectSession(session: MatchSession?) {
        _selectedSession.value = session
        _activeClip.value = null
        if (session != null) {
            viewModelScope.launch {
                repository.getClipsForSession(session.id).collect { clips ->
                    _sessionClips.value = clips
                    // Auto-select first clip if none is playing
                    if (_activeClip.value == null && clips.isNotEmpty()) {
                        _activeClip.value = clips.first()
                    }
                }
            }
        } else {
            _sessionClips.value = emptyList()
        }
    }

    fun selectClip(clip: DeliveryClip?) {
        _activeClip.value = clip
    }

    fun deleteSession(session: MatchSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_selectedSession.value?.id == session.id) {
                selectSession(null)
            }
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            _isProcessing.value = true
            _progressMessage.value = "Resetting and seeding pristine match video..."
            repository.reseedDemoSession()
            _isProcessing.value = false
            _progressMessage.value = ""
        }
    }

    /**
     * Spawns a brand new cricket match session, triggers Gemini frame capture, parses segments,
     * and niftily trims individual MP4 delivery files using native MediaMuxer threads.
     */
    fun processNewVideoFile(title: String, videoPath: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progressMessage.value = "Initializing match workspace..."
                _trimProgress.value = 0.0f

                val newSession = MatchSession(
                    title = title,
                    sourceVideoPath = videoPath,
                    durationMs = 600000L, // 10 minutes default
                    status = "PROCESSING"
                )
                val sessionId = repository.insertSession(newSession).toInt()
                val savedSession = repository.getSessionById(sessionId) ?: newSession.copy(id = sessionId)
                _selectedSession.value = savedSession

                // 1. Run Gemini/Simulation Video Segmentation
                val rawClips = AIVideoSegmenter.segmentCricketVideo(
                    getApplication(),
                    sessionId,
                    videoPath
                ) { progress ->
                    _progressMessage.value = progress
                }

                // 2. Insert raw segment boundaries into Room first
                repository.insertClips(rawClips)

                // 3. Initiate Native MP4 clipping phase (MediaExtractor + MediaMuxer)
                _progressMessage.value = "Muxing split MP4 clips natively on disk..."
                val finalClips = VideoTrimmer.createPhysicalVideoClips(
                    getApplication(),
                    videoPath,
                    rawClips
                ) { current, total ->
                    _progressMessage.value = "Trimming delivery $current of $total to separate MP4..."
                    _trimProgress.value = current.toFloat() / total.toFloat()
                }

                // 4. Update the saved database records with the freshly pinned disk files
                _progressMessage.value = "Updating database indexes. Match segmented successfully!"
                repository.insertClips(finalClips)

                // Mark session as fully segmented
                repository.updateSession(savedSession.copy(status = "SEGMENTED"))
                _selectedSession.value = repository.getSessionById(sessionId)

                // Select first clip for playback
                val reloadedClips = repository.getClipsForSessionDirect(sessionId)
                if (reloadedClips.isNotEmpty()) {
                    _activeClip.value = reloadedClips.first()
                }

            } catch (e: Exception) {
                _progressMessage.value = "Segmentation Error: ${e.message}"
            } finally {
                _isProcessing.value = false
                _trimProgress.value = 0.0f
            }
        }
    }

    /**
     * Allows manual adjustments to delivery parameters and saves coordinates back into Room database.
     */
    fun updateClipTiming(clip: DeliveryClip, newStartMs: Long, newEndMs: Long) {
        viewModelScope.launch {
            val updatedClip = clip.copy(
                startTimeMs = newStartMs,
                endTimeMs = newEndMs,
                aiAnalysis = clip.aiAnalysis + " (Manually refined boundaries)"
            )
            repository.updateClip(updatedClip)
            if (_activeClip.value?.id == clip.id) {
                _activeClip.value = updatedClip
            }
        }
    }

    fun updateClipMetadata(
        clip: DeliveryClip,
        outcome: String,
        eventType: String,
        bowlerName: String,
        batsmanName: String,
        speedKph: Double,
        bowlerJersey: String,
        batsmanJersey: String,
        bowlerStyle: String,
        batsmanStyle: String,
        playerRecognitionDetails: String,
        pitchLocation: String,
        spinType: String,
        trajectoryDetail: String,
        shotType: String,
        shotClassificationDetail: String
    ) {
        viewModelScope.launch {
            val updatedClip = clip.copy(
                outcome = outcome,
                eventType = eventType,
                bowlerName = bowlerName,
                batsmanName = batsmanName,
                speedKph = speedKph,
                bowlerJersey = bowlerJersey,
                batsmanJersey = batsmanJersey,
                bowlerStyle = bowlerStyle,
                batsmanStyle = batsmanStyle,
                playerRecognitionDetails = playerRecognitionDetails,
                pitchLocation = pitchLocation,
                spinType = spinType,
                trajectoryDetail = trajectoryDetail,
                shotType = shotType,
                shotClassificationDetail = shotClassificationDetail
            )
            repository.updateClip(updatedClip)
            if (_activeClip.value?.id == clip.id) {
                _activeClip.value = updatedClip
            }
        }
    }

    /**
     * Start a simulated live match stream session.
     */
    fun startLiveStream(title: String, streamUrl: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progressMessage.value = "Establishing satellite connection to live stream feed..."
                
                val liveSession = MatchSession(
                    title = title,
                    sourceVideoPath = streamUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" },
                    durationMs = 0L, // live continuous feedback
                    status = "LIVE"
                )
                val sessionId = repository.insertSession(liveSession).toInt()
                val savedSession = repository.getSessionById(sessionId) ?: liveSession.copy(id = sessionId)
                _selectedSession.value = savedSession
                _sessionClips.value = emptyList()
                _activeClip.value = null
            } catch (e: Exception) {
                _progressMessage.value = "Failed to start live stream: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Generates a new delivery clip ball-by-ball for live streams.
     */
    fun generateLiveBallClip(
        sessionId: Int,
        overNumber: Int,
        ballNumber: Int,
        eventType: String,
        outcome: String,
        bowlerName: String,
        batsmanName: String,
        speedKph: Double,
        bowlerJersey: String = "",
        batsmanJersey: String = "",
        bowlerStyle: String = "",
        batsmanStyle: String = "",
        playerRecognitionDetails: String = "",
        pitchLocation: String = "",
        spinType: String = "",
        trajectoryDetail: String = "",
        shotType: String = "",
        shotClassificationDetail: String = ""
    ) {
        viewModelScope.launch {
            val newClip = DeliveryClip(
                sessionId = sessionId,
                overNumber = overNumber,
                ballNumber = ballNumber,
                startTimeMs = System.currentTimeMillis() - 10000L, // last 10s
                endTimeMs = System.currentTimeMillis(),
                clipPath = "", // plays master stream
                eventType = eventType,
                outcome = outcome,
                bowlerName = bowlerName,
                batsmanName = batsmanName,
                speedKph = speedKph,
                cameraAngle = listOf("Main Broadcast", "Behind Bowler's Arm", "LBW Tracker Camera", "Stump Cam View").random(),
                aiAnalysis = "Live session ball detection analysis. Over $overNumber.$ballNumber. Trajectory tracker lock achieved.",
                bowlerJersey = bowlerJersey,
                batsmanJersey = batsmanJersey,
                bowlerStyle = bowlerStyle,
                batsmanStyle = batsmanStyle,
                playerRecognitionDetails = playerRecognitionDetails,
                pitchLocation = pitchLocation,
                spinType = spinType,
                trajectoryDetail = trajectoryDetail,
                shotType = shotType,
                shotClassificationDetail = shotClassificationDetail
            )
            repository.insertClips(listOf(newClip))
            
            // Refresh
            val clips = repository.getClipsForSessionDirect(sessionId)
            _sessionClips.value = clips
            _activeClip.value = newClip
        }
    }

    /**
     * Copies selected video Uri to local application safe cache and spawns full segmentation.
     */
    fun copyUriToLocalFileAndProcess(title: String, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _progressMessage.value = "Copying media stream to high-speed local sandbox..."
                
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver
                
                val fileName = "uploaded_cricket_${System.currentTimeMillis()}.mp4"
                val targetFile = java.io.File(context.cacheDir, fileName)
                
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        java.io.FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                
                _progressMessage.value = "Local import complete! Spawning AI segmenter..."
                processNewVideoFile(title, targetFile.absolutePath)
            } catch (e: Exception) {
                _progressMessage.value = "Import Error: ${e.message}"
                _isProcessing.value = false
            }
        }
    }
}
