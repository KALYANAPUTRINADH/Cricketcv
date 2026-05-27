package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class CricketRepository(private val database: AppDatabase) {
    val matchDao = database.matchDao()
    val deliveryDao = database.deliveryDao()

    val allSessions: Flow<List<MatchSession>> = matchDao.getAllSessionsFlow()

    fun getClipsForSession(sessionId: Int): Flow<List<DeliveryClip>> {
        return deliveryDao.getClipsForSessionFlow(sessionId)
    }

    suspend fun getClipsForSessionDirect(sessionId: Int): List<DeliveryClip> {
        return deliveryDao.getClipsForSessionDirect(sessionId)
    }

    suspend fun getSessionById(id: Int): MatchSession? {
        return matchDao.getSessionById(id)
    }

    suspend fun insertSession(session: MatchSession): Long {
        return matchDao.insertSession(session)
    }

    suspend fun updateSession(session: MatchSession) {
        matchDao.updateSession(session)
    }

    suspend fun deleteSession(session: MatchSession) {
        deliveryDao.deleteClipsForSession(session.id)
        matchDao.deleteSession(session)
    }

    suspend fun insertClip(clip: DeliveryClip): Long {
        return deliveryDao.insertClip(clip)
    }

    suspend fun insertClips(clips: List<DeliveryClip>) {
        deliveryDao.insertClips(clips)
    }

    suspend fun updateClip(clip: DeliveryClip) {
        deliveryDao.updateClip(clip)
    }

    suspend fun deleteClip(clip: DeliveryClip) {
        deliveryDao.deleteClip(clip)
    }

    suspend fun reseedDemoSession() {
        // Clear previous demo sessions if any, then insert
        val existing = matchDao.getAllSessionsFlow().firstOrNull() ?: emptyList()
        val demoSession = existing.find { it.isDemo }
        if (demoSession != null) {
            deleteSession(demoSession)
        }
        createAndSeedDemoSession()
    }

    suspend fun seedDemoDataIfEmpty() {
        val sessions = matchDao.getAllSessionsFlow().firstOrNull() ?: emptyList()
        if (sessions.isEmpty()) {
            createAndSeedDemoSession()
        }
    }

    private suspend fun createAndSeedDemoSession() {
        // Create a demo session referencing a high quality public MP4 streaming video or standard stream
        val demoMatch = MatchSession(
            title = "IND vs AUS ICC Championship Final (Over 1)",
            sourceVideoPath = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", // Reliable fallback video stream
            durationMs = 600000L, // 10 minutes
            isDemo = true,
            status = "SEGMENTED"
        )
        val sessionId = matchDao.insertSession(demoMatch).toInt()

        // Create 6 realistic deliveries comprising Over 1
        val clips = listOf(
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 1,
                startTimeMs = 5000L,
                endTimeMs = 25000L,
                clipPath = "", // Empty triggers virtual/master playback offset helper in UI
                eventType = "DOT",
                outcome = "Dot Ball (Solid Defense)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 143.5,
                cameraAngle = "Main Broadcast",
                aiAnalysis = "Good length delivery on off-stump. Rohit Sharma gets forward and plays a soft-handed defensive stroke to short mid-off. Fast, clean release detected at 5.2s. Bowler run-up start at 0s.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc (Jersey #56) detected via back-arch stance tracking. Rohit Sharma (Jersey #45) recognized with a low-crouch guard posture.",
                pitchLocation = "Good Length",
                spinType = "None (Fast seam)",
                trajectoryDetail = "Speed: 143.5 Kph. Pitched 6.1 meters from batsman, climbing 1.25 meters. Kept an upright seam angle with zero lateral drift.",
                shotType = "Defense",
                shotClassificationDetail = "Classic forward defensive block. Soft hands, minimal wrist rotation, bat and pad close together with face pointing to the floor."
            ),
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 2,
                startTimeMs = 35000L,
                endTimeMs = 55000L,
                clipPath = "",
                eventType = "RUNS",
                outcome = "2 Runs (Flapjacks to Square Leg)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 145.2,
                cameraAngle = "Main Broadcast",
                aiAnalysis = "Full-length delivery drifting onto the pads. Rohit Sharma clips it off the toes through square leg. Ground camera angle track shows deep fielder sliding to save two runs. Speed tracking: 145 Kph.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc #56 detected on release follow-through. Sharma #45 identified shifting weight smoothly to the front foot.",
                pitchLocation = "Full Pitch",
                spinType = "Inswing",
                trajectoryDetail = "Speed: 145.2 Kph. Pitched 4.2 meters from batsman. Curved inside late, dipping 18cm left-to-right off the seam.",
                shotType = "Flick",
                shotClassificationDetail = "Closed-face wristy flick. Swept elegantly off the middle-and-leg stump with quick wrist roll at impact."
            ),
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 3,
                startTimeMs = 65000L,
                endTimeMs = 90000L,
                clipPath = "",
                eventType = "FOUR",
                outcome = "4 Runs (Gorgeous Cover Drive)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 141.8,
                cameraAngle = "Behind Bowler's Arm",
                aiAnalysis = "Overpitched outside off. Rohit Sharma lean-in elegantly, high elbow, creams it through extra cover. Beautiful sound off the bat. Fielders don't even move. Clear boundary track verified automatically.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc #56 tracked with slightly wider release angle at crease. Sharma #45 identified using high backlift signifiers.",
                pitchLocation = "Full Pitch",
                spinType = "Outswing",
                trajectoryDetail = "Speed: 141.8 Kph. Pitched 3.8 meters from batsman. Devised 11cm lateral swing away from the right-hander.",
                shotType = "Cover Drive",
                shotClassificationDetail = "Impeccable cover drive. High front elbow, shoulder leaned in, full-blooded follow-through, striking right at the sweet-spot."
            ),
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 4,
                startTimeMs = 100000L,
                endTimeMs = 120000L,
                clipPath = "",
                eventType = "DOT",
                outcome = "Dot Ball (Play and Miss)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 147.1,
                cameraAngle = "Main Broadcast",
                aiAnalysis = "Back of length delivery, bouncing steeply. Sharma attempts a loose cut but gets beaten by the extra bounce. Safe carry to keeper Carey. Keyframe track shows Rohit shook his head. Excellent response from Starc.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc #56 detected leaping high in delivery leap. Sharma #45 detected opening up hips prematurely.",
                pitchLocation = "Short Pitch",
                spinType = "None (Fast seam)",
                trajectoryDetail = "Speed: 147.1 Kph. Pitched 8.2 meters from batsman, rising sharply up to 1.78 meters over the waist height.",
                shotType = "No Shot",
                shotClassificationDetail = "Uncommitted leave. Batsman originally motioned a cut, then quickly retracted hands, raising the bat high above shoulders."
            ),
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 5,
                startTimeMs = 130000L,
                endTimeMs = 158000L,
                clipPath = "",
                eventType = "SIX",
                outcome = "6 Runs (Massive Pull Shot)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 139.4,
                cameraAngle = "High Stand Angle",
                aiAnalysis = "Short delivery down the leg side, Rohit Sharma gets into position early and pulls it majestically over the deep backward square leg boundary for wood-cracking 6! Crowds goes ecstatic.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc #56 detected pulling back release speed. Sharma #45 identified with quick swivel on back foot.",
                pitchLocation = "Short Pitch",
                spinType = "None (Fast seam)",
                trajectoryDetail = "Speed: 139.4 Kph. Pitched 7.5 meters from batsman. Sat perfectly in the batsman's pull zone, waist-high bounce height of 1.15 meters.",
                shotType = "Pull Shot",
                shotClassificationDetail = "Elite horizontal bat pull shot. Transferred weight completely to the back leg, swinging with a flat plane and high exit velocity."
            ),
            DeliveryClip(
                sessionId = sessionId,
                overNumber = 1,
                ballNumber = 6,
                startTimeMs = 168000L,
                endTimeMs = 210000L,
                clipPath = "",
                eventType = "WICKET",
                outcome = "Wicket (Clean Bowled!)",
                bowlerName = "Mitchell Starc",
                batsmanName = "Rohit Sharma",
                speedKph = 149.3,
                cameraAngle = "Stump Cam View",
                aiAnalysis = "OUT! Thunderbolt! 149 Kph inswinging yorker crashes into the middle stump. Rohit plays all over it. Middle stump knocked flat. Starc throws his arms in celebration. Replay confirms perfect delivery seam angle.",
                bowlerJersey = "56",
                batsmanJersey = "45",
                bowlerStyle = "Left-arm Fast",
                batsmanStyle = "Right-hand Bat",
                playerRecognitionDetails = "Starc #56 detected with maximum arm speed. Sharma #45 identified with late leg trigger, leaving stump gap.",
                pitchLocation = "Yorker",
                spinType = "Inswing",
                trajectoryDetail = "Speed: 149.3 Kph. Pitched 1.2 meters in front of stumps. Dipped and swung brutally in mid-air, sweeping 24cm inland.",
                shotType = "Defense",
                shotClassificationDetail = "Failed defensive block. Bat came down crookedly, playing around the pad, resulting in a clean gap between bat and pad."
            )
        )
        deliveryDao.insertClips(clips)
    }
}
