package team.themoment.sdk.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import team.themoment.sdk.config.ExceptionProperties
import team.themoment.sdk.response.CommonApiResponse

@RestControllerAdvice
class GlobalExceptionHandler(
    private val exceptionProperties: ExceptionProperties
) {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ExpectedException::class)
    private fun expectedException(ex: ExpectedException): CommonApiResponse<Nothing> {
        logger.warn("ExpectedException : {} ", ex.message)
        logger.trace("ExpectedException Details : ", ex)

        val message = if (exceptionProperties.useEnglishMessage) {
            ex.message ?: "An error occurred"
        } else {
            translateToKorean(ex.statusCode, ex.message)
        }

        return CommonApiResponse.error(message, ex.statusCode)
    }

    private fun translateToKorean(statusCode: HttpStatus, originalMessage: String?): String {
        // 기본 HTTP 상태 코드에 대한 한글 메시지 매핑
        val defaultKoreanMessage = when (statusCode) {
            HttpStatus.BAD_REQUEST -> "잘못된 요청입니다"
            HttpStatus.UNAUTHORIZED -> "인증이 필요합니다"
            HttpStatus.FORBIDDEN -> "접근 권한이 없습니다"
            HttpStatus.NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다"
            HttpStatus.METHOD_NOT_ALLOWED -> "허용되지 않은 메서드입니다"
            HttpStatus.CONFLICT -> "요청이 충돌했습니다"
            HttpStatus.INTERNAL_SERVER_ERROR -> "서버 내부 오류가 발생했습니다"
            HttpStatus.SERVICE_UNAVAILABLE -> "서비스를 사용할 수 없습니다"
            else -> "오류가 발생했습니다"
        }

        // 원본 메시지가 상태 코드의 기본 reasonPhrase와 같으면 한글 메시지 사용
        return if (originalMessage == statusCode.reasonPhrase || originalMessage.isNullOrEmpty()) {
            defaultKoreanMessage
        } else {
            originalMessage // 커스텀 메시지는 그대로 사용
        }
    }
}
