#!/usr/bin/env python3
"""사양서-코드-테스트 추적성 검사.

    UR (SPEC.md 2장) -> REQ (SPEC.md 4장) -> 코드 [REQ-xxx] 태그
                                         -> TC (TEST.md) -> 테스트 코드 함수명

검사 항목을 모두 통과하면 docs/TRACEABILITY.md 를 다시 쓰고 0 으로 종료한다.
하나라도 실패하면 문제 목록을 출력하고 1 로 종료한다(매트릭스는 그래도 갱신한다).

단위 테스트 결과(app/build/test-results/**/TEST-*.xml)가 있으면 TC 별 통과/실패를 반영한다.
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPEC = ROOT / "docs" / "SPEC.md"
TEST = ROOT / "docs" / "TEST.md"
OUT = ROOT / "docs" / "TRACEABILITY.md"
MAIN_SOURCES = [ROOT / "app" / "src" / "main", ROOT / "firestore.rules"]
TEST_SOURCES = [ROOT / "app" / "src" / "test", ROOT / "app" / "src" / "androidTest"]
TEST_RESULTS = ROOT / "app" / "build" / "test-results"

REQ_RE = re.compile(r"REQ-[A-Z]+-\d{3}")
TC_RE = re.compile(r"TC-[A-Z]+-\d{3}-\d{2}")
UR_ROW_RE = re.compile(r"^\|\s*(UR-\d{2})\s*\|(.*)\|(.*)\|\s*$")
REQ_HEADING_RE = re.compile(r"^#{2,5}\s+(REQ-[A-Z]+-\d{3})\s+(.*)$")
TC_ROW_RE = re.compile(r"^\|\s*(TC-[A-Z]+-\d{3}-\d{2})\s*\|\s*(REQ-[A-Z]+-\d{3})\s*\|\s*(Unit|Manual|Rules)\s*\|(.*)$")
SOURCE_SUFFIXES = {".kt", ".java", ".xml", ".rules"}


def source_files(paths: list[Path]) -> list[Path]:
    files: list[Path] = []
    for p in paths:
        if p.is_file():
            files.append(p)
        elif p.is_dir():
            files.extend(f for f in sorted(p.rglob("*")) if f.is_file() and f.suffix in SOURCE_SUFFIXES)
    return files


def parse_spec():
    urs: dict[str, tuple[str, list[str]]] = {}
    reqs: dict[str, str] = {}
    for line in SPEC.read_text(encoding="utf-8").splitlines():
        if m := UR_ROW_RE.match(line):
            urs[m.group(1)] = (m.group(2).strip(), REQ_RE.findall(m.group(3)))
        elif m := REQ_HEADING_RE.match(line):
            reqs[m.group(1)] = m.group(2).strip()
    return urs, reqs


def parse_tests():
    tcs: dict[str, dict] = {}
    duplicates: list[str] = []
    for line in TEST.read_text(encoding="utf-8").splitlines():
        if m := TC_ROW_RE.match(line):
            tc_id = m.group(1)
            if tc_id in tcs:
                duplicates.append(tc_id)
            cells = [c.strip() for c in m.group(4).split("|")]
            tcs[tc_id] = {"req": m.group(2), "type": m.group(3), "title": cells[0] if cells else ""}
    return tcs, duplicates


def scan(files: list[Path], pattern: re.Pattern) -> dict[str, set[str]]:
    found: dict[str, set[str]] = defaultdict(set)
    for f in files:
        for ident in pattern.findall(f.read_text(encoding="utf-8", errors="ignore")):
            found[ident].add(str(f.relative_to(ROOT)))
    return found


def read_results() -> dict[str, str]:
    """TC ID -> PASS / FAIL / SKIP (여러 테스트에 걸치면 가장 나쁜 결과)."""
    rank = {"PASS": 0, "SKIP": 1, "FAIL": 2}
    results: dict[str, str] = {}
    if not TEST_RESULTS.exists():
        return results
    for xml in TEST_RESULTS.rglob("TEST-*.xml"):
        for case in ET.parse(xml).getroot().iter("testcase"):
            ids = TC_RE.findall(case.get("name", ""))
            if case.find("failure") is not None or case.find("error") is not None:
                status = "FAIL"
            elif case.find("skipped") is not None:
                status = "SKIP"
            else:
                status = "PASS"
            for tc in ids:
                if tc not in results or rank[status] > rank[results[tc]]:
                    results[tc] = status
    return results


def main() -> int:
    urs, reqs = parse_spec()
    tcs, duplicate_tcs = parse_tests()
    code_refs = scan(source_files(MAIN_SOURCES), REQ_RE)
    test_code_refs = scan(source_files(TEST_SOURCES), TC_RE)
    results = read_results()

    problems: list[str] = []

    # UR <-> REQ
    reqs_in_urs = defaultdict(list)
    for ur, (_, ur_reqs) in urs.items():
        if not ur_reqs:
            problems.append(f"{ur}: 분해된 REQ 가 없습니다.")
        for r in ur_reqs:
            reqs_in_urs[r].append(ur)
            if r not in reqs:
                problems.append(f"{ur}: 정의되지 않은 {r} 를 참조합니다.")
    for r in reqs:
        if r not in reqs_in_urs:
            problems.append(f"{r}: 어떤 UR 에도 연결되지 않았습니다.")

    # REQ <-> 코드
    for r in reqs:
        if r not in code_refs:
            problems.append(f"{r}: 구현 코드에 [{r}] 태그가 없습니다.")
    for r, files in code_refs.items():
        if r not in reqs:
            problems.append(f"{r}: 코드({', '.join(sorted(files))})에 있으나 SPEC 에 정의되지 않았습니다.")

    # REQ <-> TC
    for tc in duplicate_tcs:
        problems.append(f"{tc}: TEST.md 에 중복 정의되었습니다.")
    tcs_by_req = defaultdict(list)
    for tc, info in tcs.items():
        tcs_by_req[info["req"]].append(tc)
        if info["req"] not in reqs:
            problems.append(f"{tc}: 정의되지 않은 {info['req']} 를 검증합니다.")
        expected_req = "REQ-" + "-".join(tc.split("-")[1:3])
        if info["req"] != expected_req:
            problems.append(f"{tc}: ID 는 {expected_req} 를 가리키지만 REQ 열은 {info['req']} 입니다.")
    for r in reqs:
        if r not in tcs_by_req:
            problems.append(f"{r}: 검증하는 TC 가 없습니다.")

    # TC <-> 테스트 코드
    for tc, info in tcs.items():
        if info["type"] == "Unit" and tc not in test_code_refs:
            problems.append(f"{tc}: Unit 으로 정의되었으나 테스트 코드에 없습니다.")
        if info["type"] != "Unit" and tc in test_code_refs:
            problems.append(f"{tc}: {info['type']} 으로 정의되었으나 테스트 코드에 있습니다(유형을 Unit 으로 바꾸세요).")
    for tc, files in test_code_refs.items():
        if tc not in tcs:
            problems.append(f"{tc}: 테스트 코드({', '.join(sorted(files))})에 있으나 TEST.md 에 없습니다.")
    for tc, status in results.items():
        if status == "FAIL":
            problems.append(f"{tc}: 단위 테스트 실패")

    write_matrix(urs, reqs, reqs_in_urs, code_refs, tcs, tcs_by_req, test_code_refs, results, problems)

    unit = sum(1 for t in tcs.values() if t["type"] == "Unit")
    print(f"UR {len(urs)} / REQ {len(reqs)} / TC {len(tcs)} (Unit {unit}, 그 외 {len(tcs) - unit})")
    print(f"단위 테스트 결과: {'없음(미실행)' if not results else f'{len(results)}개 TC 반영'}")
    if problems:
        print(f"\n추적성 문제 {len(problems)}건:")
        for p in problems:
            print(f"  - {p}")
        return 1
    print("추적성 검사 통과 → docs/TRACEABILITY.md 갱신")
    return 0


def write_matrix(urs, reqs, reqs_in_urs, code_refs, tcs, tcs_by_req, test_code_refs, results, problems):
    def status_of(tc: str) -> str:
        t = tcs[tc]["type"]
        if t != "Unit":
            return f"{t}"
        return f"Unit:{results.get(tc, '미실행')}"

    def short(path: str) -> str:
        return path.replace("app/src/main/java/com/budgetbook/app/", "")

    lines = [
        "# 추적성 매트릭스 (TRACEABILITY)",
        "",
        f"> `tools/trace_check.py` 가 자동 생성한 문서입니다. 직접 수정하지 마세요. (생성일 {date.today().isoformat()})",
        "",
        f"- 검사 결과: **{'통과' if not problems else f'문제 {len(problems)}건'}**",
        f"- UR {len(urs)}개 · REQ {len(reqs)}개 · TC {len(tcs)}개 "
        f"(Unit {sum(1 for t in tcs.values() if t['type'] == 'Unit')}, "
        f"Manual {sum(1 for t in tcs.values() if t['type'] == 'Manual')}, "
        f"Rules {sum(1 for t in tcs.values() if t['type'] == 'Rules')})",
        f"- 단위 테스트 결과 반영: {'없음 — `./gradlew testDebugUnitTest` 후 다시 실행' if not results else f'{len(results)}개 TC'}",
        "",
        "## 1. UR → REQ",
        "",
        "| UR | 사용자 요구사항 | REQ |",
        "|---|---|---|",
    ]
    for ur, (text, ur_reqs) in urs.items():
        lines.append(f"| {ur} | {text} | {', '.join(ur_reqs)} |")

    lines += [
        "",
        "## 2. REQ → 코드 → TC",
        "",
        "| REQ | 요구사항 | UR | 구현 위치 | TC (유형:결과) |",
        "|---|---|---|---|---|",
    ]
    for r, title in reqs.items():
        code = "<br>".join(short(p) for p in sorted(code_refs.get(r, []))) or "**없음**"
        tc_cells = "<br>".join(f"{tc} ({status_of(tc)})" for tc in sorted(tcs_by_req.get(r, []))) or "**없음**"
        lines.append(f"| {r} | {title} | {', '.join(reqs_in_urs.get(r, [])) or '**없음**'} | {code} | {tc_cells} |")

    lines += [
        "",
        "## 3. TC → 테스트 코드",
        "",
        "| TC | REQ | 유형 | 내용 | 테스트 코드 | 결과 |",
        "|---|---|---|---|---|---|",
    ]
    for tc, info in tcs.items():
        files = "<br>".join(Path(p).name for p in sorted(test_code_refs.get(tc, []))) or "-"
        result = results.get(tc, "미실행") if info["type"] == "Unit" else "수동 기록(TEST.md 3장)"
        lines.append(f"| {tc} | {info['req']} | {info['type']} | {info['title']} | {files} | {result} |")

    if problems:
        lines += ["", "## 4. 문제", ""] + [f"- {p}" for p in problems]

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
