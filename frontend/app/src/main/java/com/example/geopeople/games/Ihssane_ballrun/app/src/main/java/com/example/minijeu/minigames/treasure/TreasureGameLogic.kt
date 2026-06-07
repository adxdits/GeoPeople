package com.example.minijeu.minigames.treasure

import kotlin.math.PI
import kotlin.math.sin

fun roadCenter(worldY: Float, gameWidth: Float): Float {
    val amplitude = gameWidth * 0.19f
    val wave =
        sin((worldY / 430f) * PI.toFloat()) * 0.72f +
            sin((worldY / 260f) * PI.toFloat() + 1.4f) * 0.34f +
            sin((worldY / 690f) * PI.toFloat() + 2.2f) * 0.22f

    return gameWidth / 2f + amplitude * wave
}

fun roadHalfWidthAt(worldY: Float, baseHalfWidth: Float, finishDistance: Float): Float {
    val loopedY = worldY % finishDistance
    val narrowZones = listOf(760f, 1450f, 2180f)
    val strongestNarrowing = narrowZones.maxOf { zoneCenter ->
        val distanceFromZone = kotlin.math.abs(loopedY - zoneCenter)
        (1f - distanceFromZone / 230f).coerceIn(0f, 1f)
    }

    return baseHalfWidth - strongestNarrowing * 48f
}
