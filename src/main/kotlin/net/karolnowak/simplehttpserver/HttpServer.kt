package net.karolnowak.simplehttpserver

import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

private const val EOL = "\r\n"

class HttpServer(private val fileBytes: ByteArray, private val port: Int = 80) {

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

    private fun respondTo(request: Request) =
        when (request.method) {
            "GET" -> Response(200, "Spoko", fileBytes)
            else -> Response(405, "Nope")
        }
}

private fun Socket.readRequest(): Request {
    val requestLine = getInputStream().bufferedReader().readLine() // liberal in EOL recognition
    val tokens = requestLine.split(" ")
    require(tokens.size == 3)
    return Request(tokens[0].uppercase(), tokens[1].uppercase(), tokens[2].uppercase())
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