package com.almajma.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.almajma.app.ui.screens.AlMajmaAppUi
import com.almajma.app.ui.theme.AlMajmaTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { 
      AlMajmaTheme { 
        androidx.compose.material3.Text("المَجْمَع") 
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun app_screenshot() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.almajma.app.ui.PlatformViewModel(application)
    
    composeTestRule.setContent {
      AlMajmaTheme {
        AlMajmaAppUi(viewModel = viewModel)
      }
    }

    // Capture visual representation of the app's initial state
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/app_initial.png")
  }
}
