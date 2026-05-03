# Yeogi-Beoryeo

## 🗺️ 중소형 폐가전 수거함 지도 기능

공공데이터포털의 **기후에너지환경부_분리배출 정보조회 서비스** 중 `/getSpot` API를 활용하여  
사용자가 입력한 지역의 **중소형 폐가전 수거함** 정보를 조회하고, 네이버 지도에 마커와 리스트로 표시하는 기능입니다.

현재는 Spike 단계로, 실제 API 응답을 앱에서 사용할 수 있는지 검증하고 지도 표시까지 연결하는 것을 목표로 합니다.

---

## ✅ 현재 구현된 기능

- 공공데이터포털 `/getSpot` API 호출
- 지역명 기반 분리배출 장소 조회
- `spotNm == "중소형 수거함"` 데이터만 필터링
- `addrBase` 주소를 기반으로 위도/경도 변환
- Naver Map Compose를 활용한 지도 마커 표시
- 지도 하단에 수거함 리스트 표시
- 검색어 변경 시 API 재호출 및 지도/리스트 갱신
- `NODATA_ERROR` 발생 시 앱 오류가 아닌 “검색 결과 없음”으로 처리

---

## 📸 실행 화면 및 검증 결과

이번 Spike에서는 `/getSpot` API 기반 중소형 수거함 조회가 실제 앱 화면에서 정상적으로 동작하는지 단계별로 확인했습니다.

---

### 1. 고정 지역 조회 테스트

초기 구현 단계에서는 `용답동`을 고정 검색어로 두고 `/getSpot` API 호출, 중소형 수거함 필터링, 주소 기반 좌표 변환, 지도 마커 표시가 가능한지 먼저 검증했습니다.

| 용답동 고정 조회 |
| :---: |
| <img width="260" alt="용답동 고정 조회 화면" src="https://github.com/user-attachments/assets/2f6dd52f-c018-4ea6-93ba-0fa5b97c93e4" /> |

확인 결과:

- `용답동` 기준 중소형 수거함 2개 조회 성공
- 주소 기반 Geocoder 좌표 변환 성공
- 네이버 지도 마커 표시 성공
- 하단 리스트 표시 성공

---

### 2. 검색어 기반 조회 기능

이후 검색창을 추가하여 사용자가 지역명을 입력하면 `/getSpot` API를 다시 호출하고, 검색 결과에 따라 지도 마커와 하단 리스트가 갱신되도록 구현했습니다.

| 용답동 검색 조회 |
| :---: |
| <img width="260" alt="용답동 검색 조회 화면" src="https://github.com/user-attachments/assets/883ff7c2-794a-4dd5-8156-ddaf44658ded" /> |

확인 결과:

- 검색창에 입력한 지역명 기준으로 API 재호출 성공
- 검색 결과에 따라 지도 마커 갱신 성공
- 하단 리스트 갱신 성공

---

### 3. 결과 없음 처리 개선

`약수동` 검색 시 공공데이터 API에서 `03 / NODATA_ERROR` 응답이 발생했습니다.  
초기에는 이를 오류 메시지로 표시했으나, 이후 사용자 경험을 고려하여 “검색 결과 없음” 상태로 처리하도록 수정했습니다.

| 수정 전: NODATA_ERROR 표시 | 수정 후: 결과 없음 처리 |
| :---: | :---: |
| <img width="260" alt="약수동 NODATA_ERROR 화면" src="https://github.com/user-attachments/assets/3dc48d1f-86d9-45a7-b748-3ecebe2bd7f1" /> | <img width="260" alt="약수동 결과 없음 처리 화면" src="https://github.com/user-attachments/assets/c31215f5-31b5-48d9-9663-515c15f71de5" /> |

확인 결과:

- `NODATA_ERROR`는 앱 오류가 아니라 검색 결과 없음으로 처리
- 결과가 없을 경우 빈 리스트 반환
- 화면에는 “표시할 중소형 수거함이 없습니다.” 문구 표시

---

### 4. 검색어 매칭 테스트

`구로`, `구로동` 검색어를 사용하여 `/getSpot` API의 `addr` 검색어가 어떻게 매칭되는지 확인했습니다.

| 구로 검색 | 구로동 검색 |
| :---: | :---: |
| <img width="260" alt="구로 검색 화면" src="https://github.com/user-attachments/assets/75acf56a-3bd5-4579-a9df-f00c0f69d068" /> | <img width="260" alt="구로동 검색 화면" src="https://github.com/user-attachments/assets/97bec8f0-613c-4b83-83ff-8ae2a83fe581" /> |

확인 결과:

- `구로`와 `구로동` 모두 중소형 수거함 4개가 조회되었습니다.
- 두 검색어는 현재 테스트 기준으로 동일한 검색 결과를 반환했습니다.
- 따라서 `/getSpot`의 `addr` 검색은 입력한 문자열을 기반으로 API 내부 주소 데이터와 매칭되는 방식으로 보입니다.
- 다만 모든 구/지역명 검색이 안정적으로 동작한다고 단정하기는 어려우며, API 문서상 동/읍/면 단위 검색이 가장 안정적인 기준으로 보입니다.

