package com.kukki.canvas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kukki.canvas.ui.screen.PaintApp
import com.kukki.canvas.ui.theme.CanvasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CanvasTheme(darkTheme = false) {
                PaintApp()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CanvasTheme {
        PaintApp()
    }
}