package com.example.tp4.viewmodel

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.tp4.model.Fish
import com.example.tp4.model.MemoryCard

class BoardViewModel : ViewModel() {

    var cards by mutableStateOf<List<MemoryCard>>(emptyList())
    var foundFishes by mutableStateOf<Set<Fish>>(emptySet())
    var flippedCards by mutableStateOf<Set<MemoryCard>>(emptySet())
    var attempts by mutableIntStateOf(0)
    var startTime by mutableLongStateOf(0L)

    val pairsFound: Int get() = foundFishes.size
    val totalPairs: Int get() = cards.size / 2
    val isGameComplete: Boolean get() = pairsFound == totalPairs && cards.isNotEmpty()

    val visibility: BooleanArray
        get() = BooleanArray(cards.size) { i ->
            cards[i].fish in foundFishes || cards[i] in flippedCards
        }

    fun initGame(allFishes: List<Fish>, numberOfPairs: Int) {
        cards = MemoryCard.createPairs(allFishes, numberOfPairs)
        foundFishes = emptySet()
        flippedCards = emptySet()
        attempts = 0
        startTime = 0L
    }

    fun onCardClicked(index: Int) {
        if (index < 0 || index >= cards.size) return
        val card = cards[index]
        if (card.fish in foundFishes || card in flippedCards || flippedCards.size >= 2) return

        if (startTime == 0L) {
            startTime = SystemClock.elapsedRealtime()
        }
        flippedCards = flippedCards + card
        if (flippedCards.size == 2) {
            attempts++
        }
    }

    fun checkMatch(): Boolean? {
        if (flippedCards.size != 2) return null
        val list = flippedCards.toList()
        return list[0].fish == list[1].fish
    }

    fun applyMatch() {
        val list = flippedCards.toList()
        foundFishes = foundFishes + list[0].fish
        flippedCards = emptySet()
    }

    fun hideFlipped() {
        flippedCards = emptySet()
    }
}
