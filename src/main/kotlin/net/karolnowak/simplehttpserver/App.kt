package net.karolnowak.simplehttpserver

import java.nio.file.Path

fun main(args: Array<String>) {
    val content = contentFromPath(Path.of(args.first()))
    HttpServer(content).start()
}