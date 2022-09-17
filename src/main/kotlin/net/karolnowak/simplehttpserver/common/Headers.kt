package net.karolnowak.simplehttpserver.common

import java.io.BufferedReader

//TODO handle corner cases
class Headers(reader: BufferedReader) {
    private val headers = toMap(reader)

    private fun toMap(reader: BufferedReader): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine()
            if (line.isEmpty()) return headers
            line.split(":").let { headers.put(it[0].trim(), it[1].trim()) }
        }
    }

    operator fun get(name: String) = headers[name]
}