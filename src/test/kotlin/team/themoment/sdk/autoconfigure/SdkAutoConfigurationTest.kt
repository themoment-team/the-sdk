package team.themoment.sdk.autoconfigure

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import team.themoment.sdk.exception.GlobalExceptionHandler
import team.themoment.sdk.logging.LoggingFilter
import team.themoment.sdk.response.ApiResponseWrapper

class SdkAutoConfigurationTest {
    private val contextRunner =
        WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SdkAutoConfiguration::class.java))

    @Test
    fun `registers all feature beans by default`() {
        contextRunner.run { context ->
            assertEquals(1, context.getBeansOfType(LoggingFilter::class.java).size)
            assertEquals(1, context.getBeansOfType(ApiResponseWrapper::class.java).size)
            assertEquals(1, context.getBeansOfType(GlobalExceptionHandler::class.java).size)
            assertEquals(1, context.getBeansOfType(GroupedOpenApi::class.java).size)
        }
    }

    @Test
    fun `skips feature beans when disabled by properties`() {
        contextRunner
            .withPropertyValues(
                "sdk.logging.enabled=false",
                "sdk.response.enabled=false",
                "sdk.exception.enabled=false",
                "sdk.swagger.enabled=false",
            ).run { context ->
                assertTrue(context.getBeansOfType(LoggingFilter::class.java).isEmpty())
                assertTrue(context.getBeansOfType(ApiResponseWrapper::class.java).isEmpty())
                assertTrue(context.getBeansOfType(GlobalExceptionHandler::class.java).isEmpty())
                assertTrue(context.getBeansOfType(GroupedOpenApi::class.java).isEmpty())
            }
    }
}
