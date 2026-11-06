package team.themoment.sdk.support

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import team.themoment.sdk.exception.ExpectedException
import team.themoment.sdk.response.CommonApiResponse

@SpringBootApplication
class TestApplication

data class SampleBody(
    val name: String,
)

@RestController
class SampleController {
    @GetMapping("/v1/plain")
    fun plain(): SampleBody = SampleBody("the-sdk")

    @GetMapping("/v1/empty")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun empty(): SampleBody? = null

    @GetMapping("/v1/created")
    @ResponseStatus(HttpStatus.CREATED)
    fun created(): CommonApiResponse<SampleBody> = CommonApiResponse.created("생성되었습니다.", SampleBody("new"))

    @GetMapping("/v1/accepted")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun accepted(): ResponseEntity<CommonApiResponse<SampleBody>> =
        ResponseEntity.accepted().body(CommonApiResponse(HttpStatus.ACCEPTED, 202, "Accepted", SampleBody("queued")))

    @GetMapping("/v1/created-without-data")
    @ResponseStatus(HttpStatus.CREATED)
    fun createdWithoutData(): CommonApiResponse<Nothing> = CommonApiResponse.created("생성되었습니다.")

    @GetMapping("/v1/error")
    fun error(): SampleBody = throw ExpectedException("커스텀 에러", HttpStatus.BAD_REQUEST)

    @GetMapping("/v1/not-found")
    fun notFound(): SampleBody = throw ExpectedException(HttpStatus.NOT_FOUND.reasonPhrase, HttpStatus.NOT_FOUND)
}
