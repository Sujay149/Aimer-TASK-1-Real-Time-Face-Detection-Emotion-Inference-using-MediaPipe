package com.example.facedetection.engine

import com.example.facedetection.model.EmotionState
import com.example.facedetection.model.FaceMetrics

class EmotionEngine {
    private var lastFaceTimestamp = System.currentTimeMillis()
    private val SLEEP_THRESHOLD_MS = 10000L // 10 seconds to sleep
    private val IDLE_THRESHOLD_MS = 2000L // 2 seconds to idle

    fun inferEmotion(metrics: FaceMetrics): EmotionState {
        if (!metrics.isFaceDetected) {
            val elapsed = System.currentTimeMillis() - lastFaceTimestamp
            return when {
                elapsed > SLEEP_THRESHOLD_MS -> EmotionState.Sleep
                else -> EmotionState.Idle
            }
        }

        lastFaceTimestamp = System.currentTimeMillis()

        // Rule-based inference
        val isMouthOpen = metrics.mouthOpenness > 0.35f
        val areEyesClosed = metrics.avgEyeOpenness < 0.12f
        val areEyesWide = metrics.avgEyeOpenness > 0.38f
        val areEyesRelaxed = metrics.avgEyeOpenness < 0.25f
        
        return when {
            // Sleep: Eyes closed
            areEyesClosed -> EmotionState.Sleep
            
            // Wink: One eye significantly more closed than the other
            Math.abs(metrics.leftEyeOpenness - metrics.rightEyeOpenness) > 0.25f -> EmotionState.Wink
            
            // Surprised: Mouth open + eyes wide
            isMouthOpen && areEyesWide -> EmotionState.Surprised
            
            // Happy: Smiling or (Mouth open + relaxed eyes)
            metrics.isSmiling || (isMouthOpen && areEyesRelaxed) -> EmotionState.Happy
            
            // Angry: Low brows + tight mouth
            metrics.eyebrowVerticalPos > -0.025f && metrics.mouthOpenness < 0.2f -> EmotionState.Angry
            
            // Curious: Significant head tilt/turn or raised brows 
            Math.abs(metrics.headYaw) > 12f || Math.abs(metrics.headPitch) > 8f || metrics.eyebrowVerticalPos < -0.065f -> EmotionState.Curious
            
            // Default Neutral
            else -> EmotionState.Neutral
        }
    }
}
