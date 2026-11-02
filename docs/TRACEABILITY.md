# 추적성 매트릭스 (TRACEABILITY)

> `tools/trace_check.py` 가 자동 생성한 문서입니다. 직접 수정하지 마세요. (생성일 2026-11-02)

- 검사 결과: **통과**
- UR 5개 · REQ 24개 · TC 67개 (Unit 39, Manual 25, Rules 3)
- 단위 테스트 결과 반영: 없음 — `./gradlew testDebugUnitTest` 후 다시 실행

## 1. UR → REQ

| UR | 사용자 요구사항 | REQ |
|---|---|---|
| UR-01 | 문자 수신 내역(카드 승인 내역)을 확인해서 자동으로 기입되는 가계부가 되도록 해줘. | REQ-SMS-001, REQ-SMS-002, REQ-SMS-003, REQ-SMS-004, REQ-SMS-005, REQ-SMS-006, REQ-SMS-007, REQ-SMS-008 |
| UR-02 | 기입된 내역은 월별로 도넛 그래프 형태로 항목별 지출을 볼 수 있게 해줘. | REQ-CHART-001, REQ-CHART-002, REQ-CHART-003 |
| UR-03 | 기입된 내역을 리스트로도 볼 수 있게 해줘. | REQ-LIST-001 |
| UR-04 | 기입된 리스트는 수동으로 수정할 수도 있게 해줘. | REQ-LIST-002, REQ-LIST-003, REQ-LIST-004, REQ-LIST-005 |
| UR-05 | 가계부를 다른 사람과 공유해서 구성할 수도 있게 해줘. (구글 아이디 로그인을 통한 연동이 되도록) | REQ-SHARE-001, REQ-SHARE-002, REQ-SHARE-003, REQ-SHARE-004, REQ-SHARE-005, REQ-SHARE-006, REQ-SHARE-007, REQ-SHARE-008 |

## 2. REQ → 코드 → TC

