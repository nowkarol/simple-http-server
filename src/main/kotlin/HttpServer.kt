import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

private const val EOL = "\r\n"

class HttpServer(filePath: String) {
    private var fileBytes = getFileBytes(filePath)

    private fun getFileBytes(filePath: String): ByteArray {
        val fileUrl = HttpServer::class.java.getResource(filePath)
        return Files.readAllBytes(Paths.get(fileUrl.toURI()))
    }

    fun start() {
        Thread { serveFile() }.start()
    }

    private fun serveFile() {
        val socket = ServerSocket(80, 0) // backlog 0 seems to be ignored
        while (true) {
            runCatching {
                socket.accept().use { socket ->
                    val request = socket.readRequest()
                    val outputStream: OutputStream = socket.getOutputStream()
                    val response: Response = respondTo(request)
                    outputStream.write(response.asBytes())
                    println(response)
                }
            }
        }
    }

    private fun respondTo(request: Request) =
        when (request.method) {
            "GET" -> Response(200, "Spoko", fileBytes.decodeToString())
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
internal data class Response(val statusCode: Int, val reasonPhrase: String, val responseBody: String = "") {
    private fun isSuccessful() = statusCode in (100..399)

    override fun toString(): String {
        val statusLine = "HTTP/1.1 $statusCode $reasonPhrase"
        val headers = if (isSuccessful()) "Content-Length: ${responseBody.toByteArray().size}" else "Accept: GET"
        return statusLine + EOL + headers + EOL + EOL + if (isSuccessful()) responseBody + EOL else ""
    }

    fun asBytes() = toString().toByteArray()
}