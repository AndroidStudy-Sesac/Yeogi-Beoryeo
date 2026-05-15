# Yeogi-Beoryeo

내 위치 기반 맞춤형 분리수거 가이드 및 수거 장소 안내 서비스입니다.

공공데이터포털 API와 생활폐기물 분리배출 누리집 기준 정보를 함께 활용해 사용자가 품목별 배출 방법, 주변 분리배출 장소, 지역별 배출 요일을 확인할 수 있도록 합니다.

## 주요 기능

- 품목명 검색을 통한 분리배출 방법 안내
- 품목별 상세 배출 단계, 주의사항, 관련 수거 장소 유형 제공
- 주변 분리배출 장소 안내
- 지역별 생활쓰레기, 음식물쓰레기, 재활용품 배출 요일 및 시간 안내

## 모듈 구조

- `app`: Android 앱 진입점 및 UI 계층
- `domain`: 서비스에서 사용하는 도메인 모델, Repository 인터페이스, UseCase
- `data`: 공공 API 호출, 로컬 asset 데이터, DTO, mapper, Repository 구현체

## 디렉터리 구조

```text
Yeogi-Beoryeo/
├─ app/
│  └─ src/main/java/com/team/yeogibeoryeo/
│     └─ MainActivity.kt
├─ domain/
│  └─ src/main/java/com/team/yeogibeoryeo/domain/item/
│     ├─ model/
│     │  ├─ DisposalCategory.kt
│     │  ├─ DisposalInstruction.kt
│     │  ├─ DisposalItemGuide.kt
│     │  ├─ DisposalRecyclability.kt
│     │  ├─ DisposalSubCategory.kt
│     │  └─ RelatedSpotType.kt
│     ├─ repository/
│     │  └─ DisposalItemGuideRepository.kt
│     └─ usecase/
│        ├─ GetDisposalCategoriesUseCase.kt
│        ├─ GetDisposalCategoryGuidesUseCase.kt
│        └─ SearchDisposalItemGuidesUseCase.kt
├─ data/
│  └─ src/main/
│     ├─ assets/
│     │  ├─ category_map.json
│     │  ├─ guide_detail_aliases.json
│     │  ├─ item_guide_details.json
│     │  ├─ local_items.json
│     │  ├─ related_spots.json
│     │  └─ synonyms.json
│     └─ java/com/team/yeogibeoryeo/data/
│        ├─ common/remote/
│        │  └─ WasteRecyclingServiceFactory.kt
│        └─ item/
│           ├─ local/
│           │  ├─ ItemCategoryLocalDataSource.kt
│           │  └─ ItemCategoryLocalSource.kt
│           ├─ mapper/
│           │  └─ ItemGuideMapper.kt
│           ├─ remote/
│           │  ├─ ItemApiService.kt
│           │  ├─ datasource/
│           │  │  ├─ ItemApiException.kt
│           │  │  └─ ItemRemoteDataSource.kt
│           │  └─ dto/
│           │     ├─ ItemGuideDto.kt
│           │     └─ ItemGuideResponseDto.kt
│           └─ repository/
│              └─ DisposalItemGuideRepositoryImpl.kt
└─ data/src/test/java/com/team/yeogibeoryeo/data/item/
   ├─ local/ItemGuideAssetTest.kt
   ├─ mapper/ItemGuideMapperTest.kt
   ├─ remote/datasource/ItemRemoteDataSourceTest.kt
   └─ repository/DisposalItemGuideRepositoryImplTest.kt
```

## 파일별 역할

### Domain

- `DisposalItemGuide.kt`: 품목별 배출 가이드의 최종 도메인 모델입니다. UI와 UseCase는 API DTO가 아니라 이 모델만 바라보도록 합니다. 이렇게 하면 API 응답 구조가 바뀌거나 로컬 상세 가이드를 병합하더라도 화면과 비즈니스 로직은 동일한 모델을 사용할 수 있습니다.
- `DisposalInstruction.kt`: API의 `dschgMthd`를 배출방법 단위로 표현합니다.
- `DisposalCategory.kt`: 종이류, 금속류, 전자제품류 등 서비스에서 사용하는 상위 카테고리입니다.
- `DisposalSubCategory.kt`: 골판지, 무색페트병, 소형 가전처럼 상위 카테고리 안의 세부 분류입니다.
- `DisposalRecyclability.kt`: 배출방법 또는 카테고리를 기준으로 재활용 가능 여부를 계산합니다.
- `RelatedSpotType.kt`: 전용수거함, 대형폐기물 신고, 무상방문수거처럼 장소 API 또는 UI와 연결될 수 있는 장소 유형입니다.
- `DisposalItemGuideRepository.kt`: domain 계층이 data 구현체에 직접 의존하지 않도록 하는 Repository 인터페이스입니다.
- `SearchDisposalItemGuidesUseCase.kt`: 품목명 검색 UseCase입니다.
- `GetDisposalCategoryGuidesUseCase.kt`: 카테고리별 대표 가이드 조회 UseCase입니다.
- `GetDisposalCategoriesUseCase.kt`: 서비스에서 제공하는 카테고리 목록 조회 UseCase입니다.

