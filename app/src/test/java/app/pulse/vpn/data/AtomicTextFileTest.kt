package app.pulse.vpn.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AtomicTextFileTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun replacesContentsAndKeepsUtf8() {
        val file = File(temporary.root, "profile.json")
        file.writeAtomicText("old")
        file.writeAtomicText("Новая подписка 🌊")
        assertEquals("Новая подписка 🌊", file.readText())
    }
    @Test fun leavesNoStagingFilesAfterSuccess() {
        File(temporary.root, "config.json").writeAtomicText("config")
        assertEquals(listOf("config.json"), temporary.root.list()!!.toList())
    }
    @Test fun failedReplacementCleansStagingAndPreservesDestination() {
        val folder = temporary.newFolder("config.json")
        val child = File(folder, "original").apply { writeText("keep") }
        assertThrows(Exception::class.java) { folder.writeAtomicText("new") }
        assertEquals("keep", child.readText())
        assertFalse(temporary.root.list()!!.any { it.endsWith(".pending") })
    }
    @Test fun concurrentWritersNeverMixContents() {
        val file = File(temporary.root, "config.json")
        val first = "A".repeat(20_000)
        val second = "Б".repeat(20_000)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val workers = listOf(executor.submit { repeat(10) { file.writeAtomicText(first) } }, executor.submit { repeat(10) { file.writeAtomicText(second) } })
            workers.forEach { it.get() }
        } finally { executor.shutdownNow() }
        assertTrue(file.readText() in listOf(first, second))
        assertFalse(temporary.root.list()!!.any { it.endsWith(".pending") })
    }
}
