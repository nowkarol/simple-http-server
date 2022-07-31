package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors.toList

fun contentFromPath(path: Path) =
    when {
        Files.isRegularFile(path) -> FileContentProvider(path)
        Files.isDirectory(path) -> DirectoryContentProvider(path)
        else -> throw IllegalAccessException("Path $path is neither file nor directory, only them are supported")
    }

interface ContentProvider {
    fun contains(requestTarget: String): Boolean
    fun doesntContain(requestTarget: String) = contains(requestTarget).not()
    fun getResource(requestTarget: String): Content
}

class FileContentProvider(private val path: Path) : ContentProvider {
    override fun contains(requestTarget: String) = requestTarget == ROOT
    override fun getResource(requestTarget: String) = FileContent(path)
}

class DirectoryContentProvider(private val path: Path) : ContentProvider {
    override fun contains(requestTarget: String) =
        requestTarget == ROOT || Files.exists(requestTarget.relativeTo(path))

    override fun getResource(requestTarget: String): Content {
        val targetPath = requestTarget.relativeTo(path)
        return when {
            requestTarget == ROOT -> DirectoryListing(Files.list(path).collect(toList()), path)
            Files.isRegularFile(targetPath) -> FileContent(targetPath)
            Files.isDirectory(targetPath) -> DirectoryListing(Files.list(targetPath).collect(toList()), path)
            else -> throw IllegalAccessException("Cannot get resource $requestTarget")
        }
    }
}

private fun String.relativeTo(path: Path): Path = path.resolve(this.drop(1)) // remove leading slash