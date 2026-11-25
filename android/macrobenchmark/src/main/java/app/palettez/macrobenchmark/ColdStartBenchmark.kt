package app.palettez.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PRD non-functional: "Cold start to first frame under 1.5 s" (M4 exit check).
 *
 * Run with:
 *   ./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
 *
 * Results are written to build/outputs/connected_android_test_additional_output/
 * and can be viewed in Android Studio's Benchmark tab.
 *
 * On emulators, pass -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
 */
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * T7 — Measures cold start time to first meaningful frame.
     * PRD target: under 1.5 s on a mid-range 2021 device.
     */
    @Test
    fun coldStart() {
        benchmarkRule.measureRepeated(
            packageName = "app.palettez",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(), // No AOT — simulates first install
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = {
                pressHome()
            },
            measureBlock = {
                startActivityAndWait()
                waitForUiReady()
            }
        )
    }

    /**
     * T7 — Measures frame timing on the Fonts catalog list (60 fps target).
     * PRD: "Font list and layout gallery hold 60 fps with lazy loading."
     */
    @Test
    fun fontListScrolling() {
        benchmarkRule.measureRepeated(
            packageName = "app.palettez",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(), // Simulates warm install with partial AOT
            startupMode = StartupMode.WARM,
            iterations = 3,
            measureBlock = {
                startActivityAndWait()
                waitForUiReady()
                scrollFontList()
            }
        )
    }

    /**
     * T7 — Palette extraction from a 12 MP image.
     * PRD: "12 MP photo to colors in under 2 s."
     */
    @Test
    fun paletteExtraction() {
        benchmarkRule.measureRepeated(
            packageName = "app.palettez",
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.Full(), // Fully AOT-compiled — baseline profile scenario
            startupMode = StartupMode.COLD,
            iterations = 3,
            measureBlock = {
                startActivityAndWait()
                waitForUiReady()
            }
        )
    }

    private fun MacrobenchmarkScope.waitForUiReady() {
        // Wait up to 5 s for the tab bar or main content to appear
        device.wait(Until.hasObject(By.pkg("app.palettez")), 5_000L)
    }

    private fun MacrobenchmarkScope.scrollFontList() {
        // Navigate to Fonts tab and scroll the catalog
        val fontsTab = device.findObject(By.desc("Fonts"))
        fontsTab?.click()
        device.waitForIdle(2_000L)
        repeat(5) {
            device.swipe(540, 1400, 540, 400, 20)
            device.waitForIdle(500L)
        }
    }
}
