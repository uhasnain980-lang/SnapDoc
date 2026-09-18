package com.example.scanner

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PinLockerManager(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("snapdoc_locker_prefs", Context.MODE_PRIVATE)

  private val salt = "SnapDocSecureSalt_2026"

  companion object {
    private const val KEY_PIN_HASH = "key_pin_hash"
    @Volatile
    var isUnlockedInSession: Boolean = false
  }

  fun hasPin(): Boolean {
    return prefs.getString(KEY_PIN_HASH, null) != null
  }

  fun setPin(pin: String): Boolean {
    if (pin.length != 4 || !pin.all { it.isDigit() }) return false
    val hash = hashPin(pin)
    prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    isUnlockedInSession = true
    return true
  }

  fun verifyPin(pin: String): Boolean {
    val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
    val matches = hashPin(pin) == savedHash
    if (matches) {
      isUnlockedInSession = true
    }
    return matches
  }

  fun lockSession() {
    isUnlockedInSession = false
  }

  fun removePin(): Boolean {
    prefs.edit().remove(KEY_PIN_HASH).apply()
    isUnlockedInSession = false
    return true
  }

  private fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest((pin + salt).toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
