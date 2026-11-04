package team.themoment.sdk.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.json.JsonMapper

class CommonApiResponseJsonTest {
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `serializes status as enum name`() {
        val json = mapper.writeValueAsString(CommonApiResponse.error("에러", HttpStatus.BAD_REQUEST))

        assertEquals("BAD_REQUEST", mapper.readTree(json).get("status").asString())
    }

    @Test
    fun `deserializes status from enum name`() {
        val holder = mapper.readValue("""{"status":"NOT_FOUND"}""", StatusHolder::class.java)

        assertEquals(HttpStatus.NOT_FOUND, holder.status)
    }

    @Test
    fun `deserializes status from toString form`() {
        val holder = mapper.readValue("""{"status":"404 NOT_FOUND"}""", StatusHolder::class.java)

        assertEquals(HttpStatus.NOT_FOUND, holder.status)
    }

    class StatusHolder {
        @field:JsonDeserialize(using = HttpStatusNameDeserializer::class)
        var status: HttpStatus? = null
    }
}
