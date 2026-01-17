package team.themoment.sdk.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sdk.exception")
data class ExceptionProperties(
    val useEnglishMessage: Boolean = true,
)
