package com.kukki.canvas.ui.screen

import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kukki.canvas.ui.component.ColorPicker
import com.kukki.canvas.ui.component.ControlsBar
import com.kukki.canvas.ui.component.StatusBarColor
import com.kukki.canvas.ui.component.SubtitleText
import com.kukki.canvas.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun viewToBitmap(view: View): Bitmap? {
    return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
}

@Composable
fun PaintApp() {
    val showStrokeSelector = remember { mutableStateOf(false) }
    val showBgColorPicker = remember { mutableStateOf(false) }
    val showBrushColorPicker = remember { mutableStateOf(false) }

    val strokeWidth = remember { mutableStateOf(5f) }
    val usedColors = remember { mutableStateOf(mutableSetOf(Color.Black, Color.White, Color.Gray)) }
    // on every change of brush or color start a new path and save old one in list

    val drawColor = remember { mutableStateOf(Color.Black) }
    val bgColor = remember { mutableStateOf(WhiteLite) }
    val topBgColor = Purple50

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
                ColorPicker(showBgColorPicker, bgColor, drawColor)
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        modifier = Modifier
                            .fillMaxWidth(),
                        backgroundColor = topBgColor,
                        title = {
                            Text(modifier = Modifier.fillMaxWidth(), text = "Canvas", textAlign = TextAlign.Center)
                        },
                        actions = {

                        }
                    )
                },
                content = {
                    paths.value.add(PathState(path = Path(), color = drawColor.value, stroke = strokeWidth.value))
                    DrawingCanvas(drawColor, strokeWidth, usedColors, paths.value)
                },
                bottomBar = {
                    BottomAppBar(backgroundColor = topBgColor) {

                        ControlsBar(
                            onDownloadClick = {

                            },
                            onColorClick = {
                                showStrokeSelector.value = false
                                showBgColorPicker.value = false
                                showBrushColorPicker.value = !(showBrushColorPicker.value)
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                            onBgColorClick = {
                                showStrokeSelector.value = false
                                showBrushColorPicker.value = false
                                showBgColorPicker.value = !(showBgColorPicker.value)
                                coroutineScope.launch {
                                    sheetState.show()
                                }
                            },
                            onSizeClick = {
                                showStrokeSelector.value = true
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
fun DrawingCanvas(drawColor: MutableState<Color>, drawBrush: MutableState<Float>, usedColors: MutableState<MutableSet<Color>>, paths: List<PathState>) {
    val currentPath = paths.last().path
    val movePath = remember { mutableStateOf<Offset?>(null) }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .padding(top = 100.dp)
        .pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentPath.moveTo(it.x, it.y)
                    usedColors.value.add(drawColor.value)
                }
                MotionEvent.ACTION_MOVE -> {
                    movePath.value = Offset(it.x, it.y)
                }
                else -> {
                    movePath.value = null
                }
            }
            true
        },//pointerInteropFilter
        onDraw = {
            movePath.value?.let {
                currentPath.lineTo(it.x, it.y)
                drawPath(
                    path = currentPath,
                    color = drawColor.value,
                    style = Stroke(drawBrush.value)
                )
            }
            paths.forEach {
                drawPath(
                    path = it.path,
                    color = it.color,
                    style = Stroke(it.stroke)
                )
            }
        })
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
