package team.themoment.sdk.response

import org.springframework.http.HttpStatus
import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer

/**
 * Jackson 3은 enum을 기본적으로 toString()으로 직렬화하므로 HttpStatus가 "200 OK"로 나간다.
 * 1.x와 동일한 응답 형식("OK")을 유지하기 위해 enum 이름으로 직렬화한다.
 */
class HttpStatusNameSerializer : ValueSerializer<HttpStatus>() {
    override fun serialize(
        value: HttpStatus,
        gen: JsonGenerator,
        ctxt: SerializationContext,
    ) {
        gen.writeString(value.name)
    }
}

/**
 * enum 이름("OK")과 toString() 형식("200 OK")을 모두 받아들인다.
 */
class HttpStatusNameDeserializer : ValueDeserializer<HttpStatus>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): HttpStatus {
        val raw = p.valueAsString?.trim().orEmpty()
        return HttpStatus.entries.firstOrNull { it.name == raw || it.toString() == raw }
            ?: ctxt.handleWeirdStringValue(HttpStatus::class.java, raw, "Unknown HttpStatus") as HttpStatus
    }
}
