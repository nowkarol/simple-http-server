package net.karolnowak.simplehttpserver.request

import net.karolnowak.simplehttpserver.EOL

const val NO_HEADER = ""

data class Range(private val rangeHeader: String?) {
    fun multipleRanges() = rangeHeader?.contains(",") ?: false

    fun asIntRange(contentLength: Int): IntRange {
        val range = rangeHeader?.removePrefix("bytes=")
        return when {
            range == null -> 0..contentLength.dec()
            range.indexOf("-") == 0 -> 0..range.drop(1).toInt()
            range.last() == '-' -> range.dropLast(1).toInt()..contentLength.dec()
            else -> range.split("-").let { (start, end) -> start.toInt()..end.toInt() }
        }
    }

    fun asResponseHeader(contentLength: Int): String =
        rangeHeader?.let { "Content-Range: bytes ${asIntRange(contentLength).toString().replace("..", "-")}/${contentLength}$EOL" } ?: NO_HEADER
}
fun NoRange() = Range(null)