package team.themoment.sdk.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import team.themoment.sdk.support.TestApplication
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// MockMvc는 form 본문을 미리 파라미터로 채워 주므로, 서블릿 컨테이너의 본문 파싱을 거치도록 실제 서버로 검증한다.
@SpringBootTest(classes = [TestApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoggingFilterFormRequestTest {
    @LocalServerPort
    private var port: Int = 0

    private val client = HttpClient.newHttpClient()

    @Test
    fun `form body parameters reach the controller after the body is cached for logging`() {
        val data = postForm("/v1/form?source=query", "grant_type=authorization_code&code=a%2Bb&scope=openid+profile")

        assertEquals(listOf("authorization_code"), data.values("grant_type"))
        assertEquals(listOf("a+b"), data.values("code"))
        assertEquals(listOf("openid profile"), data.values("scope"))
        assertEquals(listOf("query"), data.values("source"))
    }

    @Test
    fun `query and body values for the same name are both kept with query first`() {
        val data = postForm("/v1/form?scope=query", "scope=body")

        assertEquals(listOf("query", "body"), data.values("scope"))
    }

    private fun JsonNode.values(name: String): List<String> = this[name].let { node -> (0 until node.size()).map { node[it].asString() } }

    private fun postForm(
        path: String,
        body: String,
    ): JsonNode {
        val response =
            client.send(
                HttpRequest
                    .newBuilder(URI.create("http://localhost:$port$path"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString(),
            )
        assertEquals(200, response.statusCode(), response.body())
        return JsonMapper.builder().build().readTree(response.body())["data"]
    }
}
