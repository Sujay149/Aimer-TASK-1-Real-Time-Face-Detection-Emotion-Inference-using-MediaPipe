package com.example.facedetection.model

data class FaceMetrics(
    val isFaceDetected: Boolean = false,
    val leftEyeOpenness: Float = 0f,
    val rightEyeOpenness: Float = 0f,
    val mouthOpenness: Float = 0f,
    val eyebrowVerticalPos: Float = 0f, // Relative to eye
    val headYaw: Float = 0f,  // Left/Right
    val headPitch: Float = 0f, // Up/Down
    val headRoll: Float = 0f,  // Tilt
    val isSmiling: Boolean = false,
    val isSurprised: Boolean = false
) {
    val avgEyeOpenness: Float get() = (leftEyeOpenness + rightEyeOpenness) / 2f
}
