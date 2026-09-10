#!/usr/bin/env python3
"""Summarize warning occurrences in the lint/build console log."""

from __future__ import annotations

import argparse
import re
from collections.abc import Iterable
from dataclasses import dataclass
from html import escape
from pathlib import Path
from urllib.parse import unquote, urlsplit

ANSI_ESCAPE = re.compile(r"\x1b\[[0-?]*[ -/]*[@-~]")
KOTLIN_LOCATION = re.compile(
    r"^(.*?\.k(?:ts|t))(?::\d+(?::\d+)?:?|:\s*\(\d+,\s*\d+\):)\s*(.*)$"
)
JAVA_WARNING = re.compile(r"^(.+\.java):\d+: warning: (.*)$")
ANDROID_WARNING = re.compile(r"^(.*?): (D8|R8): (.*)$")
WARNING_PREFIX = re.compile(r"^(?:warning|warn):\s*", re.IGNORECASE)
NAVER_ARTIFACT = re.compile(r"map-sdk-\d[^/]*\.(?:jar|aar)$")
MAX_ROWS = 50
MAX_CELL_LENGTH = 400


@dataclass(frozen=True)
class Warning:
    origin: str
    tool: str
    source: str
    message: str


def classify_source(source: str, repository: str) -> tuple[str, str]:
    if source.startswith("file:"):
        source = unquote(urlsplit(source).path)
    source = source.replace("\\", "/")
    repository = repository.replace("\\", "/").rstrip("/")
    if re.match(r"^/[A-Za-z]:/", source):
        source = source[1:]
    filename = source.rsplit("/", 1)[-1]
    if NAVER_ARTIFACT.fullmatch(filename):
        return "NAVER Map SDK", filename
    if filename.endswith((".jar", ".aar")) or "/.gradle/caches/" in source:
        return "외부 라이브러리", filename
    if source.startswith(repository + "/"):
        source = source[len(repository) + 1 :]
    if source.endswith(".gradle.kts") or source.startswith(("gradle/", "buildSrc/")):
        return "빌드 설정", filename
    if source.startswith(("app/", "common/", "data/", "domain/", "presentation/")):
        return "앱 코드", source
    return "출처 미확인", filename or "-"


def parse_warning(line: str, repository: str) -> Warning | None:
    line = ANSI_ESCAPE.sub("", line).strip()
    if line.startswith("w: "):
        message = line[3:]
        match = KOTLIN_LOCATION.match(message)
        if match:
            source, message = match.groups()
            origin, source = classify_source(source, repository)
            return Warning(origin, "Kotlin", source, message)
        return Warning("출처 미확인", "Kotlin", "-", message)
    match = JAVA_WARNING.match(line)
    if match:
        source, message = match.groups()
        origin, source = classify_source(source, repository)
        return Warning(origin, "Java", source, message)
    prefix = WARNING_PREFIX.match(line)
    if prefix:
        message = line[prefix.end() :]
        match = ANDROID_WARNING.match(message)
        if match:
            source, tool, message = match.groups()
            origin, source = classify_source(source, repository)
            return Warning(origin, tool, source, message)
        return Warning("출처 미확인", "기타", "-", message)
    # Gradle --warning-mode all emits individual deprecations without a prefix.
    if " has been deprecated" in line or " have been deprecated" in line:
        return Warning("빌드 도구", "Gradle", "-", line)
    return None


def collect_warnings(lines: Iterable[str], repository: str) -> dict[Warning, list[int]]:
    warnings: dict[Warning, list[int]] = {}
    for line_number, line in enumerate(lines, start=1):
        warning = parse_warning(line, repository)
        if warning:
            warnings.setdefault(warning, []).append(line_number)
    return warnings


def table_cell(value: str) -> str:
    value = " ".join(value.split())
    if len(value) > MAX_CELL_LENGTH:
        value = value[:MAX_CELL_LENGTH] + "..."
    value = escape(value)
    for character in "|`*_[]\\":
        value = value.replace(character, f"&#{ord(character)};")
    return f"<code>{value}</code>"


def log_link(url: str, label: str) -> str:
    parsed = urlsplit(url)
    if parsed.scheme != "https" or not parsed.netloc:
        raise ValueError("로그 링크는 HTTPS URL이어야 합니다.")
    return f'<a href="{escape(url, quote=True)}">{label}</a>'


def render_summary(warnings: dict[Warning, list[int]], log_url: str) -> str:
    total = sum(len(lines) for lines in warnings.values())
    output = [
        "## 빌드 경고",
        "",
        f"이번 빌드 로그에서 경고 {total:,}회, 고유 유형 {len(warnings):,}개를 확인했습니다.",
        "캐시로 실행하지 않은 작업의 경고는 포함하지 않습니다.",
        "",
    ]
    if log_url:
        output.extend([log_link(log_url, "원본 빌드 로그"), ""])
    if warnings:
        output.extend(
            [
                "| 구분 | 도구 | 파일 | 경고 | 횟수 | 첫 로그 줄 |",
                "| --- | --- | --- | --- | ---: | ---: |",
            ]
        )
        ordered = sorted(warnings.items(), key=lambda item: (-len(item[1]), item[1][0]))
        for warning, lines in ordered[:MAX_ROWS]:
            cells = (warning.origin, warning.tool, warning.source, warning.message)
            output.append(
                "| "
                + " | ".join(table_cell(cell) for cell in cells)
                + f" | {len(lines):,} | {lines[0]} |"
            )
        output.append("")
        if len(warnings) > MAX_ROWS:
            output.append(f"발생 횟수가 많은 {MAX_ROWS}개 유형을 표시했습니다.")
        output.append(
            "경고의 전체 내용과 나머지 발생 위치는 원본 로그에서 확인할 수 있습니다."
        )
    return "\n".join(output) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--log", type=Path, required=True)
    parser.add_argument("--log-url", default="")
    parser.add_argument("--repository", default=str(Path.cwd()))
    args = parser.parse_args()
    if not args.log.is_file():
        print("## 빌드 경고\n\n빌드 로그가 없어 경고를 집계하지 못했습니다.")
        return 1
    with args.log.open(encoding="utf-8") as log:
        warnings = collect_warnings(log, args.repository)
    print(render_summary(warnings, args.log_url), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
