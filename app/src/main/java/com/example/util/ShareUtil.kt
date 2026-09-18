package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {

  /**
   * Shares a PDF file using Android's native share sheet.
   */
  fun sharePdf(context: Context, pdfFile: File, title: String = "Share Document") {
    if (!pdfFile.exists()) return

    val uri: Uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      pdfFile
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      putExtra(Intent.EXTRA_SUBJECT, pdfFile.nameWithoutExtension)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  /**
   * Shares plain text (e.g. OCR extracted text) via Android's native share system.
   */
  fun shareText(context: Context, text: String, subject: String = "Extracted Document Text") {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, text)
      putExtra(Intent.EXTRA_SUBJECT, subject)
    }

    val chooser = Intent.createChooser(intent, "Share Extracted Text").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  /**
   * Shares image files (e.g. PDF to images export) via Android native share.
   */
  fun shareImages(context: Context, imageFiles: List<File>, title: String = "Share Pages") {
    if (imageFiles.isEmpty()) return

    val uris = ArrayList<Uri>()
    for (file in imageFiles) {
      if (file.exists()) {
        uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
      }
    }

    val intent = if (uris.size == 1) {
      Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uris.first())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    } else {
      Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    }

    val chooser = Intent.createChooser(intent, title).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
  }

  /**
   * Opens a PDF in an external PDF viewer.
   */
  fun openPdf(context: Context, pdfFile: File) {
    if (!pdfFile.exists()) return

    val uri = FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      pdfFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
      context.startActivity(intent)
    } catch (e: Exception) {
      // Fallback to chooser
      val chooser = Intent.createChooser(intent, "Open PDF").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(chooser)
    }
  }
}
