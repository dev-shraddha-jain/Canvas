package com.kukki.canvas.ui

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kukki.canvas.ui.screen.PathState
import com.kukki.canvas.ui.theme.CanvasTheme

@Composable
fun DrawBox(
    modifier: Modifier = Modifier,
    drawController: DrawController,
    bitmapCallback: (ImageBitmap?, Throwable?) -> Unit,
    drawColor: MutableState<Color>,
    strokeWidth: MutableState<Float>
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            ComposeView(context).apply {
                setContent {
                    CanvasTheme(darkTheme = false) {
                        LaunchedEffect(drawController) {
                            Log.e("", "xcxcxc inside DrawBox launched effect")
                            drawController.trackBitmaps(this@apply, this, bitmapCallback)
                        }
                        Canvas(
                            modifier = modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            drawController.insertNewPath(offset)
                                            drawController.updateLatestPath(offset)
                                            drawController.pathList
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            drawController.insertNewPath(offset)
                                            // println("DRAG!")
                                        }
                                    ) { change, _ ->
                                        val newPoint = change.position
                                        drawController.updateLatestPath(newPoint)
                                    }
                                },
                            onDraw = {
                                drawController.pathList.forEach { pw ->
                                    drawPath(
                                        createPath(pw.points),
                                        color = pw.strokeColor,
                                        alpha = pw.alpha,
                                        style = Stroke(
                                            width = pw.strokeWidth,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            })
                    }
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalComposeUiApi::class)
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