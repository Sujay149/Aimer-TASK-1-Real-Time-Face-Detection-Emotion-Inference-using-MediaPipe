package com.example.facedetection.model

sealed class EmotionState {
    object Idle : EmotionState()
    object Sleep : EmotionState()
    object Neutral : EmotionState()
    object Happy : EmotionState()
    object Angry : EmotionState()
    object Surprised : EmotionState()
    object Curious : EmotionState()
    object Wink : EmotionState()
    object Annoyed : EmotionState()
}