| REQ | 요구사항 | UR | 구현 위치 | TC (유형:결과) |
|---|---|---|---|---|
| REQ-SMS-001 | 카드 승인 문자 실시간 자동 기입 | UR-01 | app/src/main/AndroidManifest.xml<br>sms/SmsIngestor.kt<br>sms/SmsReceiver.kt | TC-SMS-001-01 (Unit:미실행)<br>TC-SMS-001-02 (Manual)<br>TC-SMS-001-03 (Unit:미실행) |
| REQ-SMS-002 | 카드사 문자 해석 | UR-01 | domain/sms/CardSmsParser.kt | TC-SMS-002-01 (Unit:미실행)<br>TC-SMS-002-02 (Unit:미실행)<br>TC-SMS-002-03 (Unit:미실행)<br>TC-SMS-002-04 (Unit:미실행)<br>TC-SMS-002-05 (Unit:미실행)<br>TC-SMS-002-06 (Unit:미실행)<br>TC-SMS-002-07 (Unit:미실행)<br>TC-SMS-002-08 (Unit:미실행)<br>TC-SMS-002-09 (Unit:미실행) |
| REQ-SMS-003 | 승인 취소 처리 | UR-01 | domain/MonthlySummaryCalculator.kt<br>domain/model/Transaction.kt<br>domain/sms/CardSmsParser.kt | TC-SMS-003-01 (Unit:미실행)<br>TC-SMS-003-02 (Unit:미실행)<br>TC-SMS-003-03 (Manual) |
| REQ-SMS-004 | 카드 승인 문자가 아닌 문자 제외 | UR-01 | domain/sms/CardSmsParser.kt | TC-SMS-004-01 (Unit:미실행)<br>TC-SMS-004-02 (Unit:미실행)<br>TC-SMS-004-03 (Unit:미실행)<br>TC-SMS-004-04 (Unit:미실행)<br>TC-SMS-004-05 (Unit:미실행) |
| REQ-SMS-005 | 문자함 가져오기 | UR-01 | app/src/main/AndroidManifest.xml<br>sms/SmsInboxImporter.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionListScreen.kt | TC-SMS-005-01 (Manual)<br>TC-SMS-005-02 (Manual) |
| REQ-SMS-006 | 중복 기입 방지 | UR-01 | data/FirestoreLedgerRepository.kt<br>data/LedgerRepository.kt<br>domain/TransactionIdGenerator.kt<br>sms/SmsIngestor.kt | TC-SMS-006-01 (Unit:미실행)<br>TC-SMS-006-02 (Unit:미실행)<br>TC-SMS-006-03 (Unit:미실행)<br>TC-SMS-006-04 (Manual) |
| REQ-SMS-007 | 항목 자동 분류 | UR-01 | domain/CategoryClassifier.kt<br>sms/SmsIngestor.kt | TC-SMS-007-01 (Unit:미실행)<br>TC-SMS-007-02 (Unit:미실행)<br>TC-SMS-007-03 (Unit:미실행)<br>TC-SMS-007-04 (Unit:미실행) |
| REQ-SMS-008 | 문자 권한 | UR-01 | ui/BudgetBookRoot.kt | TC-SMS-008-01 (Manual)<br>TC-SMS-008-02 (Manual) |
| REQ-CHART-001 | 월별 항목별 지출 도넛 그래프 | UR-02 | domain/MonthlySummaryCalculator.kt<br>domain/model/Category.kt<br>ui/MainViewModel.kt<br>ui/chart/StatsScreen.kt | TC-CHART-001-01 (Unit:미실행)<br>TC-CHART-001-02 (Manual)<br>TC-CHART-001-03 (Unit:미실행)<br>TC-CHART-001-04 (Unit:미실행) |
| REQ-CHART-002 | 월 이동 | UR-02 | ui/MainViewModel.kt<br>ui/chart/StatsScreen.kt<br>ui/components/MonthSelector.kt | TC-CHART-002-01 (Manual) |
| REQ-CHART-003 | 범례, 비율, 총액 | UR-02 | domain/MoneyFormat.kt<br>domain/MonthlySummaryCalculator.kt<br>ui/chart/StatsScreen.kt | TC-CHART-003-01 (Unit:미실행)<br>TC-CHART-003-02 (Unit:미실행)<br>TC-CHART-003-03 (Manual) |
| REQ-LIST-001 | 월별 내역 리스트 | UR-03 | data/LedgerRepository.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionListScreen.kt | TC-LIST-001-01 (Manual) |
| REQ-LIST-002 | 내역 수정 | UR-04 | data/LedgerRepository.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionEditDialog.kt<br>ui/list/TransactionListScreen.kt | TC-LIST-002-01 (Manual) |
| REQ-LIST-003 | 내역 삭제 | UR-04 | data/LedgerRepository.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionEditDialog.kt | TC-LIST-003-01 (Manual) |
| REQ-LIST-004 | 수동 내역 추가 | UR-04 | data/LedgerRepository.kt<br>ui/BudgetBookRoot.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionEditDialog.kt | TC-LIST-004-01 (Manual) |
| REQ-LIST-005 | 입력 검증 | UR-04 | domain/TransactionValidator.kt<br>ui/MainViewModel.kt<br>ui/list/TransactionEditDialog.kt | TC-LIST-005-01 (Unit:미실행)<br>TC-LIST-005-02 (Unit:미실행)<br>TC-LIST-005-03 (Unit:미실행)<br>TC-LIST-005-04 (Unit:미실행)<br>TC-LIST-005-05 (Unit:미실행)<br>TC-LIST-005-06 (Manual) |
| REQ-SHARE-001 | 구글 계정 로그인 | UR-05 | data/AuthRepository.kt<br>ui/LoginScreen.kt<br>ui/MainViewModel.kt | TC-SHARE-001-01 (Manual)<br>TC-SHARE-001-02 (Manual) |
| REQ-SHARE-002 | 로그아웃 | UR-05 | data/AuthRepository.kt<br>ui/MainViewModel.kt<br>ui/share/ShareScreen.kt | TC-SHARE-002-01 (Manual) |
| REQ-SHARE-003 | 가계부 생성 | UR-05 | data/LedgerRepository.kt<br>domain/model/Ledger.kt<br>ui/MainViewModel.kt<br>ui/share/ShareScreen.kt | TC-SHARE-003-01 (Manual)<br>TC-SHARE-003-02 (Manual) |
| REQ-SHARE-004 | 초대 코드 발급/공유 | UR-05 | domain/InviteCodeGenerator.kt<br>ui/share/ShareScreen.kt<br>firestore.rules | TC-SHARE-004-01 (Unit:미실행)<br>TC-SHARE-004-02 (Unit:미실행)<br>TC-SHARE-004-03 (Manual) |
| REQ-SHARE-005 | 초대 코드로 참여 | UR-05 | data/LedgerRepository.kt<br>domain/InviteCodeGenerator.kt<br>ui/MainViewModel.kt<br>ui/share/ShareScreen.kt<br>firestore.rules | TC-SHARE-005-01 (Unit:미실행)<br>TC-SHARE-005-02 (Manual)<br>TC-SHARE-005-03 (Manual) |
| REQ-SHARE-006 | 구성원 간 실시간 동기화 | UR-05 | data/FirestoreLedgerRepository.kt<br>data/LedgerRepository.kt<br>domain/model/Ledger.kt<br>ui/MainViewModel.kt | TC-SHARE-006-01 (Manual) |
| REQ-SHARE-007 | 접근 제어 | UR-05 | firestore.rules | TC-SHARE-007-01 (Rules)<br>TC-SHARE-007-02 (Rules)<br>TC-SHARE-007-03 (Rules) |
| REQ-SHARE-008 | 현재 가계부 선택 | UR-05 | data/SettingsStore.kt<br>sms/SmsIngestor.kt<br>ui/BudgetBookRoot.kt<br>ui/MainViewModel.kt | TC-SHARE-008-01 (Manual)<br>TC-SHARE-008-02 (Unit:미실행) |

