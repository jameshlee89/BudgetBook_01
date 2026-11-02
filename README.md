# 공유 가계부 (BudgetBook)

카드 승인 문자를 자동으로 기입하고, 월별 도넛 그래프와 내역 리스트로 보여주며, 구글 계정으로 다른 사람과 함께 쓰는 Android 가계부 앱.

- 사양서: [docs/SPEC.md](docs/SPEC.md)
- 테스트 사양: [docs/TEST.md](docs/TEST.md)
- 추적성 매트릭스: [docs/TRACEABILITY.md](docs/TRACEABILITY.md) (자동 생성)

## 빌드 준비

1. Android Studio (JDK 17 포함) 로 이 폴더를 연다. Gradle wrapper jar 가 없으면 터미널에서 `gradle wrapper --gradle-version 8.9` 를 한 번 실행한다.
2. [Firebase 콘솔](https://console.firebase.google.com)에서 프로젝트를 만들고 Android 앱(`com.budgetbook.app`)을 등록한다.
   - 디버그 키 SHA-1 등록: `./gradlew signingReport`
   - `google-services.json` 을 받아 `app/` 에 넣는다.
3. Authentication → 로그인 방법 → **Google** 사용 설정.
4. Firestore Database 생성 후 [firestore.rules](firestore.rules) 배포: `firebase deploy --only firestore:rules`

## 테스트와 추적성

```bash
./gradlew testDebugUnitTest      # 단위 테스트 (함수명에 TC ID 포함)
python3 tools/trace_check.py     # UR→REQ→코드→TC→테스트 코드 검사 + TRACEABILITY.md 갱신
```

요구사항을 바꿀 때: `SPEC.md` 수정 → 코드에 `[REQ-xxx]` 태그 → `TEST.md` 에 TC 추가 → 테스트 코드 작성 → `trace_check.py` 실행.
