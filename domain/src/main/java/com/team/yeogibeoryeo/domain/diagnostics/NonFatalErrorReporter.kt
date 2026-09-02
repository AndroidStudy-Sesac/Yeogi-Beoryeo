package com.team.yeogibeoryeo.domain.diagnostics

/**
 * 처리한 API 또는 캐시 실패를 기록하는 공통 계약입니다.
 *
 * 실패를 처리하는 한 경계에서만 호출합니다. 정상적인 빈 결과에는 호출하지 않습니다.
 * 원본 오류는 취소와 치명 오류를 구분하는 데만 사용하며 기록 장치에 전달하지 않습니다.
 * 직접 전달한 CancellationException과 Error는 debug에서도 원객체로 다시 던집니다.
 * 알려진 wrapper의 해석과 요청 단위 중복 기록 방지는 호출 계층의 책임입니다.
 * 기록 장치의 취소를 제외한 RuntimeException은 기존 오류 안내나 재시도를 바꾸지 않습니다.
 *
 * [기능 연결 가이드](../../../../../../../../../docs/non-fatal-error-reporting.md)에
 * 호출 순서, 필드 선택, 기능별 경계와 회귀 테스트 기준을 정리했습니다.
 */
interface NonFatalErrorReporter {
    fun report(error: Throwable, context: NonFatalErrorContext)
}

/** 사용자 입력이나 자유 문자열 대신 정해진 분류값만 기록합니다. */
data class NonFatalErrorContext(
    val api: NonFatalApi,
    val stage: NonFatalStage,
    val category: NonFatalCategory,
    val httpStatusClass: NonFatalHttpStatusClass = NonFatalHttpStatusClass.NOT_AVAILABLE,
    val retryCount: NonFatalRetryCount = NonFatalRetryCount.NONE,
    val isPartialResult: Boolean = false,
)

enum class NonFatalApi {
    ITEM_GUIDE,
    COLLECTION_SPOT,
    REGIONAL_GUIDE,
}

enum class NonFatalStage {
    REMOTE_REQUEST,
    RESPONSE_PARSING,
    CACHE_READ,
    CACHE_WRITE,
}

enum class NonFatalCategory {
    NETWORK,
    TIMEOUT,
    HTTP,
    PARSING,
    CACHE,
}

enum class NonFatalHttpStatusClass {
    NOT_AVAILABLE,
    INFORMATIONAL,
    SUCCESS,
    REDIRECTION,
    CLIENT_ERROR,
    SERVER_ERROR,
}

/** 세 번 이상 재시도한 실패는 한 범주로 묶어 기록값의 종류를 제한합니다. */
enum class NonFatalRetryCount {
    NONE,
    ONE,
    TWO,
    THREE_OR_MORE,
}
