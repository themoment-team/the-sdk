package team.themoment.sdk.logging

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import org.springframework.util.StreamUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Enumeration

class CachedRequestBodyWrapper
    @Throws(IOException::class)
    constructor(
        request: HttpServletRequest,
    ) : HttpServletRequestWrapper(request) {
        val cachedBody: ByteArray = StreamUtils.copyToByteArray(request.inputStream)

        // 본문을 먼저 읽었으므로 서블릿 컨테이너는 form 본문을 파라미터로 해석하지 않는다. 캐시한 본문에서 직접 채운다.
        private val parameters: Map<String, Array<String>> by lazy { mergeFormBodyParameters(super.getParameterMap()) }

        override fun getInputStream(): ServletInputStream {
            val byteArrayInputStream = ByteArrayInputStream(cachedBody)
            return object : ServletInputStream() {
                override fun read(): Int = byteArrayInputStream.read()

                override fun isFinished(): Boolean = byteArrayInputStream.available() == 0

                override fun isReady(): Boolean = true

                override fun setReadListener(readListener: ReadListener?) {
                }
            }
        }

        override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(inputStream, bodyCharset()))

        override fun getParameter(name: String): String? = parameters[name]?.firstOrNull()

        override fun getParameterMap(): Map<String, Array<String>> = parameters

        override fun getParameterNames(): Enumeration<String> = Collections.enumeration(parameters.keys)

        override fun getParameterValues(name: String): Array<String>? = parameters[name]

        private fun bodyCharset(): Charset = characterEncoding?.let { Charset.forName(it) } ?: StandardCharsets.UTF_8

        // 서블릿 명세상 form 본문은 POST에서만 파라미터가 된다. PUT/PATCH/DELETE는 Spring FormContentFilter가 처리한다.
        private fun isFormPost(): Boolean =
            method.equals("POST", ignoreCase = true) &&
                contentType?.substringBefore(';')?.trim().equals(FORM_CONTENT_TYPE, ignoreCase = true)

        private fun mergeFormBodyParameters(queryParameters: Map<String, Array<String>>): Map<String, Array<String>> {
            if (!isFormPost() || cachedBody.isEmpty()) return queryParameters

            val merged = LinkedHashMap(queryParameters)
            parseFormBody().forEach { (name, values) ->
                merged[name] = (merged[name] ?: emptyArray()) + values.toTypedArray()
            }
            return Collections.unmodifiableMap(merged)
        }

        private fun parseFormBody(): Map<String, List<String>> {
            val charset = bodyCharset()
            return String(cachedBody, charset)
                .split('&')
                .filter { it.isNotEmpty() }
                .mapNotNull { decodePair(it, charset) }
                .groupBy({ it.first }, { it.second })
        }

        // 컨테이너와 같이 잘못 인코딩된 쌍은 요청 전체를 실패시키지 않고 건너뛴다.
        private fun decodePair(
            pair: String,
            charset: Charset,
        ): Pair<String, String>? {
            val separatorIndex = pair.indexOf('=')
            val rawName = if (separatorIndex < 0) pair else pair.substring(0, separatorIndex)
            val rawValue = if (separatorIndex < 0) "" else pair.substring(separatorIndex + 1)
            return try {
                URLDecoder.decode(rawName, charset) to URLDecoder.decode(rawValue, charset)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        private companion object {
            const val FORM_CONTENT_TYPE = "application/x-www-form-urlencoded"
        }
    }
