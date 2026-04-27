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
    // A 12mm tape only has a printable area of about 9mm (roughly 64 pixels)
    // centered on the 128-pixel print head.
    val maxPrintHeight = 64f
    var currentFontSize = 60f
    
    val textPaint = TextPaint().apply {
      color = Color.BLACK
      typeface = Typeface.DEFAULT_BOLD
      isAntiAlias = false
    }

    // Function to calculate layout for a given font size
    fun calculateLayout(fontSize: Float): StaticLayout {
      textPaint.textSize = fontSize
      var maxWidth = 0f
      text.split("\n").forEach { line ->
        val w = textPaint.measureText(line)
        if (w > maxWidth) maxWidth = w
      }
      val exactWidth = maxWidth.toInt().coerceAtLeast(1)
      
      return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, exactWidth)
          .setAlignment(Layout.Alignment.ALIGN_NORMAL)
          .setLineSpacing(0f, 1f)
          .setIncludePad(false)
          .build()
    }

    // Shrink font size until the total height fits within the print head
    var layout = calculateLayout(currentFontSize)
    while (layout.height > maxPrintHeight && currentFontSize > 10f) {
      currentFontSize -= 2f
      layout = calculateLayout(currentFontSize)
    }

    val width = layout.width.coerceAtLeast(1)
    val height = PRINTER_HEAD_WIDTH

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    // Center text vertically
    val yOffset = (height - layout.height) / 2f
    canvas.save()
    canvas.translate(0f, yOffset)
    layout.draw(canvas)
    canvas.restore()

    return bitmap
  }
}
