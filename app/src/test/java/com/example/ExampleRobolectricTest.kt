package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.scanner.PinLockerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SnapDoc AI", appName)
  }

  @Test
  fun `test pin locker manager flow`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val locker = PinLockerManager(context)

    locker.removePin()
    assertFalse(locker.hasPin())

    val setSuccess = locker.setPin("1234")
    assertTrue(setSuccess)
    assertTrue(locker.hasPin())

    assertTrue(locker.verifyPin("1234"))
    assertFalse(locker.verifyPin("9999"))

    locker.lockSession()
    assertFalse(PinLockerManager.isUnlockedInSession)
  }
}
