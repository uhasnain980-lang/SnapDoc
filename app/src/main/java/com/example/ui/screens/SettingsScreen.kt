package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.components.AdConfig
import com.example.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel,
  onNavigateBack: () -> Unit
) {
  val context = LocalContext.current
  val currentThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
  val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
          Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Language Selector
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.language),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // English
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.setLanguage("en")
                val locales = LocaleListCompat.forLanguageTags("en")
                AppCompatDelegate.setApplicationLocales(locales)
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentLanguage == "en",
              onClick = {
                viewModel.setLanguage("en")
                val locales = LocaleListCompat.forLanguageTags("en")
                AppCompatDelegate.setApplicationLocales(locales)
              }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("English", style = MaterialTheme.typography.bodyLarge)
          }

          // Urdu (اردو)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.setLanguage("ur")
                val locales = LocaleListCompat.forLanguageTags("ur")
                AppCompatDelegate.setApplicationLocales(locales)
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentLanguage == "ur",
              onClick = {
                viewModel.setLanguage("ur")
                val locales = LocaleListCompat.forLanguageTags("ur")
                AppCompatDelegate.setApplicationLocales(locales)
              }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("اردو (Urdu)", style = MaterialTheme.typography.bodyLarge)
          }
        }
      }

      // Theme Mode Selector
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = stringResource(R.string.theme),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // System
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentThemeMode == AppThemeMode.SYSTEM,
              onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("System Default", style = MaterialTheme.typography.bodyMedium)
          }

          // Light
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.setThemeMode(AppThemeMode.LIGHT) }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentThemeMode == AppThemeMode.LIGHT,
              onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.light_theme), style = MaterialTheme.typography.bodyMedium)
          }

          // Dark
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.setThemeMode(AppThemeMode.DARK) }
              .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = currentThemeMode == AppThemeMode.DARK,
              onClick = { viewModel.setThemeMode(AppThemeMode.DARK) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.dark_theme), style = MaterialTheme.typography.bodyMedium)
          }
        }
      }

      // Security & Private Locker Reset
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Privacy & Security",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "• 100% Offline-First architecture\n• All documents are processed on-device\n• Zero cloud uploads or third-party servers\n• Local SHA-256 protected Private Locker",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
          )

          if (viewModel.pinLocker.hasPin()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.pinLocker.removePin()
                  Toast.makeText(context, "Locker PIN removed.", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Reset Private Locker PIN", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }

      // AdMob Ready Notice & About
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "About SnapDoc AI",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Version 1.0.0 (Production Release)\nTurn your smartphone camera into a powerful document scanner. Scan documents, receipts, notes and IDs, clean the image, create PDFs and extract text with on-device OCR.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
