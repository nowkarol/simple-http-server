package net.karolnowak.simplehttpserver.response

import net.karolnowak.simplehttpserver.Content
import net.karolnowak.simplehttpserver.EOL
import net.karolnowak.simplehttpserver.NoContent
import net.karolnowak.simplehttpserver.request.NoRange
import net.karolnowak.simplehttpserver.request.Range

internal data class Response(val statusCode: Int, val reasonPhrase: String, val content: Content = NoContent(), val range: Range = NoRange()) {
    private fun isSuccessful() = statusCode in (100..399)
    private fun statusLineAndHeaders(): String {
        val statusLine = "HTTP/1.1 $statusCode $reasonPhrase"
        val headers =
            when {
                isSuccessful() ->
                    "Content-Length: ${content.length(range)}$EOL" +
                    "Content-Type: ${content.type().raw}$EOL"+
                     range.asResponseHeader(content.length()) +
                    "Accept-Ranges: bytes$EOL" +
                    "Connection: Keep-Alive$EOL"

                else -> "Content-Length: 0$EOL" +
                        "Allow: GET$EOL"
            }

        return statusLine + EOL + headers + EOL
    }

    fun asBytes() = statusLineAndHeaders().toByteArray() + content.asByteArray(range)
}