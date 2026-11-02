# 공유 가계부 앱 테스트 사양서 (TEST)

| 항목 | 내용 |
|---|---|
| 문서 버전 | 0.1.0 |
| 기준 사양 | [SPEC.md](SPEC.md) 0.1.0 |
| 추적성 | [TRACEABILITY.md](TRACEABILITY.md) (`python3 tools/trace_check.py` 로 생성) |

## 1. 테스트 유형과 실행 방법

| 유형 | 설명 | 실행 |
|---|---|---|
| Unit | JVM 단위 테스트. 함수명에 TC ID 를 포함한다. | `./gradlew testDebugUnitTest` |
| Manual | 실기기(또는 에뮬레이터)에서 사람이 확인 | 아래 절차대로 수행 후 3장 결과표에 기록 |
| Rules | Firestore 보안 규칙. Firebase 에뮬레이터 또는 콘솔 Rules Playground 에서 확인 | `firebase emulators:start --only firestore` |

- 추적성 검사: `python3 tools/trace_check.py` — 단위 테스트를 먼저 실행했다면 `app/build/test-results` 의 결과를 읽어 TC 별 통과/실패를 매트릭스에 반영한다.
- 에뮬레이터에서 문자 수신 시험: Android Studio > Extended Controls > Phone > SMS, 또는 `adb emu sms send 15447200 "<본문>"`.

## 2. 테스트 케이스

표의 **REQ** 열이 추적 대상 요구사항이다. Unit 의 **위치** 열은 테스트 클래스이다.

### 2.1 문자 자동 기입 (SMS)

