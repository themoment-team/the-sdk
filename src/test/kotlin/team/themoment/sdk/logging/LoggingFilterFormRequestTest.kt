package team.themoment.sdk.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import team.themoment.sdk.support.TestApplication
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

// MockMvc는 form 본문을 미리 파라미터로 채워 주므로, 서블릿 컨테이너의 본문 파싱을 거치도록 실제 서버로 검증한다.
@SpringBootTest(
    classes = [TestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["sdk.logging.enabled=true"],
)
class LoggingFilterFormRequestTest {
    @LocalServerPort
    private var port: Int = 0

    private val client = HttpClient.newHttpClient()

    @Test
    fun `form body parameters reach the controller after the body is cached for logging`() {
        val data = sendForm("POST", "/v1/form?source=query", "grant_type=authorization_code&code=a%2Bb&scope=openid+profile")

        assertTrue(data.required("bodyCached").asBoolean(), "LoggingFilter가 본문을 캐시한 요청이어야 회귀를 검증할 수 있습니다.")
        assertEquals(listOf("authorization_code"), data.param("grant_type"))
        assertEquals(listOf("a+b"), data.param("code"))
        assertEquals(listOf("openid profile"), data.param("scope"))
        assertEquals(listOf("query"), data.param("source"))
    }

    @Test
    fun `query and body values for the same name are both kept with query first`() {
        val data = sendForm("POST", "/v1/form?scope=query", "scope=body")

        assertEquals(listOf("query", "body"), data.param("scope"))
    }

    @Test
    fun `non ascii query and body values are decoded with the same charset`() {
        val encoded = URLEncoder.encode("한글", StandardCharsets.UTF_8)

        val data = sendForm("POST", "/v1/form?msg=$encoded", "msg=$encoded")

        assertEquals(listOf("한글", "한글"), data.param("msg"))
    }

    // PUT form 본문은 Spring FormContentFilter가 캐시한 본문을 다시 읽어 채운다. 이 전제가 깨지면 여기서 드러난다.
    @Test
    fun `put form body parameters are filled once by FormContentFilter`() {
        val data = sendForm("PUT", "/v1/form", "scope=body")

        assertTrue(data.required("bodyCached").asBoolean())
        assertEquals(listOf("body"), data.param("scope"))
    }

    private fun JsonNode.param(name: String): List<String> =
        required("params").required(name).let { node -> (0 until node.size()).map { node[it].asString() } }

    private fun sendForm(
        method: String,
        path: String,
        body: String,
    ): JsonNode {
        val response =
            client.send(
                HttpRequest
                    .newBuilder(URI.create("http://localhost:$port$path"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method(method, HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        assertEquals(200, response.statusCode(), response.body())
        return JsonMapper
            .builder()
            .build()
            .readTree(response.body())
            .required("data")
    }
}
