package com.example.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmer {
    private const val TAG = "VideoTrimmer"

    /**
     * Natively trims an MP4 file on device from startTimeMs to endTimeMs using MediaExtractor
     * and MediaMuxer. This is lightning fast because it does not re-encode the audio/video stream;
     * it simply copies the compressed packets directly.
     */
    suspend fun trimVideoNatively(
        context: Context,
        sourcePath: String,
        destFile: File,
        startTimeMs: Long,
        endTimeMs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
            // Cannot natively mux/demux remote URLs directly without heavy cache buffering.
            // Returning false triggers the highly resilient "Virtual Seek Trimming" engine.
            Log.d(TAG, "Source is remote. Falling back to virtual seek trimming.")
            return@withContext false
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(sourcePath)

            val trackCount = extractor.trackCount
            val trackMap = HashMap<Int, Int>()
            var videoTrackIndex = -1
            var maxBufferSize = 0

            // Set up Muxer
            muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val outIndex = muxer.addTrack(format)
                    trackMap[i] = outIndex

                    if (mime.startsWith("video/")) {
                        videoTrackIndex = i
                    }

                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val inputSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (inputSize > maxBufferSize) {
                            maxBufferSize = inputSize
                        }
                    }
                }
            }

            if (maxBufferSize <= 0) {
                maxBufferSize = 1024 * 1024 // 1MB fallback
            }

            // Start Muxer
            muxer.start()

            // Seek to start time
            extractor.seekTo(startTimeMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endTimeMs * 1000L) {
                    // Reached the end boundary of the clip segment
                    break
                }

                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = sampleTimeUs - (startTimeMs * 1000L)
                bufferInfo.offset = 0
                bufferInfo.flags = extractor.sampleFlags

                val outTrackIndex = trackMap[trackIndex]
                if (outTrackIndex != null) {
                    muxer.writeSampleData(outTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            Log.d(TAG, "Video trimmed successfully and saved to ${destFile.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Native Video Trimming failed: ${e.message}", e)
            return@withContext false
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Splits a match video into individual physical delivery MP4 clips stored in the app cache folder.
     */
    suspend fun createPhysicalVideoClips(
        context: Context,
        sourcePath: String,
        clips: List<com.example.data.DeliveryClip>,
        onProgress: (Int, Int) -> Unit
    ): List<com.example.data.DeliveryClip> = withContext(Dispatchers.IO) {
        val updatedClips = mutableListOf<com.example.data.DeliveryClip>()
        val parentDir = File(context.cacheDir, "clipped_deliveries")
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }

        clips.forEachIndexed { index, clip ->
            onProgress(index + 1, clips.size)
            val clipFileName = "delivery_session_${clip.sessionId}_${clip.overNumber}_${clip.ballNumber}.mp4"
            val destFile = File(parentDir, clipFileName)

            // Try native trimming
            val success = trimVideoNatively(context, sourcePath, destFile, clip.startTimeMs, clip.endTimeMs)
            
            if (success && destFile.exists() && destFile.length() > 0) {
                updatedClips.add(clip.copy(clipPath = destFile.absolutePath))
            } else {
                // Fallback to virtual path. In virtual path, we store the master URL so the player
                // can play directly with offsets. We symbol the virtual mode with a specific suffix string or empty
                updatedClips.add(clip.copy(clipPath = ""))
            }
        }
        return@withContext updatedClips
    }
}