### Data - Remote

- `WasteRecyclingServiceFactory.kt`: 기후에너지환경부 분리배출 정보조회 서비스의 Retrofit 생성 지점입니다. `/getItem`과 `/getSpot`은 같은 base URL을 사용하므로 공통 factory로 분리했습니다.
- `ItemApiService.kt`: `/getItem` Retrofit API 정의입니다.
- `ItemRemoteDataSource.kt`: API 호출, 페이지네이션, 응답 코드 처리를 담당합니다. `NODATA_ERROR`는 빈 리스트로 변환하고, 실제 오류 코드는 예외로 전달합니다.
- `ItemApiException.kt`: API 응답 코드와 메시지를 보존하기 위한 예외 타입입니다.
- `ItemGuideDto.kt`: `/getItem`의 item 단위 응답 DTO입니다.
- `ItemGuideResponseDto.kt`: `/getItem` 전체 응답 구조 DTO입니다.

### Data - Local

- `ItemCategoryLocalSource.kt`: 로컬 asset을 읽는 데이터 소스의 인터페이스입니다. 테스트에서 fake 구현을 넣기 쉽도록 분리했습니다.
- `ItemCategoryLocalDataSource.kt`: `assets`의 JSON 파일을 읽어 category, alias, 상세 가이드, 관련 장소, 로컬 보완 품목을 제공합니다.
- `ItemGuideMapper.kt`: API DTO와 로컬 상세 가이드를 합쳐 `DisposalItemGuide`로 변환합니다. 직접 매칭, alias 매칭, 배출방법 fallback 매칭을 모두 이곳에서 처리합니다.
- `DisposalItemGuideRepositoryImpl.kt`: remote 결과와 local 보완 데이터를 조합하는 Repository 구현체입니다.

### Test

- `ItemGuideMapperTest.kt`: DTO가 도메인 모델로 변환될 때 상세 가이드, alias, fallback, 카테고리 보정이 제대로 적용되는지 검증합니다.
- `ItemRemoteDataSourceTest.kt`: `/getItem` 응답 코드, 페이지네이션, 빈 결과 처리를 검증합니다.
- `DisposalItemGuideRepositoryImplTest.kt`: 검색어 보정, remote/local 조합, 카테고리 조회 흐름을 검증합니다.
- `ItemGuideAssetTest.kt`: JSON asset의 alias 타깃, enum 참조, related spot 값이 깨지지 않았는지 검증합니다.

## 품목별 배출 방법(`/getItem`) 구조

`/getItem`은 공공데이터포털의 기후에너지환경부 분리배출 정보조회 서비스에서 품목명과 대표 배출방법을 조회합니다.

API 응답은 기본적으로 다음 두 필드를 사용합니다.

- `itemNm`: 품목명
- `dschgMthd`: 배출방법

서비스에서는 API 응답만 그대로 보여주지 않고, 로컬 asset에 정리한 공식 누리집 기준 상세 가이드를 함께 병합합니다.

### API DTO 대신 도메인 모델을 사용하는 이유

`ItemGuideDto`는 `/getItem` 응답을 그대로 표현하기 위한 data 계층 모델입니다. 현재 API가 내려주는 핵심 값은 `itemNm`, `dschgMthd`뿐이므로, 이 DTO를 그대로 화면까지 전달하면 서비스에서 필요한 정보가 부족해집니다.

서비스에서 실제로 필요한 값은 다음처럼 API 응답보다 넓습니다.

- 품목명
- 대표 배출방법
- 상세 배출 단계
- 주의사항
- 분리배출 팁
- 상위 카테고리와 세부 카테고리
- 재활용 가능 여부
- 관련 수거 장소 유형

그래서 data 계층에서 `ItemGuideDto`를 `DisposalItemGuide`로 변환합니다. 이 구조의 장점은 다음과 같습니다.

