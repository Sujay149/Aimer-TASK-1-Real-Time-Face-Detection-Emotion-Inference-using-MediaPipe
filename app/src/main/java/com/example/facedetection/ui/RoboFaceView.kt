package com.example.facedetection.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.example.facedetection.model.EmotionState

class RoboFaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentEmotion: EmotionState = EmotionState.Idle
    
    // Animation factors (0.0 to 1.0)
    private var leftEyeOpenness = 1f
    private var rightEyeOpenness = 1f
    private var mouthCurve = 0f
    private var mouthWidth = 0.4f
    private var eyeAngularity = 0f // For angry brows
    private var eyeScale = 1f
    private var shakeOffset = 0f

    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 25f
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    fun setEmotion(emotion: EmotionState) {
        if (currentEmotion == emotion) return
        currentEmotion = emotion
        animateToState(emotion)
    }

    private fun animateToState(emotion: EmotionState) {
        val targetLeftEye = when (emotion) {
            EmotionState.Sleep -> 0.05f
            EmotionState.Surprised -> 1.3f
            EmotionState.Wink -> 0.05f
            else -> 1.0f
        }
        val targetRightEye = when (emotion) {
            EmotionState.Sleep -> 0.05f
            EmotionState.Surprised -> 1.3f
            else -> 1.0f
        }
        val targetMouthCurve = when (emotion) {
            EmotionState.Happy -> 0.8f
            EmotionState.Angry -> -0.5f
            EmotionState.Surprised -> 0.2f
            else -> 0f
        }
        val targetEyeAngularity = if (emotion == EmotionState.Angry) 1.2f else 0f
        val targetEyeScale = if (emotion == EmotionState.Surprised) 1.25f else 1.0f

        animateValue(leftEyeOpenness, targetLeftEye) { leftEyeOpenness = it }
        animateValue(rightEyeOpenness, targetRightEye) { rightEyeOpenness = it }
        animateValue(mouthCurve, targetMouthCurve) { mouthCurve = it }
        animateValue(eyeAngularity, targetEyeAngularity) { eyeAngularity = it }
        animateValue(eyeScale, targetEyeScale) { eyeScale = it }
        
        if (emotion == EmotionState.Angry) {
            startShake()
        } else {
            shakeOffset = 0f
        }
        invalidate()
    }

    private fun startShake() {
        ValueAnimator.ofFloat(0f, 10f, -10f, 0f).apply {
            duration = 100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 5
            addUpdateListener {
                shakeOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateValue(from: Float, to: Float, update: (Float) -> Unit) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                update(it.animatedValue as Float)
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(shakeOffset, 0f) // Apply shake for Angry state
        
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2
        val centerY = h / 2

        val baseColor = when (currentEmotion) {
            EmotionState.Happy -> Color.parseColor("#00E676") // Green
            EmotionState.Angry -> Color.parseColor("#FF5252") // Red
            EmotionState.Surprised -> Color.parseColor("#40C4FF") // Blue
            EmotionState.Sleep -> Color.parseColor("#9E9E9E") // Gray
            EmotionState.Idle -> Color.parseColor("#BDBDBD") // Light Gray
            EmotionState.Wink -> Color.parseColor("#FFD740") // Amber
            else -> Color.parseColor("#B2FF59") // Lime
        }

        mainPaint.color = baseColor
        glowPaint.color = baseColor

        drawEyes(canvas, centerX, centerY)
        drawMouth(canvas, centerX, centerY)
    }

    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float) {
        val eyeSpacing = width * 0.2f
        val eyeY = cy - height * 0.1f
        val eyeSize = width * 0.08f * eyeScale

        // Left Eye
        drawEye(canvas, cx - eyeSpacing, eyeY, eyeSize, leftEyeOpenness, isLeft = true)
        // Right Eye
        drawEye(canvas, cx + eyeSpacing, eyeY, eyeSize, rightEyeOpenness, isLeft = false)
    }

    private fun drawEye(canvas: Canvas, ex: Float, ey: Float, size: Float, openness: Float, isLeft: Boolean) {
        val path = Path()
        val h = size * openness
        
        if (eyeAngularity > 0) {
            // Angry angled eyes
            val angleShift = eyeAngularity * 25f
            if (isLeft) {
                path.moveTo(ex - size, ey - h/2 + angleShift)
                path.lineTo(ex + size, ey - h/2)
            } else {
                path.moveTo(ex - size, ey - h/2)
                path.lineTo(ex + size, ey - h/2 + angleShift)
            }
            path.lineTo(ex + size, ey + h/2)
            path.lineTo(ex - size, ey + h/2)
            path.close()
        } else {
            // Oval/Round eyes
            val rect = RectF(ex - size, ey - h, ex + size, ey + h)
            path.addOval(rect, Path.Direction.CW)
        }

        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, mainPaint)
    }

    private fun drawMouth(canvas: Canvas, cx: Float, cy: Float) {
        val mouthY = cy + height * 0.15f
        val mw = width * mouthWidth
        val mh = height * 0.1f * Math.abs(mouthCurve).coerceAtLeast(0.1f)
        
        val path = Path()
        path.moveTo(cx - mw/2, mouthY)
        
        if (currentEmotion == EmotionState.Surprised) {
            val rect = RectF(cx - mw/4, mouthY - mh, cx + mw/4, mouthY + mh)
            path.addOval(rect, Path.Direction.CW)
        } else {
            path.quadTo(cx, mouthY + (mouthCurve * 100f), cx + mw/2, mouthY)
        }

        canvas.drawPath(path, glowPaint)
        canvas.drawPath(path, mainPaint)
    }
}
