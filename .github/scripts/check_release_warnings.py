#!/usr/bin/env python3
"""Check release build warnings against the known NAVER Map SDK baseline."""

from __future__ import annotations

import argparse
import re
import sys
from collections.abc import Iterable
from pathlib import Path


EXPECTED_NAVER_R8_WARNINGS = 584
NAVER_R8_WARNING = re.compile(
    r"^WARNING: (?:.*[/\\])?map-sdk-3\.23\.2-runtime\.jar: R8: "
    r"Expected stack map table for method with non-linear control flow\. "
    r"In later version of R8, the method may be assumed not reachable\.$"
)
WARNING_LINE = re.compile(
    r"^(?:WARNING: |WARN: |\[WARN\]|w: |warning: )|:\d+:\s+warning:"
)

Diagnostic = tuple[int, str]


def analyze_warnings(lines: Iterable[str]) -> tuple[int, tuple[Diagnostic, ...]]:
    known_count = 0
    unexpected: list[Diagnostic] = []

    for line_number, raw_line in enumerate(lines, start=1):
        line = raw_line.strip()
        if NAVER_R8_WARNING.fullmatch(line):
            known_count += 1
        elif WARNING_LINE.search(line):
            unexpected.append((line_number, line))

    return known_count, tuple(unexpected)


def validate_warnings(
    known_count: int,
    unexpected: tuple[Diagnostic, ...],
) -> None:
    errors: list[str] = []
    if known_count != EXPECTED_NAVER_R8_WARNINGS:
        errors.append(
            "NAVER Map SDK R8 warning count changed: "
            f"expected {EXPECTED_NAVER_R8_WARNINGS}, found {known_count}"
        )
    if unexpected:
        errors.append(f"Unexpected build warnings found: {len(unexpected)}")
    if errors:
        raise ValueError("\n".join(errors))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("log", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        lines = args.log.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeError) as error:
        print(f"Release build log could not be read: {error}", file=sys.stderr)
        return 2

    known_count, unexpected = analyze_warnings(lines)
    print(
        "NAVER Map SDK R8 stack-map warnings: "
        f"{known_count}/{EXPECTED_NAVER_R8_WARNINGS}"
    )
    print(f"Unexpected build warnings: {len(unexpected)}")

    for line_number, line in unexpected[:10]:
        print(f"line {line_number}: {line}", file=sys.stderr)

    try:
        validate_warnings(known_count, unexpected)
    except ValueError as error:
        print(error, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
