# GEMINI.md

This file provides guidance to Gemini CLI when working with code in this repository.

## Communication

Always respond in Korean.

## Project Overview

A modular Spring Boot SDK library (not an application) distributed via JitPack. It provides reusable auto-configured components for Spring Boot projects: HTTP logging, response wrapping, Swagger/OpenAPI setup, and exception handling.

- **Language**: Kotlin, Java 17 toolchain
- **Framework**: Spring Boot 3.x
- **Build**: Gradle (Kotlin DSL) 9.2
- **Package root**: `team.themoment.sdk`

## Commands

```bash
# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "team.themoment.sdk.SomeTest"

# Lint check (ktlint)
./gradlew ktlintCheck

# Auto-fix lint issues
./gradlew ktlintFormat

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

## Architecture

### Auto-Configuration Entry Point

`SdkAutoConfiguration` (`autoconfigure/`) is the sole Spring Boot auto-configuration class, registered in:

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

It reads `@ConfigurationProperties` beans from `config/` and conditionally registers each feature bean via `@ConditionalOnProperty`. All features default to `enabled = true`.

### Feature Modules

| Feature            | Bean class               | Config prefix   | Key files                                                               |
|--------------------|--------------------------|-----------------|-------------------------------------------------------------------------|
| Logging filter     | `LoggingFilter`          | `sdk.logging`   | `logging/LoggingFilter.kt`, `logging/CachedRequestBodyWrapper.kt`       |
| Response wrapping  | `ApiResponseWrapper`     | `sdk.response`  | `response/ApiResponseWrapper.kt`, `response/CommonApiResponse.kt`       |
| Swagger/OpenAPI    | `SwaggerConfig`          | `sdk.swagger`   | `swagger/SwaggerConfig.kt`                                              |
| Exception handling | `GlobalExceptionHandler` | `sdk.exception` | `exception/GlobalExceptionHandler.kt`, `exception/ExpectedException.kt` |

### Key Patterns

**Response wrapping** (`ApiResponseWrapper`): A `ResponseBodyAdvice` that wraps all controller responses in `CommonApiResponse<T>` (`{status, code, message, data}`). `null` return values become 204 No Content. Already-wrapped `CommonApiResponse` bodies are passed through unchanged. URL exclusion uses Ant path matching.

**Logging** (`LoggingFilter`): An `OncePerRequestFilter` that logs request/response details with a UUID correlation ID. Multipart request bodies are logged as `[multipart omitted]`. Uses `CachedRequestBodyWrapper` to allow the request body to be read multiple times.

**Exception handling** (`GlobalExceptionHandler`): Handles `ExpectedException` only. When `sdk.exception.use-english-message=false`, standard HTTP status codes are mapped to predefined Korean messages; custom messages bypass translation.

**Logger extensions** (`logging/logger/LoggerExtensions.kt`): SLF4J convenience extensions — `Any.logger()`, `Any.companionLogger()`, `logger(name)`, `logger(clazz)`.

### Configuration Properties

Each feature's properties class lives in `config/` and binds to `sdk.<feature>.*`. `not-logging-urls` and `not-wrapping-urls` accept Ant-style URL patterns.

See `src/main/resources/application.yml.example` for all available options.