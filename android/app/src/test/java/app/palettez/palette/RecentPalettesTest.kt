package app.palettez.palette

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class RecentPalettesTest {

    private lateinit var tempDir: File
    private lateinit var repo: RecentPalettesRepository

    @Before
    fun setUp() {
        tempDir = File.createTempFile("palettez_test_recents", "").apply {
            delete()
            mkdirs()
        }
        repo = RecentPalettesRepository(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun sampleSwatches(n: Int = 3): List<Swatch> {
        return (0 until n).map { i ->
            Swatch(
                r = (i * 50).coerceIn(0, 255),
                g = (i * 60).coerceIn(0, 255),
                b = (i * 70).coerceIn(0, 255),
                share = 1f / n,
            )
        }
    }

    @Test
    fun PA_recent_saveAndRetrieve() {
        assertTrue(repo.getAll().isEmpty())

        val swatches = sampleSwatches(5)
        val saved = repo.saveOrUpdate(
            id = "test-1",
            swatches = swatches,
            name = "Test Sunset",
        )

        assertEquals("test-1", saved.id)
        assertEquals("Test Sunset", saved.name)
        assertEquals(5, saved.swatches.size)

        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals("test-1", all[0].id)
        assertEquals(5, all[0].swatches.size)
        assertEquals(swatches[0].hex, all[0].swatches[0].hex)
    }

    @Test
    fun PA_recent_updateExistingSessionDoesNotDuplicate() {
        repo.saveOrUpdate("session-1", sampleSwatches(3), name = "Initial")
        assertEquals(1, repo.getAll().size)
        assertEquals(3, repo.getAll()[0].swatches.size)

        // User changes color count on the same image
        repo.saveOrUpdate("session-1", sampleSwatches(6), name = "Updated")
        val all = repo.getAll()
        assertEquals(1, all.size)
        assertEquals(6, all[0].swatches.size)
        assertEquals("Updated", all[0].name)
    }

    @Test
    fun PA_recent_deleteRemovesItem() {
        repo.saveOrUpdate("item-1", sampleSwatches(2))
        repo.saveOrUpdate("item-2", sampleSwatches(4))
        assertEquals(2, repo.getAll().size)

        repo.delete("item-1")
        val remaining = repo.getAll()
        assertEquals(1, remaining.size)
        assertEquals("item-2", remaining[0].id)
    }

    @Test
    fun PA_recent_clearAllRemovesEverything() {
        repo.saveOrUpdate("item-1", sampleSwatches(2))
        repo.saveOrUpdate("item-2", sampleSwatches(3))
        repo.saveOrUpdate("item-3", sampleSwatches(4))
        assertEquals(3, repo.getAll().size)

        repo.clearAll()
        assertTrue(repo.getAll().isEmpty())
    }

    @Test
    fun PA_recent_maxCapacityEviction() {
        for (i in 1..25) {
            repo.saveOrUpdate("item-$i", sampleSwatches(2), name = "Palette $i")
        }

        val all = repo.getAll()
        assertEquals(20, all.size)
        // Most recent should be item-25
        assertEquals("item-25", all.first().id)
        // Oldest kept should be item-6 (items 1 to 5 evicted)
        assertEquals("item-6", all.last().id)
    }

    @Test
    fun PA_recent_relativeTimeFormatting() {
        val recent = RecentPalette(
            id = "format-test",
            name = "Test",
            timestamp = System.currentTimeMillis(),
            swatches = sampleSwatches(2),
        )
        assertEquals("Just now", recent.formattedTime())
    }
}
