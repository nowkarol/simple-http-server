package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList

interface Content {
    fun asByteArray(): ByteArray
}

fun contentFromPath(path: Path) =
    when  {
        Files.isRegularFile(path) -> FileContent(path)
        Files.isDirectory(path) -> DirectoryContent(path)
        else -> throw IllegalAccessException("Path $path is neither file nor directory, only them are supported")
    }

class FileContent(private val path: Path): Content  {

    override fun asByteArray(): ByteArray =
        Files.readAllBytes(path)
}

class DirectoryContent(private val path: Path):Content  {

    override fun asByteArray(): ByteArray =
        Files.list(path).collect(toList())
            .joinToString(separator = EOL) { it.toHtml() }
            .toByteArray()
}

private fun Path.toHtml(): String = this.fileName.toString()