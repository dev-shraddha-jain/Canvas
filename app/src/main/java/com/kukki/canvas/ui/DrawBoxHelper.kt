package com.kukki.canvas.ui


import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.view.doOnLayout
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


//Model
data class PathWrapper(
    var points: SnapshotStateList<Offset>,
    val strokeWidth: Float = 5f,
    val strokeColor: Color,
    val alpha: Float = 1f
)

data class DrawBoxPayLoad(val bgColor: Color, val path: List<PathWrapper>)
class DrawController {

    private val _redoPathList = mutableStateListOf<PathWrapper>()
    private val _undoPathList = mutableStateListOf<PathWrapper>()
    internal val pathList: SnapshotStateList<PathWrapper> = _undoPathList


    private val _historyTracker = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val historyTracker = _historyTracker.asSharedFlow()

    fun trackHistory(
        scope: CoroutineScope,
        trackHistory: (undoCount: Int, redoCount: Int) -> Unit
    ) {
        historyTracker
            .onEach { trackHistory(_undoPathList.size, _redoPathList.size) }
            .launchIn(scope)
    }


    private val _bitmapGenerators = MutableSharedFlow<Bitmap.Config>(extraBufferCapacity = 1)
    private val bitmapGenerators = _bitmapGenerators.asSharedFlow()

    fun saveBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888) =
        _bitmapGenerators.tryEmit(config)

    var opacity by mutableStateOf(1f)
        private set

    var strokeWidth by mutableStateOf(10f)
        private set

    var color by mutableStateOf(Color.Red)
        private set

    var bgColor by mutableStateOf(Color.Black)
        private set

    fun changeOpacity(value: Float) {
        opacity = value
    }

    fun changeColor(value: Color) {
        color = value
    }

    fun changeBgColor(value: Color) {
        bgColor = value
    }

    fun changeStrokeWidth(value: Float) {
        strokeWidth = value
    }

    fun importPath(drawBoxPayLoad: DrawBoxPayLoad) {
        reset()
        bgColor = drawBoxPayLoad.bgColor
        _undoPathList.addAll(drawBoxPayLoad.path)
        _historyTracker.tryEmit("${_undoPathList.size}")
    }

    fun exportPath() = DrawBoxPayLoad(bgColor, pathList.toList())


    fun unDo() {
        if (_undoPathList.isNotEmpty()) {
            val last = _undoPathList.last()
            _redoPathList.add(last)
            _undoPathList.remove(last)
            _historyTracker.tryEmit("Undo - ${_undoPathList.size}")
        }
    }

    fun reDo() {
        if (_redoPathList.isNotEmpty()) {
            val last = _redoPathList.last()
            _undoPathList.add(last)
            _redoPathList.remove(last)
            _historyTracker.tryEmit("Redo - ${_redoPathList.size}")
        }
    }


    fun reset() {
        _redoPathList.clear()
        _undoPathList.clear()
        _historyTracker.tryEmit("-")
    }

    fun updateLatestPath(newPoint: Offset) {
        val index = _undoPathList.lastIndex
        _undoPathList[index].points.add(newPoint)
    }

    fun insertNewPath(newPoint: Offset) {
        val pathWrapper = PathWrapper(
            points = mutableStateListOf(newPoint),
            strokeColor = color,
            alpha = opacity,
            strokeWidth = strokeWidth,
        )
        _undoPathList.add(pathWrapper)
        _redoPathList.clear()
        _historyTracker.tryEmit("${_undoPathList.size}")
    }

    fun trackBitmaps(
        it: View,
        coroutineScope: CoroutineScope,
        onCaptured: (ImageBitmap?, Throwable?) -> Unit
    ) = bitmapGenerators
        .mapNotNull { config -> it.drawBitmapFromView(it.context, config) }
        .onEach { bitmap -> onCaptured(bitmap.asImageBitmap(), null) }
        .catch { error -> onCaptured(null, error) }
        .launchIn(coroutineScope)
}

@Composable
fun rememberDrawController(): DrawController {
    return remember { DrawController() }
}

fun createPath(points: List<Offset>) = Path().apply {
    if (points.size > 1) {
        var oldPoint: Offset? = null
        this.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val point: Offset = points[i]
            oldPoint?.let {
                val midPoint = calculateMidpoint(it, point)
                if (i == 1) {
                    this.lineTo(midPoint.x, midPoint.y)
                } else {
                    this.quadraticBezierTo(it.x, it.y, midPoint.x, midPoint.y)
                }
            }
            oldPoint = point
        }
        oldPoint?.let { this.lineTo(it.x, oldPoint.y) }
    }
}

private fun calculateMidpoint(start: Offset, end: Offset) =
    Offset((start.x + end.x) / 2, (start.y + end.y) / 2)

internal suspend fun View.drawBitmapFromView(context: Context, config: Bitmap.Config): Bitmap =
    suspendCoroutine { continuation ->
        doOnLayout { view ->
            if (Build.VERSION_CODES.O > Build.VERSION.SDK_INT) {
                continuation.resume(view.drawToBitmap(config))
                return@doOnLayout
            }

            val window =
                (context as? Activity)?.window ?: error("Can't get window from the Context")

            Bitmap.createBitmap(width, height, config).apply {
                val (x, y) = IntArray(2).apply { view.getLocationInWindow(this) }
                PixelCopy.request(
                    window,
                    getRect(x, y),
                    this,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) continuation.resume(this) else continuation.resumeWithException(
                            RuntimeException("Bitmap generation failed")
                        )
                    },
                    Handler(Looper.getMainLooper())
                )
            }
        }
    }


fun createBitmapFromView(view: View, width: Int, height: Int): Bitmap {
    view.layoutParams = LinearLayoutCompat.LayoutParams(
        LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
        LinearLayoutCompat.LayoutParams.WRAP_CONTENT
    )

    view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )

    view.layout(0, 0, width, height)

    val canvas = android.graphics.Canvas()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    canvas.setBitmap(bitmap)
    view.draw(canvas)

    return bitmap
}

private fun View.getRect(x: Int, y: Int): android.graphics.Rect {
    val viewWidth = this.width
    val viewHeight = this.height
    return android.graphics.Rect(x, y, viewWidth + x, viewHeight + y)
}


internal fun Context.saveImage(bitmap: Bitmap): Uri? {
    var uri: Uri? = null
    try {
        val fileName = System.nanoTime().toString() + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val directory =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val file = File(directory, fileName)
                put(MediaStore.MediaColumns.DATA, file.absolutePath)
            }
        }

        uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.apply {
                    clear()
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, values, null, null)
            }
        }
        return uri
    } catch (e: java.lang.Exception) {
        if (uri != null) {
            // Don't leave an orphan entry in the MediaStore
            contentResolver.delete(uri, null, null)
        }
        throw e
    }
}