package net.karolnowak.simplehttpserver.request

import net.karolnowak.simplehttpserver.common.Headers

internal data class Request(val method: String, val requestTarget: String, val httpVersion: String, val headers: Headers) {
    fun isValid() = !requestTarget.contains("..") && !requestTarget.uppercase().contains("%2E%2E")
    fun getRange(): Range = Range(headers["Range"])
}