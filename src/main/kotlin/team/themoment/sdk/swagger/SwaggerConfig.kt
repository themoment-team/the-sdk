package team.themoment.sdk.swagger

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ResolvableType
import org.springframework.http.HttpEntity
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
        OperationCustomizer { operation, handlerMethod ->
            val returnType = ResolvableType.forMethodParameter(handlerMethod.returnType)
            val bodyType =
                if (HttpEntity::class.java.isAssignableFrom(returnType.toClass())) {
                    returnType.`as`(HttpEntity::class.java).getGeneric(0)
                } else {
                    returnType
                }
            val returnsEnvelope = CommonApiResponse::class.java.isAssignableFrom(bodyType.toClass())
            addResponseBodyWrapperSchemaExample(operation, returnsEnvelope)

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
    fun commonApiResponseModelConverter(): ModelConverter =
        ModelConverter { type, context, chain ->
            val schema = if (chain.hasNext()) chain.next().resolve(type, context, chain) else null
            val rawClass = type.type?.let { Json.mapper().constructType(it).rawClass }
            if (schema != null && rawClass == CommonApiResponse::class.java) {
                val envelope = schema.`$ref`?.substringAfterLast('/')?.let { context.definedModels[it] } ?: schema

                @Suppress("UNCHECKED_CAST")
                val status = envelope.properties?.get("status") as? Schema<String>
                status?.setEnum(HttpStatus.entries.map { it.name })
                val data = envelope.properties?.get("data")
                if (data != null) {
                    // NON_NULL makes data optional, including when its only possible value is null.
                    envelope.required = envelope.required?.filterNot { it == "data" }?.takeIf { it.isNotEmpty() }
                    if (!data.excludeNullValue()) envelope.properties.remove("data")
                }
            }
            schema
        }

    private fun addResponseBodyWrapperSchemaExample(
        operation: Operation,
        returnsEnvelope: Boolean,
    ) {
        val commonResponseRef =
            ModelConverters
                .getInstance()
                .resolveAsResolvedSchema(AnnotatedType(CommonApiResponse::class.java).resolveAsRef(true))
                .schema.`$ref`
        operation.responses?.forEach { (responseCode, response) ->
            val status = responseCode.toIntOrNull()?.let(HttpStatus::resolve) ?: return@forEach
            if (status in setOf(HttpStatus.NO_CONTENT, HttpStatus.RESET_CONTENT, HttpStatus.NOT_MODIFIED)) {
                response.content = null
                return@forEach
            }
            response.content?.forEach media@{ (_, mediaType) ->
                val originalSchema = mediaType.schema ?: return@media
                if (originalSchema is WrappedResponseSchema) return@media
                val alreadyWrapped = returnsEnvelope || originalSchema.`$ref` != null && originalSchema.`$ref` == commonResponseRef
                if (!status.is2xxSuccessful && !alreadyWrapped) return@media
                mediaType.schema = wrapSchema(originalSchema, status, alreadyWrapped)
            }
        }
    }

    private fun wrapSchema(
        originalSchema: Schema<*>,
        status: HttpStatus,
        alreadyWrapped: Boolean,
    ): Schema<*> =
        WrappedResponseSchema().apply {
            required = listOf("status", "code", "message")
            addProperty(
                "status",
                StringSchema().apply {
                    setEnum(HttpStatus.entries.map { it.name })
                    example = status.name
                },
            )
            addProperty("code", IntegerSchema().example(status.value()))
            if (alreadyWrapped) {
                // Keep the resolved generic data schema and response-specific message.
                addAllOfItem(originalSchema)
            } else {
                addProperty("message", StringSchema().example(status.reasonPhrase))
                addProperty("data", originalSchema)
            }
        }

    private fun Schema<*>.excludeNullValue(): Boolean {
        if (type == "null" || types == setOf("null")) return false
        nullable = null
        types = types?.filterNot { it == "null" }?.toMutableSet()
        oneOf?.let { alternatives ->
            oneOf = alternatives.filter { it.excludeNullValue() }
            if (oneOf.isEmpty()) return false
        }
        anyOf?.let { alternatives ->
            anyOf = alternatives.filter { it.excludeNullValue() }
            if (anyOf.isEmpty()) return false
        }
        return true
    }

    private class WrappedResponseSchema : ObjectSchema()
}