- **API 응답 변경에 대한 영향 최소화**: API 필드명이나 응답 구조가 바뀌어도 mapper와 DTO만 수정하면 됩니다.
- **UI 요구사항 반영**: UI는 `dschgMthd` 한 줄이 아니라 상세 단계, 주의사항, 카테고리까지 필요로 하므로 도메인 모델이 더 적합합니다.
- **로컬 데이터 병합 가능**: API 결과와 `assets`의 상세 가이드, alias, 카테고리 정보를 하나의 모델로 합칠 수 있습니다.
- **테스트 용이성**: mapper 테스트를 통해 API 응답이 서비스 모델로 올바르게 바뀌는지 독립적으로 검증할 수 있습니다.
- **계층 분리**: domain 계층이 Retrofit, kotlinx serialization, API 응답 구조에 직접 의존하지 않습니다.

즉 DTO는 "외부 API 응답을 받기 위한 모델"이고, `DisposalItemGuide`는 "우리 서비스가 사용자에게 보여주기 위한 모델"입니다.

### API와 로컬 데이터를 함께 사용하는 이유

`/getItem` API는 품목별 배출방법을 조회하는 데 꼭 필요하지만, API만으로는 현재 서비스가 목표로 하는 안내 품질을 만들기 어렵습니다.

`/getItem` 응답은 예를 들어 다음처럼 단순합니다.

```json
{
  "itemNm": "유리병",
  "dschgMthd": "재활용폐기물, 보증금 환급"
}
```

이 응답만 사용하면 사용자는 "재활용폐기물"이라는 대표 방법만 알 수 있고, 실제로 어떻게 비우고, 씻고, 라벨을 제거해야 하는지, 어떤 경우에 보증금 환급을 받을 수 있는지, 깨진 유리는 어떻게 해야 하는지는 알기 어렵습니다.

또한 API만 사용할 때 다음 한계가 있습니다.

- **상세 안내 부족**: `dschgMthd`는 대표 배출방법이며, 배출 단계나 주의사항까지 제공하지 않습니다.
- **검색어 차이**: 사용자는 `휴대폰`으로 검색하지만 API는 `핸드폰`에서 결과가 나오는 것처럼 실제 검색어와 API 품목명이 다를 수 있습니다.
- **구체 품목과 대표 가이드의 분리 필요**: `맥주캔`, `참치캔`, `사료 캔`은 모두 `금속캔` 대표 가이드를 공유할 수 있습니다.
- **카테고리 정보 부족**: API 응답만으로는 종이류, 플라스틱류, 전기전자제품류 같은 앱 내부 카테고리를 안정적으로 구성하기 어렵습니다.
- **사용자 안내 문구 부족**: `NODATA_ERROR`나 단순 배출방법만으로는 사용자에게 충분한 행동 안내를 제공하기 어렵습니다.

그래서 `/getItem`은 "공식 API 기반 품목명과 대표 배출방법을 확인하는 출처"로 사용하고, 생활폐기물 분리배출 누리집 기준으로 정리한 로컬 JSON을 병합해 사용자에게 필요한 상세 가이드를 제공합니다.

정리하면 역할은 다음과 같습니다.

- `/getItem`: 현재 검색어에 해당하는 공식 품목명과 대표 배출방법 확인
- `item_guide_details.json`: 공식 누리집 기준 상세 배출 단계와 주의사항 제공
- `guide_detail_aliases.json`: 구체 품목을 대표 상세 가이드로 연결
- `category_map.json`: 앱에서 사용할 카테고리 정보 제공
- `synonyms.json`: 사용자 검색어와 API 검색어 차이 보정

### 데이터 병합 흐름

1. 사용자가 품목명을 검색합니다.
2. `ItemRemoteDataSource`가 `/getItem`을 호출합니다.
3. `ItemGuideMapper`가 API 응답을 `DisposalItemGuide` 도메인 모델로 변환합니다.
4. 변환 과정에서 다음 순서로 상세 가이드를 찾습니다.
   - 품목명과 `item_guide_details.json` 키 직접 매칭
   - `guide_detail_aliases.json`을 통한 대표 가이드 매칭
   - `dschgMthd` 기반 fallback 매칭
5. 최종적으로 배출방법, 상세 단계, 주의사항, 팁, 카테고리, 관련 장소 유형을 포함한 도메인 모델을 반환합니다.

### 로컬 asset 파일

- `item_guide_details.json`: 공식 생활폐기물 분리배출 누리집 기준 대표 상세 가이드
- `guide_detail_aliases.json`: 구체 품목명을 대표 상세 가이드로 연결하는 별칭 맵
- `category_map.json`: 품목 또는 대표 가이드별 도메인 카테고리 매핑
- `related_spots.json`: 품목별 관련 수거 장소 유형 매핑
- `synonyms.json`: 사용자 검색어를 API 검색어로 보정하는 동의어 맵
- `local_items.json`: API에서 잘 조회되지 않는 보완 품목

