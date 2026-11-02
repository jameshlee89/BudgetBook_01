# 공유 가계부 (BudgetBook)

카드 승인 문자를 자동으로 기입하고, 월별 도넛 그래프와 내역 리스트로 보여주며, 구글 계정으로 다른 사람과 함께 쓰는 Android 가계부 앱.

- 사양서: [docs/SPEC.md](docs/SPEC.md)
- 테스트 사양: [docs/TEST.md](docs/TEST.md)
- 추적성 매트릭스: [docs/TRACEABILITY.md](docs/TRACEABILITY.md) (자동 생성)

## 개발 환경

| 항목 | 버전 |
|---|---|
| Android Studio | 2026.2 이상 (내장 JDK 25 사용) |
| Gradle / Android Gradle Plugin | 9.6.1 / 9.5.0 (Kotlin 내장) |
| Kotlin | 2.4.21 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

## 빌드 준비

전체 흐름: **① Gradle 동기화 → ② Firebase 프로젝트 → ③ 앱 등록 + SHA-1 → ④ Google 로그인 켜기 → ⑤ google-services.json 받기 → ⑥ Firestore 만들기와 규칙 게시 → ⑦ 실행**

④를 먼저 하고 ⑤에서 파일을 받아야 합니다. 순서가 바뀌면 로그인용 정보(웹 클라이언트 ID)가 파일에 들어가지 않습니다.

### ① Android Studio에서 Gradle 동기화

1. 이 폴더를 연 상태에서 오른쪽 위 코끼리 아이콘(**Sync Project with Gradle Files**)을 누릅니다. 상단에 "Gradle project detected / Load" 배너가 보이면 그것을 눌러도 됩니다.
2. 처음에는 Gradle과 라이브러리를 내려받느라 몇 분 걸립니다. 아래쪽 **Build** 창에 `BUILD SUCCESSFUL` 이 나오면 끝입니다.
3. JDK 오류가 나면 **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** 를 `jbr-25 (JetBrains Runtime)` 으로 맞춥니다.

이 단계에서는 `google-services.json` 이 없어도 됩니다. 앱 실행(⑦) 때만 필요합니다.

### ② Firebase 프로젝트 만들기

1. https://console.firebase.google.com 에 앱에서 쓸 구글 계정으로 로그인합니다.
2. **프로젝트 만들기** → 이름(예: `budgetbook`) 입력 → Google Analytics 는 꺼도 됩니다 → 만들기.

### ③ Android 앱 등록과 SHA-1

1. 프로젝트 개요 화면에서 **Android 아이콘(앱 추가)** 을 누릅니다.
2. 입력값

   | 항목 | 값 |
   |---|---|
   | Android 패키지 이름 | `com.budgetbook.app` (정확히 일치해야 함) |
   | 앱 닉네임 | 아무거나 (예: 공유 가계부) |
   | 디버그 서명 인증서 SHA-1 | 아래 명령으로 확인한 값 |

3. SHA-1 확인: 터미널에서 실행합니다(`-J-Duser.language=en` 은 한국어 환경의 keytool 오류를 피하기 위한 옵션).

   ```bash
   "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -J-Duser.language=en \
     -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA1
   ```

   디버그 키는 PC마다 다릅니다. 다른 PC에서 빌드하면 그 PC의 SHA-1도 Firebase **프로젝트 설정 → 내 앱 → 디지털 지문 추가** 에 등록해야 합니다.
4. 이 화면의 "구성 파일 다운로드"와 "SDK 추가" 단계는 **건너뛰고** 콘솔로 돌아갑니다. 파일은 ④ 다음에 받고, SDK 는 이미 코드에 들어 있습니다.

### ④ Google 로그인 켜기

1. 왼쪽 메뉴 **빌드 → Authentication → 시작하기**.
2. **로그인 방법** 탭 → **Google** → 사용 설정 → 프로젝트 지원 이메일 선택 → 저장.

### ⑤ google-services.json 받기

