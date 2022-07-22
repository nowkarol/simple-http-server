package net.karolnowak.simplehttpserver

import java.nio.file.Path

interface Content {
    fun asByteArray(): ByteArray
    fun type(): ContentType
    fun length() = asByteArray().size
}

data class ContentType(val raw: String)

class FileContent(private val byteArray: ByteArray) : Content {

    override fun asByteArray() = byteArray

    override fun type() = ContentType("binary/octet-stream")
}

class DirectoryListing(private val files: List<Path>) : Content {
    override fun asByteArray() = listFiles()

    override fun type() = ContentType("text/html; charset=UTF-8")

    private fun listFiles() =
        """<!DOCTYPE html>
          |<body>
          |   <ul>
          |      ${files.joinToString(separator = "") { it.toHtmlListItem() }}
          |   </ul>
          |</body>
        """.trimMargin("|").toByteArray()
}

private fun Path.toHtmlListItem(): String = """<li><a href=${this.fileName}><b>${this.fileName}</b></a></li>"""

class NoContent : Content {
    override fun asByteArray() = ByteArray(0)
    override fun type() = throw IllegalStateException("NoContent cannot be asked for type")
    override fun length() = 0
}