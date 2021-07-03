import java.io.OutputStream
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Paths

class HttpServer(private val filePath: String) {

    fun start() {
        val fileUrl = HttpServer::class.java.getResource(filePath)
        val fileBytes: ByteArray = Files.readAllBytes(Paths.get(fileUrl.toURI()))
        Thread { serveFileOnce(fileBytes) }.start()
    }

    private fun statusLine() = "HTTP/1.1 200 spoko\r\n"
    private fun headers(fileContent: ByteArray) = "Content-Length: ${fileContent.size}\r\n\r\n"


    private fun serveFileOnce(fileBytes: ByteArray) {
        ServerSocket(80).accept().use { socket ->
            val outputStream: OutputStream = socket.getOutputStream()
            val response = statusLine() + headers(fileBytes)
            outputStream.write(response.toByteArray() + fileBytes)
            println(response)
        }
    }
}