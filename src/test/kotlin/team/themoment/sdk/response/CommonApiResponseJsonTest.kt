package team.themoment.sdk.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper

class CommonApiResponseJsonTest {
    private val mapper = JsonMapper.builder().build()

    @ParameterizedTest
    @EnumSource(HttpStatus::class, names = ["OK", "CREATED", "BAD_REQUEST"])
    fun `serializes status using Jackson 3 defaults`(status: HttpStatus) {
        val response = CommonApiResponse<Nothing>(status, status.value(), status.reasonPhrase)
        val json = mapper.readTree(mapper.writeValueAsString(response))

        assertEquals(status.toString(), json["status"].asString())
        assertEquals(status.value(), json["code"].asInt())
    }

    @Test
    fun `uses the application mapper enum setting`() {
        val mapper = JsonMapper.builder().disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING).build()
        val json = mapper.writeValueAsString(CommonApiResponse.error("에러", HttpStatus.BAD_REQUEST))

        assertEquals("BAD_REQUEST", mapper.readTree(json)["status"].asString())
    }
}
