# Focused coverage 운영 정책

Focused coverage는 JVM unit test로 검증할 business logic의 현재 상태와 테스트 보강 방향을 확인하는 참고 지표입니다. 앱 전체 코드의 테스트 비율이나 UI 품질을 뜻하지 않습니다. 수치보다 미검증 조건 분기, 오류 처리와 상태 전환을 찾는 데 우선 사용합니다.

## 현재 baseline

| 항목 | 값 |
|---|---|
| 측정 source 기준 | `9bf5c6320e9bcb673856cb6ad0af5350103220c7`의 production·test source |
| 측정 설정 기준 | 이 문서와 같은 revision의 Kover filter |
| 최초 CI 검증 commit | `d289f2a5885517acacca1c608d5f6c66f7366216` |
| 측정 시각 | 2026-08-06 11:36 KST |
| 측정 도구 | Kover 0.9.9, AGP 9.2.1, Kotlin 2.2.10, JDK 21 |
| 측정 variant | Android `debug` + JVM `jvm` |
| 포함 module | `app`, `data`, `domain`, `presentation` |
| unit test | 파일 108개, `@Test` 1,016개 |
| Line | 89.92% (5,658/6,292) |
| Branch | 75.05% (2,452/3,267) |
| CI 운영 | 현재 수치와 baseline 대비 증감을 정보성 report로 제공 |
| 상세 report | `focused-coverage-<run-id>-<attempt>` artifact |

Baseline은 최초 측정의 covered/total을 참고 기준으로 보존합니다. CI는 현재 수치와 baseline 대비 증감을 표시하며, coverage 수치만으로 check를 실패시키지 않습니다.

| 모듈 | Line | Branch |
|---|---:|---:|
| `app` | 83.33% (205/246) | 77.61% (104/134) |
| `data` | 77.78% (1,278/1,643) | 65.30% (542/830) |
| `domain` | 95.67% (1,946/2,034) | 79.33% (971/1,224) |
| `presentation` | 94.09% (2,229/2,369) | 77.39% (835/1,079) |

Baseline은 표의 production·test source와 filter로 생성했습니다. 각 CI 실행에서 측정한 정확한 commit은 Actions Summary에 표시합니다. 측정 범위가 바뀌면 변경 이유와 전후 수치를 같은 PR에 기록하고 새 baseline을 확정합니다.

## 측정 대상

### app

앱 사용 가이드의 상태·좌표 계산과 navigation argument 변환·복원 정책을 측정합니다.

### domain

Use case와 입력 정규화, 호환성·선택·검색 policy를 측정합니다.

### data

JVM에서 실행할 수 있는 repository, mapper, parser, normalizer와 data source를 측정합니다. Room·DataStore를 사용하는 repository도 DAO나 storage를 fake로 교체해 transaction·변환 로직을 검증할 수 있으면 포함합니다.

### presentation

ViewModel, mapper, policy, formatter와 화면 결과를 결정하는 순수 상태·계산 helper를 측정합니다. 같은 source file에 Compose 코드가 있어도 `@Composable` declaration은 제외합니다.

파일명만으로 포함 여부를 결정하지 않습니다. 사용자 결과나 상태를 결정하는 조건·변환·오류 처리 로직인지 확인합니다.

## 제외 대상

1. `@Composable` 렌더링 declaration과 Preview
2. Activity, Application, NavHost와 Android framework 연결 코드
3. Naver Map, 위치 권한, logger 같은 device integration
4. DTO, Entity, 단순 data class와 상수 모음
5. Repository·API·DAO interface와 DI module
6. `R`, `BuildConfig`, Manifest와 Hilt·Room·KSP·serialization 생성 코드
7. Room database·DAO 연결과 local JVM에서 실행할 수 없는 device integration

테스트 작성이 어렵다는 이유만으로 business logic을 제외하지 않습니다. Android integration으로 분류한 코드는 instrumented test처럼 알맞은 검증 수단과 연결합니다. Android instrumented test 결과는 Kover focused coverage 수치에 포함되지 않습니다.

## 로컬 실행

프로젝트에 필요한 `local.properties`와 debug Firebase 설정을 준비한 뒤 실행합니다.

Kover와 Gradle은 HTML·XML report를 생성합니다. Python script는 전체와 모듈별 raw covered/total을 집계하고 baseline과 현재 수치의 차이를 계산해 Actions Summary를 만듭니다.

```shell
./gradlew :koverXmlReportFocused :koverHtmlReportFocused
```

생성 위치:

1. HTML: `build/reports/kover/focused/html/index.html`
2. XML: `build/reports/kover/focused/report.xml`

현재 수치와 baseline 차이는 다음 명령으로 확인합니다.

```shell
python -X utf8 .github/scripts/focused_coverage_summary.py \
  --report build/reports/kover/focused/report.xml \
  --properties gradle.properties
```

Summary script의 회귀 test는 다음 명령으로 확인합니다.

```shell
python -X utf8 -m unittest discover -s .github/scripts -p 'test_focused_coverage_summary.py'
```

Windows PowerShell에서는 줄 연속 문자 대신 명령을 한 줄로 실행합니다.

## GitHub Actions 결과 확인

