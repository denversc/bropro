package com.denversc.bropro

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

enum class VerticalAlignment {
    TOP, CENTER, BOTTOM
}

enum class HorizontalAlignment {
    LEFT, CENTER, RIGHT
}

enum class ColorMode {
    NORMAL, INVERTED
}

object LabelBitmapGenerator {
  private const val PRINTER_HEAD_WIDTH = 128 // Height of the bitmap for printing

  fun createLabelBitmap(
      text: String, 
      customFontSize: Float? = null,
      alignment: VerticalAlignment = VerticalAlignment.CENTER,
      horizontalAlignment: HorizontalAlignment = HorizontalAlignment.CENTER,
      colorMode: ColorMode = ColorMode.NORMAL
  ): Pair<Bitmap, Float> {
    // A 12mm tape only has a printable area of about 9mm (roughly 64 pixels)
    // centered on the 128-pixel print head.
    val maxPrintHeight = 64f
    var currentFontSize = customFontSize ?: 60f
    
    val textPaint = TextPaint().apply {
      color = if (colorMode == ColorMode.INVERTED) Color.WHITE else Color.BLACK
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
      
      val textAlignment = when (horizontalAlignment) {
          HorizontalAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
          HorizontalAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
          HorizontalAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
      }
      
      return StaticLayout.Builder.obtain(text, 0, text.length, textPaint, exactWidth)
          .setAlignment(textAlignment)
          .setLineSpacing(0f, 1f)
          .setIncludePad(false)
          .build()
    }

    // Shrink font size until the total height fits within the print head
    // Only auto-shrink if no custom font size is provided
    var layout = calculateLayout(currentFontSize)
    if (customFontSize == null) {
        while (layout.height > maxPrintHeight && currentFontSize > 1f) {
            currentFontSize -= 1f
            layout = calculateLayout(currentFontSize)
        }
    }

    val width = layout.width.coerceAtLeast(1)
    val height = PRINTER_HEAD_WIDTH

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    canvas.drawColor(if (colorMode == ColorMode.INVERTED) Color.BLACK else Color.WHITE)

    // Calculate vertical offset based on alignment within the 64px printable area
    // The printable area is centered at y=32 ( (128-64)/2 )
    val printableTop = (PRINTER_HEAD_WIDTH - maxPrintHeight) / 2f
    val yOffset = when (alignment) {
        VerticalAlignment.TOP -> printableTop
        VerticalAlignment.CENTER -> printableTop + (maxPrintHeight - layout.height) / 2f
        VerticalAlignment.BOTTOM -> printableTop + (maxPrintHeight - layout.height)
    }

    canvas.save()
    canvas.translate(0f, yOffset)
    layout.draw(canvas)
    canvas.restore()

    return Pair(bitmap, currentFontSize)
  }
}
