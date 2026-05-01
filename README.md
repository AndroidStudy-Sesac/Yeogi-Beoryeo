# Yeogi-Beoryeo
내 위치 기반 맞춤형 분리수거 가이드 &amp; 소형가전 수거함 안내 서비스

# 📦 쓰레기 배출 안내 앱 데이터/기능 설계 분석 문서

> 공공데이터 연동을 위한 API 구조 분석, 데이터 흐름(Data Flow) 시나리오, 개발 시 주의사항 및 앱의 핵심 기능 명세를 정리한 문서입니다.

## 📡 1. API 구조 및 도메인 분석

앱에서 사용할 3가지 핵심 공공 API의 요청(Request) 및 응답(Response) 도메인 구조입니다.

### 1.1 생활쓰레기 배출정보 조회 (행정안전부)

- **역할:** 지자체별 쓰레기 배출 요일, 시간, 방법 등 '스케줄 및 규칙' 데이터 제공.

**[요청(Request) 도메인]**

| **항목명** | **타입** | **필수여부** | **도메인 설명 및 예시** |
| --- | --- | --- | --- |
| `serviceKey` | String | 필수 | 공공데이터포털에서 받은 인증키 |
| `pageNo` | Integer | 필수 | 페이지번호 |
| `numOfRows` | Integer | 필수 | 한 페이지 결과 수(max: 100) |
| `returnType` | String | 선택 | `json` 또는 `xml`(json 권장) |
| `cond[SGG_NM::LIKE]` | String | 선택 | 시군구명 을(를) 포함하는 값 (예: `“영등포구"`)- 핵심 호출 키 |
| `cond[OPN_ATMY_GRP_CD::EQ]` | String | 선택 | 개방자치단체코드 |
| `cond[DAT_UPDT_PNT::GTE / LT]` | String | 선택 | 데이터 갱신 시점 이상/미만(YYYYMMDDHHMMSS) |
| `cond[DAT_CRTR_YMD::GTE / LT]` | String | 선택 | 데이터 기준일자 이상/미만(YYYYMMDD) |

**[핵심 응답(Response) 도메인 분석]**

이 API의 응답 데이터는 약 30개의 필드로 구성되어 있으나, 앱 화면(UI) 구성에 필요한 **핵심 필드는 '공통 정보'와 '쓰레기 종류별 규칙(패턴)'으로 요약**할 수 있습니다.

- **공통 항목:** `CTPV_NM`(시도명), `SGG_NM`(시군구명), `MNG_ZONE_TRGT_RGN_NM`(행정동/관리구역), `EMSN_PLC_TYPE`(배출장소 유형), `UNCLLT_DAY`(수거 제외일)

- **패턴화 필드 (쓰레기 종류별 Prefix 적용):**
    - **Prefix 분류:**
        - `LF_WST`(일반)
        - `FOD_WST`(음식물)
        - `RCYCL`(재활용)
        - `TMPRY_BULK_WASTE`(대형)
    - **필드 패턴:**
        - `{Prefix}_EMSN_DOW`(배출 요일)
        - `{Prefix}_EMSN_BGNG_TM`(시작 시간)
        - `{Prefix}_EMSN_END_TM`(종료 시간)
        - `{Prefix}_EMSN_MTHD`(배출 방법)
    - (예시: `FOD_WST_EMSN_DOW` = 음식물 쓰레기 배출 요일)


<details>
<summary><b>전체 응답 필드 상세 명세 (테이블 보기)</b></summary>

