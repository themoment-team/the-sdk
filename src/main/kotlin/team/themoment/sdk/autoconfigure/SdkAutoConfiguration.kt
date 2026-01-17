package team.themoment.sdk.autoconfigure

import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import team.themoment.sdk.config.ExceptionProperties
import team.themoment.sdk.config.LoggingProperties
import team.themoment.sdk.config.ResponseProperties
import team.themoment.sdk.config.SwaggerProperties
import team.themoment.sdk.exception.GlobalExceptionHandler
import team.themoment.sdk.logging.LoggingFilter
import team.themoment.sdk.response.ApiResponseWrapper
import team.themoment.sdk.response.CommonApiResponse

@AutoConfiguration
@EnableConfigurationProperties(
    LoggingProperties::class,
    ResponseProperties::class,
    SwaggerProperties::class,
    ExceptionProperties::class
)
class SdkAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
        prefix = "sdk.logging",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun loggingFilter(loggingProperties: LoggingProperties): LoggingFilter {
        return LoggingFilter(loggingProperties)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "sdk.response",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun apiResponseWrapper(responseProperties: ResponseProperties): ApiResponseWrapper {
        return ApiResponseWrapper(responseProperties)
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "sdk.swagger",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun operationCustomizer(): OperationCustomizer =
        OperationCustomizer { operation, handlerMethod ->
            val returnType = handlerMethod.method.returnType
            val hideDataField = CommonApiResponse::class.java.isAssignableFrom(returnType)
            addResponseBodyWrapperSchemaExample(operation, hideDataField)

            operation
        }

    @Bean
    @ConditionalOnProperty(
        prefix = "sdk.swagger",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun groupedOpenApi(
        swaggerProperties: SwaggerProperties,
        operationCustomizer: OperationCustomizer
    ): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(swaggerProperties.group)
            .pathsToMatch(*swaggerProperties.pathsToMatch.toTypedArray())
            .addOperationCustomizer(operationCustomizer)
            .build()

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

    @Bean
    @ConditionalOnProperty(
        prefix = "sdk.exception",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun globalExceptionHandler(exceptionProperties: ExceptionProperties): GlobalExceptionHandler {
        return GlobalExceptionHandler(exceptionProperties)
    }
}
