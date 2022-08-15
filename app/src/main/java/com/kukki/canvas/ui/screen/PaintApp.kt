package com.kukki.canvas.ui.screen


import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kukki.canvas.ui.component.horizontalGradientBackground
import com.kukki.canvas.ui.theme.CanvasTheme
import com.kukki.canvas.ui.theme.Purple200
import com.kukki.canvas.ui.theme.graySurface
import com.kukki.canvas.ui.utils.ColorPicker
import com.kukki.canvas.ui.utils.PaintDataProvider

@Composable
fun PaintApp() {
    val showBrushes = remember { mutableStateOf(false) }
    val showColorPicker = remember { mutableStateOf(false) }

    val drawBrush = remember { mutableStateOf(5f) }
    val usedColors = remember { mutableStateOf(mutableSetOf(Color.Black, Color.White, Color.Gray)) }
    // on every change of brush or color start a new path and save old one in list

    val drawColor = remember { mutableStateOf(Color.Black) }
    val bgColor = Purple200

    CanvasTheme(darkTheme = false) {
        val paths = rememberPathStateList()
        Scaffold(
            topBar = {
                PaintAppBar(
                    bgColor = bgColor,
                    onDelete = {
                        paths.value = mutableListOf()
                    }
                )
            },
            content = {
                Column {
                    if (showColorPicker.value) {
                        //show wheel picker in dialog box
                        ColorPicker(
                            onColorSelected = { color ->
                                drawColor.value = color
                            })
                    }

                    UsedColorSelectionRowView(usedColors, drawColor)

                    paths.value.add(PathState(path = Path(), color = drawColor.value, stroke = drawBrush.value))

                    Box {
                        DrawingCanvas(drawColor, drawBrush, usedColors, paths.value)
                        DrawingTools(showBrushes, drawBrush, usedColors.value)
                    }
                }

            },
            bottomBar = {
                BottomAppBar(
                    backgroundColor = bgColor
                ) {
                    val modifier = Modifier
                        .weight(0.1f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = modifier
                                .clickable {
                                    showBrushes.value = !showBrushes.value
                                },
                            imageVector = Icons.Default.Brush,
                            contentDescription = "stroke selector",
                            tint = drawColor.value
                        )

                        Divider(
                            modifier = Modifier.width(1.dp)
                        )

                        Icon(
                            modifier = modifier
                                .clickable {
                                    showColorPicker.value = !showColorPicker.value
                                },
                            imageVector = Icons.Default.Colorize,
                            contentDescription = "color picker",
                            tint = drawColor.value
                        )


                        Divider(
                            modifier = Modifier.width(1.dp)
                        )

                        Icon(
                            modifier = modifier
                                .clickable {
                                    showColorPicker.value = !showColorPicker.value
                                },
                            imageVector = Icons.Default.Save,
                            contentDescription = "save drawing",
                            tint = drawColor.value
                        )

                        Divider(
                            modifier = Modifier.width(1.dp)
                        )

                        Icon(
                            modifier = modifier
                                .clickable {

                                },
                            imageVector = Icons.Default.SavedSearch,
                            contentDescription = "used colors",
                            tint = drawColor.value
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun UsedColorSelectionRowView(
    usedColors: MutableState<MutableSet<Color>>,
    drawColor: MutableState<Color>
) {
    Row(
        modifier = Modifier
            .horizontalGradientBackground(listOf(graySurface, Color.Black))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState())
            .animateContentSize()
    ) {
        usedColors.value.forEach {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = it,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        drawColor.value = it
                    }
            )
        }
    }
}


@Composable
fun PaintAppBar(bgColor: Color, onDelete: () -> Unit) {
    TopAppBar(
        backgroundColor = bgColor,
        title = { Text("Canvas") },
        actions = {
            IconButton(onClick = onDelete, content = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            })
        }
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
fun DrawingTools(showBrushes: MutableState<Boolean>, drawBrush: MutableState<Float>, usedColors: MutableSet<Color>) {
    val strokes = remember { PaintDataProvider.strokeList }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {

        AnimatedVisibility(visible = showBrushes.value) {
            LazyColumn {
                items(strokes) {
                    IconButton(
                        onClick = {
                            drawBrush.value = it.toFloat()
                            showBrushes.value = false
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .border(
                                border = BorderStroke(
                                    width = with(LocalDensity.current) { it.toDp() },
                                    color = Color.Gray
                                ),
                                shape = CircleShape
                            )
                    ) {

                    }
                }
            }
        }
    }
}

@Composable
fun ExtendedFloatingActionButtonDemo() {
    ExtendedFloatingActionButton(
        icon = { Icon(Icons.Filled.Favorite, "") },
        text = { Text("FloatingActionButton") },
        onClick = { /*do something*/ },
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    )
}

@Composable
fun rememberPathStateList() = remember { mutableStateOf(mutableListOf<PathState>()) }

@Preview
@Composable
fun DrawingToolsPreview() {
    CanvasTheme {
//        PaintApp()
    }

}

data class PathState(
    var path: Path,
    var color: Color,
    val stroke: Float
)