| **항목명** | **타입** | **도메인 설명 및 예시** |
| --- | --- | --- |
| `CTPV_NM` | String | 시도명 (예: `"경상남도"`) |
| `DAT_CRTR_YMD` | String (Date) | 데이터 기준일 (예: `"2024-01-26"`) |
| `DAT_UPDT_PNT` | String (Datetime) | 데이터 수정일시 (예: `"2026-03-11 01:24:41"`) |
| `DAT_UPDT_SE` | String | 데이터 변경 구분 (예: `"I"`) |
| `EMSN_PLC` | String | 배출 장소 상세 (예: `"집앞"`) |
| `EMSN_PLC_TYPE` | String | 배출장소 유형 (예: `"문전수거"`) |
| `FOD_WST_EMSN_BGNG_TM` | String | 음식물 쓰레기 배출 시작 시간 (예: `"20:00"`) |
| `FOD_WST_EMSN_DOW` | String | 음식물 쓰레기 배출 요일 (예: `"수"`) |
| `FOD_WST_EMSN_END_TM` | String | 음식물 쓰레기 배출 종료 시간 (예: `"05:00"`) |
| `FOD_WST_EMSN_MTHD` | String | 음식물 쓰레기 배출 방법 (예: `"전용봉투 배출"`) |
| `LAST_MDFCN_PNT` | String (Datetime) | 최종 수정 시점 (예: `"2024-01-28 11:19:27"`) |
| `LF_WST_EMSN_BGNG_TM` | String | 일반 쓰레기 배출 시작 시간 (예: `"20:00"`) |
| `LF_WST_EMSN_DOW` | String | 일반 쓰레기 배출 요일 (예: `"수"`) |
| `LF_WST_EMSN_END_TM` | String | 일반 쓰레기 배출 종료 시간 (예: `"05:00"`) |
| `LF_WST_EMSN_MTHD` | String | 일반 쓰레기 배출 방법 (예: `"규격봉투 배출"`) |
| `MNG_DEPT_NM` | String | 담당 부서명 (예: `"환경과"`) |
| `MNG_DEPT_TELNO` | String | 담당 부서 연락처 (예: `"055-940-3503"`) |
| `MNG_NO` | String | 관리 번호 (예: `"202054700000400006"`) |
| `MNG_ZONE_NM` | String | 관리구역명 (예: `"거창군"`) |
| `MNG_ZONE_TRGT_RGN_NM` | String | 관리 대상 지역 (`+`로 복수 구분) (예: `"남상면+신원면"`) |
| `OPN_ATMY_GRP_CD` | String | 개방자치단체 코드 (예: `"5470000"`) |
| `RCYCL_EMSN_BGNG_TM` | String | 재활용 배출 시작 시간 (예: `"20:00"`) |
| `RCYCL_EMSN_DOW` | String | 재활용 배출 요일 (예: `"수"`) |
| `RCYCL_EMSN_END_TM` | String | 재활용 배출 종료 시간 (예: `"05:00"`) |
| `RCYCL_EMSN_MTHD` | String | 재활용 배출 방법 (예: `"투명 비닐봉투 배출"`) |
| `SGG_NM` | String | 시군구명 (예: `"거창군"`) |
| `TMPRY_BULK_WASTE_EMSN_BGNG_TM` | String | 대형 폐기물 배출 시작 시간 (예: `"09:00"`) |
| `TMPRY_BULK_WASTE_EMSN_END_TM` | String | 대형 폐기물 배출 종료 시간 (예: `"17:00"`) |
| `TMPRY_BULK_WASTE_EMSN_MTHD` | String | 대형 폐기물 배출 방법 (예: `"신고 후 지정 장소 배출"`) |
| `TMPRY_BULK_WASTE_EMSN_PLC` | String | 대형 폐기물 배출 장소 (예: `"거창군매립장"`) |
| `UNCLLT_DAY` | String | 수거 제외일 (`+`로 복수 구분) (예: `"명절(설 및 추석)+임시공휴일"`) |

</details>

<details>
<summary><b>실제 응답 데이터(json)</b></summary>

