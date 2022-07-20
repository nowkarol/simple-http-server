package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList

interface Content {
    fun asByteArray(resource: String): ByteArray
    fun contains(resource: String): Boolean
    fun doesntContain(resource: String): Boolean = contains(resource).not()
}

fun contentFromPath(path: Path) =
    when {
        Files.isRegularFile(path) -> FileContent(path)
        Files.isDirectory(path) -> DirectoryContent(path)
        else -> throw IllegalAccessException("Path $path is neither file nor directory, only them are supported")
    }

class FileContent(private val path: Path) : Content {

    override fun asByteArray(resource: String): ByteArray =
        Files.readAllBytes(path)

    override fun contains(resource: String) = true
}

class DirectoryContent(path: Path) : Content {
    private val files: List<Path> = Files.list(path).collect(toList())

    override fun asByteArray(resource: String): ByteArray =
        if (resource == ROOT)
            listFiles()
        else
            getFilePath(resource)!!.getBytes()

    private fun listFiles() =
        """<!DOCTYPE html>
          |<body>
          |   <ul>
          |      ${files.joinToString(separator = "") { it.toHtmlListItem() }}
          |   </ul>
          |</body>
        """.trimMargin("|").toByteArray()

    override fun contains(resource: String) =
        (getFilePath(resource) != null) or (resource == ROOT)

    private fun getFilePath(resource: String) = files.firstOrNull { it.toString().endsWith(resource) }
}

private fun Path.getBytes(): ByteArray = Files.readAllBytes(this)

private fun Path.toHtmlListItem(): String = """<li><a href=${this.fileName}><b>${this.fileName}</b></a></li>"""