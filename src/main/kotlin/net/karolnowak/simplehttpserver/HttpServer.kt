package net.karolnowak.simplehttpserver

import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

const val EOL = "\r\n"
const val ROOT = """/"""

class HttpServer(private val contentProvider: ContentProvider, private val port: Int = 80) {

    fun start() {
        Thread { serveFile() }.start()
    }

    private fun serveFile() {
        val socket = ServerSocket(port, 0) // backlog 0 seems to be ignored
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

    private fun respondTo(request: Request): Response {
        if (request.isValid().not()) {
            return Response(400, "Your fault")
        }

        if (request.method != "GET") {
            return Response(405, "Nope")
        }
        if (contentProvider.doesntContain(request.requestTarget)) {
            return Response(404, "It doesn't exist")
        }
        return Response(200, "Spoko", contentProvider.getResource(request.requestTarget))
    }
}

private fun Socket.readRequest(): Request {
    val requestLine = getInputStream().bufferedReader().readLine() // liberal in EOL recognition
    val tokens = requestLine.split(" ")
    require(tokens.size == 3)
    return Request(tokens[0], tokens[1], tokens[2])
}

internal data class Request(val method: String, val requestTarget: String, val httpVersion: String) {
    fun isValid() = !requestTarget.contains("..") && !requestTarget.uppercase().contains("%2E%2E")
}

internal data class Response(val statusCode: Int, val reasonPhrase: String, val content: Content = NoContent()) {
    private fun isSuccessful() = statusCode in (100..399)
    private fun statusLineAndHeaders(): String {
        val statusLine = "HTTP/1.1 $statusCode $reasonPhrase"
        val headers = if (isSuccessful())
            "Content-Length: ${content.length()}${EOL}" +
            "Content-Type: ${content.type().raw}"
        else "Allow: GET"
        return statusLine + EOL + headers + EOL + EOL
    }

    fun asBytes() = statusLineAndHeaders().toByteArray() + content.asByteArray()
}