```json
{
  "response": {
    "body": {
      "dataType": "JSON",
      "items": {
        "item": [
          {
            "CTPV_NM": "서울특별시",
            "DAT_CRTR_YMD": "2022-12-31",
            "DAT_UPDT_PNT": "2026-04-28 22:32:12",
            "DAT_UPDT_SE": "I",
            "EMSN_PLC": "집앞",
            "EMSN_PLC_TYPE": "문전수거",
            "FOD_WST_EMSN_BGNG_TM": "20:00",
            "FOD_WST_EMSN_DOW": "일+월+화+수+목+금",
            "FOD_WST_EMSN_END_TM": "00:00",
            "FOD_WST_EMSN_MTHD": "물기를 제거 후 음식물 전용봉투에 담아 음식물거점전용용기에 배출",
            "LAST_MDFCN_PNT": "2022-12-01 10:45:46",
            "LF_WST_EMSN_BGNG_TM": "20:00",
            "LF_WST_EMSN_DOW": "일+월+화+수+목+금",
            "LF_WST_EMSN_END_TM": "00:00",
            "LF_WST_EMSN_MTHD": "종량제 봉투를 흩날리지 않도록 묶어 내집 내점포 앞 배출",
            "MNG_DEPT_NM": "청소과",
            "MNG_DEPT_TELNO": "26703491",
            "MNG_NO": "202031800000400001",
            "MNG_ZONE_NM": "영등포구(전역)",
            "MNG_ZONE_TRGT_RGN_NM": "영등포구",
            "OPN_ATMY_GRP_CD": "3180000",
            "RCYCL_EMSN_BGNG_TM": "20:00",
            "RCYCL_EMSN_DOW": "일+월+화+수+목+금",
            "RCYCL_EMSN_END_TM": "00:00",
            "RCYCL_EMSN_MTHD": "투명한 비닐봉투에 담아 흩날리지 않도록 묶어서 배출 내집 내점포 앞 또는 클린하우스/재활용정거장 배출",
            "SGG_NM": "영등포구",
            "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "",
            "TMPRY_BULK_WASTE_EMSN_END_TM": "",
            "TMPRY_BULK_WASTE_EMSN_MTHD": "배출자가 성상별로 수거업체와 별도 계약 후 배출및 처리",
            "TMPRY_BULK_WASTE_EMSN_PLC": "집앞",
            "UNCLLT_DAY": "명절+임시공휴일+토요일"
          }
        ]
      },
      "numOfRows": 3,
      "pageNo": 1,
      "totalCount": 1
    },
    "header": {
      "resultCode": "0",
      "resultMsg": "정상"
    }
  }
}
```

</details>



### 1.2 분리배출 정보조회 (장소) – 기후에너지환경부

- **역할:** 종량제봉투 판매소, 의류/폐형광등 수거함 등 물리적 인프라 위치 정보 제공.

**[요청(Request) 도메인]**

| **항목명** | **데이터 타입** | **필수여부** | **도메인 설명 및 예시** |
| --- | --- | --- | --- |
| `serviceKey` | String | 필수 | 공공데이터포털에서 받은 인증키 |
| `pageNo` | Integer | 필수 | 페이지번호 |
| `numOfRows` | Integer | 필수 | 한 페이지 결과 수 |
| `addr` | String | 선택 | 주소 검색어 (동/읍/면 단위) (예: `“문래동"`)- 핵심 호출 키 |
| `latitude / longitude` | Float/Decimal | 선택 | 위도/경도 (예: `37.518...`, `126.895...`) |
| `radius` | Integer | 선택 | 검색 반경(m) (예: `500`) |

**[주요 응답(Response) 도메인]**

| **항목명** | **타입** | 설명 |
| --- | --- | --- |
| `spotNm` | String | 장소명 / 수거함 유형(예: `"종량제봉투 판매소"`) |
| `addrBase` | String | 기본 주소(예: `"서울특별시 영등포구 당산로 42"`) |
| `addrDtl` | String | 상세 위치(예: `"홈플러스 영등포점"`) |

<details>
<summary><b>실제 응답 데이터(json)</b></summary>

```json
{
  "response": {
    "header": {
      "resultCode": "00",
      "resultMsg": "NORMAL SERVICE."
    },
    "body": {
      "items": {
        "item": [
          {
            "spotNm": "종량제봉투 판매소",
            "addrBase": "서울특별시 영등포구 당산로 42",
            "addrDtl": "홈플러스 영등포점"
          },
          {
            "spotNm": "폐휴대폰 배출처",
            "addrBase": "서울특별시 영등포구 당산로 42",
            "addrDtl": "지하 1층"
          }
        ]
      },
      "pageNo": 1,
      "numOfRows": 2,
      "totalCount": 19
    }
  }
}
```

</details>

### 1.3 분리배출 정보조회 (품목) – 기후에너지환경부

- **역할:** 특정 쓰레기 품목에 대한 배출 방법 검색 제공.

**[요청(Request) 도메인]**

| **파라미터명** | **데이터 타입** | **필수여부** | **도메인 설명 및 예시**              |
| --- | --- | --- |------------------------------|
| `serviceKey` | String | 필수 | 공공데이터포털에서 받은 인증키             |
| `pageNo` | Integer | 필수 | 페이지번호                        |
| `numOfRows` | Integer | 필수 | 한 페이지 결과 수                   |
| `itemNm` | String | 필수 | 품목명 검색어 (예: `“조명"`, `“우유팩"`) |

**[주요 응답(Response) 도메인]**

| 항목 | **타입** | 설명 |
| --- | --- | --- |
| `itemNm` | String | 상세 품목명 |
| `dschgMthd` | String | 배출 방법 (예: `"재활용폐기물, 대형폐기물"`) |

