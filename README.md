# 🗑️ 여기버려 - API 테스트

본 프로젝트는 공공데이터포털의 API를 활용하여 **품목별 분리배출 방법**, **주변 수거 장소**, **지역별 배출 요일 및 시간** 정보를 통합 제공하기 위한 기술 검증
프로젝트입니다.

## 🏗️ 아키텍처 구조 (SSoT & MVVM)

각각의 API가 잘 불러와지는지를 확인하고, 화면에 정보가 어떻게 보이지를 확인하기 위해 간단하게 아키텍처를 구성하였습니다.

1. **UI (Compose)**: `MainActivity.kt` - ViewModel의 상태를 관찰하여 화면을 렌더링합니다.
2. **ViewModel**: `WasteViewModel.kt` - 모든 입력 상태와 API 응답 데이터를 관리하며 로직을 처리합니다.
3. **Repository**: `WasteRepository.kt` - UI 로직과 데이터 소스를 분리하여 API 호출을 캡슐화합니다.
4. **Remote Data**: `WasteService.kt` - Retrofit 인터페이스를 통한 공공데이터 API 정의.

---

## 📡 활용 API 정보 및 전체 파라미터 상세

### 1. 기후에너지환경부 - 분리배출 정보조회 서비스 

이 API는 품목별 가이드와 근처 수거함 위치 정보를 제공합니다.
https://www.data.go.kr/data/15156866/openapi.do#/

#### [GET] /getItem (품목별 배출 방법)

| 파라미터명        | 필수 여부  |   타입   | 설명                    |
|:-------------|:------:|:------:|:----------------------|
| `serviceKey` | **필수** | String | 공공데이터포털에서 발급받은 인증키    |
| `pageNo`     | **필수** | String | 페이지 번호                |
| `numOfRows`  | **필수** | String | 한 페이지 결과 수            |
| `itemNm`     | **필수** | String | 품목명 검색어 (예: 생수병, 우유팩) |

#### [GET] /getSpot (분리배출 장소 정보)

| 파라미터명        | 필수 여부  |   타입    | 설명                                       |
|:-------------|:------:|:-------:|:-----------------------------------------|
| `serviceKey` | **필수** | String  | 공공데이터포털에서 발급받은 인증키                       |
| `pageNo`     | **필수** | String  | 페이지 번호                                   |
| `numOfRows`  | **필수** | String  | 한 페이지 결과 수                               |
| `addr`       | **필수** | String  | 주소 검색어 (동/읍/면 단위) (예: 문래동, 구좌읍)          |
| `latitude`   |   선택   | String  | 현재 위치 위도 (Y좌표). 좌표 검색 시 longitude와 함께 필수 |
| `longitude`  |   선택   | String  | 현재 위치 경도 (X좌표). 좌표 검색 시 latitude와 함께 필수  |
| `radius`     |   선택   | Integer | 검색 반경 (미터 단위). 좌표 검색 시 유효하며 기본값 500m     |

**💡 선택 파라미터 응용 및 실제 사용:**

* **좌표 기반 검색 (`latitude`, `longitude`, `radius`):** 단순 텍스트 주소 검색(`addr`)의 한계를 보완하여, 사용자의 현재 위치를 기반으로
  반경 내의 수거 장소를 정밀하게 찾을 때 활용합니다.
* **실제 코드 적용:** `WasteViewModel`의 `latitudeText`, `longitudeText`, `radiusValue` 상태를 통해 사용자가 직접
  입력하거나 슬라이더로 조절할 수 있도록 구현되어 있습니다.

---

### 2. 행정안전부 - 생활쓰레기 배출정보 조회서비스

이 API는 각 지자체별 배출 요일, 시간, 구체적인 배출 방법을 제공합니다.

#### [GET] /info (배출 정보 데이터 조회)

| API 파라미터명                   | 내부 변수명               |   필수   |   타입   | 설명                               |
|:----------------------------|:---------------------|:------:|:------:|:---------------------------------|
| `serviceKey`                | `serviceKey`         | **필수** | String | 공공데이터포털에서 발급받은 인증키               |
| `pageNo`                    | `pageNo`             | **필수** |  Int   | 페이지 번호                           |
| `numOfRows`                 | `numOfRows`          | **필수** |  Int   | 한 페이지 결과 수 (최대 100)              |
| `returnType`                | `returnType`         |   선택   | String | 응답 데이터 타입 (JSON/XML)             |
| `cond[SGG_NM::LIKE]`        | **`sggName`**        |   선택   | String | **시군구명(SGG)**을 포함하는 검색어 (예: 노원구) |
| `cond[OPN_ATMY_GRP_CD::EQ]` | `administrativeCode` |   선택   | String | 개방자치단체코드 (행정 코드)                 |
| `cond[DAT_UPDT_PNT::GTE]`   | `updatedFrom`        |   선택   | String | 데이터 갱신 시점 **이후**(YYYYMMDDHHMMSS) |
| `cond[DAT_UPDT_PNT::LT]`    | `updatedUntil`       |   선택   | String | 데이터 갱신 시점 **이전**(YYYYMMDDHHMMSS) |
| `cond[DAT_CRTR_YMD::GTE]`   | `baseDateFrom`       |   선택   | String | 데이터 기준일자 **이후**(YYYYMMDD)        |
| `cond[DAT_CRTR_YMD::LT]`    | `baseDateUntil`      |   선택   | String | 데이터 기준일자 **이전**(YYYYMMDD)        |

