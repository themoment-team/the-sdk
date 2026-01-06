package team.themoment.sdk.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.RestControllerAdvice
import team.themoment.sdk.config.ExceptionProperties
import team.themoment.sdk.config.LoggingProperties
import team.themoment.sdk.config.ResponseProperties
import team.themoment.sdk.config.SwaggerProperties
import team.themoment.sdk.exception.GlobalExceptionHandler
import team.themoment.sdk.logging.LoggingFilter
import team.themoment.sdk.response.ApiResponseWrapper
import team.themoment.sdk.swagger.SwaggerConfig

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
    @RestControllerAdvice
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
    fun swaggerConfig(swaggerProperties: SwaggerProperties): SwaggerConfig {
        return SwaggerConfig(swaggerProperties)
    }

    @Bean
    @RestControllerAdvice
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