<details>
<summary><b>실제 응답 데이터(json)</b></summary>

```json
{
  "response": {
    "header": {
      "resultCode": "00",
      "resultMsg": "NORMAL SERVICE."
    },
    "body": {
      "items": {
        "item": [
          {
            "itemNm": "조명제품",
            "dschgMthd": "전용수거함"
          },
          {
            "itemNm": "조명 유리커버",
            "dschgMthd": "특수규격봉투, 대형폐기물"
          },
          {
            "itemNm": "스탠드 조명",
            "dschgMthd": "재활용폐기물, 대형폐기물"
          }
        ]
      },
      "pageNo": 1,
      "numOfRows": 10,
      "totalCount": 3
    }
  }
}
```

</details>

## 🗺️2. 데이터 흐름 및 병렬 호출 시나리오

사용자의 진입 경로에 따라 최적의 성능을 내기 위해 두 가지 데이터 흐름을 가집니다.

### 2.1 [Path A] 키워드 기반 검색 (순차 호출 - Sequential)

사용자가 직접 '동' 이름을 입력했을 때 적용되는 흐름입니다. 주소 정보가 불분명하므로 첫 번째 API 결과에서 '구' 이름을 추출하는 과정이 추가됩니다.

```
[키워드 입력 (예: "문래동")]
    ↓
[Step 1. 환경부 API 호출 (addr="문래동")] ➡️ 수거함 위치 리스트 획득
    ↓
[Step 2. 주소 문자열 파싱] ➡️ 첫 번째 항목의 주소에서 "영등포구"(시군구) 추출
    ↓
[Step 3. 행안부 API 호출 (sggName="영등포구")] ➡️ 배출 규칙 데이터 획득
    ↓
[Step 4. UI Rendering] ➡️ 수거함 위치 + 해당 구 전체 스케줄 통합 출력
```

### 2.2 [Path B] 위치 기반 검색 (병렬 호출 - Parallel)

GPS 권한이 있고 좌표를 통해 행정구역을 미리 확정할 수 있을 때의 흐름입니다.

```
[GPS 획득]
    ↓
[Step 1. Reverse Geocoding] ➡️ "영등포구"(시군구), "문래동"(동) 추출
    ↓
    ├──▶ [Step 2. 행안부 API 호출 ("영등포구")] ──(async)──▶ Room DB 스케줄 캐싱
    │                                                            ↓
    ├──▶ [Step 3. 환경부 API 호출 ("문래동")] ───(async)──▶ Room DB 인프라 캐싱
    ↓                                                            ↓
[Step 4. UI Rendering] ◀── (두 API 응답 완료 후 StateFlow 업데이트) ───────┘
```

- **Step 1. Reverse Geocoding:** `FusedLocationProviderClient`로 얻은 좌표를 지도 API에 전달하여 호출에 필요한 행정구역 키(`"영등포구"`, `"문래동"`)를 추출합니다.
- **Step 2 & 3. 데이터 병렬 Fetch (Coroutines 적용):**
    - 두 API는 서로 의존성이 없으므로, 코루틴의 `async`를 활용해 **직렬 호출 시 발생하는 대기 시간을 절반으로 줄여 초기 렌더링 속도를 극대화**합니다.
    - **스케줄:** `시군구` 단위로 호출 후 전체 데이터를 Room DB에 Insert 합니다.
        - **동일 구 내에서 다른 동으로 이동할 경우, 추가적인 네트워크 API 호출 없이 로컬 DB에서 즉각적으로 스케줄 정보를 꺼내와 UI를 렌더링**하기 위함입니다.
    - **인프라:** `동` 단위로 호출 후 위경도 리스트를 Room DB에 Insert 합니다.

## ⚖️ 3. 아키텍처 설계 주요 포인트

설계 과정에서 성능과 사용성 간의 트레이드오프(Trade-off)가 발생하는 주요 지점입니다.

**배출 스케줄 데이터의 로컬 DB 캐싱 범위 설정 (SGG-Level vs Dong-Level)**
현재 행안부 스케줄 API는 '시군구(예: 영등포구)' 단위로 호출하면 산하의 전체 동 데이터가 한 번에 내려옵니다. 이 데이터를 내부 DB에 어떻게 캐싱할지에 대한 2가지 방안입니다.

