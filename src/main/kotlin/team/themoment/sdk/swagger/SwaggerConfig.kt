package team.themoment.sdk.swagger

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.HandlerMethod
import team.themoment.sdk.config.ResponseProperties
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
class SwaggerConfig(
    private val swaggerProperties: SwaggerProperties,
    private val responseProperties: ResponseProperties,
) {
    private val matcher = AntPathMatcher()

    fun api(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(swaggerProperties.group)
            .pathsToMatch(*swaggerProperties.pathsToMatch.toTypedArray())
            .addOperationCustomizer(customOperationCustomizer())
            .build()

    private fun customOperationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            if (!responseProperties.enabled) {
                return@OperationCustomizer operation
            }

            val requestPath = getRequestPath(handlerMethod)
            if (isNotWrappingURL(requestPath)) {
                return@OperationCustomizer operation
            }

            val returnType = handlerMethod.method.returnType
            val hideDataField = CommonApiResponse::class.java.isAssignableFrom(returnType)
            addResponseBodyWrapperSchemaExample(operation, hideDataField)

            operation
        }

    private fun addResponseBodyWrapperSchemaExample(
        operation: Operation,
        hideDataField: Boolean,
    ) {
        operation.responses["200"]?.content?.let { content ->
            content.forEach { (_, mediaType) ->
                val originalSchema = mediaType.schema
                mediaType.schema = wrapSchema(originalSchema, hideDataField)
            }
        }
    }

    private fun wrapSchema(
        originalSchema: Schema<*>?,
        hideDataField: Boolean,
    ): Schema<*> =
        Schema<Any>().apply {
            addProperty("status", Schema<String>().type("string").example("OK"))
            addProperty("code", Schema<Int>().type("integer").example(200))
            addProperty("message", Schema<String>().type("string").example("OK"))
            if (!hideDataField) {
                addProperty("data", originalSchema)
            }
        }

    private fun getRequestPath(handlerMethod: HandlerMethod): String {
        val method = handlerMethod.method
        val methodPath = sequenceOf(
            method.getAnnotation(RequestMapping::class.java)?.value,
            method.getAnnotation(RequestMapping::class.java)?.path,
            method.getAnnotation(GetMapping::class.java)?.value,
            method.getAnnotation(GetMapping::class.java)?.path,
            method.getAnnotation(PostMapping::class.java)?.value,
            method.getAnnotation(PostMapping::class.java)?.path,
            method.getAnnotation(PutMapping::class.java)?.value,
            method.getAnnotation(PutMapping::class.java)?.path,
            method.getAnnotation(DeleteMapping::class.java)?.value,
            method.getAnnotation(DeleteMapping::class.java)?.path,
            method.getAnnotation(PatchMapping::class.java)?.value,
            method.getAnnotation(PatchMapping::class.java)?.path
        ).firstNotNullOfOrNull { it?.firstOrNull() } ?: ""
        val classPathArray = handlerMethod.beanType.getAnnotation(RequestMapping::class.java)?.value
            ?: handlerMethod.beanType.getAnnotation(RequestMapping::class.java)?.path
            ?: emptyArray()
        val classPath = classPathArray.firstOrNull() ?: ""

        return "$classPath$methodPath"
    }

    private fun isNotWrappingURL(requestURI: String): Boolean =
        responseProperties.notWrappingUrls.any { pattern ->
            matcher.match(pattern, requestURI)
        }
}