`Android CI` workflow의 `Focused Coverage` job을 엽니다. Summary 표에서 다음 항목을 확인합니다.

`Focused Coverage`는 coverage 수치만으로 CI check를 실패시키지 않습니다. Unit test, report 생성, XML 파싱이나 baseline 입력 검증이 실패하면 check에도 반영됩니다.

1. `현재`: 해당 commit의 covered/total과 coverage 비율
2. `baseline`: 최초로 확정한 coverage 비율
3. `차이`: baseline 대비 percentage point 변화
4. `모듈별 coverage`: `app`, `data`, `domain`, `presentation`의 Line·Branch 현재 수치와 baseline 대비 변화
5. `상태`: Line이나 Branch가 baseline보다 낮으면 merge 차단 없이 `확인 필요`로 표시

`Line`은 실행된 source line 비율입니다. `Branch`는 조건문의 각 경로가 실행된 비율입니다. 조건 분기 회귀를 볼 때는 Branch를 먼저 확인합니다.

상세 분석이 필요하면 Summary의 artifact를 내려받아 HTML의 `index.html`을 엽니다. 빨간 줄은 실행되지 않은 코드이고, 노란 줄은 일부 branch만 실행된 코드입니다. XML은 CI와 분석 도구에서 사용합니다. Artifact는 14일 동안 보존됩니다.

다음 상태는 coverage가 높은 것으로 해석하지 않습니다.

1. `0/0`: filter나 variant 설정 오류 가능성이 있습니다.
2. Report 없음: test·compile 실패와 report 생성 실패를 나눠 확인합니다.
3. 비율 급변: covered/total과 filter 변경을 먼저 비교합니다.

## Coverage 하락 대응

1. 같은 commit, task와 filter로 측정됐는지 확인합니다.
2. HTML report에서 새로 누락된 line·branch를 찾습니다.
3. 원인을 미검증 동작, dead code, 측정 범위 변경으로 구분합니다.
4. 미검증 동작이면 사용자 결과를 기준으로 test를 추가합니다.
5. Dead code면 코드를 제거합니다.
6. 범위 밖 코드라면 정책 근거를 적고 filter를 수정한 뒤 baseline을 다시 측정합니다.

수치만 맞추기 위한 임의 exclusion 추가와 getter·constructor만 호출하는 test 추가는 허용하지 않습니다.

새 PR에서 coverage가 낮아지면 변경한 동작의 위험과 미검증 경로를 확인하고 필요한 test를 보강합니다. Coverage 하락 자체만으로 test를 추가하지 않습니다. 기존 미검증 branch를 새로 발견했지만 PR과 관련이 없으면 후속 이슈로 관리합니다. 저장·삭제·복원·검색·호환성·오류 처리 경로는 우선 처리합니다.

## 후속 테스트 이슈 선정

다음 조건 중 하나 이상이면 사용자 동작과 위험 단위로 이슈를 만듭니다.

1. 사용자에게 다른 결과를 만드는 미검증 branch
2. 저장·삭제·복원·동기화 로직
3. 검색어·지역·API 응답 정규화
4. Legacy compatibility
5. Cancellation·fallback·error 처리
6. Navigation argument 누락·복원
7. 최근 수정 빈도나 호출 범위가 큰 로직
8. Deterministic unit test를 작성할 수 있는 로직

단순 위임 use case, DTO·Entity constructor, 생성 코드, Compose 표현 코드와 수치만 높이는 test는 후속 이슈로 만들지 않습니다. 같은 사용자 동작을 class별 이슈로 중복 분리하지 않습니다.

최초 report에서 다음 공백을 우선 후보로 확인했습니다.

1. 즐겨찾기 저장·삭제·snapshot transaction과 mapper
2. 홈 지역 가이드 대표 즐겨찾기 저장·복원
3. 즐겨찾기 지도 이동 navigation argument 복원
4. 지역 option 조합과 local source 전달
5. Report에서 확인되는 mapper·normalizer의 미실행 오류 branch

후속 이슈에는 보호할 사용자 동작, 미실행 branch, 실패·경계 조건, 추가할 test, 실행 task와 참고 coverage를 기록합니다.

## 정책 변경 규칙

1. 새 business logic이 기존 allowlist에 포함되는지 PR에서 확인합니다.
2. Filter 변경은 coverage 수치 변경과 같은 수준으로 리뷰합니다.
3. 측정 대상 추가로 수치가 낮아져도 기존 위험을 숨기기 위해 제외하지 않습니다.
4. 측정 source, Kover filter나 모듈 package mapping이 의도적으로 바뀔 때만 baseline을 갱신합니다. Test 추가나 coverage 하락만으로 baseline을 바꾸지 않습니다.
5. Baseline을 갱신하면 영향받은 모듈, 이전·새 raw covered/total, 변경 이유와 기준 commit을 문서에 함께 기록합니다.
6. Coverage는 test 품질의 보조 지표입니다. 사용자 결과와 실패 경로를 보호하는 assertion을 우선합니다.
7. Baseline required check나 모듈별 strict gate는 허용 하락 폭, 적용 범위와 예외 기준을 팀에서 합의한 뒤 별도 이슈로 도입합니다.
