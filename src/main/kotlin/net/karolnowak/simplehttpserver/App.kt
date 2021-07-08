package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val content = Files.readAllBytes(Path.of(args.first()))
    HttpServer(content).start()
}