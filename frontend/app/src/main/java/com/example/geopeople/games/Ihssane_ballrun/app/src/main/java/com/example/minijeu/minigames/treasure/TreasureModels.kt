package com.example.minijeu.minigames.treasure

enum class GamePhase {
    Playing,
    Treasure,
    Cleaning,
    Won,
    Lost
}

data class Star(
    val worldY: Float,
    val offset: Float
)

data class TreasureGameConfig(
    val roadHalfWidth: Float = 155f,
    val playerRadius: Float = 22f,
    val finishDistance: Float = 2800f,
    val startSafeFrames: Int = 90,
    val roadSpeed: Float = 4.4f,
    val tiltAcceleration: Float = 0.135f,
    val friction: Float = 0.89f,
    val buttonImpulse: Float = 6.5f,
    val shakeThreshold: Float = 5.5f,
    val breathThreshold: Int = 9000
)

val treasureStars = listOf(
    Star(360f, -56f),
    Star(650f, 42f),
    Star(940f, -34f),
    Star(1260f, 58f),
    Star(1580f, -48f),
    Star(1920f, 36f),
    Star(2260f, -62f),
    Star(2520f, 44f)
)
