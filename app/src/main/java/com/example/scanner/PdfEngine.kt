package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class PdfPageSize {
  A4,
  ORIGINAL
}

enum class PdfOrientation {
  PORTRAIT,
  LANDSCAPE
}

enum class ImageQuality(val compressionPercent: Int) {
  HIGH(90),
  MEDIUM(70),
  ECONOMY(45)
}

data class PdfGenerationOptions(
  val filename: String = "SnapDoc_${System.currentTimeMillis()}",
  val pageSize: PdfPageSize = PdfPageSize.A4,
  val orientation: PdfOrientation = PdfOrientation.PORTRAIT,
  val quality: ImageQuality = ImageQuality.HIGH
)

object PdfEngine {

  // A4 dimensions at 72 DPI (Standard PDF points)
  private const val A4_PORTRAIT_WIDTH = 595
  private const val A4_PORTRAIT_HEIGHT = 842

  /**
   * Generates a multi-page PDF from a list of page Bitmaps and saves to app's document directory.
   */
  suspend fun createPdfFromBitmaps(
    context: Context,
    bitmaps: List<Bitmap>,
    options: PdfGenerationOptions
  ): File = withContext(Dispatchers.IO) {
    val documentsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
    val cleanFilename = if (options.filename.endsWith(".pdf", ignoreCase = true)) {
      options.filename
    } else {
      "${options.filename}.pdf"
    }
    val pdfFile = File(documentsDir, cleanFilename)

    val document = PdfDocument()

    try {
      for (i in bitmaps.indices) {
        val originalBitmap = bitmaps[i]

        // Compress bitmap according to quality
        val compressedBitmap = compressBitmap(originalBitmap, options.quality.compressionPercent)

        val (pageWidth, pageHeight) = when (options.pageSize) {
          PdfPageSize.A4 -> {
            if (options.orientation == PdfOrientation.PORTRAIT) {
              A4_PORTRAIT_WIDTH to A4_PORTRAIT_HEIGHT
            } else {
              A4_PORTRAIT_HEIGHT to A4_PORTRAIT_WIDTH
            }
          }
          PdfPageSize.ORIGINAL -> {
            compressedBitmap.width to compressedBitmap.height
          }
        }

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Compute aspect-fit destination rect
        val srcRatio = compressedBitmap.width.toFloat() / compressedBitmap.height.toFloat()
        val dstRatio = pageWidth.toFloat() / pageHeight.toFloat()

        val dstRect = if (srcRatio > dstRatio) {
          // Wider than target: scale by width
          val drawHeight = (pageWidth / srcRatio).toInt()
          val topOffset = (pageHeight - drawHeight) / 2
          Rect(0, topOffset, pageWidth, topOffset + drawHeight)
        } else {
          // Taller than target: scale by height
          val drawWidth = (pageHeight * srcRatio).toInt()
          val leftOffset = (pageWidth - drawWidth) / 2
          Rect(leftOffset, 0, leftOffset + drawWidth, pageHeight)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(compressedBitmap, null, dstRect, paint)

        document.finishPage(page)
      }

      FileOutputStream(pdfFile).use { fos ->
        document.writeTo(fos)
      }
    } finally {
      document.close()
    }

    pdfFile
  }

  /**
   * Generates a thumbnail JPG for a given bitmap and saves to cache/internal.
   */
  suspend fun saveThumbnail(
    context: Context,
    bitmap: Bitmap,
    filename: String
  ): File = withContext(Dispatchers.IO) {
    val thumbsDir = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
    val thumbFile = File(thumbsDir, "thumb_$filename.jpg")

    val maxDimension = 320
    val scale = minOf(
      maxDimension.toFloat() / bitmap.width,
      maxDimension.toFloat() / bitmap.height,
      1.0f
    )
    val scaled = Bitmap.createScaledBitmap(
      bitmap,
      (bitmap.width * scale).toInt().coerceAtLeast(1),
      (bitmap.height * scale).toInt().coerceAtLeast(1),
      true
    )

    FileOutputStream(thumbFile).use { fos ->
      scaled.compress(Bitmap.CompressFormat.JPEG, 85, fos)
    }
    thumbFile
  }

  /**
   * Converts a PDF file into a list of exported images (JPG or PNG).
   */
  suspend fun exportPdfToImages(
    context: Context,
    pdfFile: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
  ): List<File> = withContext(Dispatchers.IO) {
    val exportedFiles = mutableListOf<File>()
    val exportDir = File(context.filesDir, "exported_images").apply { if (!exists()) mkdirs() }

    val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(fileDescriptor)

    try {
      val baseName = pdfFile.nameWithoutExtension
      val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"

      for (pageIndex in 0 until renderer.pageCount) {
        val page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(
          page.width * 2, // 2x resolution for clean export
          page.height * 2,
          Bitmap.Config.ARGB_8888
        )
        // White background for transparent PDF backgrounds
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        page.close()

        val imageFile = File(exportDir, "${baseName}_page_${pageIndex + 1}.$extension")
        FileOutputStream(imageFile).use { fos ->
          bitmap.compress(format, 95, fos)
        }
        exportedFiles.add(imageFile)
      }
    } finally {
      renderer.close()
      fileDescriptor.close()
    }

    exportedFiles
  }

  /**
   * Renders the first page of a PDF as a Bitmap thumbnail.
   */
  suspend fun renderPdfThumbnail(
    pdfFile: File
  ): Bitmap? = withContext(Dispatchers.IO) {
    try {
      val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
      val renderer = PdfRenderer(fileDescriptor)
      if (renderer.pageCount == 0) {
        renderer.close()
        fileDescriptor.close()
        return@withContext null
      }
      val page = renderer.openPage(0)
      val bitmap = Bitmap.createBitmap(
        page.width,
        page.height,
        Bitmap.Config.ARGB_8888
      )
      val canvas = Canvas(bitmap)
      canvas.drawColor(android.graphics.Color.WHITE)
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
      page.close()
      renderer.close()
      fileDescriptor.close()
      bitmap
    } catch (e: Exception) {
      null
    }
  }

  private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
    if (quality >= 95) return bitmap
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    val byteArray = stream.toByteArray()
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
  }
}