**💡 선택 파라미터 응용 및 실제 사용:**

* **지역 검색 (`sggName`):** 'SGG'는 **시군구(Si-Gun-Gu)** 의 약어로, 지자체 단위 검색의 핵심 키입니다. 사용자가 입력한 지역명을 기반으로 해당
  시군구의 전체 데이터를 필터링하는 데 사용합니다.
* **시간 기반 필터링 (`updatedFrom`, `updatedUntil`, `baseDateFrom`, `baseDateUntil`):** 특정 시점 이후에 업데이트된 최신
  정보만 가져오거나, 과거의 기준 데이터를 조회할 때 사용하여 데이터의 신뢰성을 확보합니다.
* **특정 단체 필터링 (`administrativeCode`):** 전국 데이터 중 특정 자치단체의 코드값을 알고 있을 때, 텍스트 검색보다 정확하게 타겟팅할 수 있습니다.
* **실제 코드 적용:** `WasteViewModel`의 `updatedFrom`, `updatedUntil`, `baseDateFrom`, `baseDateUntil`,
  `administrativeCode` 필드를 통해 "상세 설정" UI에서 입력받아 API 요청 시 `Query` 파라미터로 전달합니다.

---

## 📊 기후에너지환경부 API 상세 응답 모델 (품목별 배출 방법)

`/getItem` API 응답의 `body` 내 `item` 객체 필드 정의입니다. 아래 예시는 **`itemNm = 유리`** 파라미터를 사용하여 조회된 실제 데이터를 기반으로
작성되었습니다.

### [조회 파라미터 예시 (유리 검색)]

| 파라미터명        | 입력값       | 설명         |
|:-------------|:----------|:-----------|
| `serviceKey` | `API_KEY` | 발급받은 인증키   |
| `pageNo`     | `1`       | 페이지 번호     |
| `numOfRows`  | `1000`    | 한 페이지 결과 수 |
| `itemNm`     | `유리`      | 품목명 검색어    |

### [필드 상세 정의]

| 필드명 (JSON Key) | 한글 항목명 | 데이터 예시           |
|:---------------|:-------|:-----------------|
| `itemNm`       | 품목명    | `유리병`            |
| `dschgMthd`    | 배출방법   | `재활용폐기물, 보증금 환급` |

#### [참고] 실제 응답 JSON (JSON Response Example)

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
            "itemNm": "유리병",
            "dschgMthd": "재활용폐기물, 보증금 환급"
          },
          {
            "itemNm": "유리컵",
            "dschgMthd": "특수규격봉투"
          }
        ]
      },
      "pageNo": 1,
      "numOfRows": 1000,
      "totalCount": 6
    }
  }
}
```

---

## 📊 기후에너지환경부 API 상세 응답 모델 (장소 정보)

`/getSpot` API 응답의 `body` 내 `item` 객체 필드 정의입니다. 아래 예시는 **`addr = 인사동`** 파라미터를 사용하여 조회된 실제 데이터를 기반으로
작성되었습니다.

### [조회 파라미터 예시 (인사동 검색)]

| 파라미터명        | 입력값       | 설명                |
|:-------------|:----------|:------------------|
| `serviceKey` | `API_KEY` | 발급받은 인증키          |
| `pageNo`     | `1`       | 페이지 번호            |
| `numOfRows`  | `10`      | 한 페이지 결과 수        |
| `addr`       | `인사동`     | 주소 검색어 (동/읍/면 단위) |

### [필드 상세 정의]

| 필드명 (JSON Key) | 한글 항목명 | 데이터 예시               |
|:---------------|:-------|:---------------------|
| `spotNm`       | 장소명    | `폐건전지 수거함`           |
| `addrBase`     | 기본 주소  | `서울특별시 종로구 인사동길 7-1` |
| `addrDtl`      | 상세 주소  | `승동교회 (또는 구체적 위치)`   |

#### [참고] 실제 응답 JSON (JSON Response Example)

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
            "spotNm": "폐휴대폰 배출처",
            "addrBase": "경상남도 진주시 진양호로 483",
            "addrDtl": "매장 내"
          },
          {
            "spotNm": "폐건전지 수거함",
            "addrBase": "서울특별시 종로구 인사동길 7-1",
            "addrDtl": "승동교회"
          }
        ]
      },
      "pageNo": 1,
      "numOfRows": 10,
      "totalCount": 36
    }
  }
}
```

