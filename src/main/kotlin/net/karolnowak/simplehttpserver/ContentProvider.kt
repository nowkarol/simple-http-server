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
    override fun contains(requestTarget: String) = path.toString().endsWith(requestTarget) || requestTarget == ROOT
    override fun getResource(requestTarget: String) = FileContent(Files.readAllBytes(path))
}

class DirectoryContentProvider(path: Path) : ContentProvider {
    private val files = Files.list(path).collect(toList())
    override fun contains(requestTarget: String) =
        files.any { it.toString().endsWith(requestTarget) } || requestTarget == ROOT

    override fun getResource(requestTarget: String) =
        if (requestTarget == ROOT) DirectoryListing(files)
        else FileContent(getFileBytes(requestTarget))

    private fun getFileBytes(resource: String) =
        Files.readAllBytes(files.firstOrNull { it.toString().endsWith(resource) })

}