현재 첨부 기준 730개 품목은 모두 상세 가이드와 카테고리에 연결되어 있습니다.

## JSON 파일을 assets에 둔 이유

품목별 상세 가이드와 alias 매핑은 코드 로직이라기보다 서비스 운영 데이터에 가깝습니다. 그래서 Kotlin 코드에 하드코딩하지 않고 `data/src/main/assets`의 JSON 파일로 분리했습니다.

이 구조를 선택한 이유는 다음과 같습니다.

- **공식 누리집 기준 데이터의 변경 가능성**: 분리배출 기준이나 품목 매핑은 추후 바뀔 수 있으므로 코드 수정 없이 JSON만 갱신할 수 있게 했습니다.
- **리뷰 용이성**: `guide_detail_aliases.json`을 보면 어떤 품목이 어떤 대표 가이드로 연결되는지 한눈에 확인할 수 있습니다.
- **테스트 용이성**: `ItemGuideAssetTest`에서 JSON의 alias 타깃, enum 값, related spot 값이 유효한지 자동 검증할 수 있습니다.
- **관심사 분리**: mapper는 병합 규칙만 담당하고, 실제 품목 데이터는 asset이 담당합니다.
- **오프라인 보완 가능성**: API에서 검색되지 않거나 표현이 부족한 품목도 로컬 데이터로 보완할 수 있습니다.

단, 이 JSON들은 앱에 번들되는 정적 데이터이므로 배포 후 즉시 서버에서 바뀌는 데이터는 아닙니다. 운영 중 실시간 갱신이 필요해지면 다음 대안을 고려할 수 있습니다.

| 대안 | 설명 | 장점 | 단점 |
|:---|:---|:---|:---|
| 원격 JSON 호스팅 | GitHub Raw, Firebase Hosting, S3 같은 정적 파일 저장소에 JSON을 올리고 앱이 내려받는 방식 | 서버 구현 없이 빠르게 갱신 가능, 현재 assets 구조를 거의 그대로 재사용 가능 | 캐싱, 버전 관리, 장애 시 fallback 정책이 필요 |
| Firebase Remote Config | alias 버전, 공지성 문구, 일부 설정값처럼 작은 데이터를 원격 설정으로 관리 | Android에서 적용이 쉽고 점진적 배포가 가능 | 730개 전체 상세 가이드처럼 큰 구조화 데이터에는 적합하지 않음 |
| 자체 API 서버 | 품목, alias, 상세 가이드, 버전 정보를 서버 DB에서 관리하고 앱이 API로 조회 | 검색/필터/운영 관리가 가장 유연함 | 서버, DB, 관리자 도구, 운영 비용이 필요 |
| 앱 업데이트 유지 | 지금처럼 assets에 포함하고 앱 릴리즈로 갱신 | 구현이 단순하고 오프라인에서도 동작 | 데이터 수정만으로도 앱 배포가 필요 |

현재 단계에서는 `앱 업데이트 유지` 방식이 가장 적합합니다. 이유는 `/getItem` 자체는 공공 API에서 계속 조회하고, 로컬 JSON은 공식 누리집 기준의 상세 가이드와 alias를 보완하는 정적 기준 데이터이기 때문입니다. 아직 운영 서버나 관리자 도구가 없는 상태에서 자체 API를 먼저 만들면 범위가 커지고, `Remote Config`는 데이터 크기와 구조상 맞지 않습니다.

추후 배포 이후에도 품목 매핑을 자주 수정해야 한다면 1차 대안으로 `원격 JSON 호스팅`을 추천합니다. 현재 JSON 파일 구조를 거의 유지하면서도 앱 재배포 없이 데이터를 교체할 수 있고, 앱에는 마지막으로 성공한 JSON을 캐시한 뒤 실패 시 assets 기본값으로 fallback하는 구조를 만들 수 있기 때문입니다.

## 공공 API 설정

API 키는 `local.properties`에 저장하고 `BuildConfig`를 통해 주입합니다.

```properties
PUBLIC_DATA_SERVICE_KEY=공공데이터포털_디코딩된_서비스키
```

`ItemApiService`는 `serviceKey`를 일반 쿼리 파라미터로 전달하므로, 현재 규칙은 공공데이터포털에서 제공하는 디코딩된 키를 저장하고 Retrofit이 요청 시 필요한 인코딩을 처리하도록 맡기는 방식입니다.

## API 응답 코드 처리

