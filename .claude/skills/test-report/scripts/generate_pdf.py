#!/usr/bin/env python3
"""Gradle 테스트 결과 XML을 파싱하여 PDF 리포트를 생성한다."""

import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

try:
    from fpdf import FPDF
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "fpdf2", "-q"])
    from fpdf import FPDF


FONT_PATH = "/System/Library/Fonts/AppleSDGothicNeo.ttc"


def parse_test_results(results_dir: Path) -> dict:
    suites = []
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_time = 0.0

    for xml_file in sorted(results_dir.glob("*.xml")):
        tree = ET.parse(xml_file)
        root = tree.getroot()

        suite_name = root.get("name", xml_file.stem)
        tests = int(root.get("tests", 0))
        failures = int(root.get("failures", 0))
        errors = int(root.get("errors", 0))
        skipped = int(root.get("skipped", 0))
        time_taken = float(root.get("time", 0))

        failed_tests = []
        for testcase in root.findall("testcase"):
            failure = testcase.find("failure")
            error = testcase.find("error")
            issue = failure if failure is not None else error
            if issue is not None:
                failed_tests.append({
                    "name": testcase.get("name", "unknown"),
                    "classname": testcase.get("classname", ""),
                    "message": (issue.get("message") or issue.text or "")[:300],
                })

        suites.append({
            "name": suite_name,
            "tests": tests,
            "failures": failures,
            "errors": errors,
            "skipped": skipped,
            "time": time_taken,
            "failed_tests": failed_tests,
        })

        total_tests += tests
        total_failures += failures
        total_errors += errors
        total_skipped += skipped
        total_time += time_taken

    return {
        "suites": suites,
        "total_tests": total_tests,
        "total_failures": total_failures,
        "total_errors": total_errors,
        "total_skipped": total_skipped,
        "total_passed": total_tests - total_failures - total_errors - total_skipped,
        "total_time": total_time,
    }


