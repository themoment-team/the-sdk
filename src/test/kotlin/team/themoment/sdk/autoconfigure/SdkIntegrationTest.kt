package team.themoment.sdk.autoconfigure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import team.themoment.sdk.support.TestApplication
import tools.jackson.databind.json.JsonMapper

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
            jsonPath("$.status") { value("200 OK") }
            jsonPath("$.code") { value(200) }
            jsonPath("$.message") { value("OK") }
            jsonPath("$.data.name") { value("the-sdk") }
        }
    }

    @Test
    fun `plain DTO preserves declared status in HTTP response and OpenAPI`() {
        val cases =
            listOf(
                Triple("/v1/plain-created", 201, "Created"),
                Triple("/v1/plain-accepted", 202, "Accepted"),
            )
        for ((path, code, message) in cases) {
            mockMvc.get(path).andExpect {
                status { isEqualTo(code) }
                jsonPath("$.status") { value("$code ${message.uppercase()}") }
                jsonPath("$.code") { value(code) }
                jsonPath("$.message") { value(message) }
                jsonPath("$.data.name") { exists() }
            }
        }
        for (docsPath in listOf("/v3/api-docs", "/v3/api-docs/API")) {
            val json =
                mockMvc
                    .get(docsPath)
                    .andExpect { status { isOk() } }
                    .andReturn()
                    .response.contentAsString
            val document = JsonMapper.builder().build().readTree(json)
            for ((path, code, message) in cases) {
                val schema = document["paths"][path]["get"]["responses"][code.toString()]["content"]["*/*"]["schema"]
                assertEquals("$code ${message.uppercase()}", schema["properties"]["status"]["example"].asString())
                assertEquals(code, schema["properties"]["code"]["example"].asInt())
                assertEquals(message, schema["properties"]["message"]["example"].asString())
                assertEquals("#/components/schemas/SampleBody", schema["properties"]["data"]["\$ref"].asString())
            }
        }
    }

    @Test
    fun `bodyless status is preserved without serializing a DTO`() {
        for (code in listOf(204, 205, 304)) {
            mockMvc.get("/v1/configured-status?status=$code").andExpect {
                status { isEqualTo(code) }
                content { string("") }
            }
        }
    }

    @Test
    fun `null body preserves explicit status and defaults OK to no content`() {
        for (code in listOf(200, 201, 202, 204, 205, 304)) {
            mockMvc.get("/v1/configured-status?status=$code&missing=true").andExpect {
                status { isEqualTo(if (code == 200) 204 else code) }
                content { string("") }
            }
        }
    }

    @Test
    fun `nonstandard status passes through without an incorrect envelope`() {
        mockMvc.get("/v1/configured-status?status=299").andExpect {
            status { isEqualTo(299) }
            jsonPath("$.name") { value("configured") }
            jsonPath("$.status") { doesNotExist() }
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
            jsonPath("$.status") { value("201 CREATED") }
            jsonPath("$.code") { value(201) }
            jsonPath("$.message") { value("생성되었습니다.") }
            jsonPath("$.data.name") { value("new") }
        }
    }

    @Test
    fun `handles ExpectedException with custom message`() {
        mockMvc.get("/v1/error").andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value("400 BAD_REQUEST") }
            jsonPath("$.code") { value(400) }
            jsonPath("$.message") { value("커스텀 에러") }
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `translates default reason phrase to Korean message`() {
        mockMvc.get("/v1/not-found").andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value("404 NOT_FOUND") }
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

    @Test
    fun `OpenAPI keeps generic data for created and accepted responses`() {
        val mapper = JsonMapper.builder().build()
        val cases =
            listOf(
                Triple("/v1/created", "201", "201 CREATED"),
                Triple("/v1/accepted", "202", "202 ACCEPTED"),
            )
        for (docsPath in listOf("/v3/api-docs", "/v3/api-docs/API")) {
            val json =
                mockMvc
                    .get(docsPath)
                    .andExpect { status { isOk() } }
                    .andReturn()
                    .response.contentAsString
            val document = mapper.readTree(json)
            for ((path, code, status) in cases) {
                val schema = document["paths"][path]["get"]["responses"][code]["content"]["*/*"]["schema"]
                assertEquals(status, schema["properties"]["status"]["example"].asString())
                assertEquals(code.toInt(), schema["properties"]["code"]["example"].asInt())
                assertFalse(schema["properties"].has("data"))
                val ref = schema["allOf"][0]["\$ref"].asString().substringAfterLast('/')
                val envelope = document["components"]["schemas"][ref]
                val data = envelope["properties"]["data"]
                val payloadRef = data["\$ref"] ?: data["oneOf"][0]["\$ref"]
                assertEquals("#/components/schemas/SampleBody", payloadRef.asString())
                assertFalse(data.path("oneOf").any { it.path("type").asString() == "null" })
                assertFalse(envelope.path("required").any { it.asString() == "data" })
                val statusProperty = envelope["properties"]["status"]
                assertTrue(statusProperty["enum"].any { it.asString() == status })
                assertEquals(
                    HttpStatus.entries.map { it.toString() }.toSet(),
                    statusProperty["enum"].asSequence().map { it.asString() }.toSet(),
                )
                assertFalse(statusProperty.has("example"))
                assertFalse(envelope["properties"]["code"].has("example"))
                assertEquals("object", schema["type"].asString())
                assertEquals("string", schema["properties"]["status"]["type"].asString())
                assertEquals("integer", schema["properties"]["code"]["type"].asString())
            }
            assertFalse(document["paths"]["/v1/empty"]["get"]["responses"]["204"].has("content"))
        }
    }

    @Test
    fun `wrapped response without payload omits data in JSON and schema`() {
        mockMvc.get("/v1/created-without-data").andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("201 CREATED") }
            jsonPath("$.data") { doesNotExist() }
        }
        val json =
            mockMvc
                .get("/v3/api-docs")
                .andExpect { status { isOk() } }
                .andReturn()
                .response.contentAsString
        val document = JsonMapper.builder().build().readTree(json)
        val schema = document["paths"]["/v1/created-without-data"]["get"]["responses"]["201"]["content"]["*/*"]["schema"]
        assertNotNull(schema)
        val ref = schema["allOf"][0]["\$ref"].asString().substringAfterLast('/')
        assertFalse(document["components"]["schemas"][ref]["properties"].has("data"))
    }

    @Test
    fun `ordinary DTO with SDK-like name retains its schema and is wrapped`() {
        mockMvc.get("/v1/upstream").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("200 OK") }
            jsonPath("$.data.status") { value("UPSTREAM_OK") }
            jsonPath("$.data.code") { value(42) }
        }
        val json =
            mockMvc
                .get("/v3/api-docs")
                .andReturn()
                .response.contentAsString
        val document = JsonMapper.builder().build().readTree(json)
        val schema = document["paths"]["/v1/upstream"]["get"]["responses"]["200"]["content"]["*/*"]["schema"]
        assertEquals("#/components/schemas/CommonApiResponseDto", schema["properties"]["data"]["\$ref"].asString())
        val component = document["components"]["schemas"]["CommonApiResponseDto"]
        assertTrue(component["required"].any { it.asString() == "data" })
        val data = component["properties"]["data"]
        assertTrue(data.path("type").any { it.asString() == "null" } || data.path("oneOf").any { it.path("type").asString() == "null" })
    }

    @Test
    fun `nullable DTO declares both runtime outcomes without ResponseStatus`() {
        mockMvc.get("/v1/nullable").andExpect {
            status { isOk() }
            jsonPath("$.data.name") { value("found") }
        }
        mockMvc.get("/v1/nullable?missing=true").andExpect { status { isNoContent() } }
        val json =
            mockMvc
                .get("/v3/api-docs")
                .andReturn()
                .response.contentAsString
        val document = JsonMapper.builder().build().readTree(json)
        val responses = document["paths"]["/v1/nullable"]["get"]["responses"]
        assertTrue(responses["200"].has("content"))
        assertFalse(responses["204"].has("content"))
    }
}