1. 톱니바퀴 → **프로젝트 설정 → 일반** 탭 → 내 앱(`com.budgetbook.app`) → **google-services.json** 다운로드.
2. 받은 파일을 이 프로젝트의 `app/` 폴더에 넣습니다 → `app/google-services.json`.
3. 이 파일은 `.gitignore` 에 들어 있어 GitHub 에 올라가지 않습니다.

### ⑥ Firestore 만들기와 보안 규칙 게시

1. 왼쪽 메뉴 **빌드 → Firestore Database → 데이터베이스 만들기**.
2. 위치는 `asia-northeast3 (서울)`, 모드는 **프로덕션 모드** 를 고릅니다. 위치는 나중에 바꿀 수 없습니다.
3. 만들어지면 **규칙** 탭을 열고, 이 프로젝트의 [firestore.rules](firestore.rules) 내용 전체를 붙여넣은 뒤 **게시** 를 누릅니다.
   (Firebase CLI 가 있다면 `firebase deploy --only firestore:rules` 로도 됩니다.)

### ⑦ 실행

1. 기기 준비
   - **에뮬레이터**: Device Manager 에서 Google Play 이미지가 들어간 기기(예: `Medium Phone API 37`)를 고르고, 에뮬레이터 안의 설정에서 구글 계정을 추가합니다.
   - **실제 폰**: 개발자 옵션 → USB 디버깅을 켜고 연결합니다.
2. 위쪽 실행 대상에서 `app` 과 기기를 고르고 ▶(Run)를 누릅니다.
3. 앱에서 **Google 계정으로 로그인** → 문자 권한 **허용**.
4. 카드 문자 테스트(에뮬레이터): 에뮬레이터 옆 `⋯` → **Phone → SMS** 에 아래 문구를 넣고 Send.

   ```
   [Web발신]
   신한카드(1234)승인 홍*동 12,500원(일시불)11/02 12:34 스타벅스 누적1,234,567원
   ```

   날짜(`11/02`)는 오늘 날짜로 바꾸면 이번 달 화면에 바로 나옵니다.

### 문제 해결

| 증상 | 원인과 조치 |
|---|---|
| 빌드 시 `File google-services.json is missing` | ⑤ 파일이 `app/` 에 없음 |
| 빌드 시 `No matching client found for package name` | Firebase 에 등록한 패키지 이름이 `com.budgetbook.app` 과 다름 |
| 로그인 시 `No credentials available` | 기기에 구글 계정이 없음 → 앱이 구글 로그인 화면을 대신 띄운다. 그래도 실패하면 기기 **설정 → Google** 에서 계정을 추가하고 Play 스토어를 한 번 열어 Play 서비스를 갱신 |
| 로그인 시 `DEVELOPER_ERROR`, `[10]`, `[16]` 등 | SHA-1 미등록·불일치, 또는 ④ 이전에 받은 json 사용 → SHA-1 확인 후 json 다시 받기 |
| `default_web_client_id` 를 찾을 수 없음 | json 에 웹 클라이언트 정보가 없음 → ④ 후 json 다시 받기 |
| 로그인 후 "기본 가계부 생성 실패: PERMISSION_DENIED" | ⑥ 보안 규칙이 게시되지 않음 |
| 문자가 기입되지 않음 | 문자 권한 거부(상단 배너의 "허용"), 또는 지원하지 않는 형식 → 내역 탭 "문자함에서 가져오기"로 재시도 |

## 테스트와 추적성

```bash
./gradlew testDebugUnitTest      # 단위 테스트 (함수명에 TC ID 포함)
python3 tools/trace_check.py     # UR→REQ→코드→TC→테스트 코드 검사 + TRACEABILITY.md 갱신
```

- 터미널에서 `./gradlew` 를 쓰려면 Android Studio 내장 JDK 를 지정합니다:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- 단위 테스트도 빌드 과정에서 `google-services.json` 이 필요합니다.
- 요구사항을 바꿀 때: `SPEC.md` 수정 → 코드에 `[REQ-xxx]` 태그 → `TEST.md` 에 TC 추가 → 테스트 코드 작성 → `trace_check.py` 실행.