`/getItem` 응답 코드는 data 계층에서 다음처럼 처리합니다.

- `00`, `200`: 성공
- `3`, `03`: 검색 결과 없음, 빈 리스트 반환
- `10`, `11`, `99`: `ItemApiException`으로 전달

검색 결과 없음은 오류가 아니라 빈 결과로 처리하며, UI에서는 "검색 결과가 없습니다"처럼 안내할 수 있습니다.

## 검증

아래 명령으로 domain 테스트, data 단위 테스트, app debug 빌드를 함께 확인합니다.

```powershell
.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug
```

품목 가이드 관련 주요 검증은 다음을 포함합니다.

- API 응답 DTO를 도메인 모델로 변환
- 상세 가이드 직접 매칭
- alias 기반 대표 가이드 매칭
- 배출방법 기반 fallback 매칭
- asset 파일의 enum/alias 참조 무결성

## 향후 고려사항

이번 이슈는 `/getItem` 품목 검색에 필요한 domain/data 계층 구현을 목표로 하며, 화면 구현은 포함하지 않습니다. 다만 presentation 계층이나 다른 API 담당 작업에서 이어서 고려하면 좋은 지점이 있습니다.

### 카테고리 조회 결과의 배출방법 표시

`getCategoryGuides()`는 현재 로컬 `category_map.json`과 상세 가이드를 기반으로 카테고리별 대표 품목 목록을 만듭니다. 이 목록은 API 검색 결과가 아니므로 `dschgMthd` 기반 `instructions`가 비어 있을 수 있습니다.

이 부분은 화면 구성을 어떻게 가져가느냐에 따라 달라질 수 있습니다. 예를 들어 카테고리 화면을 "대표 품목을 빠르게 훑는 목록"으로 만들면 상세 가이드만으로 충분하지만, 검색 결과 화면과 같은 카드 컴포넌트를 재사용한다면 배출방법 라벨이 필요할 수 있습니다.

추후 카테고리 화면에서 각 품목 카드에 "배출 방법"까지 함께 보여주어야 한다면 다음 중 하나를 선택할 수 있습니다.

- 카테고리 목록에서는 상세 가이드의 `steps`, `cautions`, `tip`만 보여주고 배출방법 라벨은 생략
- `category_map.json` 또는 별도 로컬 파일에 대표 배출방법을 추가
- 카테고리 화면 진입 시 필요한 품목만 `/getItem`으로 조회해 배출방법을 채움

현재는 카테고리 조회에서 불필요한 API 호출을 만들지 않는 쪽을 우선했습니다.

### 검색 결과 없음 및 오류 상태 표시

data 계층에서는 `/getItem`의 `NODATA_ERROR`를 오류가 아니라 빈 리스트로 변환합니다. 따라서 화면 구현 시에는 다음 상태를 구분해서 보여주는 것이 좋습니다.

- 검색어가 비어 있음: 검색어 입력 유도
- 검색 결과 없음: "검색 결과가 없습니다" 안내
- API 오류 또는 네트워크 오류: 일시적 오류 안내와 재시도 동작 제공

이 구분은 UI/UX 설계에 따라 문구와 노출 위치가 달라질 수 있으므로, 이번 data 계층에서는 상태를 강제로 확정하지 않았습니다.

### 복합 배출방법과 alias 보강

`ItemGuideMapper`는 직접 매칭, alias 매칭, 배출방법 기반 fallback을 순서대로 적용합니다. 다만 `종량제봉투, 대형폐기물`처럼 복합 배출방법이 내려오는 품목은 단일 대표 가이드로 자동 보강하지 않습니다.

복합 케이스는 품목의 실제 재질이나 배출 조건에 따라 안내가 달라질 수 있으므로, 화면 구현이나 사용자 검색 로그를 보면서 `guide_detail_aliases.json`에 우선순위 높은 품목부터 수동 보강하는 방식이 안전합니다.

### 공통 Retrofit 설정 확장

`WasteRecyclingServiceFactory`는 `/getItem`과 `/getSpot`이 같은 base URL을 공유할 수 있도록 공통 생성 지점으로 만들었습니다.

추후 `/getSpot` 구현이 들어오면 다음 확장을 함께 고려할 수 있습니다.

- `createSpotApiService()` 추가
- 공통 `OkHttpClient` 설정
- timeout 설정
- debug 빌드에서만 HTTP logging interceptor 적용
- API별 base URL이 달라지는 경우 별도 factory 또는 provider 분리

이번 이슈에서는 `/getItem` 호출에 필요한 최소 공통 factory만 구현했습니다.
