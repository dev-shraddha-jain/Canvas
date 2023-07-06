package com.kukki.canvas.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.ak1.rangvikalp.RangVikalp

@Composable
fun StatusBarColor(topBgColor: Color) {
    // Remember a SystemUiController
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()

    DisposableEffect(systemUiController, useDarkIcons) {
        // Update all of the system bar colors to be transparent, and use
        // dark icons if we're in light theme
        systemUiController.setSystemBarsColor(
            color = topBgColor,
            darkIcons = useDarkIcons
        )
        onDispose {}
    }

}

@Composable
fun ControlsBar(
    onDownloadClick: () -> Unit,
    onColorClick: () -> Unit,
    onBgColorClick: () -> Unit,
    onSizeClick: () -> Unit,
    onClearClick: () -> Unit,
    colorValue: MutableState<Color>,
    bgColorValue: MutableState<Color>,
) {
    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
        MenuItems(
            image = Icons.Outlined.Download,
            desc = "download",
            colorTint = colorValue.value,
        ) {
            onDownloadClick()
        }
        MenuItems(
            image = Icons.Outlined.Refresh,
            desc = "reset",
            colorTint = colorValue.value,
        ) {
            onClearClick()
        }
        MenuItems(
            image = Icons.Outlined.FormatPaint,
            desc = "background color",
            colorTint = bgColorValue.value,
        ) {
            onBgColorClick()
        }
        MenuItems(
            image = Icons.Outlined.Brush,
            desc = "stroke color",
            colorTint = colorValue.value
        ) {
            onColorClick()
        }
        MenuItems(
            image = Icons.Outlined.LineWeight,
            desc = "stroke size",
            colorTint = colorValue.value
        ) {
            onSizeClick()
        }
    }
}

@Composable
fun RowScope.MenuItems(
    image: ImageVector,
    desc: String,
    colorTint: Color,
    onClick: () -> Unit
) {
    val modifier = Modifier.size(24.dp)
    IconButton(
        onClick = onClick, modifier = Modifier.weight(1f, true)
    ) {
        Icon(
            imageVector = image,
            contentDescription = desc,
            tint = colorTint,
            modifier = modifier
        )
    }
}
