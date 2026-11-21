package com.wissam.filemaster.ui
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
fun Drawable.asPainter(): Painter = BitmapPainter(toBitmap().asImageBitmap())
