package team.themoment.sdk.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sdk.swagger")
data class SwaggerProperties(
    val title: String = "API Documentation",
    val description: String = "API Documentation",
    val version: String = "v1",
    val group: String = "API",
    val pathsToMatch: List<String> = listOf("/v1/**")
)
