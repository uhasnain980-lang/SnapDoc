package com.example.scanner

import android.graphics.Bitmap
import android.graphics.PointF
import java.util.UUID

data class ScannedPage(
  val id: String = UUID.randomUUID().toString(),
  val originalBitmap: Bitmap,
  var enhancedBitmap: Bitmap = originalBitmap,
  var quad: CropQuad = CropQuad(),
  var rotationDegrees: Float = 0f,
  var filterType: FilterType = FilterType.AUTO,
  var brightness: Float = 0f,
  var contrast: Float = 0f,
  var extractedText: String? = null,
  var signatureBitmap: Bitmap? = null,
  var signatureX: Float = 0.5f,
  var signatureY: Float = 0.8f,
  var signatureScale: Float = 0.35f
)