## 3. TC → 테스트 코드

| TC | REQ | 유형 | 내용 | 테스트 코드 | 결과 |
|---|---|---|---|---|---|
| TC-SMS-001-01 | REQ-SMS-001 | Unit | 신한카드 승인 문자를 기입기에 넣는다 | SmsIngestorTest.kt | 미실행 |
| TC-SMS-001-02 | REQ-SMS-001 | Manual | 앱을 종료한 상태에서 `adb emu sms send 15447200 "[Web발신] 신한카드(1234)승인 홍*동 12,500원(일시불)11/02 12:34 스타벅스 누적1,234,567원"` | - | 수동 기록(TEST.md 3장) |
| TC-SMS-001-03 | REQ-SMS-001 | Unit | 로그인 안 됨 / 가계부 없음 / 일반 문자 | SmsIngestorTest.kt | 미실행 |
| TC-SMS-002-01 | REQ-SMS-002 | Unit | 신한카드 단문(누적 포함) | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-02 | REQ-SMS-002 | Unit | 삼성카드 장문 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-03 | REQ-SMS-002 | Unit | KB국민 (가맹점이 일시 다음 줄) | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-04 | REQ-SMS-002 | Unit | 현대카드 (카드 뒤 등급 표기) | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-05 | REQ-SMS-002 | Unit | 롯데·하나·우리·NH·BC 형식 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-06 | REQ-SMS-002 | Unit | 할부(03개월) 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-07 | REQ-SMS-002 | Unit | 12/31 승인 문자를 다음 해 1/1 에 수신 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-08 | REQ-SMS-002 | Unit | 체크카드 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-002-09 | REQ-SMS-002 | Unit | 가맹점 없는 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-003-01 | REQ-SMS-003 | Unit | `승인취소` / `취소` 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-003-02 | REQ-SMS-003 | Unit | 지출과 취소가 섞인 월 합계 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-SMS-003-03 | REQ-SMS-003 | Manual | 취소 문자 수신 후 내역 탭 확인 | - | 수동 기록(TEST.md 3장) |
| TC-SMS-004-01 | REQ-SMS-004 | Unit | 개인 문자, 인증번호, 택배(금액·일시 포함) | CardSmsParserTest.kt | 미실행 |
| TC-SMS-004-02 | REQ-SMS-004 | Unit | 결제예정·청구 안내 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-004-03 | REQ-SMS-004 | Unit | 누적/잔액 금액이 먼저 나오는 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-004-04 | REQ-SMS-004 | Unit | 승인거절 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-004-05 | REQ-SMS-004 | Unit | (광고) 문자 | CardSmsParserTest.kt | 미실행 |
| TC-SMS-005-01 | REQ-SMS-005 | Manual | 권한 허용 후 문자함에 이번 달 카드 문자 3건이 있는 상태에서 "문자함에서 가져오기" | - | 수동 기록(TEST.md 3장) |
| TC-SMS-005-02 | REQ-SMS-005 | Manual | 이전 달로 이동 후 가져오기 | - | 수동 기록(TEST.md 3장) |
| TC-SMS-006-01 | REQ-SMS-006 | Unit | 같은 문자를 다른 수신 시각으로 해석 | SmsIngestorTest.kt | 미실행 |
| TC-SMS-006-02 | REQ-SMS-006 | Unit | 금액/취소 여부만 다른 문자, 수동 ID 2개 | SmsIngestorTest.kt | 미실행 |
| TC-SMS-006-03 | REQ-SMS-006 | Unit | 기입 → 항목 수정 → 같은 문자 재수신 | SmsIngestorTest.kt | 미실행 |
| TC-SMS-006-04 | REQ-SMS-006 | Manual | 실시간 수신된 내역이 있는 달에 "문자함에서 가져오기" → 1건 삭제 → 다시 가져오기 | - | 수동 기록(TEST.md 3장) |
| TC-SMS-007-01 | REQ-SMS-007 | Unit | 항목별 대표 가맹점 12종 | CategoryClassifierTest.kt | 미실행 |
| TC-SMS-007-02 | REQ-SMS-007 | Unit | 쿠팡이츠 / 쿠팡 | CategoryClassifierTest.kt | 미실행 |
| TC-SMS-007-03 | REQ-SMS-007 | Unit | 키워드 없는 가맹점 | CategoryClassifierTest.kt | 미실행 |
| TC-SMS-007-04 | REQ-SMS-007 | Unit | KTX / KT / CU / CUBE | CategoryClassifierTest.kt | 미실행 |
| TC-SMS-008-01 | REQ-SMS-008 | Manual | 첫 로그인 후 권한 대화상자에서 "거부" | - | 수동 기록(TEST.md 3장) |
| TC-SMS-008-02 | REQ-SMS-008 | Manual | 권한 거부 상태에서 수동 추가·통계·공유 사용, "문자함에서 가져오기" 누름 | - | 수동 기록(TEST.md 3장) |
| TC-CHART-001-01 | REQ-CHART-001 | Unit | 식비 2건, 카페 1건 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-CHART-001-02 | REQ-CHART-001 | Manual | 서로 다른 항목 3건 이상 입력 후 통계 탭 | - | 수동 기록(TEST.md 3장) |
| TC-CHART-001-03 | REQ-CHART-001 | Unit | 10/31 23:59, 12/01 00:00 거래 포함 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-CHART-001-04 | REQ-CHART-001 | Unit | 거래 없음 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-CHART-002-01 | REQ-CHART-002 | Manual | 통계 탭에서 이전/다음 달 이동 후 내역 탭으로 전환 | - | 수동 기록(TEST.md 3장) |
| TC-CHART-003-01 | REQ-CHART-003 | Unit | 1,000 / 6,000 / 3,000 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-CHART-003-02 | REQ-CHART-003 | Unit | 금액 표기 | MonthlySummaryCalculatorTest.kt | 미실행 |
| TC-CHART-003-03 | REQ-CHART-003 | Manual | 통계 탭 범례 확인 | - | 수동 기록(TEST.md 3장) |
| TC-LIST-001-01 | REQ-LIST-001 | Manual | 서로 다른 일시 3건 입력 후 내역 탭 | - | 수동 기록(TEST.md 3장) |
| TC-LIST-002-01 | REQ-LIST-002 | Manual | 문자로 기입된 행을 눌러 금액·항목·메모 변경 후 저장 | - | 수동 기록(TEST.md 3장) |
| TC-LIST-003-01 | REQ-LIST-003 | Manual | 수정 창에서 "이 내역 삭제" → 확인 | - | 수동 기록(TEST.md 3장) |
| TC-LIST-004-01 | REQ-LIST-004 | Manual | + 버튼으로 현금 8,000원 식비 추가 | - | 수동 기록(TEST.md 3장) |
| TC-LIST-005-01 | REQ-LIST-005 | Unit | 올바른 입력(쉼표·앞뒤 공백) | TransactionValidatorTest.kt | 미실행 |
| TC-LIST-005-02 | REQ-LIST-005 | Unit | 금액 abc / 0 / 음수 / 10억 초과 / 10억 | TransactionValidatorTest.kt | 미실행 |
| TC-LIST-005-03 | REQ-LIST-005 | Unit | 사용처 공백·51자, 메모 201자 | TransactionValidatorTest.kt | 미실행 |
| TC-LIST-005-04 | REQ-LIST-005 | Unit | 일시 형식 오류, 13월, 빈 값 | TransactionValidatorTest.kt | 미실행 |
| TC-LIST-005-05 | REQ-LIST-005 | Unit | 금액·사용처·일시 모두 오류 | TransactionValidatorTest.kt | 미실행 |
| TC-LIST-005-06 | REQ-LIST-005 | Manual | 수정 창에서 금액을 비우고 저장 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-001-01 | REQ-SHARE-001 | Manual | 첫 실행 → "Google 계정으로 로그인" → 계정 선택 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-001-02 | REQ-SHARE-001 | Manual | 로그인 후 앱 강제 종료 → 재실행 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-002-01 | REQ-SHARE-002 | Manual | 공유 탭 "로그아웃" → 카드 문자 수신 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-003-01 | REQ-SHARE-003 | Manual | 새 계정으로 최초 로그인 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-003-02 | REQ-SHARE-003 | Manual | 공유 탭에서 "여행 경비" 생성 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-004-01 | REQ-SHARE-004 | Unit | 초대 코드 1,000개 생성 | InviteCodeGeneratorTest.kt | 미실행 |
| TC-SHARE-004-02 | REQ-SHARE-004 | Unit | 초대 코드 1,000개 중복 | InviteCodeGeneratorTest.kt | 미실행 |
| TC-SHARE-004-03 | REQ-SHARE-004 | Manual | 공유 탭 "보내기" | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-005-01 | REQ-SHARE-005 | Unit | ` abc-234 ` 정규화, 길이·문자 검사 | InviteCodeGeneratorTest.kt | 미실행 |
| TC-SHARE-005-02 | REQ-SHARE-005 | Manual | 계정 B 에서 계정 A 의 코드를 소문자로 입력해 참여 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-005-03 | REQ-SHARE-005 | Manual | 형식이 틀린 코드 / 없는 코드 입력 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-006-01 | REQ-SHARE-006 | Manual | A·B 두 기기에서 같은 가계부를 연 상태로 A 가 추가·수정·삭제, A 기기에 카드 문자 수신 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-007-01 | REQ-SHARE-007 | Rules | 비구성원 계정으로 `ledgers/{id}` 와 `transactions` 읽기/쓰기 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-007-02 | REQ-SHARE-007 | Rules | 구성원(비소유자)이 members 에서 다른 사람 제거, ownerUid/inviteCode 변경 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-007-03 | REQ-SHARE-007 | Rules | 비구성원이 틀린 joinedWith 로 참여 / 다른 uid 를 추가 / `invites` 목록 조회 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-008-01 | REQ-SHARE-008 | Manual | 가계부 2개에 속한 상태에서 제목을 눌러 B 선택 → 카드 문자 수신 → 앱 재실행 | - | 수동 기록(TEST.md 3장) |
| TC-SHARE-008-02 | REQ-SHARE-008 | Unit | 기입기에 주는 현재 가계부를 A → B 로 변경 | SmsIngestorTest.kt | 미실행 |
