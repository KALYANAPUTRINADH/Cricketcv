package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.data.DeliveryClip
import com.example.data.MatchSession

// Professional Polish Color Palettes styled specifically for Sports Analytics
val CosmicBg = Color(0xFFF3F5F7)        // Smooth Light Gray background
val DarkSurface = Color(0xFFFFFFFF)     // High-contrast clean white surface
val DarkCard = Color(0xFFFFFFFF)        // Structured clean white card container
val NeonGreen = Color(0xFF10B981)       // Green boundary / Dot indications
val DeepRed = Color(0xFFEF4444)         // Wicket red indications
val CrispBlue = Color(0xFF2563EB)       // Stadium primary blue accent
val CoralAccent = Color(0xFFF59E0B)      // Amber alerts / Speed tracking
val BorderColor = Color(0xFFE2E8F0)      // Slate-200 light thin borders

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CricketSegmenterApp(
    viewModel: CricketViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val trimProgress by viewModel.trimProgress.collectAsState()

    val sessions by viewModel.allSessions.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val clips by viewModel.sessionClips.collectAsState()
    val activeClip by viewModel.activeClip.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editMetadataClip by remember { mutableStateOf<DeliveryClip?>(null) }

    // Upload & Live Stream state declarations
    var selectedLocalVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isLiveAutoGenerating by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLocalVideoUri = uri
        }
    }

    Surface(
        color = CosmicBg,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp) // padding safe areas handled manually
        ) {
            // Header Top Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Blue-600 rounded logo container
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "CricSeg Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "CricSeg AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B) // slate-800
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF10B981), RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PROCESSING LIVE",
                                    color = Color(0xFF64748B), // slate-500
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.resetDemoData() },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFFF1F5F9), // slate-100
                                contentColor = Color(0xFF475569)  // slate-600
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh, 
                                contentDescription = "Reset/Reseed Demo Match",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB), // Primary Blue
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add match video", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze Stream", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Central Progress overlay for video uploads & analyzing
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "AI Segmenter Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        if (trimProgress > 0f) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { trimProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }

            // Main Body Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // section 1: Match selection list
                item {
                    Text(
                        text = "Match Sessions Directory",
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No match sessions available", color = Color(0xFF64748B))
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sessions) { session ->
                                val isSelected = selectedSession?.id == session.id
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .clickable { viewModel.selectSession(session) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else DarkSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else BorderColor
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (session.isDemo) "DEMO TAPE" else "NEW STREAM",
                                                color = if (session.isDemo) CoralAccent else Color(0xFF10B981),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            IconButton(
                                                onClick = { viewModel.deleteSession(session) },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Session",
                                                    tint = Color(0xFF94A3B8), // slate-400
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = session.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF1E293B),
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (session.status == "SEGMENTED") "6 Balls Configured" else "Processing Raw Feed",
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Player & Stats Segment
                selectedSession?.let { session ->
                    // Video Player Module
                    item {
                        Text(
                            text = "Primary Frame Playback",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (session.status == "LIVE") {
                            // Professional LIVE Continuous Player
                            VideoPlayerView(
                                videoUrl = session.sourceVideoPath,
                                startTimeMs = 0L,
                                endTimeMs = 0L,
                                isClipMode = false,
                                modifier = Modifier.clip(RoundedCornerShape(16.dp))
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // LIVE OVERVIEW & CONTROL ROOM CONSOLE
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep stylish charcoal
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "● LIVE FEED",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Continuous Broadcast Stream",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Status Pulse indication
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFF22C55E), RoundedCornerShape(4.dp))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "LIVE BALL-BY-BALL CONTROL CONSOLE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF3B82F6),
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Either automate detection telemetry using our simulated background thread or manually mark frame transitions on-the-fly.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Auto Detection Toggle
                                        Button(
                                            onClick = { isLiveAutoGenerating = !isLiveAutoGenerating },
                                            modifier = Modifier.weight(1.2f).height(42.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isLiveAutoGenerating) Color(0xFF10B981) else Color(0xFF334155),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isLiveAutoGenerating) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                                    contentDescription = "Auto",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isLiveAutoGenerating) "Auto-Detecting" else "Auto-Detect Balls",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Manual Trigger
                                        Button(
                                            onClick = { showManualAddDialog = true },
                                            modifier = Modifier.weight(1.2f).height(42.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF2563EB),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Manual",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Trigger Ball Clip",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // If a clip is selected and active in Live mode, showcase its live-metrics panel as overlay!
                            activeClip?.let { liveClip ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "SELECTED LIVE CLIP TELEMETRY",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(CrispBlue, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "BALL ${liveClip.overNumber}.${liveClip.ballNumber}",
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = liveClip.outcome,
                                                    color = Color(0xFF0F172A),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            IconButton(
                                                onClick = { editMetadataClip = liveClip },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit live metadata",
                                                    tint = CrispBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            activeClip?.let { clip ->
                                // Custom integrated Video Player
                                VideoPlayerView(
                                    videoUrl = clip.clipPath,
                                    startTimeMs = clip.startTimeMs,
                                    endTimeMs = clip.endTimeMs,
                                    isClipMode = true,
                                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                                )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action and telemetry details of the currently selected clip boundary
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkCard),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val colorForEvent = when (clip.eventType) {
                                                "WICKET" -> DeepRed
                                                "FOUR", "SIX" -> NeonGreen
                                                "RUNS" -> CrispBlue
                                                else -> Color(0xFF64748B)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(colorForEvent, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "BALL ${clip.overNumber}.${clip.ballNumber}",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = clip.eventType,
                                                    color = Color(0xFF475569),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Edit metadata toggle
                                        Row {
                                            IconButton(onClick = { editMetadataClip = clip }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Clip Details", tint = Color(0xFF64748B))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = clip.outcome,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // BRAND NEW COMPREHENSIVE SPORTS METADATA ANALYTICS PANEL
                                    Text(
                                        text = "PLAYER DETECTION & RECOGNITION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrispBlue,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                     
                                    // Bowler & Batsman Profile Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Bowler Card
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                             border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                     modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("BOWLER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                                    if (clip.bowlerJersey.isNotEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("#${clip.bowlerJersey}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(clip.bowlerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                if (clip.bowlerStyle.isNotEmpty()) {
                                                    Text(clip.bowlerStyle, fontSize = 10.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                         
                                        // Batsman Card
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            border = BorderStroke(1.dp, BorderColor)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("BATSMAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                                    if (clip.batsmanJersey.isNotEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("#${clip.batsmanJersey}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(clip.batsmanName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                if (clip.batsmanStyle.isNotEmpty()) {
                                                    Text(clip.batsmanStyle, fontSize = 10.sp, color = Color(0xFF64748B))
                                                }
                                            }
                                        }
                                    }
                                     
                                    if (clip.playerRecognitionDetails.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                                 .padding(8.dp)
                                         ) {
                                             Row(verticalAlignment = Alignment.Top) {
                                                 Icon(
                                                     imageVector = Icons.Default.Search,
                                                     contentDescription = "Recognition Icon",
                                                     tint = CrispBlue,
                                                     modifier = Modifier.size(14.dp).padding(top = 1.dp)
                                                 )
                                                 Spacer(modifier = Modifier.width(6.dp))
                                                 Text(
                                                     text = "CV Recognition: ${clip.playerRecognitionDetails}",
                                                     fontSize = 11.sp,
                                                     color = Color(0xFF1E40AF),
                                                     lineHeight = 15.sp
                                                 )
                                             }
                                         }
                                    }
                                     
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "BALL TRAJECTORY ANALYSIS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralAccent,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                     
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.spacedBy(6.dp)
                                             ) {
                                                 // Speed Badge
                                                 Box(
                                                     modifier = Modifier
                                                         .background(Color(0xFFFFF7ED), RoundedCornerShape(6.dp))
                                                         .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(6.dp))
                                                         .padding(horizontal = 8.dp, vertical = 4.dp)
                                                 ) {
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(Icons.Default.Star, contentDescription = "Speed", tint = CoralAccent, modifier = Modifier.size(11.dp))
                                                         Spacer(modifier = Modifier.width(4.dp))
                                                         Text("${clip.speedKph} Kph", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                                     }
                                                 }
                                                 
                                                 // Pitch Location Badge
                                                 if (clip.pitchLocation.isNotEmpty()) {
                                                     Box(
                                                         modifier = Modifier
                                                             .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                                             .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                                             .padding(horizontal = 8.dp, vertical = 4.dp)
                                                     ) {
                                                         Text(clip.pitchLocation, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                                     }
                                                 }
                                                 
                                                 // Spin/Seam Badge
                                                 if (clip.spinType.isNotEmpty()) {
                                                     Box(
                                                         modifier = Modifier
                                                             .background(Color(0xFFF5F3FF), RoundedCornerShape(6.dp))
                                                             .border(1.dp, Color(0xFFEDE9FE), RoundedCornerShape(6.dp))
                                                             .padding(horizontal = 8.dp, vertical = 4.dp)
                                                     ) {
                                                         Text(clip.spinType, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                                     }
                                                 }
                                             }
                                             
                                             if (clip.trajectoryDetail.isNotEmpty()) {
                                                 Spacer(modifier = Modifier.height(8.dp))
                                                 Text(
                                                     text = clip.trajectoryDetail,
                                                     fontSize = 11.sp,
                                                     color = Color(0xFF475569),
                                                     lineHeight = 15.sp
                                                 )
                                             }
                                        }
                                    }
                                     
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "BATSMAN SHOT CLASSIFICATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                     
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 modifier = Modifier.fillMaxWidth()
                                             ) {
                                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                                     Box(
                                                         modifier = Modifier
                                                             .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                                             .padding(horizontal = 10.dp, vertical = 4.dp)
                                                     ) {
                                                         Text(
                                                             text = clip.shotType.ifEmpty { "Unclassified Action" },
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.ExtraBold,
                                                             color = Color(0xFF065F46)
                                                         )
                                                     }
                                                 }
                                                 Text("Camera Angle: ${clip.cameraAngle}", fontSize = 10.sp, color = Color(0xFF64748B))
                                             }
                                             
                                             if (clip.shotClassificationDetail.isNotEmpty()) {
                                                 Spacer(modifier = Modifier.height(8.dp))
                                                 Text(
                                                     text = clip.shotClassificationDetail,
                                                     fontSize = 11.sp,
                                                     color = Color(0xFF475569),
                                                     lineHeight = 15.sp
                                                 )
                                             }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(12.dp))
                                     
                                    Row {
                                        Icon(Icons.Default.Info, contentDescription = "AI Notes Logo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Gemini Video Analysis Commentary:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = clip.aiAnalysis,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 16.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // TIMELINE ADJUSTER PANEL (MANUAL OVERRIDE / FINE TUNING)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)), // elegant inset background
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Manual Delivery Boundary Tuning",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Icon(Icons.Default.Settings, contentDescription = "Fine adjusting icon", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Start range adjustment
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Release (Start): ${clip.startTimeMs / 1000f}s", fontSize = 11.sp, color = Color(0xFF475569))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, (clip.startTimeMs - 1000).coerceAtLeast(0L), clip.endTimeMs) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("-1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, (clip.startTimeMs - 100).coerceAtLeast(0L), clip.endTimeMs) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("-0.1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, (clip.startTimeMs + 100).coerceAtMost(clip.endTimeMs - 500L), clip.endTimeMs) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("+0.1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, (clip.startTimeMs + 1000).coerceAtMost(clip.endTimeMs - 500L), clip.endTimeMs) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("+1s", fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                                Slider(
                                                    value = clip.startTimeMs.toFloat(),
                                                    onValueChange = { viewModel.updateClipTiming(clip, it.toLong(), clip.endTimeMs) },
                                                    valueRange = 0f..(clip.endTimeMs - 500L).toFloat(),
                                                    modifier = Modifier.fillMaxWidth().height(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // End range adjustment
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Dead Ball (End): ${clip.endTimeMs / 1000f}s", fontSize = 11.sp, color = Color(0xFF475569))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, clip.startTimeMs, (clip.endTimeMs - 1000).coerceAtLeast(clip.startTimeMs + 500L)) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("-1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, clip.startTimeMs, (clip.endTimeMs - 100).coerceAtLeast(clip.startTimeMs + 500L)) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("-0.1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, clip.startTimeMs, clip.endTimeMs + 100L) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("+0.1s", fontSize = 10.sp)
                                                        }
                                                        TextButton(
                                                            onClick = { viewModel.updateClipTiming(clip, clip.startTimeMs, clip.endTimeMs + 1000L) },
                                                            contentPadding = PaddingValues(0.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        ) {
                                                            Text("+1s", fontSize = 10.sp)
                                                        }
                                                    }
                                                }
                                                Slider(
                                                    value = clip.endTimeMs.toFloat(),
                                                    onValueChange = { viewModel.updateClipTiming(clip, clip.startTimeMs, it.toLong()) },
                                                    valueRange = (clip.startTimeMs + 500L).toFloat()..250000f,
                                                    modifier = Modifier.fillMaxWidth().height(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(DarkSurface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No ball clips detected in session workspace.", color = Color.Gray)
                        }
                        }
                    }

                    // Section 3: Overs Grid selection module
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Over 1 Delivery Grid",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (clips.isEmpty()) {
                            Text("Awaiting stream data segmentation...", color = Color(0xFF64748B))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                clips.forEach { clip ->
                                    val isActive = activeClip?.id == clip.id
                                    val btnColor = when (clip.eventType) {
                                        "WICKET" -> DeepRed
                                        "FOUR", "SIX" -> NeonGreen
                                        "RUNS" -> CrispBlue
                                        else -> Color(0xFF64748B)
                                    }

                                    Button(
                                        onClick = { viewModel.selectClip(clip) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(55.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isActive) btnColor else Color(0xFFF1F5F9),
                                            contentColor = if (isActive) Color.White else Color(0xFF1E293B)
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isActive) btnColor else BorderColor
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${clip.overNumber}.${clip.ballNumber}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = when (clip.eventType) {
                                                    "WICKET" -> "W"
                                                    "FOUR" -> "4"
                                                    "SIX" -> "6"
                                                    "RUNS" -> "R"
                                                    else -> "•"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) Color.White else btnColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Mini Stats Dashboard Card
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Over Telemetry Report",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TOTAL BALLS", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("${clips.size}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                    VerticalDivider()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("WICKETS", fontSize = 10.sp, color = DeepRed, fontWeight = FontWeight.Bold)
                                        Text("${clips.count { it.eventType == "WICKET" }}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                    VerticalDivider()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("FOURS/SIXES", fontSize = 10.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                                        Text("${clips.count { it.eventType == "FOUR" || it.eventType == "SIX" }}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                    VerticalDivider()
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("AVG SPEED", fontSize = 10.sp, color = CoralAccent, fontWeight = FontWeight.Bold)
                                        val avgSpeed = if(clips.isNotEmpty()) clips.map { it.speedKph }.average() else 0.0
                                        Text(String.format("%.1f", avgSpeed) + " Kph", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue to segment and add a custom match
    if (showAddDialog) {
        var matchTitle by remember { mutableStateOf("") }
        var videoUrl by remember { mutableStateOf("") }
        var activeTab by remember { mutableStateOf(0) } // 0: Presets, 1: Local Upload, 2: Live Stream

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Match Video or Live Stream", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            containerColor = Color.White,
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Custom Capsule Tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        listOf("Presets / Url", "Local Upload", "Live Stream").forEachIndexed { index, label ->
                            val selected = activeTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { activeTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = matchTitle,
                        onValueChange = { matchTitle = it },
                        label = { Text("Match Event Title") },
                        placeholder = { Text("e.g. India vs Australia Live Telemetry") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (activeTab) {
                        0 -> { // Presets / Url
                            OutlinedTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("Video Feed URL / Path") },
                                placeholder = { Text("https://commondatastorage.googleapis.com/.../sintel.mp4") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF1E293B)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Select a Video Feed Dataset Preset:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    Triple("IND vs ENG T20 Death Bats", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "T20 Heavy Hitting Over"),
                                    Triple("Aus Domestic Shield Classic", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "Inswinging Pace Attack Over")
                                ).forEach { preset ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                matchTitle = preset.first
                                                videoUrl = preset.second
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, BorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Preset Broadcast Source icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(preset.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                                Text(preset.third, fontSize = 9.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Local Device Upload
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { videoPickerLauncher.launch("video/*") },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, if (selectedLocalVideoUri != null) Color(0xFF10B981) else BorderColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedLocalVideoUri != null) Icons.Default.CheckCircle else Icons.Default.Share,
                                        contentDescription = "Upload video",
                                        tint = if (selectedLocalVideoUri != null) Color(0xFF10B981) else Color(0xFF64748B),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (selectedLocalVideoUri != null) "Video Selected Successfully!" else "Touch to explorer device files",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedLocalVideoUri != null) Color(0xFF047857) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (selectedLocalVideoUri != null) selectedLocalVideoUri!!.path ?: "Local file ready" else "Accepts local .mp4, .mkv, .avi clips",
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                        2 -> { // Interactive Live Stream
                            OutlinedTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("Stream HLS / RTMP Playback URL") },
                                placeholder = { Text("e.g. http://feeds.test.com/broadcasting.m3u8") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF1E293B)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("PRO LIVE TRACKER CONSOLE ENABLED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "By launching in Live Stream Mode, you can inspect the continuous feed stream and manually OR automatically trigger real-time ball-by-ball analysis telemetry blocks.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF1E3A8A),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = matchTitle.ifEmpty {
                            when (activeTab) {
                                1 -> "Uploaded Device Match"
                                2 -> "Live Stream Broadcast"
                                else -> "Custom Segmenter Feed"
                            }
                        }

                        when (activeTab) {
                            0 -> { // Presets / Url
                                val url = videoUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }
                                viewModel.processNewVideoFile(title, url)
                            }
                            1 -> { // Local Upload
                                selectedLocalVideoUri?.let { uri ->
                                    viewModel.copyUriToLocalFileAndProcess(title, uri)
                                }
                            }
                            2 -> { // Live Stream
                                val url = videoUrl.ifEmpty { "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" }
                                viewModel.startLiveStream(title, url)
                            }
                        }
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    enabled = when (activeTab) {
                        1 -> selectedLocalVideoUri != null
                        else -> true
                    }
                ) {
                    Text(
                        text = when (activeTab) {
                            2 -> "Launch Live Room"
                            else -> "Start Segmentation"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Interactive Auto-Generator Ticker for active streams
    if (selectedSession?.status == "LIVE" && isLiveAutoGenerating) {
        LaunchedEffect(clips.size, isLiveAutoGenerating) {
            kotlinx.coroutines.delay(10000) // 10 seconds per new delivery
            val currentSession = selectedSession
            if (currentSession != null && isLiveAutoGenerating) {
                val nextOverNum = (clips.size / 6) + 1
                val nextBallNum = (clips.size % 6) + 1
                val eventTypes = listOf("DOT", "RUNS", "FOUR", "SIX", "WICKET")
                val selectedType = eventTypes.random()
                val outcomes = when (selectedType) {
                    "WICKET" -> listOf("Clean Bowled", "Caught at Mid On", "LBW Appeal Confirmed", "Run Out by Keeper").random()
                    "FOUR" -> listOf("Superb Cover Drive for 4", "Nifty Sweep Shot through Fine Leg", "Edge boundaries past Slip").random()
                    "SIX" -> listOf("Massive Pull Shot over Deep Midwicket", "Straight Drive clearing the sight screen").random()
                    "RUNS" -> listOf("Quick Single to Mid Off", "Double taken through Deep Point", "Tuck to Square Leg for 1").random()
                    else -> listOf("Defended back to bowler", "Left alone outside off stump", "Beaten by the swing").random()
                }
                val bowlerNames = listOf("J. Bumrah", "M. Starc", "P. Cummins", "R. Ashwin", "J. Anderson")
                val batsmanNames = listOf("V. Kohli", "S. Smith", "R. Sharma", "J. Root", "K. Williamson")

                viewModel.generateLiveBallClip(
                    sessionId = currentSession.id,
                    overNumber = nextOverNum,
                    ballNumber = nextBallNum,
                    eventType = selectedType,
                    outcome = outcomes,
                    bowlerName = bowlerNames.random(),
                    batsmanName = batsmanNames.random(),
                    speedKph = (125..152).random().toDouble(),
                    bowlerJersey = (1..99).random().toString(),
                    batsmanJersey = (1..99).random().toString(),
                    bowlerStyle = listOf("Fast Delivery", "Right Arm Leg Break", "Left Arm Orth").random(),
                    batsmanStyle = listOf("RHB (Orthodox)", "LHB (Aggressive)").random(),
                    playerRecognitionDetails = "Real-time AI telemetry locked on uniform tracking.",
                    pitchLocation = listOf("Good Length", "Full Pitch", "Short Bouncer", "Yorker Zone").random(),
                    spinType = listOf("Inswing", "Outswing", "Leg Spin", "Off-Spin").random(),
                    trajectoryDetail = "Ball arc tracked fully from hand-release to bounce.",
                    shotType = listOf("Cover Drive", "Pull Shot", "Sweep Shot", "Backward Defense").random(),
                    shotClassificationDetail = "High-precision bat-ball impact node verified."
                )
            }
        }
    }

    // Modal to generate custom ball clips manually on-demand
    if (showManualAddDialog) {
        val nextOverNum = (clips.size / 6) + 1
        val nextBallNum = (clips.size % 6) + 1

        var manualOutcome by remember { mutableStateOf("Clean Straight Drive for Four") }
        var manualBowler by remember { mutableStateOf("J. Bumrah") }
        var manualBatsman by remember { mutableStateOf("V. Kohli") }
        var manualSpeedStr by remember { mutableStateOf("142") }
        var manualEventType by remember { mutableStateOf("FOUR") }

        var manualBowlerJersey by remember { mutableStateOf("93") }
        var manualBatsmanJersey by remember { mutableStateOf("18") }
        var manualBowlerStyle by remember { mutableStateOf("Fast Bowler") }
        var manualBatsmanStyle by remember { mutableStateOf("RHB") }
        var manualPitchLocation by remember { mutableStateOf("Good Length") }
        var manualSpinType by remember { mutableStateOf("Inswing") }
        var manualTrajectoryDetail by remember { mutableStateOf("Standard swing trajectory") }
        var manualShotType by remember { mutableStateOf("Straight Drive") }
        var manualShotClassificationDetail by remember { mutableStateOf("Classic stance and middle bat punch") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Generate Custom Ball-by-Ball Clip", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            containerColor = Color.White,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Manually trigger telemetry indexing for Ball $nextOverNum.$nextBallNum.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = manualOutcome,
                        onValueChange = { manualOutcome = it },
                        label = { Text("Outcome / Commentary") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualBowler,
                            onValueChange = { manualBowler = it },
                            label = { Text("Bowler") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualBatsman,
                            onValueChange = { manualBatsman = it },
                            label = { Text("Batsman") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualBowlerJersey,
                            onValueChange = { manualBowlerJersey = it },
                            label = { Text("Bowler Jersey #") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualBatsmanJersey,
                            onValueChange = { manualBatsmanJersey = it },
                            label = { Text("Batsman Jersey #") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualBowlerStyle,
                            onValueChange = { manualBowlerStyle = it },
                            label = { Text("Bowler Style") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualBatsmanStyle,
                            onValueChange = { manualBatsmanStyle = it },
                            label = { Text("Batsman Style") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualSpeedStr,
                            onValueChange = { manualSpeedStr = it },
                            label = { Text("Speed (Kph)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualPitchLocation,
                            onValueChange = { manualPitchLocation = it },
                            label = { Text("Pitch Zone") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualSpinType,
                            onValueChange = { manualSpinType = it },
                            label = { Text("Spin/Seam Type") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualShotType,
                            onValueChange = { manualShotType = it },
                            label = { Text("Shot Played") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Event type selector (DOT, RUNS, FOUR, SIX, WICKET)
                    Text("Select Play Event Marker Category:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("DOT", "RUNS", "FOUR", "SIX", "WICKET").forEach { type ->
                            val isSelected = manualEventType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { manualEventType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val session = selectedSession
                        if (session != null) {
                            val speed = manualSpeedStr.toDoubleOrNull() ?: 135.0
                            viewModel.generateLiveBallClip(
                                sessionId = session.id,
                                overNumber = nextOverNum,
                                ballNumber = nextBallNum,
                                eventType = manualEventType,
                                outcome = manualOutcome,
                                bowlerName = manualBowler,
                                batsmanName = manualBatsman,
                                speedKph = speed,
                                bowlerJersey = manualBowlerJersey,
                                batsmanJersey = manualBatsmanJersey,
                                bowlerStyle = manualBowlerStyle,
                                batsmanStyle = manualBatsmanStyle,
                                playerRecognitionDetails = "Active target acquisition details verified manually.",
                                pitchLocation = manualPitchLocation,
                                spinType = manualSpinType,
                                trajectoryDetail = manualTrajectoryDetail,
                                shotType = manualShotType,
                                shotClassificationDetail = manualShotClassificationDetail
                            )
                        }
                        showManualAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Delivery clip", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal to refine clip metadata
    editMetadataClip?.let { clip ->
        var outcome by remember { mutableStateOf(clip.outcome) }
        var bowlerName by remember { mutableStateOf(clip.bowlerName) }
        var batsmanName by remember { mutableStateOf(clip.batsmanName) }
        var speedKphStr by remember { mutableStateOf(clip.speedKph.toString()) }
        var selectedType by remember { mutableStateOf(clip.eventType) }
        
        var bowlerJersey by remember { mutableStateOf(clip.bowlerJersey) }
        var batsmanJersey by remember { mutableStateOf(clip.batsmanJersey) }
        var bowlerStyle by remember { mutableStateOf(clip.bowlerStyle) }
        var batsmanStyle by remember { mutableStateOf(clip.batsmanStyle) }
        var playerRecognitionDetails by remember { mutableStateOf(clip.playerRecognitionDetails) }
        var pitchLocation by remember { mutableStateOf(clip.pitchLocation) }
        var spinType by remember { mutableStateOf(clip.spinType) }
        var trajectoryDetail by remember { mutableStateOf(clip.trajectoryDetail) }
        var shotType by remember { mutableStateOf(clip.shotType) }
        var shotClassificationDetail by remember { mutableStateOf(clip.shotClassificationDetail) }

        AlertDialog(
            onDismissRequest = { editMetadataClip = null },
            title = { Text("Refine Delivery Analytics", color = Color(0xFF0F172A)) },
            containerColor = Color.White,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SECTION 1: DELIVERY BASIC DETAILS
                    Text(
                        text = "DELIVERY OUTCOME & CATEGORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrispBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    OutlinedTextField(
                        value = outcome,
                        onValueChange = { outcome = it },
                        label = { Text("Ball Outcome / Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = speedKphStr,
                        onValueChange = { speedKphStr = it },
                        label = { Text("Delivery Speed (Kph)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Event category pills
                    Text("Event Category Tag:", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("DOT", "RUNS", "FOUR", "SIX", "WICKET").forEach { type ->
                            val isSelected = selectedType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else BorderColor,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = BorderColor)
                    
                    // SECTION 2: PLAYER DETECTION & RECOGNITION
                    Text(
                        text = "PLAYER DETECTION & RECOGNITION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrispBlue,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bowlerName,
                            onValueChange = { bowlerName = it },
                            label = { Text("Bowler Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = batsmanName,
                            onValueChange = { batsmanName = it },
                            label = { Text("Batsman Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bowlerJersey,
                            onValueChange = { bowlerJersey = it },
                            label = { Text("Bowler Jersey #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = batsmanJersey,
                            onValueChange = { batsmanJersey = it },
                            label = { Text("Batsman Jersey #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bowlerStyle,
                            onValueChange = { bowlerStyle = it },
                            label = { Text("Bowler Style") },
                            placeholder = { Text("e.g. LBG / Fast") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = batsmanStyle,
                            onValueChange = { batsmanStyle = it },
                            label = { Text("Batsman Style") },
                            placeholder = { Text("e.g. RHB") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = playerRecognitionDetails,
                        onValueChange = { playerRecognitionDetails = it },
                        label = { Text("Detection Cues / Description") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = BorderColor)
                    
                    // SECTION 3: BALL TRAJECTORY ANALYSIS
                    Text(
                        text = "BALL TRAJECTORY ANALYSIS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralAccent,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pitchLocation,
                            onValueChange = { pitchLocation = it },
                            label = { Text("Pitch Zone / Length") },
                            placeholder = { Text("e.g. Good Length") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = spinType,
                            onValueChange = { spinType = it },
                            label = { Text("Spin / Seam Type") },
                            placeholder = { Text("e.g. Off-Spin") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = trajectoryDetail,
                        onValueChange = { trajectoryDetail = it },
                        label = { Text("Detailed Trajectory Estimation") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = BorderColor)
                    
                    // SECTION 4: SHOT SELECTION CLASSIFIER
                    Text(
                        text = "SHOT TYPE CLASSIFICATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    OutlinedTextField(
                        value = shotType,
                        onValueChange = { shotType = it },
                        label = { Text("Shot Played Type") },
                        placeholder = { Text("e.g. Cover Drive") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shotClassificationDetail,
                        onValueChange = { shotClassificationDetail = it },
                        label = { Text("Swing & Stance Analytics Detail") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val speed = speedKphStr.toDoubleOrNull() ?: clip.speedKph
                        viewModel.updateClipMetadata(
                            clip = clip,
                            outcome = outcome,
                            eventType = selectedType,
                            bowlerName = bowlerName,
                            batsmanName = batsmanName,
                            speedKph = speed,
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
                        editMetadataClip = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editMetadataClip = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(BorderColor)
    )
}