| TC ID | REQ | 유형 | 테스트 내용 | 기대 결과 | 위치 |
|---|---|---|---|---|---|
| TC-SMS-001-01 | REQ-SMS-001 | Unit | 신한카드 승인 문자를 기입기에 넣는다 | 현재 가계부에 금액·가맹점·항목·SMS 출처로 1건 저장 | SmsIngestorTest |
| TC-SMS-001-02 | REQ-SMS-001 | Manual | 앱을 종료한 상태에서 `adb emu sms send 15447200 "[Web발신] 신한카드(1234)승인 홍*동 12,500원(일시불)11/02 12:34 스타벅스 누적1,234,567원"` | 앱을 열면 내역 탭에 스타벅스 12,500원(카페/간식)이 있다 | - |
| TC-SMS-001-03 | REQ-SMS-001 | Unit | 로그인 안 됨 / 가계부 없음 / 일반 문자 | 각각 NOT_SIGNED_IN / NO_LEDGER / NOT_CARD_SMS, 저장 없음 | SmsIngestorTest |
| TC-SMS-002-01 | REQ-SMS-002 | Unit | 신한카드 단문(누적 포함) | 카드명(1234)·12,500·11/02 12:34·가맹점(공백 포함)·일시불 | CardSmsParserTest |
| TC-SMS-002-02 | REQ-SMS-002 | Unit | 삼성카드 장문 | 45,000·교촌치킨 | CardSmsParserTest |
| TC-SMS-002-03 | REQ-SMS-002 | Unit | KB국민 (가맹점이 일시 다음 줄) | KB국민카드(1234)·8,900·GS25역삼점 | CardSmsParserTest |
| TC-SMS-002-04 | REQ-SMS-002 | Unit | 현대카드 (카드 뒤 등급 표기) | 현대카드·120,000·이마트 성수점 | CardSmsParserTest |
| TC-SMS-002-05 | REQ-SMS-002 | Unit | 롯데·하나·우리·NH·BC 형식 | 카드명·금액·가맹점 모두 정확 | CardSmsParserTest |
| TC-SMS-002-06 | REQ-SMS-002 | Unit | 할부(03개월) 문자 | installmentMonths=3 | CardSmsParserTest |
| TC-SMS-002-07 | REQ-SMS-002 | Unit | 12/31 승인 문자를 다음 해 1/1 에 수신 | 연도 = 전년도 | CardSmsParserTest |
| TC-SMS-002-08 | REQ-SMS-002 | Unit | 체크카드 문자 | 신한체크(1234) | CardSmsParserTest |
| TC-SMS-002-09 | REQ-SMS-002 | Unit | 가맹점 없는 문자 | `미확인 가맹점` | CardSmsParserTest |
| TC-SMS-003-01 | REQ-SMS-003 | Unit | `승인취소` / `취소` 문자 | isCancel=true, 금액·가맹점 추출 | CardSmsParserTest |
| TC-SMS-003-02 | REQ-SMS-003 | Unit | 지출과 취소가 섞인 월 합계 | 취소만큼 차감, 0 이 된 항목은 제외 | MonthlySummaryCalculatorTest |
| TC-SMS-003-03 | REQ-SMS-003 | Manual | 취소 문자 수신 후 내역 탭 확인 | `취소 +금액` 파란색 표시, 통계 총액 감소 | - |
| TC-SMS-004-01 | REQ-SMS-004 | Unit | 개인 문자, 인증번호, 택배(금액·일시 포함) | null | CardSmsParserTest |
| TC-SMS-004-02 | REQ-SMS-004 | Unit | 결제예정·청구 안내 | null | CardSmsParserTest |
| TC-SMS-004-03 | REQ-SMS-004 | Unit | 누적/잔액 금액이 먼저 나오는 문자 | 결제 금액만 추출 | CardSmsParserTest |
| TC-SMS-004-04 | REQ-SMS-004 | Unit | 승인거절 문자 | null | CardSmsParserTest |
| TC-SMS-004-05 | REQ-SMS-004 | Unit | (광고) 문자 | null | CardSmsParserTest |
| TC-SMS-005-01 | REQ-SMS-005 | Manual | 권한 허용 후 문자함에 이번 달 카드 문자 3건이 있는 상태에서 "문자함에서 가져오기" | `문자 N건 확인 · 3건 추가 · 중복 0건` 표시, 리스트에 3건 | - |
| TC-SMS-005-02 | REQ-SMS-005 | Manual | 이전 달로 이동 후 가져오기 | 이전 달 문자만 기입 | - |
| TC-SMS-006-01 | REQ-SMS-006 | Unit | 같은 문자를 다른 수신 시각으로 해석 | 같은 거래 ID | SmsIngestorTest |
| TC-SMS-006-02 | REQ-SMS-006 | Unit | 금액/취소 여부만 다른 문자, 수동 ID 2개 | 서로 다른 ID | SmsIngestorTest |
| TC-SMS-006-03 | REQ-SMS-006 | Unit | 기입 → 항목 수정 → 같은 문자 재수신 | DUPLICATE, 1건 유지, 수정 내용 유지 | SmsIngestorTest |
| TC-SMS-006-04 | REQ-SMS-006 | Manual | 실시간 수신된 내역이 있는 달에 "문자함에서 가져오기" → 1건 삭제 → 다시 가져오기 | 첫 가져오기 중복으로 집계, 삭제한 내역은 되살아나지 않음 | - |
| TC-SMS-007-01 | REQ-SMS-007 | Unit | 항목별 대표 가맹점 12종 | 기대 항목으로 분류 | CategoryClassifierTest |
| TC-SMS-007-02 | REQ-SMS-007 | Unit | 쿠팡이츠 / 쿠팡 | 식비 / 쇼핑 | CategoryClassifierTest |
| TC-SMS-007-03 | REQ-SMS-007 | Unit | 키워드 없는 가맹점 | 기타 | CategoryClassifierTest |
| TC-SMS-007-04 | REQ-SMS-007 | Unit | KTX / KT / CU / CUBE | 교통 / 통신 / 생활 / 기타 | CategoryClassifierTest |
| TC-SMS-008-01 | REQ-SMS-008 | Manual | 첫 로그인 후 권한 대화상자에서 "거부" | 상단 경고 배너와 "허용" 버튼 표시, 누르면 다시 요청 | - |
| TC-SMS-008-02 | REQ-SMS-008 | Manual | 권한 거부 상태에서 수동 추가·통계·공유 사용, "문자함에서 가져오기" 누름 | 다른 기능 정상, 가져오기는 권한 요청 | - |

### 2.2 월별 도넛 그래프 (CHART)

