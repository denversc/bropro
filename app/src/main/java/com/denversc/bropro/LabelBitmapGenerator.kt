package com.denversc.bropro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

object LabelBitmapGenerator {
  private const val PRINTER_HEAD_WIDTH = 128 // Height of the bitmap for printing

  fun createLabelBitmap(text: String): Bitmap {
    val textPaint = TextPaint().apply {
      color = Color.BLACK
      textSize = 60f
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false // Crisp edges for printing
    }

    // Measure text to determine bitmap length
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, 1000)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1f)
        .setIncludePad(false)
        .build()

    val width = layout.width.coerceAtLeast(1)
    val height = PRINTER_HEAD_WIDTH

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    // Center text vertically on the 128px print head
    val yOffset = (height - layout.height) / 2f
    canvas.save()
    canvas.translate(0f, yOffset)
    layout.draw(canvas)
    canvas.restore()

    return bitmap
  }
}
