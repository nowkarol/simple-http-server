package net.karolnowak.simplehttpserver

import net.karolnowak.simplehttpserver.common.Headers
import net.karolnowak.simplehttpserver.request.NoRange
import net.karolnowak.simplehttpserver.request.Request
import net.karolnowak.simplehttpserver.response.Response
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

const val EOL = "\r\n"
const val ROOT = """/"""
const val ENCODED_SPACE = "%20"

class HttpServer(private val contentProvider: ContentProvider, private val port: Int = 80) {

    fun start() {
        Thread { serveFile() }.start()
    }

    private fun serveFile() {
        val socket = ServerSocket(port, 10) // ten connections are accepted automatically, it doesn't happen after socket.accept()!
        while (true) {
            runCatching {
                socket.accept().use { socket ->
                    val request = socket.readRequest()
                    val outputStream: OutputStream = socket.getOutputStream()
                    val response: Response = respondTo(request)
                    outputStream.write(response.asBytes())
//                    println(response) recognize binary data before printing
                }
            }
        }
    }

    private fun respondTo(request: Request): Response =
        when {
            request.isValid().not() -> Response(400, "Your fault")
            request.method != "GET" -> Response(405, "Nope")
            contentProvider.doesntContain(request.requestTarget) -> Response(404, "It doesn't exist")
            request.getRange().multipleRanges() -> Response(416, "Sorry won't send this Range")
            request.getRange() != NoRange() -> Response(206, "Here is your part", contentProvider.getResource(request.requestTarget), request.getRange())
            else -> Response(200, "Spoko", contentProvider.getResource(request.requestTarget))
        }

    private fun Socket.readRequest(): Request {
        val reader = getInputStream().bufferedReader()
        val requestLine = reader.readLine() // liberal in EOL recognition
        val tokens = requestLine.split(" ")
        require(tokens.size == 3)
        return Request(tokens[0], tokens[1].decodeSpaces(), tokens[2], Headers(reader))
    }

}

fun String.decodeSpaces(): String = this.replace(ENCODED_SPACE, " ")
fun String.encodeSpaces(): String = this.replace(" ", ENCODED_SPACE)