| TC ID | REQ | 유형 | 테스트 내용 | 기대 결과 | 위치 |
|---|---|---|---|---|---|
| TC-CHART-001-01 | REQ-CHART-001 | Unit | 식비 2건, 카페 1건 | 항목별 합계·총액 정확 | MonthlySummaryCalculatorTest |
| TC-CHART-001-02 | REQ-CHART-001 | Manual | 서로 다른 항목 3건 이상 입력 후 통계 탭 | 항목 색으로 나뉜 도넛, 조각 크기가 비율과 일치 | - |
| TC-CHART-001-03 | REQ-CHART-001 | Unit | 10/31 23:59, 12/01 00:00 거래 포함 | 11월 합계에서 제외 | MonthlySummaryCalculatorTest |
| TC-CHART-001-04 | REQ-CHART-001 | Unit | 거래 없음 | total=0, 조각 없음 | MonthlySummaryCalculatorTest |
| TC-CHART-002-01 | REQ-CHART-002 | Manual | 통계 탭에서 이전/다음 달 이동 후 내역 탭으로 전환 | 그래프·리스트가 같은 월로 바뀜, 첫 화면은 이번 달 | - |
| TC-CHART-003-01 | REQ-CHART-003 | Unit | 1,000 / 6,000 / 3,000 | 금액 내림차순, 비율 합 1.0, 첫 비율 0.6 | MonthlySummaryCalculatorTest |
| TC-CHART-003-02 | REQ-CHART-003 | Unit | 금액 표기 | `12,500원`, `0원`, `1,234,567원` | MonthlySummaryCalculatorTest |
| TC-CHART-003-03 | REQ-CHART-003 | Manual | 통계 탭 범례 확인 | 가운데 총지출, 범례에 색·이름·%(소수1자리)·금액 | - |

### 2.3 내역 리스트 / 수정 (LIST)

| TC ID | REQ | 유형 | 테스트 내용 | 기대 결과 | 위치 |
|---|---|---|---|---|---|
| TC-LIST-001-01 | REQ-LIST-001 | Manual | 서로 다른 일시 3건 입력 후 내역 탭 | 최신순 정렬, 각 행에 일시·항목·카드·금액, 상단 건수/합계 | - |
| TC-LIST-002-01 | REQ-LIST-002 | Manual | 문자로 기입된 행을 눌러 금액·항목·메모 변경 후 저장 | 리스트·통계에 반영, `수정됨` 표시 | - |
| TC-LIST-003-01 | REQ-LIST-003 | Manual | 수정 창에서 "이 내역 삭제" → 확인 | 리스트·통계에서 사라짐 | - |
| TC-LIST-004-01 | REQ-LIST-004 | Manual | + 버튼으로 현금 8,000원 식비 추가 | 리스트에 `수동` 표시로 추가, 통계 반영 | - |
| TC-LIST-005-01 | REQ-LIST-005 | Unit | 올바른 입력(쉼표·앞뒤 공백) | Valid, 정리된 값 | TransactionValidatorTest |
| TC-LIST-005-02 | REQ-LIST-005 | Unit | 금액 abc / 0 / 음수 / 10억 초과 / 10억 | 앞의 4개 오류, 10억 통과 | TransactionValidatorTest |
| TC-LIST-005-03 | REQ-LIST-005 | Unit | 사용처 공백·51자, 메모 201자 | 각 1개 오류 | TransactionValidatorTest |
| TC-LIST-005-04 | REQ-LIST-005 | Unit | 일시 형식 오류, 13월, 빈 값 | 각 1개 오류 | TransactionValidatorTest |
| TC-LIST-005-05 | REQ-LIST-005 | Unit | 금액·사용처·일시 모두 오류 | 오류 3개 동시 보고 | TransactionValidatorTest |
| TC-LIST-005-06 | REQ-LIST-005 | Manual | 수정 창에서 금액을 비우고 저장 | 창이 닫히지 않고 빨간 오류 문구 표시 | - |

### 2.4 공유 / 로그인 (SHARE)

