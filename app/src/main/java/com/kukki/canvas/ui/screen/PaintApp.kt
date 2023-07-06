@file:OptIn(ExperimentalMaterialApi::class)

package com.kukki.canvas.ui.screen

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kukki.canvas.ui.DrawBox
import com.kukki.canvas.ui.component.ControlsBar
import com.kukki.canvas.ui.component.StatusBarColor
import com.kukki.canvas.ui.component.SubtitleText
import com.kukki.canvas.ui.rememberDrawController
import com.kukki.canvas.ui.saveImage
import com.kukki.canvas.ui.theme.CanvasTheme
import com.kukki.canvas.ui.theme.WhiteLite
import com.kukki.canvas.ui.theme.graySurface
import com.kukki.canvas.ui.theme.pink
import io.ak1.rangvikalp.RangVikalp
import kotlinx.coroutines.launch

@Composable
fun viewToBitmap(view: View): Bitmap? {
    return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PaintApp() {
    val context = LocalContext.current
    val showStrokeSelector = remember { mutableStateOf(false) }
    val showBgColorPicker = remember { mutableStateOf(false) }
    val showBrushColorPicker = remember { mutableStateOf(false) }

    val strokeWidth = remember { mutableStateOf(5f) }
    val usedColors = remember { mutableStateOf(mutableSetOf(Color.Black, Color.White, Color.Gray)) }
    // on every change of brush or color start a new path and save old one in list
    val drawController = rememberDrawController()

    val drawColor = remember { mutableStateOf(Color.Black) }
    val bgColor = remember { mutableStateOf(WhiteLite) }
    val topBgColor = pink

    val sheetState: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val paths = rememberPathStateList()
    val coroutineScope = rememberCoroutineScope()

    StatusBarColor(topBgColor)

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            if (showStrokeSelector.value) {
                Column(
                    modifier = Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                ) {
                    SubtitleText(subtitle = "Stroke Width ${strokeWidth.value.toInt()}")

                    Slider(
                        value = strokeWidth.value,
                        onValueChange = {
                            strokeWidth.value = it
                            drawController.changeStrokeWidth(it)
                        },
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
//                        steps = 20,
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (drawColor.value == Color.Black) graySurface else drawColor.value,
                            activeTrackColor = if (drawColor.value == Color.Black) graySurface else drawColor.value,
                            inactiveTrackColor = Color.Black,
                            activeTickColor = Color.Black,
                            inactiveTickColor = Color.Black
                        )
                    )
                }

            } else {
                RangVikalp(isVisible = true, showShades = true) {
                    if (showBgColorPicker.value) {
                        drawController.changeBgColor(it)
                        bgColor.value = it
                    } else {
                        drawController.changeColor(it)
                        drawColor.value = it
                    }
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = topBgColor,
                        title = {
                            Text(modifier = Modifier.fillMaxWidth(), text = "Canvas", textAlign = TextAlign.Center)
                        },
                        actions = {

                        }
                    )
                },
                backgroundColor = bgColor.value,
                content = {
                    paths.value.add(PathState(path = Path(), color = drawColor.value, stroke = strokeWidth.value))
                    DrawBox(
                        modifier = Modifier
                            .fillMaxSize(),
                        drawController = drawController,
                        bitmapCallback = { imageBitmap, throwable ->
                            imageBitmap?.let {
                                context.saveImage(imageBitmap.asAndroidBitmap())
                            }
                        },
                        drawColor = drawColor,
                        strokeWidth = strokeWidth
                    )
                },
                bottomBar = {
                    BottomAppBar(backgroundColor = topBgColor) {
                        ControlsBar(
                            onDownloadClick = {
                                drawController.saveBitmap()
                            },
                            onColorClick = {
                                showBrushColorPicker.value = true

                                showStrokeSelector.value = false
                                showBgColorPicker.value = false

                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                            onBgColorClick = {
                                showBgColorPicker.value = true

                                showStrokeSelector.value = false
                                showBrushColorPicker.value = false

                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                            onSizeClick = {
                                showStrokeSelector.value = true

                                showBrushColorPicker.value = false
                                showBgColorPicker.value = false

                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                            onClearClick = {
                                paths.value = mutableListOf()

                                bgColor.value = WhiteLite
                                drawColor.value = Color.Black
                            },
                            colorValue = drawColor,
                            bgColorValue = bgColor,
                        )
                    }

                }
            )
        },
    )
}


@Composable
fun rememberPathStateList() = remember { mutableStateOf(mutableListOf<PathState>()) }

@Preview
@Composable
fun DrawingToolsPreview() {
    CanvasTheme {
        PaintApp()
    }

}

data class PathState(
    var path: Path,
    var color: Color,
    val stroke: Float
)
