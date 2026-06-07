package com.example.minijeu

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.minijeu.minigames.treasure.TreasureGameScreen
import com.example.minijeu.ui.theme.MiniJeuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiniJeuTheme {
                TreasureGameScreen(
                    onGameEnd = { isVictory ->
                        setResult(if (isVictory) Activity.RESULT_OK else Activity.RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}
