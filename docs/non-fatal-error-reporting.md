# 비치명 오류 기록 연결 가이드

기능에서 처리하는 API와 캐시 실패는 `NonFatalErrorReporter`로 기록합니다. reporter는 오류를 분류하거나 재시도하지 않습니다. 호출 계층이 실패의 의미와 기록 책임을 정하고, reporter는 허용한 정보만 Crashlytics에 전달합니다.

공통 기반만 추가된 상태이며 기능별 연결은 별도 작업입니다. 담당자와 진행 상태는 [이슈 #439](https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/issues/439)에서 관리합니다.

## 연결 순서

1. 실패를 만든 source부터 repository, use case, ViewModel까지 추적합니다. 예외가 `Result`, 빈 결과, 부분 결과로 바뀌기 전에 기록을 맡을 한 지점을 정합니다.
2. 해당 클래스의 기존 Hilt 생성자에 `private val reporter: NonFatalErrorReporter`를 추가합니다. `domain.diagnostics`의 계약을 사용하며 기능 모듈에서 Firebase나 app 구현체를 직접 참조하지 않습니다. app의 `DiagnosticsModule`이 binding을 제공합니다.
3. 정상 결과와 취소를 제외한 뒤, 확인한 실패 종류로 `NonFatalErrorContext`를 만듭니다. 아래 필드 기준으로 표현할 수 없는 실패는 임의로 다른 범주에 넣지 않고 공통 계약에 필요한 변경부터 정합니다.
4. 같은 요청의 같은 실패는 한 번만 기록합니다. 기존 반환값, 예외 전파, 캐시, 재시도와 UI 오류 안내는 유지합니다.
5. 기록 횟수와 발생하면 안 되는 부수 효과를 테스트한 뒤 기능 PR에 연결한 위치와 검증 근거를 적습니다.

## 기록 여부와 책임

| 상황 | 처리 |
| --- | --- |
| API 요청, 응답 파싱, 캐시 작업의 처리 가능한 실패 | 원인을 소유한 경계에서 분류해 한 번 기록 |
| 정상적인 빈 목록, 미검색, 데이터 없음 응답 | 기록하지 않음 |
| caller 또는 상위 coroutine의 취소 | 기록하지 않고 원래 취소를 전파 |
| `Error` 등 치명 오류 | 비치명 오류로 바꾸지 않고 전파 |
| 재시도로 복구된 중간 실패 | 매 시도마다 기록하지 않음. 정한 요청 단위의 최종 실패에서 기록 |
| 실패 때문에 일부 결과만 반환 | 원인을 확인한 뒤 `isPartialResult = true`로 기록하고 기존 부분 결과 유지 |
| 조회 상한 등 정상 정책으로 결과가 제한됨 | 부분 결과라는 이유만으로 오류를 만들지 않음 |
| 여러 caller가 같은 진행 중 요청을 공유 | 기다리는 caller마다 기록하지 않고 실제 요청을 수행한 경계에서 한 번 기록 |

reporter 자체에는 중복 제거, 재시도, 부분 결과 판정 기능이 없습니다. source에서 기록한 예외를 repository나 ViewModel에서 다시 기록하지 않습니다. 원인을 잃은 결과만 보고 새 예외를 만들어 보완하지 않습니다.

## 필드 선택

모든 타입은 `com.team.yeogibeoryeo.domain.diagnostics`에 있습니다.

| 필드 | 값과 기준 |
| --- | --- |
| `api` | 품목 `ITEM_GUIDE`, 지도 수거 장소 `COLLECTION_SPOT`, 지역 가이드 `REGIONAL_GUIDE` |
| `stage` | 요청 `REMOTE_REQUEST`, 응답 변환 `RESPONSE_PARSING`, 캐시 읽기 `CACHE_READ`, 캐시 쓰기 `CACHE_WRITE` |
| `category` | 연결 실패 `NETWORK`, 처리 가능한 시간 초과 `TIMEOUT`, 확인된 HTTP 실패 `HTTP`, 파싱 실패 `PARSING`, 캐시 실패 `CACHE` |
| `httpStatusClass` | 상태 코드를 실제로 받은 경우 100~199 `INFORMATIONAL`, 200~299 `SUCCESS`, 300~399 `REDIRECTION`, 400~499 `CLIENT_ERROR`, 500~599 `SERVER_ERROR`. 상태 코드를 알 수 없으면 `NOT_AVAILABLE` |
| `retryCount` | 앱이 해당 요청에서 실행한 재시도 횟수. 재시도 없음 `NONE`, 1회 `ONE`, 2회 `TWO`, 3회 이상 `THREE_OR_MORE` |
| `isPartialResult` | 실패 후 일부 결과를 반환하기로 결정한 경우만 `true`. 기본값은 `false` |

`httpStatusClass` 기본값은 `NOT_AVAILABLE`, `retryCount` 기본값은 `NONE`입니다. 페이지 번호나 가져온 페이지 수를 재시도 횟수로 사용하지 않습니다. SDK 내부 재시도 횟수나 예외 message를 분석해 필드를 추정하지 않습니다. `TIMEOUT`은 취소 예외를 무조건 기록하라는 뜻이 아닙니다.

원본 예외는 `report(error, context)`의 `error`로 전달하되, 별도의 log나 전역 custom key에 복사하지 않습니다. reporter는 원본 message, cause, suppressed exception과 stack trace를 버리고 고정된 분류값으로 새 예외를 만듭니다. 검색어, 주소, 지역명 원문, 좌표, API 키, 인증 정보, 전체 URL query, 응답 body와 사용자 식별값을 필드나 새 enum 값으로 추가하지 않습니다.

## 단일 요청 호출 예시

앱이 재시도하지 않는 지도 요청에서 network timeout 기록을 맡은 한 경계의 예시입니다. `request`는 하나의 최종 요청을 실행하며, 내부에서 같은 실패를 기록하지 않는다는 전제입니다. 기존 예외 전파를 유지합니다. 이 helper를 공통 코드에 추가하라는 뜻은 아니며 실제 연결에서는 기존 실패 처리 위치에 기록 호출을 넣습니다.

```kotlin
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import kotlinx.coroutines.CancellationException
import java.net.SocketTimeoutException

suspend fun <T> fetchMapOnce(
    reporter: NonFatalErrorReporter,
    request: suspend () -> T,
): T = try {
    request()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: SocketTimeoutException) {
    reporter.report(
        error = failure,
        context = NonFatalErrorContext(
            api = NonFatalApi.COLLECTION_SPOT,
            stage = NonFatalStage.REMOTE_REQUEST,
            category = NonFatalCategory.TIMEOUT,
        ),
    )
    throw failure
}
```

`CancellationException`과 그 하위 타입인 `TimeoutCancellationException`은 reporter에 직접 전달해도 같은 객체로 다시 던집니다. debug에서도 같습니다. `SocketTimeoutException`은 coroutine 취소가 아니므로 별도로 분류할 수 있습니다.

cause에 취소 예외가 있다는 이유만으로 wrapper 전체를 취소로 바꾸지 않습니다. 반대로 caller 취소를 새 일반 예외로 감싸 기록해서도 안 됩니다. timeout을 만든 주체와 wrapper 계약을 확인하고, 생산자가 소유한 시간 제한 실패와 caller의 취소를 구분합니다.

## 기능별 연결 지점

### 품목 검색

[품목 repository](../data/src/main/java/com/team/yeogibeoryeo/data/item/repository/DisposalItemGuideRepositoryImpl.kt)는 현재 [앱에 포함된 JSON과 lazy 저장값](../data/src/main/java/com/team/yeogibeoryeo/data/item/local/ItemCategoryLocalDataSource.kt)을 읽습니다. 공백 검색, 일치 항목 없음과 상세 조회의 `null`은 정상 결과이며 기록하지 않습니다.

파일 읽기나 JSON 변환 실패를 기록하려면 실제 발생 위치와 필요한 분류를 먼저 정합니다. 현재 계약에는 asset 전용 단계가 없으므로 packaged asset 실패를 원격 요청이나 응답 실패로 표시하지 않습니다. `ITEM_GUIDE`가 있다는 이유만으로 기존 단계에 맞지 않는 실패까지 기록하지 않습니다.

### 지도

[지도 원격 source](../data/src/main/java/com/team/yeogibeoryeo/data/spot/remote/datasource/SpotRemoteDataSource.kt)는 키워드 검색의 후속 페이지 실패를 부분 결과로 바꾸며, 위치 검색은 후속 페이지 실패 후 누적 목록을 반환하는 경로가 있습니다. repository의 최종 예외만 관측하면 이 실패를 놓칠 수 있으므로 원본 예외가 남아 있는 처리 위치를 확인합니다.

첫 요청 실패와 후속 페이지 실패를 구분하고, 페이지 반복 전체를 하나의 요청으로 볼 때의 기록 책임을 정합니다. 성공 빈 목록과 `NO_DATA` 응답은 기록하지 않습니다. 기존 부분 결과 반환 동작을 바꾸거나 같은 실패를 source와 repository에 이중으로 추가하지 않습니다.

### 지역 가이드

[지역 가이드 repository](../data/src/main/java/com/team/yeogibeoryeo/data/regionalguide/repository/RegionalDisposalGuideRepositoryImpl.kt)는 진행 중 요청을 여러 caller가 공유합니다. 각 `await()` 뒤에 기록을 붙이면 동일 실패가 중복될 수 있습니다. 실제 fetch를 실행하는 경계와 [원격 source](../data/src/main/java/com/team/yeogibeoryeo/data/regionalguide/remote/RegionalGuideRemoteDataSource.kt)의 부분 결과 변환 경계를 함께 확인합니다.

`Result.failure`만으로는 부분 결과의 실패 원인을 모두 관측할 수 없습니다. `PAGE_LIMIT`은 조회 상한이므로 오류로 간주하지 않습니다. source가 자신이 소유한 timeout을 `RegionalGuideLookupException` 또는 부분 결과로 바꾸는 계약과, 상위 scope 취소를 전파하는 경로를 구분합니다. 기록을 추가해도 부분 결과를 캐시에 저장하지 않는 규칙과 공유 요청 정리 동작은 유지합니다.

## 기능 연결 PR의 검증

기능 테스트에서는 `NonFatalErrorReporter`를 구현한 기록용 fake를 주입합니다. debug 구현체는 호출을 버리므로 실제 호출 횟수와 context 검증을 대신할 수 없습니다. [공통 reporter 테스트](../app/src/test/java/com/team/yeogibeoryeo/core/diagnostics/CrashlyticsNonFatalErrorReporterTest.kt)는 정제와 기록 정책을 확인하고, 기능 테스트는 호출 책임을 확인합니다.

1. 정상 성공, 빈 결과, 데이터 없음은 호출 0회인지 확인합니다.
2. 처리 가능한 최종 실패는 정한 경계에서 1회 호출하고 API, 단계, 오류 범주가 실제 발생 경로와 맞는지 확인합니다.
3. 실제 상위 scope 취소와 기능이 소유한 timeout을 각각 발생시킵니다. 취소된 caller가 추가 기록, 재시도와 UI 변경을 시작하지 않는지 확인합니다. 공유 요청은 caller 취소와 producer 취소를 나누어 검증하고, 독립 producer의 정상 완료와 캐시 저장은 기존 계약대로 유지합니다.
4. 재시도 성공/소진, 부분 결과, 정상 조회 상한, 공유 요청을 검증합니다. 같은 실제 요청을 기다리는 caller 수가 기록 수를 늘리지 않아야 합니다.
5. 기록 뒤에도 기존 반환값, 오류 안내, 재시도, 캐시 정책과 공유 요청 정리를 유지하는지 확인합니다.
6. 검색어, 주소, 인증 정보와 응답 원문이 추가 log나 전역 key에 들어가지 않는지 확인합니다.

debug는 Firebase 기록 함수를 호출하지 않습니다. release reporter도 기존 수집 설정을 바꾸지 않으므로 호출 성공이 서버 수신을 보장하지는 않습니다. 공통 단위 테스트는 외부 전송 없이 실행하며 실제 Firebase 수신은 별도의 승인된 검증에서 확인합니다.
