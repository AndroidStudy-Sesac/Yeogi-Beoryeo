# 변경 이력

이 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르며,
버전은 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

## [0.1.0] - 2026-07-14

### Added

- 품목 검색과 품목별 분리배출 방법, 특징, 주의사항 안내
- 현재 위치와 지역 검색을 이용한 주변 수거 장소 지도 조회
- 지역별 배출 요일, 시간, 장소 안내
- 품목, 수거 장소, 지역 가이드 즐겨찾기
- 앱 출처와 개인정보처리방침 확인
- Firebase Crashlytics를 이용한 비정상 종료 및 오류 진단

### Changed

- Google Play 배포 빌드에 R8 코드 최적화, 난독화, 미사용 리소스 축소 적용
- 앱의 출처 및 이용조건 화면에 정부 정보 공식 출처 링크와 정부기관 비제휴 안내 추가

### Security

- 저장소 밖의 업로드 키로 서명된 Google Play 배포용 AAB 생성 절차

[Unreleased]: https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/releases/tag/v0.1.0
