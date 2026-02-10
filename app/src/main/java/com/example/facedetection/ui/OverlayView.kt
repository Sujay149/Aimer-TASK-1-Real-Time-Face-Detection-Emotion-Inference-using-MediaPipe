package com.example.facedetection.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.facedetection.model.FaceMetrics
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var results: FaceLandmarkerResult? = null
    private var metrics: FaceMetrics? = null
    private var showLandmarks = true

    private val dotPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.FILL
        strokeWidth = 2f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }

    fun setResults(result: FaceLandmarkerResult?, faceMetrics: FaceMetrics) {
        this.results = result
        this.metrics = faceMetrics
        invalidate()
    }

    fun toggleLandmarks(show: Boolean) {
        showLandmarks = show
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        metrics?.let { m ->
            canvas.drawText("Detected: ${m.isFaceDetected}", 50f, 100f, textPaint)
            canvas.drawText("Eye Openness: ${"%.2f".format(m.avgEyeOpenness)}", 50f, 150f, textPaint)
            canvas.drawText("Mouth Ratio: ${"%.2f".format(m.mouthOpenness)}", 50f, 200f, textPaint)
            canvas.drawText("Head Yaw: ${"%.1f".format(m.headYaw)}", 50f, 250f, textPaint)
            canvas.drawText("Head Pitch: ${"%.1f".format(m.headPitch)}", 50f, 300f, textPaint)
        }

        if (showLandmarks) {
            results?.faceLandmarks()?.forEach { landmarks ->
                for (landmark in landmarks) {
                    canvas.drawCircle(
                        landmark.x() * width,
                        landmark.y() * height,
                        3f,
                        dotPaint
                    )
                }
            }
        }
    }
}
