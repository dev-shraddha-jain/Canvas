package com.kukki.canvas.ui.utils

import androidx.compose.ui.graphics.Color


object PaintDataProvider {
    val brushColors = listOf(
        Color.Black,
        Color.LightGray,
        Color.Gray,
        Color.Magenta,
        Color.Cyan
    )

    val strokeList = (1..50 step 5).toList()
}
