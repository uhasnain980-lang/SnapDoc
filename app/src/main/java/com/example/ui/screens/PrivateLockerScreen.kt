package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.scanner.PinLockerManager
import com.example.ui.MainViewModel
import com.example.util.ShareUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateLockerScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val pinLocker = viewModel.pinLocker
  val lockedDocs by viewModel.lockedDocuments.collectAsStateWithLifecycle()

  var isSessionUnlocked by remember { mutableStateOf(PinLockerManager.isUnlockedInSession) }
  var enteredPin by remember { mutableStateOf("") }
  var setupFirstPin by remember { mutableStateOf<String?>(null) }
  var pinError by remember { mutableStateOf<String?>(null) }

  val hasExistingPin = remember { pinLocker.hasPin() }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isSessionUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.private_locker),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (isSessionUnlocked) {
            TextButton(
              onClick = {
                pinLocker.lockSession()
                isSessionUnlocked = false
                enteredPin = ""
              }
            ) {
              Text("Lock Now")
            }
          }
        }
      )
    }
  ) { paddingValues ->
    if (!isSessionUnlocked) {
      // PIN Keypad UI
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 32.dp)
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = when {
              !hasExistingPin && setupFirstPin == null -> "Create 4-Digit Locker PIN"
              !hasExistingPin && setupFirstPin != null -> "Confirm Your 4-Digit PIN"
              else -> "Enter Locker PIN"
            },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Keep personal documents and sensitive IDs local and protected.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (pinError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = pinError!!,
              color = MaterialTheme.colorScheme.error,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
          }

          Spacer(modifier = Modifier.height(28.dp))

          // 4 PIN Dots Indicator
          Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for (i in 0 until 4) {
              val isFilled = i < enteredPin.length
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(
                    if (isFilled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                  )
                  .border(
                    1.5.dp,
                    if (isFilled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    CircleShape
                  )
              )
            }
          }
        }

        // Numeric Keypad
        NumericKeypad(
          onDigitClick = { digit ->
            if (enteredPin.length < 4) {
              val updated = enteredPin + digit
              enteredPin = updated
              pinError = null

              if (updated.length == 4) {
                if (!hasExistingPin) {
                  // Setup mode
                  if (setupFirstPin == null) {
                    setupFirstPin = updated
                    enteredPin = ""
                  } else {
                    if (updated == setupFirstPin) {
                      pinLocker.setPin(updated)
                      isSessionUnlocked = true
                    } else {
                      pinError = "PINs did not match. Try again."
                      setupFirstPin = null
                      enteredPin = ""
                    }
                  }
                } else {
                  // Verification mode
                  if (pinLocker.verifyPin(updated)) {
                    isSessionUnlocked = true
                  } else {
                    pinError = "Incorrect PIN. Please try again."
                    enteredPin = ""
                  }
                }
              }
            }
          },
          onBackspace = {
            if (enteredPin.isNotEmpty()) {
              enteredPin = enteredPin.dropLast(1)
              pinError = null
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 24.dp)
        )
      }
    } else {
      // Unlocked Documents List
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Locker Unlocked (${lockedDocs.size} documents)",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                  text = "Protected documents are excluded from main search and recent list.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        if (lockedDocs.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  Icons.Default.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "No locked documents",
                  style = MaterialTheme.typography.titleSmall
                )
                Text(
                  text = "Use the 'Move to Private Locker' option on any document to protect it.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          items(lockedDocs, key = { it.id }) { doc ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Default.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                  )
                  Text(
                    text = "${doc.pageCount} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Open
                IconButton(onClick = { ShareUtil.openPdf(context, File(doc.pdfPath)) }) {
                  Icon(Icons.Default.PictureAsPdf, contentDescription = "Open")
                }

                // Share
                IconButton(onClick = { ShareUtil.sharePdf(context, File(doc.pdfPath), doc.title) }) {
                  Icon(Icons.Default.Share, contentDescription = "Share")
                }

                // Unlock back to main library
                IconButton(onClick = { viewModel.toggleDocumentLock(doc.id, false) }) {
                  Icon(Icons.Default.LockOpen, contentDescription = "Unlock from Locker", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun NumericKeypad(
  onDigitClick: (String) -> Unit,
  onBackspace: () -> Unit,
  modifier: Modifier = Modifier
) {
  val rows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", "DEL")
  )

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    for (row in rows) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        for (item in row) {
          if (item.isEmpty()) {
            Spacer(modifier = Modifier.size(68.dp))
          } else if (item == "DEL") {
            Box(
              modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .clickable { onBackspace() },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            Box(
              modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .clickable { onDigitClick(item) },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = item,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }
}
