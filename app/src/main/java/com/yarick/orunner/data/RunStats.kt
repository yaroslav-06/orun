package com.yarick.orunner.data

data class RunStats(
    val distanceMeters: Float = 0f,
    val startTime: Long = 0L,
    val elevationGainMeters: Float = 0f,
    val elevationLossMeters: Float = 0f,
    val currentPaceSecPerKm: Float = 0f,
    val heartRate: Int? = null,
    val isFinished: Boolean = false
)
