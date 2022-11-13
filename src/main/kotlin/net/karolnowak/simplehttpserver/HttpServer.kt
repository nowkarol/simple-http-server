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

const val DEFAULT_SERVER_PORT = 80
const val SOCKET_BACKLOG_SIZE = 10

class HttpServer(private val contentProvider: ContentProvider, private val port: Int = DEFAULT_SERVER_PORT) {

    fun start() {
        Thread { serveFile() }.start()
    }

    private fun serveFile() {
        val socket = ServerSocket(port, SOCKET_BACKLOG_SIZE) // those connections are accepted automatically, it doesn't happen after socket.accept()!
        while (true) {
            runCatching {
                socket.accept().let { socket ->
                    Thread {
                            while (true) {
                                val request = socket.readRequest()
                                if (request == null) {
                                    socket.close()  // TODO won't close connection and remove thread until other side does it!
                                    return@Thread
                                }
                                val outputStream: OutputStream = socket.getOutputStream()
                                val response: Response = respondTo(request)
                                outputStream.write(response.asBytes())
                            }
                    }.start()
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

    private fun Socket.readRequest(): Request? {
        val reader = getInputStream().bufferedReader()
        val requestLine = reader.readLine() ?: return null // liberal in EOL recognition
        val tokens = requestLine.split(" ")
        require(tokens.size == 3)
        return Request(tokens[0], tokens[1].decodeSpaces(), tokens[2], Headers(reader))
    }

}

fun String.decodeSpaces(): String = this.replace(ENCODED_SPACE, " ")
fun String.encodeSpaces(): String = this.replace(" ", ENCODED_SPACE)