# Firestore 공지사항 운영 가이드

설정 > 공지사항은 Cloud Firestore의 `notices` 컬렉션을 조회합니다. 홈 배너와 장애 중인 기능 화면에 표시하는 긴급 공지는 Firebase Remote Config에서 관리합니다.

## 문서 구조

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | string | 예 | 목록과 상세에 표시할 제목입니다. 공백으로만 작성할 수 없습니다. |
| `body` | string | 예 | 상세에 표시할 본문입니다. 공백으로만 작성할 수 없습니다. |
| `publishedAt` | timestamp | 예 | 게시일입니다. 목록은 이 값을 기준으로 최신순 정렬합니다. |
| `updatedAt` | timestamp | 아니요 | 본문을 수정한 시각입니다. |
| `isPublished` | boolean | 예 | `true`인 문서만 앱에서 조회합니다. |

문서 ID에는 운영자가 식별하기 쉬운 고유값을 사용합니다. 앱은 문서 ID를 공지 식별자로 사용하므로 게시한 문서의 ID를 바꾸지 않습니다.

## 게시와 수정

1. Firebase Console에서 `notices` 컬렉션에 문서를 만들고 `isPublished`를 `false`로 둡니다.
2. 필수 필드와 timestamp 타입을 확인합니다.
3. 게시할 때 `isPublished`를 `true`로 바꿉니다.
4. 게시 후 제목이나 본문을 수정하면 `updatedAt`도 현재 시각으로 바꿉니다.
5. 공지를 숨기려면 `isPublished`를 `false`로 바꿉니다.

사용자가 공지사항 화면에 들어가거나 오류 화면에서 재시도하면 Firestore를 다시 조회합니다. 따라서 게시·수정·숨김은 앱을 새로 릴리즈하지 않아도 다음 조회부터 반영됩니다.

## 접근 제어

`firestore.rules`는 게시 상태인 공지만 앱에서 읽도록 허용하고, 앱의 생성·수정·삭제는 모두 거부합니다. 공지 작성은 Firebase Console이나 권한이 있는 Admin SDK에서만 합니다.

Security Rules 변경은 코드 병합만으로 Firebase 프로젝트에 반영되지 않습니다. 검토가 끝난 규칙을 별도로 배포한 뒤 게시 공지와 비공개 공지의 조회 결과를 확인합니다.