---

## 🔐 API Key 설정

API Key는 Git에 올리지 않고 `local.properties`에서 관리합니다.

프로젝트 루트의 `local.properties`에 아래 값을 추가해야 합니다.

```properties
NAVER_CLIENT_ID=네이버_클라우드_플랫폼_Maps_Client_ID
PUBLIC_DATA_SERVICE_KEY=공공데이터포털_서비스키
```

### 키 설명

| Key | 용도 |
| --- | --- |
| `NAVER_CLIENT_ID` | Naver Map SDK 초기화에 사용 |
| `PUBLIC_DATA_SERVICE_KEY` | 공공데이터포털 `/getSpot` API 호출에 사용 |

### 주의 사항

- `local.properties`는 `.gitignore`에 포함되어 있어야 합니다.
- 실제 API Key는 GitHub에 커밋하지 않습니다.
- 네이버 지도 SDK에서는 Client Secret이 아니라 **Client ID / NCP Key ID**를 사용합니다.
- 공공데이터포털 서비스키는 URL 호출 시 인코딩 문제가 생길 수 있으므로 앱 내부에서 `URLEncoder.encode()` 처리 후 사용합니다.
- 현재 앱 실행에 필요한 키는 `NAVER_CLIENT_ID`, `PUBLIC_DATA_SERVICE_KEY`입니다.
- `NAVER_CLIENT_SECRET`, `NAVER_SEARCH_CLIENT_ID`, `NAVER_SEARCH_CLIENT_SECRET`은 이번 앱 런타임 기능에서는 사용하지 않습니다.

---

## ⚙️ Gradle 설정 요약

`app/build.gradle.kts`에서 `local.properties` 값을 읽어 `BuildConfig`로 전달합니다.

```kotlin
val naverId = localProperties.getProperty("NAVER_CLIENT_ID")?.trim()
    ?: throw GradleException("local.properties에 NAVER_CLIENT_ID를 추가해야 합니다.")

val publicDataKey = localProperties.getProperty("PUBLIC_DATA_SERVICE_KEY")?.trim()
    ?: throw GradleException("local.properties에 PUBLIC_DATA_SERVICE_KEY를 추가해야 합니다.")

defaultConfig {
    manifestPlaceholders["NAVER_CLIENT_ID"] = naverId
    buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverId\"")
    buildConfigField("String", "PUBLIC_DATA_SERVICE_KEY", "\"$publicDataKey\"")
}

buildFeatures {
    compose = true
    buildConfig = true
}
```

---

## 🧩 주요 파일 구조

```text
app/src/main/java/com/team/yeogibeoryeo
├── MainActivity.kt
├── PublicWasteSpotMapScreen.kt
├── WasteSpot.kt
├── WasteSpotApi.kt
└── WasteSpotGeocoder.kt
```

---

## 📄 파일별 역할

### `MainActivity.kt`

앱 시작 지점입니다.

주요 역할:

- Naver Map SDK 초기화
- `BuildConfig.NAVER_CLIENT_ID`를 사용하여 네이버 지도 인증 설정
- `PublicWasteSpotMapScreen()` 호출

```kotlin
NaverMapSdk.getInstance(this).client =
    NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_CLIENT_ID)
```

---

### `WasteSpot.kt`

중소형 수거함 정보를 앱 내부에서 사용하기 위한 데이터 모델입니다.

```kotlin
data class WasteSpot(
    val spotName: String,
    val address: String,
    val detailLocation: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
```

| 필드 | 설명 |
| --- | --- |
| `spotName` | 장소명. 예: 중소형 수거함 |
| `address` | 기본 주소. API의 `addrBase` |
| `detailLocation` | 상세 위치. API의 `addrDtl` |
| `latitude` | 지도 마커 표시용 위도 |
| `longitude` | 지도 마커 표시용 경도 |

---

### `WasteSpotApi.kt`

공공데이터포털 `/getSpot` API 호출과 응답 파싱을 담당합니다.

주요 역할:

- `addr` 기반 API 요청
- JSON 응답 파싱
- `spotNm == "중소형 수거함"` 필터링
- `NODATA_ERROR` 발생 시 빈 리스트 반환

현재 호출 구조:

```text
https://apis.data.go.kr/1482000/WasteRecyclingService/getSpot
?serviceKey=...
&pageNo=1
&numOfRows=100
&addr=검색어
&_type=json
```

필터링 기준:

```kotlin
spot.spotName == "중소형 수거함"
```

---

### `WasteSpotGeocoder.kt`

API 응답에는 위도/경도 좌표가 포함되어 있지 않기 때문에,  
`addrBase` 주소를 기반으로 Android `Geocoder`를 사용하여 좌표를 변환합니다.

주요 역할:

- 주소 문자열을 위도/경도로 변환
- 변환된 좌표를 `WasteSpot`에 추가
- 좌표 변환 실패 시 해당 항목은 지도 마커 표시 대상에서 제외

