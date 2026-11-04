package team.themoment.sdk.autoconfigure

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import team.themoment.sdk.support.TestApplication

@SpringBootTest(
    classes = [TestApplication::class],
    properties = ["sdk.exception.use-english-message=false"],
)
@AutoConfigureMockMvc
class SdkIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `wraps plain controller response in CommonApiResponse`() {
        mockMvc.get("/v1/plain").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("OK") }
            jsonPath("$.code") { value(200) }
            jsonPath("$.message") { value("OK") }
            jsonPath("$.data.name") { value("the-sdk") }
        }
    }

    @Test
    fun `returns 204 No Content when controller returns null`() {
        mockMvc.get("/v1/empty").andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `passes through already wrapped response with its status`() {
        mockMvc.get("/v1/created").andExpect {
            status { isCreated() }
            jsonPath("$.code") { value(201) }
            jsonPath("$.message") { value("생성되었습니다.") }
            jsonPath("$.data.name") { value("new") }
        }
    }

    @Test
    fun `handles ExpectedException with custom message`() {
        mockMvc.get("/v1/error").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("커스텀 에러") }
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `translates default reason phrase to Korean message`() {
        mockMvc.get("/v1/not-found").andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("요청한 리소스를 찾을 수 없습니다") }
        }
    }

    @Test
    fun `exposes OpenAPI document with wrapped response schema`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/v1/plain'].get.responses['200'].content['*/*'].schema.properties.status") { exists() }
            jsonPath("$.paths['/v1/plain'].get.responses['200'].content['*/*'].schema.properties.data") { exists() }
        }
    }
}
