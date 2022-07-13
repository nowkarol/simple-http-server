package net.karolnowak.simplehttpserver

import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val content = Files.readAllBytes(Path.of(args.first()))
    println("Content size: ${content.size}")
    HttpServer(content).start()
}