def generate_pdf(data: dict, output_path: Path) -> None:
    pdf = FPDF()
    pdf.add_font("korean", "", FONT_PATH, uni=True)
    pdf.add_font("korean", "B", FONT_PATH, uni=True)
    pdf.set_auto_page_break(auto=True, margin=20)

    # --- 표지 ---
    pdf.add_page()
    pdf.set_font("korean", "B", 24)
    pdf.ln(40)
    pdf.cell(0, 15, "테스트 실행 리포트", align="C", new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("korean", "", 12)
    pdf.ln(10)
    pdf.cell(0, 10, f"생성일시: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}", align="C", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 10, f"프로젝트: {Path.cwd().name}", align="C", new_x="LMARGIN", new_y="NEXT")

    # --- 전체 요약 ---
    pdf.ln(20)
    pdf.set_font("korean", "B", 16)
    pdf.cell(0, 10, "전체 요약", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(5)

    all_passed = data["total_failures"] == 0 and data["total_errors"] == 0
    status_text = "ALL PASSED" if all_passed else "FAILED"

    pdf.set_font("korean", "B", 14)
    if all_passed:
        pdf.set_text_color(34, 139, 34)
    else:
        pdf.set_text_color(220, 20, 20)
    pdf.cell(0, 10, status_text, align="C", new_x="LMARGIN", new_y="NEXT")
    pdf.set_text_color(0, 0, 0)

    pdf.ln(5)
    pdf.set_font("korean", "", 12)

    summary_items = [
        ("전체 테스트", str(data["total_tests"])),
        ("성공", str(data["total_passed"])),
        ("실패", str(data["total_failures"])),
        ("에러", str(data["total_errors"])),
        ("스킵", str(data["total_skipped"])),
        ("총 실행 시간", f"{data['total_time']:.2f}초"),
    ]

    col_w = 90
    for label, value in summary_items:
        pdf.cell(col_w, 8, f"  {label}", new_x="RIGHT", new_y="TOP")
        pdf.cell(col_w, 8, value, new_x="LMARGIN", new_y="NEXT")

    # --- 테스트 클래스별 결과 ---
    pdf.add_page()
    pdf.set_font("korean", "B", 16)
    pdf.cell(0, 10, "테스트 클래스별 결과", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(5)

    pdf.set_font("korean", "B", 10)
    pdf.set_fill_color(230, 230, 230)
    pdf.cell(80, 8, "클래스", border=1, fill=True, new_x="RIGHT", new_y="TOP")
    pdf.cell(25, 8, "테스트", border=1, fill=True, align="C", new_x="RIGHT", new_y="TOP")
    pdf.cell(25, 8, "성공", border=1, fill=True, align="C", new_x="RIGHT", new_y="TOP")
    pdf.cell(25, 8, "실패", border=1, fill=True, align="C", new_x="RIGHT", new_y="TOP")
    pdf.cell(25, 8, "시간(초)", border=1, fill=True, align="C", new_x="LMARGIN", new_y="NEXT")

    pdf.set_font("korean", "", 10)
    for suite in data["suites"]:
        short_name = suite["name"].rsplit(".", 1)[-1] if "." in suite["name"] else suite["name"]
        passed = suite["tests"] - suite["failures"] - suite["errors"] - suite["skipped"]
        failed = suite["failures"] + suite["errors"]

        pdf.cell(80, 8, short_name[:30], border=1, new_x="RIGHT", new_y="TOP")
        pdf.cell(25, 8, str(suite["tests"]), border=1, align="C", new_x="RIGHT", new_y="TOP")
        pdf.cell(25, 8, str(passed), border=1, align="C", new_x="RIGHT", new_y="TOP")

        if failed > 0:
            pdf.set_text_color(220, 20, 20)
        pdf.cell(25, 8, str(failed), border=1, align="C", new_x="RIGHT", new_y="TOP")
        pdf.set_text_color(0, 0, 0)

        pdf.cell(25, 8, f"{suite['time']:.2f}", border=1, align="C", new_x="LMARGIN", new_y="NEXT")

    # --- 실패 상세 ---
    all_failures = [ft for s in data["suites"] for ft in s["failed_tests"]]
    if all_failures:
        pdf.add_page()
        pdf.set_font("korean", "B", 16)
        pdf.set_text_color(220, 20, 20)
        pdf.cell(0, 10, f"실패한 테스트 ({len(all_failures)}건)", new_x="LMARGIN", new_y="NEXT")
        pdf.set_text_color(0, 0, 0)
        pdf.ln(5)

        for i, ft in enumerate(all_failures, 1):
            pdf.set_font("korean", "B", 11)
            pdf.cell(0, 8, f"{i}. {ft['name']}", new_x="LMARGIN", new_y="NEXT")
            pdf.set_font("korean", "", 9)
            pdf.set_text_color(100, 100, 100)
            pdf.cell(0, 6, f"   클래스: {ft['classname']}", new_x="LMARGIN", new_y="NEXT")
            pdf.set_text_color(0, 0, 0)
            pdf.set_font("korean", "", 10)
            msg = ft["message"].replace("\n", " ").strip()
            if msg:
                pdf.multi_cell(0, 6, f"   {msg}")
            pdf.ln(3)

    pdf.output(str(output_path))


if __name__ == "__main__":
    project_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else Path.cwd()
    results_dir = project_dir / "build" / "test-results" / "test"

    if not results_dir.exists() or not list(results_dir.glob("*.xml")):
        print(f"ERROR: 테스트 결과를 찾을 수 없습니다: {results_dir}")
        sys.exit(1)

    data = parse_test_results(results_dir)
    output_path = project_dir / "test-report.pdf"
    generate_pdf(data, output_path)

    print(f"PDF 생성 완료: {output_path}")
    print(f"전체: {data['total_tests']} | 성공: {data['total_passed']} | 실패: {data['total_failures']} | 에러: {data['total_errors']} | 스킵: {data['total_skipped']}")
