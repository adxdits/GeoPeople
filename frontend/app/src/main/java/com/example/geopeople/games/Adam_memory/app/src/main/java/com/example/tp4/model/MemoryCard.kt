package com.example.tp4.model

import java.io.Serializable

data class MemoryCard(
    val fish: Fish,
    val sample: Int
) : Serializable {
    companion object {
        fun createPairs(fishes: List<Fish>, numberOfPairs: Int): List<MemoryCard> {
            val selected = fishes.pickRandomElements(numberOfPairs)
            return selected.flatMap { fish ->
                listOf(MemoryCard(fish, 0), MemoryCard(fish, 1))
            }.shuffled()
        }
    }
}

fun <T> List<T>.pickRandomElements(n: Int): List<T> {
    require(n <= size) { "Cannot pick $n elements from a list of size $size" }
    return shuffled().take(n)
}
