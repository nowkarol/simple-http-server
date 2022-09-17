package net.karolnowak.simplehttpserver

import net.karolnowak.simplehttpserver.request.NoRange
import net.karolnowak.simplehttpserver.request.Range
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.MILLISECONDS

interface Content {
    fun asByteArray(range: Range = NoRange()): ByteArray
    fun type(): ContentType
    fun length(range: Range = NoRange()) = asByteArray(range).size
}

data class ContentType(val raw: String)

class FileContent(path: Path) : Content {
    private val FALLBACK_CONTENT_TYPE = ContentType("binary/octet-stream")
    private val CONTENT_TYPE_CHECK_TIMEOUT_MS = 50L

    private val contentType = getContentType(path)
    private val byteArray = Files.readAllBytes(path)

    override fun asByteArray(range: Range): ByteArray =
        byteArray.sliceArray(range.asIntRange(byteArray.size))

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
            val fileTypeCheck = Runtime.getRuntime().exec(arrayOf("file", "-I", "$path"))
            // cannot use exec with single String method because it tokenizes path with spaces despite surrounding it with quotes
            // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4272706
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

class DirectoryListing(files: List<Path>, private val basePath: Path) : Content {
    private val sortedFiles = files.sortedWith(
        compareByDescending<Path> { Files.isDirectory(it) } // directories first
            .thenComparing(compareBy { it.fileName }))

    override fun asByteArray(range: Range): ByteArray {
        val listingBytes = listFiles()
        return  listingBytes.sliceArray(range.asIntRange(listingBytes.size))
    }


    override fun type() = ContentType("text/html; charset=UTF-8")
    private val UL_ELEMENT_INDENT: String = "      "

    private fun listFiles() =
        """<!DOCTYPE html>
          |<body>
          |   <ul>
          |      ${sortedFiles.joinToString(separator = "\n" + UL_ELEMENT_INDENT) { it.toHtmlListItem(basePath) }}
          |   </ul>
          |</body>
        """.trimMargin("|").toByteArray()
}

private fun Path.toHtmlListItem(basePath: Path): String = """<li><a href="/${basePath.relativize(this).encodeSpaces()}"><b>${this.fileName}</b></a></li>"""

private fun Path.encodeSpaces(): String = this.toString().encodeSpaces()

class NoContent : Content {
    override fun asByteArray(range: Range) = ByteArray(0)
    override fun type() = throw IllegalStateException("NoContent cannot be asked for type")
    override fun length(range: Range) = 0
}