# Google Play 한국어 스토어 등록정보

‘내 주변에서 소형 가전을 무료로 배출할 수 있는 장소’를 중심으로 앱을 소개합니다. 아래 문구와 이미지를 Google Play 스토어 등록정보의 각 항목에 사용합니다.

## 등록 문구

| 등록 항목 | 파일 | 글자 수 |
| --- | --- | ---: |
| 앱 이름 | [title.txt](title.txt) | 17 |
| 간단한 설명 | [short-description.txt](short-description.txt) | 35 |
| 자세한 설명 | [full-description.txt](full-description.txt) | 2,216 |
| 이미지 대체 텍스트 | [alt-text.txt](alt-text.txt) | 이미지별 140자 이내 |

앱 이름은 ‘여기버려 소형 가전 수거함 찾기’로, 가까운 수거함의 위치를 찾는 기능을 설명합니다. 간단한 설명은 ‘소형 가전을 무료로 버릴 수 있는 내 주변 수거함을 찾아보세요.’로 등록합니다.

가까운 중소형 폐가전 수거함을 찾아 직접 가져가 배출하는 흐름을 먼저 설명합니다. 수거함마다 배출 가능한 품목과 이용 대상이 다를 수 있다는 안내를 포함하며, 대형 폐가전 무상방문수거는 별도로 설명합니다. 자세한 설명에는 비정부 앱 고지와 정부 및 공식 정보 출처를 함께 유지합니다.

## 휴대전화 스크린샷

아래 순서로 등록합니다. 각 파일은 1080 × 1920 px의 불투명 RGB PNG이며, 실제 앱의 라이트 모드 화면에 Pixel 9 Pro 프레임을 적용했습니다.

| 순서 | 이미지 | 소개 내용 |
| --- | --- | --- |
| 1 | [01-nearby-free.png](screenshots/01-nearby-free.png) | 내 주변의 무료 가전 배출 장소를 지도에서 찾습니다. |
| 2 | [02-disposal-guide.png](screenshots/02-disposal-guide.png) | 가져가기 전에 소형 가전의 배출 방법과 주의사항을 확인합니다. |
| 3 | [03-save-place.png](screenshots/03-save-place.png) | 찾아둔 수거 장소를 즐겨찾기에 저장합니다. |
| 4 | [04-more-guides.png](screenshots/04-more-guides.png) | 홈에서 품목별 분리배출 정보와 지역 안내를 확인합니다. |
| 5 | [05-region-guide.png](screenshots/05-region-guide.png) | ‘안내’ 탭에서 지역별 배출 요일, 시간과 방법을 확인합니다. |

## 그래픽 이미지

[feature-graphic.png](feature-graphic.png)는 스토어 등록정보의 ‘그래픽 이미지’ 항목에 등록합니다. 휴대전화 스크린샷과 별도로 사용하는 1024 × 500 px의 불투명 RGB PNG입니다.

‘소형 가전, 무료로 버릴 곳을 내 주변에서’라는 문구와 선풍기, 전기밥솥, 헤어드라이어 일러스트로 앱의 핵심 용도를 소개합니다.

## 화면과 자료 출처

- 지도, 중소형 폐가전 안내와 장소 즐겨찾기는 네이티브 Android 앱 1.2.0 debug 빌드에서 캡처했습니다. 지도는 서울시청의 공개 예시 좌표를 기준으로 실제 조회한 수거함을 보여줍니다.
- 홈과 지역 안내는 [#596](https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/pull/596) 검증에 사용한 1.2.0 debug 빌드에서 캡처했습니다. 지역 안내는 경기도 양평군 양평읍 공흥1리의 실제 조회 결과를 보여줍니다.
- Android SDK의 공식 Pixel 9 Pro 스킨으로 기기 프레임을 적용했습니다. 화면의 글자와 데이터를 다시 그리거나 편집하지 않았습니다.
- 그래픽 이미지의 가전 일러스트는 이미지 생성 도구로 제작했습니다. 추가 문구에는 Pretendard를 사용했으며, 글꼴 라이선스는 [THIRD_PARTY_NOTICES.md](../../../THIRD_PARTY_NOTICES.md)에 기재되어 있습니다.
- 무료 배출 안내는 [인천광역시 공식 안내](https://www.incheon.go.kr/recycle/RC100300/3069041)를 참고했습니다. 앱의 공공데이터와 공식 안내 출처는 [DATA_SOURCES.md](../../../DATA_SOURCES.md)에서도 확인할 수 있습니다.
- 이미지 규격과 대체 텍스트 기준은 [Google Play 공식 안내](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko)를 따릅니다.
