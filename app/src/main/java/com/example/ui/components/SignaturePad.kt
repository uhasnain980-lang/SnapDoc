package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class SignatureStroke(
  val path: Path,
  val color: Color,
  val strokeWidth: Float
)

@Composable
fun SignatureDialog(
  onDismiss: () -> Unit,
  onSignatureCreated: (Bitmap) -> Unit
) {
  val strokes = remember { mutableStateListOf<SignatureStroke>() }
  var currentPath by remember { mutableStateOf<Path?>(null) }
  var selectedColor by remember { mutableStateOf(Color.Black) }
  val strokeWidth = 6f

  val colorOptions = listOf(
    Color.Black to "Black",
    Color(0xFF1D4ED8) to "Blue",
    Color(0xFFDC2626) to "Red"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Draw Signature",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Row {
            IconButton(
              onClick = {
                if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
              },
              enabled = strokes.isNotEmpty(),
              modifier = Modifier.testTag("undo_signature")
            ) {
              Icon(Icons.Default.Undo, contentDescription = "Undo")
            }
            IconButton(
              onClick = { strokes.clear() },
              enabled = strokes.isNotEmpty(),
              modifier = Modifier.testTag("clear_signature")
            ) {
              Icon(Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Drawing Canvas
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .pointerInput(selectedColor) {
                detectDragGestures(
                  onDragStart = { offset ->
                    val newPath = Path().apply { moveTo(offset.x, offset.y) }
                    currentPath = newPath
                  },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    currentPath?.lineTo(
                      change.position.x,
                      change.position.y
                    )
                  },
                  onDragEnd = {
                    currentPath?.let {
                      strokes.add(SignatureStroke(it, selectedColor, strokeWidth))
                      currentPath = null
                    }
                  }
                )
              }
          ) {
            for (stroke in strokes) {
              drawPath(
                path = stroke.path,
                color = stroke.color,
                style = Stroke(
                  width = stroke.strokeWidth,
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round
                )
              )
            }
            currentPath?.let {
              drawPath(
                path = it,
                color = selectedColor,
                style = Stroke(
                  width = strokeWidth,
                  cap = StrokeCap.Round,
                  join = StrokeJoin.Round
                )
              )
            }
          }

          if (strokes.isEmpty() && currentPath == null) {
            Text(
              text = "Sign with finger here",
              color = Color.LightGray,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.align(Alignment.Center)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Color selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Ink Color: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(8.dp))
          colorOptions.forEach { (color, _) ->
            Box(
              modifier = Modifier
                .padding(horizontal = 6.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (selectedColor == color) 2.5.dp else 1.dp,
                  color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.LightGray,
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              IconButton(onClick = { selectedColor = color }) {
                if (selectedColor == color) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("cancel_signature")
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (strokes.isNotEmpty()) {
                val bitmap = createSignatureBitmap(strokes, 400, 200)
                onSignatureCreated(bitmap)
              }
              onDismiss()
            },
            enabled = strokes.isNotEmpty(),
            modifier = Modifier.testTag("save_signature")
          ) {
            Text("Add to Document")
          }
        }
      }
    }
  }
}

/**
 * Creates transparent bitmap from strokes.
 */
private fun createSignatureBitmap(
  strokes: List<SignatureStroke>,
  width: Int,
  height: Int
): Bitmap {
  val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = android.graphics.Canvas(bitmap)

  for (stroke in strokes) {
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = stroke.color.toArgb()
      strokeWidth = stroke.strokeWidth
      style = AndroidPaint.Style.STROKE
      strokeCap = AndroidPaint.Cap.ROUND
      strokeJoin = AndroidPaint.Join.ROUND
    }
    canvas.drawPath(stroke.path.asAndroidPath(), paint)
  }

  return bitmap
}
