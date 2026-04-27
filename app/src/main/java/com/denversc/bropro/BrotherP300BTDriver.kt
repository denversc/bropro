package com.denversc.bropro

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BrotherP300BTDriver {
  private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
  private val TAG = "BrotherP300BTDriver"

  @SuppressLint("MissingPermission")
  fun findPairedDevice(): BluetoothDevice? {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
    return adapter.bondedDevices.find { it.name?.contains("P300BT", ignoreCase = true) == true }
  }

  @SuppressLint("MissingPermission")
  suspend fun printLabel(text: String): Result<Unit> = withContext(Dispatchers.IO) {
    val device = findPairedDevice() ?: return@withContext Result.failure(Exception("P300BT not paired"))
    val bitmap = LabelBitmapGenerator.createLabelBitmap(text)

    var socket: BluetoothSocket? = null
    try {
      socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket.connect()
      val out = socket.outputStream

      // 1. Initialization
      out.write(ByteArray(100) { 0 }) // 100 nulls to clear
      out.write(byteArrayOf(0x1B, 0x40)) // ESC @ (Initialize)

      // 2. Set to Raster Mode
      out.write(byteArrayOf(0x1B, 0x69, 0x61, 0x01)) // ESC i a 1

      // 3. Set Media & Quality (12mm tape default)
      // ESC i z [flags] [type] [width] [length] [count] [reserved]
      out.write(byteArrayOf(0x1B, 0x69, 0x7A, 0x84.toByte(), 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))

      // 4. Send Raster Data line-by-line
      for (x in 0 until bitmap.width) {
        val lineData = ByteArray(16)
        for (y in 0 until 128) {
          val pixel = bitmap.getPixel(x, y)
          if (pixel != Color.WHITE) {
            val byteIdx = y / 8
            val bitIdx = 7 - (y % 8)
            lineData[byteIdx] = (lineData[byteIdx].toInt() or (1 shl bitIdx)).toByte()
          }
        }
        out.write(byteArrayOf(0x67, 0x00, 0x10)) // 'g' command + length (16 bytes)
        out.write(lineData)
      }

      // 5. Finalize and Print
      out.write(byteArrayOf(0x1A)) // Control-Z (Print)
      out.flush()
      
      Result.success(Unit)
    } catch (e: IOException) {
      Log.e(TAG, "Print failed", e)
      Result.failure(e)
    } finally {
      socket?.close()
    }
  }
}
