package app.palettez.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the Baseline Profile for Palettez (T4 — PRD: "Baseline Profiles shipped").
 *
 * Run once against a rooted/emulator device:
 *   ./gradlew :macrobenchmark:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect(
            packageName = "app.palettez",
        ) {
            // 1. Launch the app and wait for the tab bar to load
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.pkg("app.palettez")), 5_000L)
            device.waitForIdle(1_000L)

            // 2. Scroll the Palette tab (startup path, extraction UI)
            repeat(3) {
                device.swipe(540, 1400, 540, 600, 10)
                device.waitForIdle(200L)
            }

            // 3. Navigate to Fonts tab and scroll the catalog list
            val fontsTab = device.findObject(By.desc("Fonts"))
            fontsTab?.let {
                it.click()
                device.waitForIdle(1_000L)
                repeat(5) {
                    device.swipe(540, 1400, 540, 400, 10)
                    device.waitForIdle(200L)
                }
            }

            // 4. Navigate to Grid tab
            val gridTab = device.findObject(By.desc("Grid"))
            gridTab?.let {
                it.click()
                device.waitForIdle(1_000L)
            }
        }
    }
}