예시 변환 결과:

| 주소 | 위도 | 경도 |
| --- | --- | --- |
| 서울특별시 성동구 용답중앙23길 20 | 37.5641128 | 127.0555127 |
| 서울특별시 성동구 천호대로 394 | 37.5613909 | 127.0626029 |

---

### `PublicWasteSpotMapScreen.kt`

중소형 수거함 지도 화면입니다.

주요 역할:

- 지역명 검색 UI 제공
- 검색 버튼 클릭 시 `/getSpot` API 재호출
- 로딩 상태 표시
- 네이버 지도 표시
- 수거함 마커 표시
- 하단 리스트 표시
- 검색 결과가 없을 경우 안내 문구 표시

화면 구성:

```text
지역명 검색창 + 검색 버튼
↓
네이버 지도
↓
검색 지역 / 수거함 개수
↓
수거함 리스트
```

---

## 🔎 테스트한 검색어

| 검색어 | 결과 |
| --- | --- |
| `용답동` | 중소형 수거함 2개 조회 성공 |
| `구로` | 중소형 수거함 4개 조회 성공 |
| `구로동` | 중소형 수거함 조회 성공 |
| `신당동` | 중소형 수거함 조회 성공 |
| `약수동` | `NODATA_ERROR`, 결과 없음 처리 |

---

## 🧪 검증 결과

현재까지 검증된 흐름은 다음과 같습니다.

```text
/getSpot API 호출
→ JSON 응답 수신
→ 중소형 수거함 필터링
→ addrBase 기반 좌표 변환
→ 네이버 지도 마커 표시
→ 하단 리스트 표시
→ 검색어 변경 시 재조회
```

검증 결과:

- 공공데이터 API 호출 성공
- 중소형 수거함 데이터 필터링 성공
- 주소 기반 좌표 변환 성공
- 네이버 지도 마커 표시 성공
- 하단 리스트 표시 성공
- 검색어 기반 지역 변경 성공
- 결과 없음 처리 성공

---

## ⚠️ 현재 한계

### 1. API 응답에 위도/경도 없음

`/getSpot` API 문서에는 위치 기반 검색 파라미터가 존재하지만,  
실제 응답에서는 `latitude`, `longitude` 좌표 필드를 확인하지 못했습니다.

현재는 `addrBase`를 Android Geocoder로 변환하여 지도 마커를 표시합니다.

---

### 2. 위치 기반 검색은 아직 안정적으로 사용하지 못함

문서상 `latitude`, `longitude`, `radius` 파라미터가 존재하지만,  
실제 테스트에서는 원하는 결과를 정상적으로 얻지 못했습니다.

현재 구현에서는 위치 기반 검색 대신 `addr` 검색어 기반 조회를 우선 사용합니다.

---

### 3. 검색어 기준이 API 내부 주소 데이터에 의존함

`addr` 검색은 동 단위가 가장 안정적으로 보입니다.

다만 `구로`처럼 일부 구/지역명 검색도 가능하지만,  
`약수동`처럼 행정동/법정동 또는 API 내부 주소 기준 차이로 결과가 없을 수 있습니다.

---

### 4. Geocoder 결과는 환경에 따라 달라질 수 있음

Android Geocoder는 기기 환경이나 네트워크 상태에 따라 결과가 달라질 수 있습니다.

추후 정식 구현 시 다음 방식을 검토할 수 있습니다.

- 네이버 Geocoding API 사용
- 주소-좌표 변환 결과 캐싱
- 서버 측 좌표 변환
- Room DB에 좌표 저장

---

### 5. 현재는 `pageNo=1`, `numOfRows=100` 기준 조회

현재 Spike 구현에서는 첫 페이지 기준으로 최대 100개를 조회합니다.

검색 결과가 100개를 초과하는 지역의 경우, 추후 `totalCount`를 기반으로 페이지를 반복 호출하는 구조가 필요할 수 있습니다.

---

## 🚀 추후 작업

- 현재 위치 권한 처리
- 현재 위치 기반 주변 검색 재검증
- 지도 카메라 이동 후 “이 위치에서 검색” 기능
- 검색어 자동완성 또는 최근 검색어 저장
- 마커 클릭 시 하단 리스트 항목 강조
- 리스트 항목 클릭 시 해당 마커 위치로 카메라 이동
- 주소 기반 Geocoding 결과 캐싱
- Room DB 기반 장소 북마크 저장
- 팀 공통 Repository / DB 구조와 연동
- API 응답 페이지네이션 처리

---

## 📝 관련 작업

- 이전 Spike: 로컬 JSON 기반 네이버 지도 마커 표시 검증
- 현재 Spike: 공공데이터 `/getSpot` API 기반 중소형 수거함 지도 표시 검증
- 이전 Spike: 로컬 JSON 기반 네이버 지도 마커 표시 검증
- 현재 Spike: 공공데이터 `/getSpot` API 기반 중소형 수거함 지도 표시 검증
