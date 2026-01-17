package team.themoment.sdk.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sdk.response")
data class ResponseProperties(
    val enabled: Boolean = true,
    val notWrappingUrls: List<String> =
        listOf(
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/swagger-ui/**",
        ),
)
