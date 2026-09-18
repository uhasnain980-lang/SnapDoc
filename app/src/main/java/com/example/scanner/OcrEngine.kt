package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

sealed class OcrResult {
  data class Success(val text: String) : OcrResult()
  data class Error(val message: String) : OcrResult()
}

object OcrEngine {

  /**
   * Extracts text from a Bitmap using Google ML Kit on-device text recognition.
   */
  suspend fun extractText(bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
    try {
      val inputImage = InputImage.fromBitmap(bitmap, 0)
      val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

      suspendCancellableCoroutine { continuation ->
        recognizer.process(inputImage)
          .addOnSuccessListener { visionText ->
            val recognizedText = visionText.text.trim()
            if (recognizedText.isEmpty()) {
              continuation.resume(OcrResult.Success(""))
            } else {
              continuation.resume(OcrResult.Success(recognizedText))
            }
          }
          .addOnFailureListener { exception ->
            continuation.resume(
              OcrResult.Error(
                exception.localizedMessage
                  ?: "On-device OCR failed. Ensure Google Play Services is available and up to date."
              )
            )
          }
      }
    } catch (e: Throwable) {
      OcrResult.Error(
        e.localizedMessage
          ?: "On-device OCR is currently unavailable on this device configuration."
      )
    }
  }

  /**
   * Saves extracted text as a .TXT file in the app's files directory.
   */
  suspend fun saveAsTxt(context: Context, filename: String, content: String): File =
    withContext(Dispatchers.IO) {
      val txtDir = File(context.filesDir, "extracted_texts").apply { if (!exists()) mkdirs() }
      val cleanName = if (filename.endsWith(".txt", ignoreCase = true)) filename else "$filename.txt"
      val file = File(txtDir, cleanName)
      FileOutputStream(file).use { fos ->
        fos.write(content.toByteArray(Charsets.UTF_8))
      }
      file
    }
}
