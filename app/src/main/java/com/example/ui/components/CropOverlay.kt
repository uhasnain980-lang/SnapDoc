package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.scanner.CropQuad
import kotlin.math.sqrt

@Composable
fun InteractiveCropView(
  bitmap: Bitmap,
  cropQuad: CropQuad,
  onCropQuadChanged: (CropQuad) -> Unit,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val cornerRadiusPx = with(density) { 14.dp.toPx() }
  val primaryColor = MaterialTheme.colorScheme.primary

  BoxWithConstraints(modifier = modifier) {
    val containerWidth = maxWidth.value
    val containerHeight = maxHeight.value

    // Calculate aspect fit dimensions for image inside container
    val imgRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val contRatio = containerWidth / containerHeight

    val (renderedWidth, renderedHeight) = if (imgRatio > contRatio) {
      containerWidth to (containerWidth / imgRatio)
    } else {
      (containerHeight * imgRatio) to containerHeight
    }

    var quad by remember(cropQuad) { mutableStateOf(cropQuad) }
    var draggingCorner by remember { mutableStateOf<Int?>(null) } // 0: TL, 1: TR, 2: BR, 3: BL

    Box(
      modifier = Modifier
        .size(renderedWidth.dp, renderedHeight.dp)
        .pointerInput(quad) {
          detectDragGestures(
            onDragStart = { offset ->
              val normX = (offset.x / size.width).coerceIn(0f, 1f)
              val normY = (offset.y / size.height).coerceIn(0f, 1f)

              val corners = listOf(
                quad.topLeft,
                quad.topRight,
                quad.bottomRight,
                quad.bottomLeft
              )

              // Find closest corner within touch radius
              var closestIdx = -1
              var minDistance = Float.MAX_VALUE
              for (i in corners.indices) {
                val c = corners[i]
                val d = sqrt((c.x - normX) * (c.x - normX) + (c.y - normY) * (c.y - normY))
                if (d < minDistance) {
                  minDistance = d
                  closestIdx = i
                }
              }

              // Threshold in normalized coordinates (~48dp)
              if (minDistance < 0.2f) {
                draggingCorner = closestIdx
              } else {
                draggingCorner = null
              }
            },
            onDrag = { change, _ ->
              change.consume()
              val targetIdx = draggingCorner ?: return@detectDragGestures
              val newX = (change.position.x / size.width).coerceIn(0f, 1f)
              val newY = (change.position.y / size.height).coerceIn(0f, 1f)

              val updated = when (targetIdx) {
                0 -> quad.copy(topLeft = PointF(newX, newY))
                1 -> quad.copy(topRight = PointF(newX, newY))
                2 -> quad.copy(bottomRight = PointF(newX, newY))
                3 -> quad.copy(bottomLeft = PointF(newX, newY))
                else -> quad
              }
              quad = updated
              onCropQuadChanged(updated)
            },
            onDragEnd = {
              draggingCorner = null
            }
          )
        }
    ) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Document Page",
        modifier = Modifier.fillMaxSize()
      )

      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val p0 = Offset(quad.topLeft.x * w, quad.topLeft.y * h)
        val p1 = Offset(quad.topRight.x * w, quad.topRight.y * h)
        val p2 = Offset(quad.bottomRight.x * w, quad.bottomRight.y * h)
        val p3 = Offset(quad.bottomLeft.x * w, quad.bottomLeft.y * h)

        // Semi-transparent darkened overlay outside crop area
        val fullPath = Path().apply {
          moveTo(0f, 0f)
          lineTo(w, 0f)
          lineTo(w, h)
          lineTo(0f, h)
          close()
        }
        val cropPath = Path().apply {
          moveTo(p0.x, p0.y)
          lineTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
          close()
        }

        // Draw bounding lines
        drawPath(
          path = cropPath,
          color = primaryColor,
          style = Stroke(width = 3.dp.toPx())
        )

        // Draw 4 corner handles
        val cornerPoints = listOf(p0, p1, p2, p3)
        for (pt in cornerPoints) {
          drawCircle(
            color = Color.White,
            radius = cornerRadiusPx,
            center = pt
          )
          drawCircle(
            color = primaryColor,
            radius = cornerRadiusPx * 0.7f,
            center = pt
          )
        }
      }
    }
  }
}
