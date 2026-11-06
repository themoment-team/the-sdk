package team.themoment.sdk.swagger

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContextImpl
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.method.HandlerMethod
import team.themoment.sdk.config.SwaggerProperties
import team.themoment.sdk.response.CommonApiResponse
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class SwaggerConfigTest {
    private val customizer = SwaggerConfig(SwaggerProperties()).operationCustomizer()
    private val mapper = JsonMapper.builder().build()

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["OK", "CREATED", "ACCEPTED"])
    fun `declared success examples match CommonApiResponse serialization`(status: HttpStatus) {
        val payload = Schema<Any>().type("object").addProperty("name", Schema<String>().type("string"))
        val operation = operation(status.value().toString(), payload)

        customize(operation)

        val schema = schema(operation, status.value().toString())
        val json = mapper.valueToTree<JsonNode>(CommonApiResponse(status, status.value(), status.reasonPhrase, "sample"))
        assertEquals(json["status"].asString(), schema.properties["status"]?.example)
        assertEquals(json["code"].asInt(), schema.properties["code"]?.example)
        assertEquals(json["message"].asString(), schema.properties["message"]?.example)
        assertSame(payload, schema.properties["data"])
        assertEquals("object", schema.type)
        assertFalse(schema.required.orEmpty().contains("data"))
    }

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["OK", "CREATED", "ACCEPTED", "BAD_REQUEST"])
    fun `wrapped generic reference is preserved without nesting data`(status: HttpStatus) {
        val original = Schema<Any>().`$ref`("#/components/schemas/RenamedEnvelope")
        val operation = operation(status.value().toString(), original)

        customize(operation, "wrapped")

        val schema = schema(operation, status.value().toString())
        assertTrue(containsSchema(schema, original))
        assertNull(schema.properties?.get("data"))
        assertEquals(status.toString(), schema.properties["status"]?.example)
        assertEquals(status.value(), schema.properties["code"]?.example)
    }

    @Test
    fun `ResponseEntity containing CommonApiResponse retains generic reference`() {
        val original = Schema<Any>().`$ref`("#/components/schemas/RenamedEnvelope")
        val operation = operation("202", original)

        customize(operation, "entity")

        val schema = schema(operation, "202")
        assertTrue(containsSchema(schema, original))
        assertNull(schema.properties?.get("data"))
    }

    @Test
    fun `inline envelope preserves data and custom message without changing shared schema`() {
        val payload = Schema<Any>().`$ref`("#/components/schemas/SampleBody")
        val original =
            Schema<Any>()
                .type("object")
                .addProperty("status", Schema<String>().example("200 OK"))
                .addProperty("code", Schema<Int>().example(200))
                .addProperty("message", Schema<String>().example("생성되었습니다."))
                .addProperty("data", payload)
        val operation = operation("201", original)

        customize(operation, "wrapped")

        val schema = schema(operation, "201")
        assertTrue(containsSchema(schema, original))
        assertNull(schema.properties?.get("data"))
        assertEquals(201, schema.properties["code"]?.example)
        assertSame(payload, original.properties["data"])
        assertEquals(200, original.properties["code"]?.example)
        assertEquals("생성되었습니다.", original.properties["message"]?.example)
    }

    @Test
    fun `customizing twice does not wrap the response again`() {
        for (original in listOf(Schema<Any>().type("object"), Schema<Any>().`$ref`("#/components/schemas/RenamedEnvelope"))) {
            val operation = operation("200", original)
            customize(operation)
            val once = schema(operation, "200")

            customize(operation)

            assertSame(once, schema(operation, "200"))
        }
    }

    @Test
    fun `bodyless and undocumented responses stay unchanged`() {
        val operation = Operation().responses(ApiResponses().addApiResponse("204", ApiResponse().description("No Content")))
        customize(operation)
        assertNull(operation.responses["204"]?.content)

        val noSchema =
            Operation().responses(
                ApiResponses().addApiResponse(
                    "200",
                    ApiResponse().content(Content().addMediaType("application/json", MediaType())),
                ),
            )
        customize(noSchema)
        assertNull(
            noSchema.responses["200"]
                ?.content
                ?.get("application/json")
                ?.schema,
        )

        customize(Operation())
    }

    @Test
    fun `raw error and default response schemas stay unchanged`() {
        for (code in listOf("400", "2XX", "4XX", "5XX", "default")) {
            val original = Schema<Any>().type("object")
            val operation = operation(code, original)
            customize(operation)
            assertSame(original, schema(operation, code))
        }
    }

    @Test
    fun `plain DTO with envelope-like fields is still wrapped`() {
        val original =
            Schema<Any>()
                .type("object")
                .addProperty("status", Schema<String>().example("OK"))
                .addProperty("code", Schema<Int>().example(200))
                .addProperty("message", Schema<String>().example("upstream"))
        val operation = operation("200", original)

        customize(operation)

        assertSame(original, schema(operation, "200").properties["data"])
    }

    @Test
    fun `plain DTO with SDK-like schema name is still wrapped`() {
        val original = Schema<Any>().`$ref`("#/components/schemas/CommonApiResponseDto")
        val operation = operation("200", original)

        customize(operation)

        assertSame(original, schema(operation, "200").properties["data"])
    }

    @Test
    fun `null-only data is removed from properties and required`() {
        val nullSchemas =
            listOf(
                Schema<Any>().type("null"),
                Schema<Any>().types(setOf("null")),
                Schema<Any>().oneOf(listOf(Schema<Any>().type("null"))),
                Schema<Any>().anyOf(listOf(Schema<Any>().oneOf(listOf(Schema<Any>().type("null"))))),
            )
        for (data in nullSchemas) {
            val envelope = convertEnvelope(data)
            assertFalse(envelope.properties.containsKey("data"))
            assertFalse(envelope.required.orEmpty().contains("data"))
        }
    }

    @Test
    fun `non-null data alternatives are preserved without nullability`() {
        val value = StringSchema().nullable(true)
        val data = Schema<Any>().anyOf(listOf(value, Schema<Any>().type("null")))

        val envelope = convertEnvelope(data)

        assertSame(data, envelope.properties["data"])
        assertEquals(listOf(value), data.anyOf)
        assertNull(value.nullable)
        assertFalse(envelope.required.orEmpty().contains("data"))
    }

    private fun convertEnvelope(data: Schema<*>): Schema<*> {
        val envelope = ObjectSchema().addProperty("status", StringSchema()).addProperty("data", data)
        envelope.required = listOf("status", "data")
        val converter = SwaggerConfig(SwaggerProperties()).commonApiResponseModelConverter()
        val delegate = ModelConverter { _, _, _ -> envelope }
        return ModelConverterContextImpl(listOf(converter, delegate)).resolve(AnnotatedType(CommonApiResponse::class.java))
    }

    private fun operation(
        code: String,
        schema: Schema<*>,
    ): Operation =
        Operation().responses(
            ApiResponses().addApiResponse(
                code,
                ApiResponse().content(Content().addMediaType("application/json", MediaType().schema(schema))),
            ),
        )

    private fun schema(
        operation: Operation,
        code: String,
    ): Schema<*> = operation.responses[code]!!.content["application/json"]!!.schema

    private fun customize(
        operation: Operation,
        method: String = "plain",
    ) {
        customizer.customize(operation, HandlerMethod(Controller(), Controller::class.java.getMethod(method)))
    }

    private fun containsSchema(
        schema: Schema<*>,
        expected: Schema<*>,
    ): Boolean = schema === expected || schema.allOf.orEmpty().any { containsSchema(it, expected) }

    class Controller {
        fun plain(): String = "sample"

        fun wrapped(): CommonApiResponse<String> = CommonApiResponse.success("OK", "sample")

        fun entity(): ResponseEntity<CommonApiResponse<String>> = ResponseEntity.ok(wrapped())
    }
}
