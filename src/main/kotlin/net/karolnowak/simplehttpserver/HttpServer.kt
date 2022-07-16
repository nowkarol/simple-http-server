package net.karolnowak.simplehttpserver

import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

const val EOL = "\r\n"
const val ROOT = """/"""

class HttpServer(private val content: Content, private val port: Int = 80) {

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
        if (request.method != "GET") {
            return Response(405, "Nope")
        }
        if (content.doesntContain(request.requestTarget)) {
            return Response(404, "It doesn't exist")
        }
        return Response(200, "Spoko", content.asByteArray(request.requestTarget))
    }
}

private fun Socket.readRequest(): Request {
    val requestLine = getInputStream().bufferedReader().readLine() // liberal in EOL recognition
    val tokens = requestLine.split(" ")
    require(tokens.size == 3)
    return Request(tokens[0], tokens[1], tokens[2])
}

internal data class Request(val method: String, val requestTarget: String, val httpVersion: String)
internal data class Response(val statusCode: Int, val reasonPhrase: String, val responseBody: ByteArray = ByteArray(0)) {
    private fun isSuccessful() = statusCode in (100..399)

    private fun statusLineAndHeaders(): String {
        val statusLine = "HTTP/1.1 $statusCode $reasonPhrase"
        val headers = if (isSuccessful()) "Content-Length: ${responseBody.size}" else "Accept: GET"
        // no Content-Type yet so browser displays it only because of magic numbers
        return statusLine + EOL + headers + EOL + EOL
    }

    fun asBytes() = statusLineAndHeaders().toByteArray() + responseBody
}