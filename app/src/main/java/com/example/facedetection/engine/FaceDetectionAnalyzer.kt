package com.example.facedetection.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.facedetection.model.FaceMetrics
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.ExecutorService
import kotlin.math.*

class FaceDetectionAnalyzer(
    private val context: Context,
    private val onResults: (FaceMetrics, FaceLandmarkerResult?) -> Unit
) : ImageAnalysis.Analyzer {

    private val TAG = "FaceDetectionAnalyzer"
    private var faceLandmarker: FaceLandmarker? = null

    init {
        setupFaceLandmarker()
    }

    fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    private fun setupFaceLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setDelegate(Delegate.CPU)
            .setModelAssetPath("face_landmarker.task")

        val optionsBuilder = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                processResult(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "MediaPipe Error: ${error.message}")
            }

        try {
            faceLandmarker = FaceLandmarker.createFromOptions(context, optionsBuilder.build())
            Log.d(TAG, "FaceLandmarker created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create FaceLandmarker: ${e.message}")
        }
    }

    override fun analyze(image: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        
        try {
            val bitmap = image.toBitmap()
            val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
            
            val matrix = Matrix().apply {
                postRotate(rotationDegrees)
                // Mirror for front camera
                postScale(-1f, 1f)
            }
            
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            
            if (faceLandmarker == null) {
                Log.w(TAG, "FaceLandmarker is null, skipping frame")
            } else {
                try {
                    faceLandmarker?.detectAsync(mpImage, frameTime)
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPipe detectAsync failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
        } finally {
            image.close()
        }
    }

    private fun processResult(result: FaceLandmarkerResult) {
        val faceLandmarks = result.faceLandmarks()
        if (faceLandmarks == null || faceLandmarks.isEmpty()) {
            onResults(FaceMetrics(isFaceDetected = false), null)
            return
        }

        val landmarks = faceLandmarks[0]
        
        // 1. Eye Openness (EAR)
        val leftEAR = calculateEAR(landmarks, 33, 160, 158, 133, 153, 144)
        val rightEAR = calculateEAR(landmarks, 362, 385, 387, 263, 373, 380)
        
        // 2. Mouth Openness (MAR)
        val mouthMAR = calculateMAR(landmarks, 78, 308, 13, 14)

        // 3. Eyebrow Position (Relative to eye)
        // Eyebrow point: 105 (left), 334 (right)
        // Eye top: 159 (left), 386 (right)
        val leftBrowPos = (landmarks[105].y() - landmarks[159].y())
        val rightBrowPos = (landmarks[334].y() - landmarks[386].y())
        val avgBrowPos = (leftBrowPos + rightBrowPos) / 2f

        // 4. Head Orientation (Simplistic Yaw, Pitch, Roll from landmarks)
        // This is a rough estimation. For real Yaw/Pitch/Roll, one might use PnP or more complex geometry.
        // Yaw: Ratio of eye distances to nose
        val nose = landmarks[1]
        val leftEyeInner = landmarks[133]
        val rightEyeInner = landmarks[362]
        val dLeft = dist(nose, leftEyeInner)
        val dRight = dist(nose, rightEyeInner)
        val yaw = (dRight - dLeft) / (dRight + dLeft) * 100f

        // Pitch: Ratio of nose to top/bottom
        val forehead = landmarks[10]
        val chin = landmarks[152]
        val dTop = dist(nose, forehead)
        val dBottom = dist(nose, chin)
        val pitch = (dBottom - dTop) / (dBottom + dTop) * 100f
        
        // Roll: Angle between eyes
        val roll = atan2(rightEyeInner.y() - leftEyeInner.y(), rightEyeInner.x() - leftEyeInner.x()) * (180 / PI).toFloat()

        val metrics = FaceMetrics(
            isFaceDetected = true,
            leftEyeOpenness = leftEAR,
            rightEyeOpenness = rightEAR,
            mouthOpenness = mouthMAR,
            eyebrowVerticalPos = avgBrowPos,
            headYaw = yaw,
            headPitch = pitch,
            headRoll = roll,
            isSmiling = mouthMAR > 0.1f && (landmarks[308].y() < landmarks[14].y()), // Simple smile check
            isSurprised = mouthMAR > 0.4f && leftEAR > 0.35f
        )

        onResults(metrics, result)
    }

    private fun calculateEAR(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, 
                             p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int): Float {
        val v1 = dist(landmarks[p2], landmarks[p6])
        val v2 = dist(landmarks[p3], landmarks[p5])
        val h = dist(landmarks[p1], landmarks[p4])
        return (v1 + v2) / (2f * h)
    }

    private fun calculateMAR(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
                             pLeft: Int, pRight: Int, pTop: Int, pBottom: Int): Float {
        val v = dist(landmarks[pTop], landmarks[pBottom])
        val h = dist(landmarks[pLeft], landmarks[pRight])
        return v / h
    }

    private fun dist(a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark, 
                     b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark): Float {
        return sqrt((a.x() - b.x()).pow(2) + (a.y() - b.y()).pow(2))
    }
}
