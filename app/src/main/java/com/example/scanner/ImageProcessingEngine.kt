package com.example.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class FilterType {
  ORIGINAL,
  AUTO,
  BLACK_AND_WHITE,
  GRAYSCALE,
  COLOR_BOOST,
  SHARPEN
}

data class CropQuad(
  val topLeft: PointF = PointF(0.05f, 0.05f),
  val topRight: PointF = PointF(0.95f, 0.05f),
  val bottomRight: PointF = PointF(0.95f, 0.95f),
  val bottomLeft: PointF = PointF(0.05f, 0.95f)
)

object ImageProcessingEngine {

  /**
   * Applies the selected filter, brightness, and contrast adjustments to the bitmap.
   */
  suspend fun applyFilter(
    source: Bitmap,
    filterType: FilterType,
    brightness: Float = 0f, // -100f to +100f
    contrast: Float = 0f    // -100f to +100f
  ): Bitmap = withContext(Dispatchers.Default) {
    val width = source.width
    val height = source.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val finalMatrix = ColorMatrix()

    // Base filter adjustments
    when (filterType) {
      FilterType.ORIGINAL -> {
        // Identity
      }
      FilterType.AUTO -> {
        // Boost contrast slightly, auto level
        val autoMatrix = ColorMatrix(floatArrayOf(
          1.2f, 0f, 0f, 0f, -10f,
          0f, 1.2f, 0f, 0f, -10f,
          0f, 0f, 1.2f, 0f, -10f,
          0f, 0f, 0f, 1f, 0f
        ))
        finalMatrix.postConcat(autoMatrix)
      }
      FilterType.GRAYSCALE -> {
        finalMatrix.setSaturation(0f)
      }
      FilterType.BLACK_AND_WHITE -> {
        // High contrast document binarization filter
        val bwMatrix = ColorMatrix(floatArrayOf(
          1.8f, 1.8f, 1.8f, 0f, -220f,
          1.8f, 1.8f, 1.8f, 0f, -220f,
          1.8f, 1.8f, 1.8f, 0f, -220f,
          0f, 0f, 0f, 1f, 0f
        ))
        finalMatrix.postConcat(bwMatrix)
      }
      FilterType.COLOR_BOOST -> {
        // Enhanced saturation for color documents, IDs, and certificates
        finalMatrix.setSaturation(1.4f)
        val boostContrast = ColorMatrix(floatArrayOf(
          1.15f, 0f, 0f, 0f, -8f,
          0f, 1.15f, 0f, 0f, -8f,
          0f, 0f, 1.15f, 0f, -8f,
          0f, 0f, 0f, 1f, 0f
        ))
        finalMatrix.postConcat(boostContrast)
      }
      FilterType.SHARPEN -> {
        // Slight high-pass clarity boost
        val sharpenMatrix = ColorMatrix(floatArrayOf(
          1.3f, -0.1f, -0.1f, 0f, 5f,
          -0.1f, 1.3f, -0.1f, 0f, 5f,
          -0.1f, -0.1f, 1.3f, 0f, 5f,
          0f, 0f, 0f, 1f, 0f
        ))
        finalMatrix.postConcat(sharpenMatrix)
      }
    }

    // Apply manual brightness (-100 to 100 mapped to color offset)
    if (brightness != 0f) {
      val brightMatrix = ColorMatrix(floatArrayOf(
        1f, 0f, 0f, 0f, brightness * 1.28f,
        0f, 1f, 0f, 0f, brightness * 1.28f,
        0f, 0f, 1f, 0f, brightness * 1.28f,
        0f, 0f, 0f, 1f, 0f
      ))
      finalMatrix.postConcat(brightMatrix)
    }

    // Apply manual contrast (-100 to 100)
    if (contrast != 0f) {
      val scale = (contrast + 100f) / 100f
      val translate = (-0.5f * scale + 0.5f) * 255f
      val contrastMatrix = ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
      ))
      finalMatrix.postConcat(contrastMatrix)
    }

    paint.colorFilter = ColorMatrixColorFilter(finalMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)

    output
  }

  /**
   * Rotates bitmap by specified degrees (e.g. 90, 180, 270).
   */
  fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return source
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
  }

  /**
   * Perspective correction using 4 arbitrary quadrilateral corners.
   */
  suspend fun warpPerspective(
    source: Bitmap,
    quad: CropQuad
  ): Bitmap = withContext(Dispatchers.Default) {
    val srcWidth = source.width.toFloat()
    val srcHeight = source.height.toFloat()

    // Absolute corner coordinates
    val p0x = quad.topLeft.x * srcWidth
    val p0y = quad.topLeft.y * srcHeight
    val p1x = quad.topRight.x * srcWidth
    val p1y = quad.topRight.y * srcHeight
    val p2x = quad.bottomRight.x * srcWidth
    val p2y = quad.bottomRight.y * srcHeight
    val p3x = quad.bottomLeft.x * srcWidth
    val p3y = quad.bottomLeft.y * srcHeight

    // Calculate approximate width and height of quadrilateral
    val topWidth = distance(p0x, p0y, p1x, p1y)
    val bottomWidth = distance(p3x, p3y, p2x, p2y)
    val leftHeight = distance(p0x, p0y, p3x, p3y)
    val rightHeight = distance(p1x, p1y, p2x, p2y)

    val targetWidth = max(topWidth, bottomWidth).coerceAtLeast(100f)
    val targetHeight = max(leftHeight, rightHeight).coerceAtLeast(100f)

    val src = floatArrayOf(
      p0x, p0y, // top-left
      p1x, p1y, // top-right
      p2x, p2y, // bottom-right
      p3x, p3y  // bottom-left
    )
    val dst = floatArrayOf(
      0f, 0f,
      targetWidth, 0f,
      targetWidth, targetHeight,
      0f, targetHeight
    )

    val matrix = Matrix()
    matrix.setPolyToPoly(src, 0, dst, 0, 4)

    val outWidth = targetWidth.toInt().coerceIn(10, 4000)
    val outHeight = targetHeight.toInt().coerceIn(10, 4000)

    val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    canvas.drawBitmap(source, matrix, paint)
    output
  }

  /**
   * Automatic edge detection heuristic to find document boundaries.
   */
  suspend fun autoDetectDocumentEdges(bitmap: Bitmap): CropQuad = withContext(Dispatchers.Default) {
    val sampleWidth = 160
    val sampleHeight = (sampleWidth * (bitmap.height.toFloat() / bitmap.width.toFloat())).toInt().coerceAtLeast(100)
    val scaled = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)

    val pixels = IntArray(sampleWidth * sampleHeight)
    scaled.getPixels(pixels, 0, sampleWidth, 0, 0, sampleWidth, sampleHeight)

    // Calculate brightness profile to find document page bounds
    var minX = sampleWidth
    var maxX = 0
    var minY = sampleHeight
    var maxY = 0

    val threshold = 180 // typical light paper luminance threshold
    for (y in 0 until sampleHeight) {
      for (x in 0 until sampleWidth) {
        val color = pixels[y * sampleWidth + x]
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        if (lum > threshold) {
          if (x < minX) minX = x
          if (x > maxX) maxX = x
          if (y < minY) minY = y
          if (y > maxY) maxY = y
        }
      }
    }

    if (maxX > minX && maxY > minY && (maxX - minX) > sampleWidth / 4 && (maxY - minY) > sampleHeight / 4) {
      val left = (minX.toFloat() / sampleWidth).coerceIn(0.02f, 0.25f)
      val right = (maxX.toFloat() / sampleWidth).coerceIn(0.75f, 0.98f)
      val top = (minY.toFloat() / sampleHeight).coerceIn(0.02f, 0.25f)
      val bottom = (maxY.toFloat() / sampleHeight).coerceIn(0.75f, 0.98f)
      CropQuad(
        topLeft = PointF(left, top),
        topRight = PointF(right, top),
        bottomRight = PointF(right, bottom),
        bottomLeft = PointF(left, bottom)
      )
    } else {
      // Default standard document crop margins
      CropQuad(
        topLeft = PointF(0.05f, 0.05f),
        topRight = PointF(0.95f, 0.05f),
        bottomRight = PointF(0.95f, 0.95f),
        bottomLeft = PointF(0.05f, 0.95f)
      )
    }
  }

  /**
   * Overlays a signature onto a base document bitmap.
   */
  fun compositeSignature(
    documentBitmap: Bitmap,
    signatureBitmap: Bitmap,
    normalizedX: Float, // 0f..1f (center position)
    normalizedY: Float, // 0f..1f
    scale: Float = 0.3f // fraction of document width
  ): Bitmap {
    val result = documentBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val targetWidth = result.width * scale.coerceIn(0.1f, 0.8f)
    val ratio = signatureBitmap.height.toFloat() / signatureBitmap.width.toFloat()
    val targetHeight = targetWidth * ratio

    val left = (result.width * normalizedX) - (targetWidth / 2f)
    val top = (result.height * normalizedY) - (targetHeight / 2f)

    val scaledSig = Bitmap.createScaledBitmap(
      signatureBitmap,
      targetWidth.toInt().coerceAtLeast(10),
      targetHeight.toInt().coerceAtLeast(10),
      true
    )

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(scaledSig, left, top, paint)
    return result
  }

  private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
  }
}
