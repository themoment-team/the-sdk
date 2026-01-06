# The SDK

Modular SDK for reusable Spring Boot components.

## Features

- **Logging Filter**: Automatic HTTP request/response logging with customizable exclusion patterns
- **Response Wrapper**: Automatic API response wrapping with consistent format
- **Swagger Configuration**: Pre-configured Swagger/OpenAPI documentation
- **Exception Handler**: Global exception handling with English/Korean message support

## Installation

### Using JitPack

Add the JitPack repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.themoment-team:the-sdk:1.0")
}
```

## Configuration

All features are auto-configured and enabled by default. You can customize them in your `application.yml`:

```yaml
sdk:
  logging:
    enabled: true
    not-logging-urls:
      - "/v3/api-docs/**"
      - "/swagger-ui/**"

  response:
    enabled: true
    not-wrapping-urls:
      - "/v3/api-docs/**"

  swagger:
    enabled: true
    title: "My API"
    description: "API Documentation"
    version: "v1"
    group: "API"
    paths-to-match:
      - "/v1/**"

  exception:
    enabled: true
    use-english-message: true  # true: English, false: Korean
```

## Usage

### Response Wrapper

The SDK automatically wraps your API responses in a consistent format:

```kotlin
@RestController
class MyController {
    @GetMapping("/users")
    fun getUsers(): List<User> {
        // Returns: CommonApiResponse<List<User>>
        return userService.findAll()
    }
}
```

Response format:
```json
{
  "status": "OK",
  "code": 200,
  "message": "OK",
  "data": [...]
}
```

### Exception Handling

Use `ExpectedException` for business logic errors:

```kotlin
import team.themoment.sdk.exception.ExpectedException
import org.springframework.http.HttpStatus

if (user == null) {
    throw ExpectedException("User not found", HttpStatus.NOT_FOUND)
}
```

### Manual Response Control

If you want to control the response format manually:

```kotlin
import team.themoment.sdk.response.CommonApiResponse

@GetMapping("/custom")
fun custom(): CommonApiResponse<String> {
    return CommonApiResponse.success("Custom message", "data")
}
```

## License

MIT License - see LICENSE file for details