| TC ID | REQ | 유형 | 테스트 내용 | 기대 결과 | 위치 |
|---|---|---|---|---|---|
| TC-SHARE-001-01 | REQ-SHARE-001 | Manual | 첫 실행 → "Google 계정으로 로그인" → 계정 선택 | 메인 화면으로 이동 | - |
| TC-SHARE-001-02 | REQ-SHARE-001 | Manual | 로그인 후 앱 강제 종료 → 재실행 | 로그인 화면 없이 메인 화면 | - |
| TC-SHARE-002-01 | REQ-SHARE-002 | Manual | 공유 탭 "로그아웃" → 카드 문자 수신 | 로그인 화면으로 이동, 문자는 기입되지 않음 | - |
| TC-SHARE-003-01 | REQ-SHARE-003 | Manual | 새 계정으로 최초 로그인 | `내 가계부` 1개 자동 생성(재로그인해도 중복 생성 없음) | - |
| TC-SHARE-003-02 | REQ-SHARE-003 | Manual | 공유 탭에서 "여행 경비" 생성 | 새 가계부가 현재 가계부로 선택되고 만든 사람 표시 | - |
| TC-SHARE-004-01 | REQ-SHARE-004 | Unit | 초대 코드 1,000개 생성 | 모두 6자리, 허용 문자만, 0/O/1/I 없음 | InviteCodeGeneratorTest |
| TC-SHARE-004-02 | REQ-SHARE-004 | Unit | 초대 코드 1,000개 중복 | 999개 이상 서로 다름 | InviteCodeGeneratorTest |
| TC-SHARE-004-03 | REQ-SHARE-004 | Manual | 공유 탭 "보내기" | 공유 시트에 초대 문구와 코드 | - |
| TC-SHARE-005-01 | REQ-SHARE-005 | Unit | ` abc-234 ` 정규화, 길이·문자 검사 | `ABC234`, 잘못된 형식 거부 | InviteCodeGeneratorTest |
| TC-SHARE-005-02 | REQ-SHARE-005 | Manual | 계정 B 에서 계정 A 의 코드를 소문자로 입력해 참여 | B 의 가계부 목록에 추가, 구성원 2명 표시 | - |
| TC-SHARE-005-03 | REQ-SHARE-005 | Manual | 형식이 틀린 코드 / 없는 코드 입력 | 각각 오류 스낵바 | - |
| TC-SHARE-006-01 | REQ-SHARE-006 | Manual | A·B 두 기기에서 같은 가계부를 연 상태로 A 가 추가·수정·삭제, A 기기에 카드 문자 수신 | B 화면에 수 초 안에 모두 반영 | - |
| TC-SHARE-006-02 | REQ-SHARE-006 | Manual | 새 계정으로 최초 로그인 직후(앱 재시작 없이) 카드 문자 수신 | 수 초 안에 통계·내역에 반영 (기본 가계부 생성 직후 구독 거부 시 재연결) | - |
| TC-SHARE-007-01 | REQ-SHARE-007 | Rules | 비구성원 계정으로 `ledgers/{id}` 와 `transactions` 읽기/쓰기 | 모두 거부 | - |
| TC-SHARE-007-02 | REQ-SHARE-007 | Rules | 구성원(비소유자)이 members 에서 다른 사람 제거, ownerUid/inviteCode 변경 | 거부 | - |
| TC-SHARE-007-03 | REQ-SHARE-007 | Rules | 비구성원이 틀린 joinedWith 로 참여 / 다른 uid 를 추가 / `invites` 목록 조회 | 거부 (올바른 코드로 자기 자신만 추가는 허용) | - |
| TC-SHARE-008-01 | REQ-SHARE-008 | Manual | 가계부 2개에 속한 상태에서 제목을 눌러 B 선택 → 카드 문자 수신 → 앱 재실행 | 문자는 B 에 기입, 재실행 후에도 B 선택 유지 | - |
| TC-SHARE-008-02 | REQ-SHARE-008 | Unit | 기입기에 주는 현재 가계부를 A → B 로 변경 | 이후 문자는 B 에 기입 | SmsIngestorTest |

## 3. 실행 결과

| 실행일 | 범위 | 결과 | 비고 |
|---|---|---|---|
| 2026-11-02 | 추적성 검사 (`tools/trace_check.py`) | 통과 | TRACEABILITY.md 생성 |
| 2026-11-02 | 문자 해석 정규식 사전 점검 | 통과 | JDK 미설치 환경이라 Kotlin 대신 동일 정규식을 Python 으로 옮겨 TC-SMS-002/003/004 입력 23건 확인 |
| 2026-11-03 | Unit (`./gradlew testDebugUnitTest`) | **통과 (39/39 TC)** | AGP 9.5.0 / Gradle 9.6.1 / JDK 25. `assembleDebug` 도 성공. 임시 더미 google-services.json 으로 빌드 |
| 2026-11-03 | Manual: TC-SHARE-001-01, TC-SHARE-003-01, TC-SMS-001-02, TC-SMS-004-01(일반 문자), TC-SMS-007-01, TC-CHART-001-02, TC-CHART-003-03 | 통과 | 에뮬레이터(API 37). 카드 문자 4건 실시간 기입·분류·도넛 반영 확인 |
| 2026-11-03 | Manual: TC-SHARE-006-02 | **결함 발견 → 수정** | 최초 로그인 직후 받은 문자가 앱 재시작 전까지 화면에 안 보임(저장은 정상). Firestore 구독이 거부된 뒤 재연결되지 않던 문제 → 오류 시 재구독하도록 수정. 수정본은 기존 계정으로 실시간 반영만 확인, 최초 로그인 시나리오 재검증 필요 |
| - | 그 외 Manual / Rules | **미실행** | |
