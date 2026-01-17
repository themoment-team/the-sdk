package team.themoment.sdk.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sdk.logging")
data class LoggingProperties(
    val notLoggingUrls: List<String> =
        listOf(
            "/v3/api-docs/**",
            "/swagger-ui/**",
        ),
)
