package app.pulse.vpn.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Readers see either the old complete file or the new complete file, never a truncated config. */
internal fun File.writeAtomicText(value: String) {
    val directory = requireNotNull(parentFile)
    check(directory.isDirectory || directory.mkdirs())
    val staged = File.createTempFile(name + ".", ".pending", directory)
    try {
        FileOutputStream(staged).use { stream ->
            stream.write(value.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        try {
            Files.move(staged.toPath(), toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(staged.toPath(), toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        staged.delete()
    }
}

