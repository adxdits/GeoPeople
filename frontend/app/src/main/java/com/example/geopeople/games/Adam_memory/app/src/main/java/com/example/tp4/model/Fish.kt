package com.example.tp4.model

import android.content.Context
import java.io.Serializable

data class Fish(
    val name: String,
    val imageFileName: String
) : Serializable {
    companion object {
        fun loadFromAssets(context: Context): List<Fish> {
            val fishFiles = context.assets.list("fishes") ?: emptyArray()
            return fishFiles
                .filter { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".webp") }
                .map { fileName ->
                    Fish(
                        name = fileName.substringBeforeLast("."),
                        imageFileName = fileName
                    )
                }
                .sortedBy { it.name }
        }
    }
}