---

## 📊 행정안전부 API 상세 응답 모델 (Full Mapping)

API 응답의 `body` 내 `item` 객체에 포함될 수 있는 모든 필드 정의 및 예시입니다. 아래 예시는 **`cond[SGG_NM::LIKE] = 종로구`** 파라미터를
사용하여 조회된 실제 데이터를 기반으로 작성되었습니다.
https://www.data.go.kr/data/15155080/openapi.do#/

### [조회 파라미터 예시 (종로구 검색)]

| 파라미터명                | 입력값       | 설명                   |
|:---------------------|:----------|:---------------------|
| `serviceKey`         | `API_KEY` | 발급받은 인증키             |
| `pageNo`             | `1`       | 페이지 번호               |
| `numOfRows`          | `10`      | 한 페이지 결과 수           |
| `cond[SGG_NM::LIKE]` | `종로구`     | 시군구명 포함 검색 (핵심 파라미터) |
| `returnType`         | `JSON`    | 응답 데이터 형식            |

### [필드 상세 정의]

| 필드명 (JSON Key)                  | 한글 항목명       | 데이터 예시 (종로구)                       | 데이터 예시 (노원구)                                     | 데이터 예시 (안성시)          | 데이터 예시 ("없음"으로 검색, 세종시) |
|:--------------------------------|:-------------|:-----------------------------------|:-------------------------------------------------|:----------------------|:------------------------|
| `OPN_ATMY_GRP_CD`               | 개방자치단체코드     | `3000000`                          | `3100000`                                        | `4080000`             | `5690000`               |
| `MNG_NO`                        | 관리번호         | `202030000000400002`               | `202431000000400001`                             | `202540800000400542`  | `202156900000400003`    |
| `CTPV_NM`                       | 시도명          | `서울특별시`                            | `서울특별시`                                          | `경기도`                 | `세종특별자치시`               |
| `SGG_NM`                        | 시군구명         | `종로구`                              | `노원구`                                            | `안성시`                 | **`없음`**                |
| `MNG_ZONE_NM`                   | 관리구역명        | `없음`                               | `6권역`                                            | `대덕면`                 | `세종특별자치시(3권역A)`         |
| `MNG_ZONE_TRGT_RGN_NM`          | 관리구역대상지역명    | `없음`                               | `공릉2동, 중계본동, 하계1동`                               | `삼한리 삼암`              | `조치원읍(상리, 평리, 교리 등)`    |
| `EMSN_PLC_TYPE`                 | 배출장소유형       | `문전수거`                             | `거점수거`                                           | `거점수거`                | `문전수거`                  |
| `EMSN_PLC`                      | 배출장소         | `집앞`                               | `내집+내점포건물앞+생활폐기물보관대`                             | `지정된 장소`              | `집앞+상가 앞`               |
| `LF_WST_EMSN_MTHD`              | 생활쓰레기배출방법    | `규격봉투에 넣어 지정된 요일에 배출`              | `규격봉투에 넣어 지정된 요일에 배출`                            | `규격봉투`                | `규격봉투에 넣어 지정된 요일에 배출`   |
| `FOD_WST_EMSN_MTHD`             | 음식물쓰레기배출방법   | `물기를 최대한 줄여 음식물만 전용봉투에 넣어 배출`      | `용기 용량에 맞는 납부필증 부착 또는 RFID 종량기기에 세대별 카드 사용하여 배출` | `규격봉투`                | `음식물류납부필증 부착`           |
| `RCYCL_EMSN_MTHD`               | 재활용품배출방법     | `투명한 비닐봉투에 담거나 끈으로 묶어서 지정된 요일에 배출` | `투명한 비닐봉투에 담거나 끈으로 묶어서 지정된 요일에 배출`               | `일반봉투 등`              | `투명한 비닐봉투에 담기`          |
| `TMPRY_BULK_WASTE_EMSN_MTHD`    | 일시적다량폐기물배출방법 | `주민센터방문 혹은 종로구홈페이지에 신청 후 배출`       | `별도안내`                                           | `스티커 등`               | `규격봉투 배출`               |
| `TMPRY_BULK_WASTE_EMSN_PLC`     | 일시적다량폐기물배출장소 | `집앞`                               | `별도안내`                                           | `신고후 집앞`              | `집앞+상가 앞`               |
| `LF_WST_EMSN_DOW`               | 생활쓰레기배출요일    | `일+월+화+수+목+금`                      | `일+월+화+수+목+금`                                    | `목`                   | `일+월+화+목+금`             |
| `FOD_WST_EMSN_DOW`              | 음식물쓰레기배출요일   | `일+월+화+수+목+금`                      | `일+월+화+수+목+금`                                    | `목`                   | `일+월+화+목+금`             |
| `RCYCL_EMSN_DOW`                | 재활용품배출요일     | `일+월+화+수+목+금`                      | `일+월+화+수+목+금`                                    | `목`                   | `월+일+화+수+금`             |
| `LF_WST_EMSN_BGNG_TM`           | 생활쓰레기배출시작시각  | `19:00`                            | `18:00`                                          | `18:00`               | `20:00`                 |
| `LF_WST_EMSN_END_TM`            | 생활쓰레기배출종료시각  | `21:00`                            | `23:59`                                          | `03:00`               | `08:00`                 |
| `FOD_WST_EMSN_BGNG_TM`          | 음식물쓰레기배출시작시각 | `19:00`                            | `18:00`                                          | `18:00`               | `20:00`                 |
| `FOD_WST_EMSN_END_TM`           | 음식물쓰레기배출종료시각 | `21:00`                            | `23:59`                                          | `03:00`               | `08:00`                 |
| `RCYCL_EMSN_BGNG_TM`            | 재활용품배출시작시각   | `19:00`                            | `18:00`                                          | `18:00`               | `20:00`                 |
| `RCYCL_EMSN_END_TM`             | 재활용품배출종료시각   | `21:00`                            | `07:00`                                          | `03:00`               | `08:00`                 |
| `TMPRY_BULK_WASTE_EMSN_BGNG_TM` | 일시적다량폐기물배출시작 | `19:00`                            | `-`                                              | `18:00`               | `-`                     |
| `TMPRY_BULK_WASTE_EMSN_END_TM`  | 일시적다량폐기물배출종료 | `21:00`                            | `-`                                              | `03:00`               | `-`                     |
| `UNCLLT_DAY`                    | 미수거일         | `토+구정연휴+추석연휴`                      | `별도안내`                                           | `명절 및 일요일`            | `명절당일, 일요일 등`           |
| `MNG_DEPT_NM`                   | 관리부서명        | `청소행정과`                            | `자원순환과`                                          | `도시환경2팀`              | `세종특별자치시청`              |
| `MNG_DEPT_TELNO`                | 관리부서전화번호     | `02-2148-2373`                     | `02-2116-3808`                                   | `031-671-6060`        | `044-300-4723`          |
| `DAT_CRTR_YMD`                  | 데이터기준일자      | `2020-07-10`                       | `2024-07-19`                                     | `2025-10-23`          | `2023-01-01`            |
| `DAT_UPDT_SE`                   | 데이터갱신구분      | `I`                                | `I`                                              | `I`                   | `I`                     |
| `DAT_UPDT_PNT`                  | 데이터갱신시점      | `2026-03-26 22:58:08`              | `2026-04-22 22:32:57`                            | `2025-12-15 16:20:32` | `2025-12-15 16:20:32`   |
| `LAST_MDFCN_PNT`                | 최종수정시점       | `2020-07-13 15:58:28`              | `2024-07-19 17:20:34`                            | `2025-11-04 14:24:41` | `2023-04-15 16:11:45`   |

