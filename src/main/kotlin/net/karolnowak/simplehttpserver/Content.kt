package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.MILLISECONDS

interface Content {
    fun asByteArray(): ByteArray
    fun type(): ContentType
    fun length() = asByteArray().size
}

data class ContentType(val raw: String)

class FileContent(path: Path) : Content {
    private val FALLBACK_CONTENT_TYPE = ContentType("binary/octet-stream")
    private val CONTENT_TYPE_CHECK_TIMEOUT_MS = 50L

    private val contentType = getContentType(path)

    private val byteArray = Files.readAllBytes(path)

    override fun asByteArray(): ByteArray = byteArray

    override fun type() = contentType


    private fun getContentType(path: Path): ContentType {
        if (!Files.exists(path)) {
            throw IllegalStateException("Cannot get Content Type of path $path") //security check
        }
        val contentType = executeFileCommand(path)
        return contentType ?: FALLBACK_CONTENT_TYPE.also { println("Failed to obtain file's Content Type") }
    }

    private fun executeFileCommand(path: Path): ContentType? {
        return try {
            val fileTypeCheck = Runtime.getRuntime().exec("file -I $path")
            fileTypeCheck.waitFor(CONTENT_TYPE_CHECK_TIMEOUT_MS, MILLISECONDS)
            if (fileTypeCheck!!.exitValue() != 0) return null
            val fileResult = fileTypeCheck
                .inputStream.readAllBytes()
                .let { String(it) }
            val mimeType = fileResult.split(":")[1].trim()
            if (mimeType.isBlank()) return null
            ContentType(mimeType)
        } catch (ex: Exception) {
            null
        }
    }
}

class DirectoryListing(files: List<Path>) : Content {
    private val sortedFiles = files.sortedWith(
        compareByDescending<Path> { Files.isDirectory(it) } // directories first
            .thenComparing(compareBy { it.fileName }))

    override fun asByteArray() = listFiles()

    override fun type() = ContentType("text/html; charset=UTF-8")

    private fun listFiles() =
        """<!DOCTYPE html>
          |<body>
          |   <ul>
          |      ${sortedFiles.joinToString(separator = "") { it.toHtmlListItem() }}
          |   </ul>
          |</body>
        """.trimMargin("|").toByteArray()
}

private fun Path.toHtmlListItem(): String = """<li><a href="${this.fileName}"><b>${this.fileName}</b></a></li>"""

class NoContent : Content {
    override fun asByteArray() = ByteArray(0)
    override fun type() = throw IllegalStateException("NoContent cannot be asked for type")
    override fun length() = 0
}