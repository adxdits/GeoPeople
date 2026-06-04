package com.example.tp4

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.tp4.ui.screens.MemoryGame
import com.example.tp4.ui.theme.Tp4Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Tp4Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MemoryGame(
                        modifier = Modifier.padding(innerPadding),
                        onGameComplete = {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