#### [참고 1] 실제 응답 JSON (종로구 예시)

```json
{
  "response": {
    "body": {
      "dataType": "JSON",
      "items": {
        "item": [
          {
            "CTPV_NM": "서울특별시",
            "DAT_CRTR_YMD": "2020-07-10",
            "DAT_UPDT_PNT": "2026-03-26 22:58:08",
            "DAT_UPDT_SE": "I",
            "EMSN_PLC": "집앞",
            "EMSN_PLC_TYPE": "문전수거",
            "FOD_WST_EMSN_BGNG_TM": "19:00",
            "FOD_WST_EMSN_DOW": "일+월+화+수+목+금",
            "FOD_WST_EMSN_END_TM": "21:00",
            "FOD_WST_EMSN_MTHD": "물기를 최대한 줄여 음식물만 전용봉투에 넣어 배출",
            "LAST_MDFCN_PNT": "2020-07-13 15:58:28",
            "LF_WST_EMSN_BGNG_TM": "19:00",
            "LF_WST_EMSN_DOW": "일+월+화+수+목+금",
            "LF_WST_EMSN_END_TM": "21:00",
            "LF_WST_EMSN_MTHD": "규격봉투에 넣어 지정된 요일에 배출",
            "MNG_DEPT_NM": "청소행정과",
            "MNG_DEPT_TELNO": "02-2148-2373",
            "MNG_NO": "202030000000400002",
            "MNG_ZONE_NM": "없음",
            "MNG_ZONE_TRGT_RGN_NM": "없음",
            "OPN_ATMY_GRP_CD": "3000000",
            "RCYCL_EMSN_BGNG_TM": "19:00",
            "RCYCL_EMSN_DOW": "일+월+화+수+목+금",
            "RCYCL_EMSN_END_TM": "21:00",
            "RCYCL_EMSN_MTHD": "투명한 비닐봉투에 담거나 끈으로 묶어서 지정된 요일에 배출",
            "SGG_NM": "종로구",
            "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "19:00",
            "TMPRY_BULK_WASTE_EMSN_END_TM": "21:00",
            "TMPRY_BULK_WASTE_EMSN_MTHD": "주민센터방문 혹은 종로구홈페이지에 신청 후 배출",
            "TMPRY_BULK_WASTE_EMSN_PLC": "집앞",
            "UNCLLT_DAY": "토+구정연휴+추석연휴"
          }
        ]
      },
      "numOfRows": 10,
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

#### [참고 2] 실제 응답 JSON (노원구 예시)

```json
{
  "response": {
    "body": {
      "dataType": "JSON",
      "items": {
        "item": [
          {
            "CTPV_NM": "서울특별시",
            "DAT_CRTR_YMD": "2024-07-19",
            "DAT_UPDT_PNT": "2026-04-22 22:32:57",
            "DAT_UPDT_SE": "I",
            "EMSN_PLC": "내집+내점포건물앞+생활폐기물보관대",
            "EMSN_PLC_TYPE": "거점수거",
            "FOD_WST_EMSN_BGNG_TM": "18:00",
            "FOD_WST_EMSN_DOW": "일+월+화+수+목+금",
            "FOD_WST_EMSN_END_TM": "23:59",
            "FOD_WST_EMSN_MTHD": "용기 용량에 맞는 납부필증 부착 또는 RFID 종량기기에 세대별 카드 사용하여 배출",
            "LAST_MDFCN_PNT": "2024-07-19 17:20:34",
            "LF_WST_EMSN_BGNG_TM": "18:00",
            "LF_WST_EMSN_DOW": "일+월+화+수+목+금",
            "LF_WST_EMSN_END_TM": "23:59",
            "LF_WST_EMSN_MTHD": "규격봉투에 넣어 지정된 요일에 배출",
            "MNG_DEPT_NM": "자원순환과",
            "MNG_DEPT_TELNO": "02-2116-3808",
            "MNG_NO": "202431000000400001",
            "MNG_ZONE_NM": "6권역",
            "MNG_ZONE_TRGT_RGN_NM": "공릉2동, 중계본동, 하계1동",
            "OPN_ATMY_GRP_CD": "3100000",
            "RCYCL_EMSN_BGNG_TM": "18:00",
            "RCYCL_EMSN_DOW": "일+월+화+수+목+금",
            "RCYCL_EMSN_END_TM": "07:00",
            "RCYCL_EMSN_MTHD": "투명한 비닐봉투에 담거나 끈으로 묶어서 지정된 요일에 배출",
            "SGG_NM": "노원구",
            "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "",
            "TMPRY_BULK_WASTE_EMSN_END_TM": "",
            "TMPRY_BULK_WASTE_EMSN_MTHD": "별도안내",
            "TMPRY_BULK_WASTE_EMSN_PLC": "별도안내",
            "UNCLLT_DAY": "별도안내"
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 6
    },
    "header": {
      "resultCode": "0",
      "resultMsg": "정상"
    }
  }
}
```

#### [참고 3] 실제 응답 JSON (안성시 예시)

```json
{
  "response": {
    "body": {
      "dataType": "JSON",
      "items": {
        "item": [
          {
            "CTPV_NM": "경기도",
            "DAT_CRTR_YMD": "2025-10-23",
            "DAT_UPDT_PNT": "2025-12-15 16:20:32",
            "DAT_UPDT_SE": "I",
            "EMSN_PLC": "지정된 장소",
            "EMSN_PLC_TYPE": "거점수거",
            "FOD_WST_EMSN_BGNG_TM": "18:00",
            "FOD_WST_EMSN_DOW": "목",
            "FOD_WST_EMSN_END_TM": "03:00",
            "FOD_WST_EMSN_MTHD": "규격봉투",
            "LAST_MDFCN_PNT": "2025-11-04 14:24:41",
            "LF_WST_EMSN_BGNG_TM": "18:00",
            "LF_WST_EMSN_DOW": "목",
            "LF_WST_EMSN_END_TM": "03:00",
            "LF_WST_EMSN_MTHD": "규격봉투",
            "MNG_DEPT_NM": "도시환경2팀",
            "MNG_DEPT_TELNO": "031-671-6060",
            "MNG_NO": "202540800000400542",
            "MNG_ZONE_NM": "대덕면",
            "MNG_ZONE_TRGT_RGN_NM": "삼한리 삼암",
            "OPN_ATMY_GRP_CD": "4080000",
            "RCYCL_EMSN_BGNG_TM": "18:00",
            "RCYCL_EMSN_DOW": "목",
            "RCYCL_EMSN_END_TM": "03:00",
            "RCYCL_EMSN_MTHD": "일반봉투 등",
            "SGG_NM": "안성시",
            "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "18:00",
            "TMPRY_BULK_WASTE_EMSN_END_TM": "03:00",
            "TMPRY_BULK_WASTE_EMSN_MTHD": "스티커 등",
            "TMPRY_BULK_WASTE_EMSN_PLC": "신고후 집앞",
            "UNCLLT_DAY": "명절 및 일요일"
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 516
    },
    "header": {
      "resultCode": "0",
      "resultMsg": "정상"
    }
  }
}
```

#### [참고 4] 실제 응답 JSON (세종특별자치시 예시)

```json
{
  "response": {
    "body": {
      "dataType": "JSON",
      "items": {
        "item": [
          {
            "CTPV_NM": "세종특별자치시",
            "DAT_CRTR_YMD": "2023-01-01",
            "DAT_UPDT_PNT": "2025-12-15 16:20:32",
            "DAT_UPDT_SE": "I",
            "EMSN_PLC": "집앞+상가 앞",
            "EMSN_PLC_TYPE": "문전수거",
            "FOD_WST_EMSN_BGNG_TM": "20:00",
            "FOD_WST_EMSN_DOW": "일+월+화+목+금",
            "FOD_WST_EMSN_END_TM": "08:00",
            "FOD_WST_EMSN_MTHD": "음식물류납부필증 부착하여 지정된 요일에 배출",
            "LAST_MDFCN_PNT": "2023-04-15 16:11:45",
            "LF_WST_EMSN_BGNG_TM": "20:00",
            "LF_WST_EMSN_DOW": "일+월+화+목+금",
            "LF_WST_EMSN_END_TM": "08:00",
            "LF_WST_EMSN_MTHD": "규격봉투에 넣어 지정된 요일에 배출",
            "MNG_DEPT_NM": "세종특별자치시청",
            "MNG_DEPT_TELNO": "044-300-4723",
            "MNG_NO": "202156900000400003",
            "MNG_ZONE_NM": "세종특별자치시(3권역A)",
            "MNG_ZONE_TRGT_RGN_NM": "조치원읍(상리, 평리, 교리, 침산리, 신안리, 봉산리, 서창리)",
            "OPN_ATMY_GRP_CD": "5690000",
            "RCYCL_EMSN_BGNG_TM": "20:00",
            "RCYCL_EMSN_DOW": "월+일+화+수+금",
            "RCYCL_EMSN_END_TM": "08:00",
            "RCYCL_EMSN_MTHD": "투명한 비닐봉투에 담거나 끈으로 묶어서 지정된 요일에 배출",
            "SGG_NM": "없음",
            "TMPRY_BULK_WASTE_EMSN_BGNG_TM": "",
            "TMPRY_BULK_WASTE_EMSN_END_TM": "",
            "TMPRY_BULK_WASTE_EMSN_MTHD": "규격봉투에 넣어 지정된 요일에 배출",
            "TMPRY_BULK_WASTE_EMSN_PLC": "집앞+상가 앞",
            "UNCLLT_DAY": "명절당일, 근로자의 날, 일요일"
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 6
    },
    "header": {
      "resultCode": "0",
      "resultMsg": "정상"
    }
  }
}
```

---

## 📄 API 응답 데이터 활용 현황

데이터의 일관성과 UI 복잡도 조절을 위해 API에서 제공하는 전체 필드 중 서비스에 필요한 항목을 선별하여 활용합니다.

### 1. 사용 중인 데이터 (UI 노출 및 로직 활용)

| API 종류 | 필드명 (JSON Key)                  | 한글 항목명        | 용도 및 사용 이유                                    |
|:-------|:--------------------------------|:--------------|:----------------------------------------------|
| **품목** | `itemNm`                        | 품목명           | 검색 대상 품목의 이름을 명시함.                            |
| **품목** | `dschgMthd`                     | 배출방법          | 품목별 구체적인 분리배출 가이드를 사용자에게 전달함.                 |
| **장소** | `spotNm`                        | 장소명           | 수거함 혹은 배출처의 이름을 안내함 (예: 폐건전지 수거함).            |
| **장소** | `addrBase`                      | 기본 주소         | 수거 장소의 도로명/지번 주소를 제공함.                        |
| **장소** | `addrDtl`                       | 상세 주소         | 건물 내 위치 등 구체적인 지점을 안내함.                       |
| **요일** | `SGG_NM`                        | 시군구명          | 검색된 정보의 관할 지자체를 확인하고 표시함.                     |
| **요일** | `MNG_ZONE_NM`                   | 관리구역명         | 시군구 내 세부 관리 구역(권역, 면 등) 정보를 제공함.              |
| **요일** | `MNG_ZONE_TRGT_RGN_NM`          | 관리구역대상지역명     | 해당 지침이 적용되는 구체적인 동/리 지역을 확인하여 사용자 지역 매칭에 활용함. |
| **요일** | `EMSN_PLC`                      | 배출장소          | 실제 쓰레기를 내놓는 장소(예: 집앞, 거점장소)를 안내함.             |
| **요일** | `EMSN_PLC_TYPE`                 | 배출장소유형        | 수거 방식(문전수거, 거점수거 등)을 사용자에게 알림.                |
| **요일** | `LF_WST_EMSN_DOW`               | 생활쓰레기 배출요일    | 일반 쓰레기를 내놓는 요일 정보를 제공함.                       |
| **요일** | `LF_WST_EMSN_MTHD`              | 생활쓰레기 배출방법    | 일반 쓰레기 배출 시 주의사항(규격봉투 등)을 안내함.                |
| **요일** | `LF_WST_EMSN_BGNG_TM`           | 생활쓰레기 배출시작시각  | 쓰레기 배출이 가능한 시작 시간을 안내함.                       |
| **요일** | `LF_WST_EMSN_END_TM`            | 생활쓰레기 배출종료시각  | 쓰레기 배출을 마쳐야 하는 종료 시간을 안내함.                    |
| **요일** | `FOD_WST_EMSN_DOW`              | 음식물쓰레기 배출요일   | 음식물 쓰레기 배출 요일을 제공함.                           |
| **요일** | `FOD_WST_EMSN_MTHD`             | 음식물쓰레기 배출방법   | 전용 봉투/칩 등 구체적인 배출 수단을 안내함.                    |
| **요일** | `FOD_WST_EMSN_BGNG_TM`          | 음식물쓰레기 배출시작시각 | 음식물 쓰레기 배출 시작 시각을 안내함.                        |
| **요일** | `FOD_WST_EMSN_END_TM`           | 음식물쓰레기 배출종료시각 | 음식물 쓰레기 배출 종료 시각을 안내함.                        |
| **요일** | `RCYCL_EMSN_DOW`                | 재활용품 배출요일     | 재활용품 배출 요일을 제공함.                              |
| **요일** | `RCYCL_EMSN_MTHD`               | 재활용품 배출방법     | 투명 페트병 분리 배출 등 품목별 상세 요령을 안내함.                |
| **요일** | `RCYCL_EMSN_BGNG_TM`            | 재활용품 배출시작시각   | 재활용품 배출 시작 시각을 안내함.                           |
| **요일** | `RCYCL_EMSN_END_TM`             | 재활용품 배출종료시각   | 재활용품 배출 종료 시각을 안내함.                           |
| **요일** | `TMPRY_BULK_WASTE_EMSN_MTHD`    | 일시적다량폐기물 배출방법 | 대형 폐기물 신청 방법 및 배출 요령을 안내함.                    |
| **요일** | `TMPRY_BULK_WASTE_EMSN_PLC`     | 일시적다량폐기물 배출장소 | 대형 폐기물을 내놓는 장소를 안내함.                          |
| **요일** | `TMPRY_BULK_WASTE_EMSN_BGNG_TM` | 일시적다량폐기물 배출시작 | 대형 폐기물 배출 시작 시간을 안내함.                         |
| **요일** | `TMPRY_BULK_WASTE_EMSN_END_TM`  | 일시적다량폐기물 배출종료 | 대형 폐기물 배출 종료 시간을 안내함.                         |
| **요일** | `UNCLLT_DAY`                    | 미수거일          | 공휴일 등 쓰레기를 가져가지 않는 날을 미리 알려 혼선을 방지함.          |
| **요일** | `MNG_DEPT_NM`                   | 관리부서명         | 정보 확인 및 민원 접수를 위한 담당 부서를 안내함.                 |
| **요일** | `MNG_DEPT_TELNO`                | 관리부서 전화번호     | 즉각적인 문의가 가능하도록 연락처를 제공함.                      |

### 2. 사용하지 않는 데이터 (필터링 로직에만 사용하거나 제외)

| 필드명 (JSON Key)    | 한글 항목명   | 미사용 또는 제한적 사용 사유                                            |
|:------------------|:---------|:------------------------------------------------------------|
| `CTPV_NM`         | 시도명      | `SGG_NM`(시군구) 정보만으로도 사용자 지역을 충분히 특정할 수 있어 UI 간소화를 위해 제외함.   |
| `OPN_ATMY_GRP_CD` | 개방자치단체코드 | 시스템 내부 관리용 코드로 사용자에게 노출할 필요 없음 (검색 필터로만 활용 가능).             |
| `MNG_NO`          | 관리번호     | 데이터 식별을 위한 고유 번호로 사용자에게 노출할 가치가 없음.                         |
| `DAT_CRTR_YMD`    | 데이터기준일자  | 데이터 관리용 메타데이터로 UI 노출 대상에서 제외함.                              |
| `DAT_UPDT_SE`     | 데이터갱신구분  | 데이터 관리용 메타데이터(수정/추가 여부)로 사용자에게 노출하지 않음.                     |
| `DAT_UPDT_PNT`    | 데이터갱신시점  | 데이터 신뢰성 확인을 위한 내부 필터링(`updatedFrom` 등)에는 활용하나 UI에는 노출하지 않음. |
| `LAST_MDFCN_PNT`  | 최종수정시점   | 시스템 관리용 시간 정보로 UI 노출 대상에서 제외함.                              |

---

## 🧠 비즈니스 로직 및 문제 해결 전략 (Core Logic)

이 프로젝트의 핵심은 **서로 다른 두 부처의 API가 요구하는 지역 검색 단위의 차이를 해결**하여 사용자에게 끊김 없는 경험을 제공하는 것입니다.

### 1. 지역 검색 단위의 불일치 문제 해결

공공데이터포털에서 제공하는 두 API는 데이터를 관리하는 행정 단위가 서로 달라 단순 검색으로는 연동이 어렵습니다.

* **기후에너지환경부 API (`getSpot`)**: **읍/면/동 단위**(예: 인사동, 문래동) 검색을 지원하며, 특정 수거 장소(Spot)를 찾는 데 최적화되어 있습니다.
* **행정안전부 API (`info`)**: **시/군/구 단위**(예: 종로구, 영등포구) 검색만 지원하며, 해당 지자체 전체의 공통 배출 지침(요일, 시간)을 제공합니다.

**[해결 로직: 지능형 지역 전파 (Context Propagation)]**
사용자가 '동' 단위로 수거 장소를 검색하면, 앱은 내부적으로 다음과 같은 단계를 거쳐 배출 요일까지 자동으로 찾아줍니다.

1. **동 단위 검색**: 사용자가 `getSpot` API를 통해 '문래동' 검색.
2. **SGG 자동 추출**: 응답받은 주소(`addrBase`)에서 `WasteRepository.extractSggFromAddress`를 통해 **'영등포구'** 정보를
   추출.
3. **상태 공유**: 추출된 시군구명을 `WasteViewModel`의 `lastDetectedSgg` 상태에 저장하여 전역적으로 공유.
4. **자동 필터링**: 사용자가 '배출 요일' 탭으로 전환 시, 저장된 '영등포구' 정보를 기반으로 행안부 API를 즉시 호출하여 정보를 노출합니다.

### 2. 데이터 예외 처리: 세종특별자치시 매핑

행정 구역 체계가 특수한 세종시의 데이터 누락 문제를 비즈니스 로직으로 방어했습니다.

* **현상**: 행안부 API는 세종시의 시군구(`SGG_NM`) 값을 **"없음"**으로 반환하여, '세종'으로 검색 시 결과가 나오지 않거나 UI에 '없음'으로 표시되는 문제
  발생.
* **해결**: `WasteRepository`에서 "세종" 검색어를 "없음"으로 변환하여 API를 호출하고, 수신된 데이터의 "없음" 값을 다시 "세종특별자치시"로 치환하여
  UI에 표시합니다.

---

## 💡 개발 시 주의사항 (검색 전략 요약)

### 1. API별 검색 범위(Granularity) 준수

데이터 소스별로 지원하는 검색어의 성격이 명확히 다릅니다.

* **환경부 (`getSpot`)**: 반드시 **'읍/면/동'** 단위(예: 성산동)로 검색해야 정확한 위치 데이터가 반환됩니다. 시군구 단위 검색 시 데이터가 너무 많거나
  누락될 수 있습니다.
* **행안부 (`info`)**: 반드시 **'시/군/구'** 단위(예: 마포구)로 검색해야 합니다. '동' 단위 검색어는 API 내부에서 인식하지 못합니다.

### 2. 검색 유도 및 필터링 전략

* 사용자가 '동'을 입력하면 환경부 API를 먼저 호출하여 상세 장소를 보여주고, 거기서 추출한 '구' 정보를 이용해 행안부 API를 연쇄 호출하는 방식이 효율적입니다.
* 행안부 결과 중 구체적인 지역명(동/리)은 `MNG_ZONE_TRGT_RGN_NM` 필드에 텍스트로 포함되어 있으므로, UI에서 2차 필터링을 수행합니다.

### 3. 페이지네이션 (Pagination)

* `pageNm`을 `1`로 설정하고 `numOfRows`를 `1000`으로 설정해서 `스페이스바`를 `itemNm`에 입력했을 때, `getItem`으로 불러온
  `totalCount`가 총 `730`개임을 알 수 있었습니다.
* 사용자 화면에 몇 개 정도를 먼저 불러올지를 설정한 뒤, 페이지네이션으로 사용자가 스크롤을 할 때마다 마저 데이터를 불러오도록 설정하는 것이 좋겠습니다.
* `totalCount`를 확인하여 마지막 페이지 여부를 판단합니다.
* `numOfRows`는 API별 제한 사항(행안부 Max 100 등)을 준수해야 합니다.

### 4. API 키 관리

* `local.properties`에 `API_KEY`를 저장하고 `BuildConfig`를 통해 접근합니다.

---

## 🛠️ 기술 스택

* **Language**: Kotlin (2.x)
* **UI**: Jetpack Compose (Material 3)
* **Network**: Retrofit 2, Kotlinx Serialization
* **Architecture**: MVVM (ViewModel, State, Repository)

---
*본 문서는 개발 진행 상황에 따라 업데이트될 수 있습니다.*
