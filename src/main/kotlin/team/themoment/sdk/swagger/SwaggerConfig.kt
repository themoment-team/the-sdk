package team.themoment.sdk.swagger

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import team.themoment.sdk.config.SwaggerProperties
import team.themoment.sdk.response.CommonApiResponse

@OpenAPIDefinition(
    info =
        Info(
            title = "API Documentation",
            description = "API Documentation",
            version = "v1",
        ),
)
@Configuration
class SwaggerConfig(
    private val swaggerProperties: SwaggerProperties,
) {
    @Bean
    fun operationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, _ ->
            addResponseBodyWrapperSchemaExample(operation)

            operation
        }

    @Bean
    fun groupedOpenApi(operationCustomizer: OperationCustomizer): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(swaggerProperties.group)
            .pathsToMatch(*swaggerProperties.pathsToMatch.toTypedArray())
            .addOperationCustomizer(operationCustomizer)
            .build()

    @Bean
    fun commonApiResponseSchemaCustomizer(): GlobalOpenApiCustomizer =
        GlobalOpenApiCustomizer { openApi ->
            openApi.components
                ?.schemas
                ?.filterKeys { it.startsWith(CommonApiResponse::class.java.simpleName) }
                ?.values
                ?.forEach { schema ->
                    val data = schema.properties?.get("data") ?: return@forEach
                    // NON_NULL omits this property instead of serializing a null value.
                    if (data.isNullSchema()) {
                        schema.properties.remove("data")
                    } else {
                        data.nullable = null
                        data.types = data.types?.filterNot { it == "null" }?.toMutableSet()
                        data.oneOf = data.oneOf?.filterNot { it.isNullSchema() }
                    }
                }
        }

    private fun addResponseBodyWrapperSchemaExample(operation: Operation) {
        operation.responses?.forEach { (responseCode, response) ->
            val status = responseCode.toIntOrNull()?.let(HttpStatus::resolve) ?: return@forEach
            if (status in setOf(HttpStatus.NO_CONTENT, HttpStatus.RESET_CONTENT, HttpStatus.NOT_MODIFIED)) {
                response.content = null
                return@forEach
            }
            response.content?.forEach media@{ (_, mediaType) ->
                val originalSchema = mediaType.schema ?: return@media
                val alreadyWrapped = originalSchema.isCommonApiResponse()
                if (!status.is2xxSuccessful && !alreadyWrapped) return@media
                if (alreadyWrapped &&
                    originalSchema.properties?.get("status")?.example == status.name &&
                    originalSchema.properties?.get("code")?.example == status.value()
                ) {
                    return@media
                }
                mediaType.schema = wrapSchema(originalSchema, status, alreadyWrapped)
            }
        }
    }

    private fun wrapSchema(
        originalSchema: Schema<*>,
        status: HttpStatus,
        alreadyWrapped: Boolean,
    ): Schema<*> =
        ObjectSchema().apply {
            required = listOf("status", "code", "message")
            addProperty("status", StringSchema().example(status.name))
            addProperty("code", IntegerSchema().example(status.value()))
            if (alreadyWrapped) {
                // Keep the resolved generic data schema and response-specific message.
                addAllOfItem(originalSchema)
            } else {
                addProperty("message", StringSchema().example(status.reasonPhrase))
                addProperty("data", originalSchema)
            }
        }

    private fun Schema<*>.isNullSchema(): Boolean = type == "null" || types == setOf("null")

    private fun Schema<*>.isCommonApiResponse(): Boolean =
        `$ref`?.substringAfterLast('/')?.startsWith(CommonApiResponse::class.java.simpleName) == true ||
            properties?.keys?.containsAll(listOf("status", "code", "message")) == true ||
            allOf.orEmpty().any { it.isCommonApiResponse() }
}