**Option A: '시군구' 단위 통째로 캐싱 (SGG-Level Caching)**

- **방식:** API 응답으로 온 영등포구의 20개 동 데이터를 모두 Room DB에 Insert.
- **장점 (Pros):** 사용자가 영등포구 내에서 다른 동(예: 문래동 → 당산동)으로 지도 패닝 시, 추가적인 네트워크 호출(API 통신) 없이 오프라인 상태에서도 0초 만에 스케줄 UI를 렌더링할 수 있어 UX가 극대화됩니다.
- **단점 (Cons):** 당장 필요 없는 다른 동네의 데이터까지 저장해야 하므로, 앱 초기 로딩 시 약간의 DB 쓰기(Write) 오버헤드가 발생합니다.

**Option B: 현재 위치한 '동' 단위만 필터링하여 캐싱 (Strict Filtering)**

- **방식:** API 응답 중 사용자가 현재 위치한 '문래동' 데이터 딱 1줄만 DB에 저장.
- **장점 (Pros):** DB 저장 공간을 가장 적게 차지하고, 불필요한 DB 쓰기 작업을 최소화합니다.
- **단점 (Cons):** 사용자가 바로 옆 동네인 '당산동'으로 이동하면, 이미 받았던 동일한 API("영등포구" 검색)를 다시 호출하여 당산동 데이터를 또 필터링해야 하므로 네트워크 트래픽이 낭비되고 로딩 딜레이가 발생합니다.

>
>
>
> 💡**개인 의견**
>
> 데이터 용량이 수십 KB 수준으로 작고, 모바일 환경에서 네트워크 지연 가능성이 존재하기 때문에, 초기 DB 쓰기 비용을 감수하더라도 끊김 없는 사용자 경험을 제공할 수 있는 **Option A 방식이 더 적합할 것으로 생각합니다.**
> 다만 초기 로딩 시 DB 쓰기 비용은 추가적으로 고려가 필요할 것 같습니다.
>

**통합 검색 엔진 구조 (Entrance is different, Exit is same)**

- `ViewModel` 내에서 `searchByKeyword`와 `searchByLocation`은 시작점(Input)은 다르지만, 최종적으로 `SpikeUiState.Success`라는 동일한 UI 상태를 공유합니다.
- **UI 일관성:** 어떤 경로로 들어오든 사용자는 동일한 카드 레이아웃(`ScheduleList`, `LocationList`)을 통해 일관된 정보를 제공받습니다.

## 🚨 4. 공공 API 연동 시 주의사항 (Troubleshooting Guide)

앱의 안정성을 위해 개발 단계에서 반드시 예외 처리(Exception Handling)가 필요한 항목들입니다.

**① 법정동 vs 행정동 불일치 이슈 (중요)**
지도 API의 리버스 지오코딩 결과(법정동)와 환경부 API에 입력해야 하는 주소 체계(행정동)가 다를 수 있습니다. (예: 종로1.2.3.4가동 vs 종로1가). API 호출 결과가 0건일 경우, 상위 행정구역(구 단위)으로 재검색하는 **Fallback 로직** 구현이 필수입니다.

**② 비정형 데이터와 NullPointerException 방지**

- **시간 데이터(`00:00`):** 데이터가 없는 빈 값(`""`)일 경우 UI에서 "시간 정보 없음"으로 노출하거나 영역을 숨김 처리. 값이 있을 경우 추가 파싱 없이 그대로 노출 가능하므로 렌더링 효율성 높습니다.
- **Null 값 다수 존재:** 대형 폐기물 배출 시간 등 특정 필드는 빈 값(Null)이 잦습니다. Data Class 정의 시 반드시 `String?` (Nullable)로 선언해야 크래시를 막을 수 있습니다.

**③ 세종특별자치시 예외 처리**
행정 체계상 세종시는 하위 '구'가 존재하지 않습니다. 리버스 지오코딩 시 시군구명이 비어있으므로, 시도명이 `"세종특별자치시"`인 경우 시군구 파라미터 조회를 생략하고 세종시 전체 데이터를 호출하도록 분기해야 합니다.

**④ 네트워크 지연 및 Offline-First 전략**
공공데이터포털 API는 응답 지연이 발생할 수 있습니다. Retrofit Timeout을 넉넉히(10~15초) 설정하고, 네트워크 호출 실패 시 Room DB에 저장된 기존 캐시 데이터를 우선 보여주는 구조가 필요합니다.