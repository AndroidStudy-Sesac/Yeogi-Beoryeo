# Gradle Dependency Verification 운영 기준

이 문서는 의존성이나 build plugin을 변경할 때 Gradle dependency verification metadata를 갱신하고 검토하는 기준을 설명합니다.

## 관리 파일

1. `gradle/verification-metadata.xml`은 artifact의 checksum과 신뢰할 PGP key 범위를 기록합니다.
2. `gradle/verification-keyring.keys`는 signature 검증에 사용하는 public key만 포함합니다. private key나 인증 정보는 저장하지 않습니다.
3. 의존성이나 plugin을 변경한 PR에는 두 파일의 변경 여부를 함께 확인합니다.

## 갱신 절차

1. version catalog나 build script에서 dependency 또는 plugin version을 변경합니다.
2. 변경된 dependency를 사용하는 build, test, lint task를 선택합니다. 공통 의존성이나 build plugin을 바꿨다면 전체 CI task를 사용합니다.
3. 먼저 `SHA-256` metadata를 갱신합니다.
4. 같은 task로 PGP metadata와 public keyring을 갱신합니다.
5. 갱신 flag 없이 같은 task를 다시 실행해 strict verification과 기존 test가 통과하는지 확인합니다.
6. dependency 변경과 verification 파일 diff를 같은 PR에서 검토합니다.

Windows에서 전체 CI 범위를 확인할 때 사용할 수 있는 기본 task는 다음과 같습니다.

```powershell
$tasks = @(
  ':common:testDebugUnitTest'
  ':data:testDebugUnitTest'
  ':presentation:testDebugUnitTest'
  ':domain:test'
  ':app:testDebugUnitTest'
  ':app:assembleDebug'
  ':app:lintDebug'
  ':common:lintDebug'
  ':data:lintDebug'
  ':presentation:lintDebug'
)

.\gradlew.bat $tasks --no-daemon --stacktrace --write-verification-metadata sha256
.\gradlew.bat $tasks --no-daemon --stacktrace --write-verification-metadata pgp,sha256 --export-keys
.\gradlew.bat $tasks --no-daemon --stacktrace
```

release나 instrumented test dependency에 영향을 주는 변경은 관련 CI task도 같은 명령에 추가합니다.

## Android Studio source artifact 처리

1. 이 프로젝트에서는 Android Studio가 project import 중 자동으로 받는 `*-sources.jar`와 `*-javadoc.jar`를 build와 runtime classpath에 사용하지 않습니다.
2. `trusted-artifacts`는 Gradle 공식 문서가 안내하는 두 파일명 정규식으로 제한합니다. 이 예외는 resolution 경로와 관계없이 같은 파일명의 artifact를 전역 신뢰하므로 checksum과 signature 검증을 수행하지 않습니다.
3. dependency diff에서 같은 파일명의 artifact가 build나 runtime classpath에 들어오지 않는지 확인합니다. 들어온다면 정규식 예외를 제거하고 영향을 받는 artifact의 정확한 checksum을 기록합니다.
4. `gradle-9.4.1-src.zip`과 같은 Gradle source distribution은 공식 checksum을 확인한 뒤 정확한 artifact checksum을 기록합니다.
5. clean cache에서 Android Studio Gradle sync와 compile task를 함께 확인합니다.

## Diff 검토 기준

초기 metadata는 현재 신뢰한 dependency set을 기준으로 생성한 baseline입니다. 이후 변경부터 새 artifact와 신뢰 근거를 함께 검토합니다.

1. 변경한 dependency의 group, name, version과 실제로 추가된 artifact가 일치하는지 확인합니다.
2. 예상하지 않은 dependency나 넓은 범위의 `trusted-key`가 추가되지 않았는지 확인합니다.
3. 직접 dependency와 build plugin은 transitive dependency보다 먼저 공식 신뢰 근거를 확인합니다.
4. PGP signature가 있으면 publisher의 공식 문서, 공식 저장소, release 안내에서 key fingerprint를 대조합니다.
5. publisher가 공식 checksum을 제공하면 같은 version과 artifact의 checksum인지 대조합니다.
6. 별도 signature나 공식 checksum이 없으면 공식 artifact repository에서 받은 파일의 checksum을 기준값으로 기록합니다. 이 값은 이후 byte 변경을 탐지하지만 publisher를 독립적으로 확인한 근거는 아닙니다.
7. `ignored-key`에 해당하는 signature를 사용하는 artifact도 `SHA-256` checksum이 있어야 합니다.
8. `gradle/verification-keyring.keys`에 private key marker가 없는지 확인합니다.

## Dependabot Security Update 처리

Dependabot security update PR은 저장소의 기본 branch인 `main`을 대상으로 생성됩니다.

1. Dependabot이 제안한 dependency 변경을 `develop` 기준 작업 branch에 먼저 적용합니다.
2. 이 문서의 갱신 절차에 따라 verification metadata와 public keyring을 함께 갱신합니다.
3. CI와 review를 통과한 변경을 `develop`에 반영합니다.
4. 기존 release PR 흐름으로 `main`에 반영합니다.
5. `main` 반영 뒤 Dependabot PR과 관련 alert가 해결됐는지 확인합니다.

## CI에서 Verification 실패 시

1. 실패한 artifact의 group, name, version, classifier와 repository를 확인합니다.
2. dependency 변경에서 예상한 artifact인지 먼저 확인합니다.
3. 운영체제별 artifact가 누락됐다면 해당 운영체제의 공식 repository artifact를 확인하고 checksum을 추가합니다.
4. Windows에서 생성한 metadata에 Linux용 AAPT2 artifact가 없었던 #471처럼, 로컬과 CI 운영체제가 다르면 platform classifier를 함께 확인합니다.
5. build와 runtime classpath에 사용하는 artifact는 검증을 끄거나 wildcard trust 범위를 추가하지 않습니다. `trusted-artifacts` 예외는 앞에서 정한 두 파일명 정규식으로만 제한합니다.
6. 신뢰 근거를 확인할 수 없으면 metadata를 갱신하지 않고 dependency 변경을 보류합니다.

## 공식 문서

1. [Gradle Dependency Verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
2. [Gradle 9.4.1 Source Distribution SHA-256](https://services.gradle.org/distributions/gradle-9.4.1-src.zip.sha256)
3. [GitHub Dependabot Pull Requests](https://docs.github.com/en/code-security/concepts/supply-chain-security/dependabot-pull-requests)
