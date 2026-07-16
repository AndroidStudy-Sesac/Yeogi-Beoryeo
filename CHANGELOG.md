# 변경 이력

이 프로젝트의 주요 변경 사항을 기록합니다.

형식은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/)를 따르며,
버전은 [Semantic Versioning](https://semver.org/lang/ko/)을 따릅니다.

## [Unreleased]

## [0.1.1] - 2026-07-16

### Added

- 저장한 지역 가이드 중 홈에 표시할 대표 지역을 직접 고정하는 기능
- 설정의 출처 및 이용조건에서 NAVER 지도 법적 고지와 오픈소스 라이선스를 확인하는 경로

### Changed

- 홈 지역 가이드 대표 지역과 이전 요약을 앱 재실행, 재수집, 로딩, 조회 실패 상황에서도 안정적으로 유지
- 저장한 지역 가이드의 긴 지역명을 최대 2줄로 표시하고 대표 지역 고정 버튼의 터치 영역과 TalkBack 설명 개선
- CI의 기본 검증과 전체 검증에 app 모듈 단위 테스트 추가

### Fixed

- 지역 선택 중 검색 후보 목록과 지역 선택 목록이 동시에 노출되는 문제 수정
- 지역 가이드 후보 상세에서 돌아오면 후보 목록의 스크롤 위치가 초기화되는 문제 수정
- 동일한 지역 가이드 후보가 여러 개 내려오면 날짜 정보가 유효한 최신 항목을 우선 적용해 중복 노출을 줄임
- NAVER 지도 로고 클릭으로 비정상 종료될 수 있는 경로를 차단하고 바텀시트에 로고가 가려지는 문제 수정
- release 테스트와 lint까지 키스토어를 요구하던 서명 검증을 실제 배포 산출물 작업에만 적용하도록 수정

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

[Unreleased]: https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/compare/v0.1.1...HEAD
[0.1.1]: https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/AndroidStudy-Sesac/Yeogi-Beoryeo/releases/tag/v0.1.0
