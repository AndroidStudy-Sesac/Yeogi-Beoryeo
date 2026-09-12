# Google Play 한국어 스토어 등록정보

‘내 주변에서 소형 가전을 무료로 배출할 수 있는 장소’를 중심으로 앱을 소개합니다. 아래 문구와 이미지를 Google Play 스토어 등록정보의 각 항목에 사용합니다.

## 등록 문구

| 등록 항목      | 파일                                             | 글자 수         |
| ---------- | ---------------------------------------------- | -----------: |
| 앱 이름       | [title.txt](title.txt)                         | 13           |
| 간단한 설명     | [short-description.txt](short-description.txt) | 30           |
| 자세한 설명     | [full-description.txt](full-description.txt)   | 2,216        |
| 이미지 대체 텍스트 | [alt-text.txt](alt-text.txt)                   | 이미지별 140자 이내 |

앱 이름은 ‘여기버려 소형가전 수거함’으로, 소형 가전을 배출할 수 있는 가까운 수거함의 위치를 찾는 기능을 설명합니다. 간단한 설명은 ‘소형 가전을 버릴 수 있는 내 주변 수거함을 찾아보세요’로 등록합니다.

앱 이름은 공백을 포함해 13자이며, [Google Play 한국어 등록정보 안내](https://support.google.com/googleplay/android-developer/answer/9859152?hl=ko)의 한글 15자 기준에 맞췄습니다. [메타데이터 정책](https://support.google.com/googleplay/android-developer/answer/9898842?hl=ko)에 따라 제목에 가격이나 프로모션으로 읽힐 수 있는 표현을 넣지 않았습니다. 간단한 설명도 [등록정보 미리보기 안내](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko)에 맞춰 핵심 기능을 설명합니다.

가까운 중소형 폐가전 수거함을 찾아 직접 가져가 배출하는 흐름을 먼저 설명합니다. 수거함마다 배출 가능한 품목과 이용 대상이 다를 수 있다는 안내를 포함하며, 대형 폐가전 무상방문수거는 별도로 설명합니다. 자세한 설명에는 비정부 앱 고지와 정부 및 공식 정보 출처를 함께 유지합니다.

## 휴대전화 스크린샷

아래 순서로 등록합니다. 각 파일은 1080 × 1920 px의 불투명 RGB PNG이며, 실제 앱의 라이트 모드 화면에 Pixel 9 Pro 프레임을 적용했습니다.

| 순서  | 이미지                                                        | 소개 내용                              |
| --- | ---------------------------------------------------------- | ---------------------------------- |
| 1   | [01-nearby-free.png](screenshots/01-nearby-free.png)       | 내 주변의 무료 가전 배출 장소를 지도에서 찾습니다.      |
| 2   | [02-disposal-guide.png](screenshots/02-disposal-guide.png) | 가져가기 전에 소형 가전의 배출 방법과 주의사항을 확인합니다. |
| 3   | [03-save-place.png](screenshots/03-save-place.png)         | 찾아둔 수거 장소를 즐겨찾기에 저장합니다.            |
| 4   | [04-more-guides.png](screenshots/04-more-guides.png)       | 홈에서 품목별 분리배출 정보와 지역 안내를 확인합니다.     |
| 5   | [05-region-guide.png](screenshots/05-region-guide.png)     | ‘안내’ 탭에서 지역별 배출 요일, 시간과 방법을 확인합니다. |

## 그래픽 이미지

[feature-graphic.png](feature-graphic.png)는 스토어 등록정보의 ‘그래픽 이미지’ 항목에 등록합니다. 휴대전화 스크린샷과 별도로 사용하는 1024 × 500 px의 불투명 RGB PNG입니다.

‘소형 가전, 무료로 버릴 곳을 내 주변에서’라는 문구와 선풍기, 전기밥솥, 헤어드라이어 일러스트로 앱의 핵심 용도를 소개합니다.

## 화면과 자료 출처

- 지도, 중소형 폐가전 안내와 장소 즐겨찾기는 네이티브 Android 앱 1.2.0 debug 빌드에서 캡처했습니다. 지도는 서울시청의 공개 예시 좌표를 기준으로 실제 조회한 수거함을 보여줍니다.
- 홈과 지역 안내는 배포한 v1.2.0 AAB에서 생성한 APK를 설치해 캡처했습니다. 소스를 다시 빌드하지 않고 배포 산출물의 코드와 리소스를 사용했습니다. 지역 안내는 경기도 양평군 양평읍 공흥1리의 실제 조회 결과를 보여줍니다.
- Android SDK의 공식 Pixel 9 Pro 스킨으로 기기 프레임을 적용했습니다. 화면의 글자와 데이터를 다시 그리거나 편집하지 않았습니다.
- 그래픽 이미지의 가전 일러스트는 이미지 생성 도구로 제작했습니다. 추가 문구에는 Pretendard를 사용했으며, 글꼴 라이선스는 [THIRD_PARTY_NOTICES.md](../../../THIRD_PARTY_NOTICES.md)에 기재되어 있습니다.
- 무료 배출 안내는 [인천광역시 공식 안내](https://www.incheon.go.kr/recycle/RC100300/3069041)를 참고했습니다. 앱의 공공데이터와 공식 안내 출처는 [DATA_SOURCES.md](../../../DATA_SOURCES.md)에서도 확인할 수 있습니다.
- 이미지 규격과 대체 텍스트 기준은 [Google Play 공식 안내](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko)를 따릅니다.

## 배포 빌드 화면 검증

2026년 9월 12일, 배포한 [v1.2.0](https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/tree/v1.2.0) AAB에서 bundletool로 APK를 생성해 Pixel 9 Pro Android Emulator에 설치했습니다. 패키지 `com.team.yeogibeoryeo`, 버전 이름 `1.2.0`, 버전 코드 `10`과 업로드 인증서의 일치를 확인했습니다.

- 홈은 최초 사용 가이드를 건너뛴 뒤 안내 카드, 품목 검색과 분리배출 분류가 보이는 화면을 캡처했습니다. 기존 소개 이미지와 표시 문구 및 구성 요소의 위치가 일치하는지 확인했습니다.
- 지역 안내는 ‘안내’ 탭에서 경기도, 양평군, 양평읍을 차례로 선택하고 조회한 뒤 공흥1리를 선택했습니다. 일반쓰레기와 음식물쓰레기의 배출 요일, 시간과 방법을 직접 확인하고, 스크롤하여 요일과 시간이 보이는 화면을 캡처했습니다. 공흥1리의 조회 결과이며 양평읍 전체의 공통 배출 기준을 뜻하지 않습니다.
- 두 화면 모두 1280 × 2856 px, 밀도 480 dpi, 글자 크기 1.0의 라이트 모드에서 캡처했습니다. 원본 화면에 공식 Pixel 9 Pro 프레임을 적용한 뒤 1080 × 1920 px 소개 이미지로 저장했습니다.

사용한 AAB의 SHA-256은 `5b9e7000a6e8a204e39e04b2ba36aca172fadea9c06a72b1d0f806751b8b7677`입니다. 스토어 이미지 4번과 5번에는 위 배포 산출물에서 새로 캡처한 화면을 사용합니다.
