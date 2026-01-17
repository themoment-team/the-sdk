package team.themoment.sdk.swagger

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import team.themoment.sdk.config.SwaggerProperties

@OpenAPIDefinition(
    info =
        Info(
            title = "API Documentation",
            description = "API Documentation",
            version = "v1",
        ),
)
class SwaggerConfig(
    private val swaggerProperties: SwaggerProperties,
) {
    fun api(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(swaggerProperties.group)
            .pathsToMatch(*swaggerProperties.pathsToMatch.toTypedArray())
            .addOpenApiCustomizer(openApiCustomizer())
            .build()

    private fun openApiCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths?.forEach { (_, pathItem) ->
                listOfNotNull(
                    pathItem.get,
                    pathItem.post,
                    pathItem.put,
                    pathItem.delete,
                    pathItem.patch,
                    pathItem.options,
                    pathItem.head
                ).forEach { operation ->
                    wrapOperationResponses(operation)
                }
            }
        }

    private fun wrapOperationResponses(operation: Operation) {
        operation.responses?.forEach { (statusCode, apiResponse) ->
            if (statusCode.startsWith("2")) {
                apiResponse.content?.forEach { (_, mediaType) ->
                    val originalSchema = mediaType.schema
                    if (originalSchema != null && !isAlreadyWrapped(originalSchema)) {
                        mediaType.schema = wrapSchemaForOpenApi(originalSchema)
                    }
                }
            }
        }
    }

    private fun isAlreadyWrapped(schema: Schema<*>): Boolean {
        val properties = schema.properties ?: return false
        return properties.containsKey("status") &&
               properties.containsKey("code") &&
               properties.containsKey("message")
    }

    private fun wrapSchemaForOpenApi(originalSchema: Schema<*>): Schema<*> =
        Schema<Any>().apply {
            type = "object"
            addProperty("status", Schema<String>().apply {
                type = "string"
                example = "OK"
            })
            addProperty("code", Schema<Int>().apply {
                type = "integer"
                example = 200
            })
            addProperty("message", Schema<String>().apply {
                type = "string"
                example = "OK"
            })
            addProperty("data", originalSchema)
        }
}
