#!/usr/bin/env python3
"""Render a Kover XML report as a concise GitHub Actions summary."""

from __future__ import annotations

import argparse
from pathlib import Path
from xml.etree import ElementTree


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--properties", type=Path, required=True)
    parser.add_argument("--commit", default="")
    parser.add_argument("--artifact-url", default="")
    parser.add_argument("--policy-url", default="")
    return parser.parse_args()


def read_properties(path: Path) -> dict[str, str]:
    properties: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        properties[key.strip()] = value.strip()
    return properties


def read_metric(root: ElementTree.Element, metric_type: str) -> tuple[int, int, float]:
    counter = next(
        (item for item in root.findall("counter") if item.get("type") == metric_type),
        None,
    )
    if counter is None:
        raise ValueError(f"{metric_type} counter가 report에 없습니다.")

    covered = int(counter.get("covered", "0"))
    missed = int(counter.get("missed", "0"))
    total = covered + missed
    if total == 0:
        raise ValueError(f"{metric_type} counter의 분모가 0입니다.")
    return covered, total, covered * 100 / total


def require_number(properties: dict[str, str], key: str) -> float:
    try:
        return float(properties[key])
    except KeyError as error:
        raise ValueError(f"{key} 값이 properties에 없습니다.") from error


def render_row(
    name: str,
    covered: int,
    total: int,
    current: float,
    baseline: float,
    minimum: float,
) -> str:
    delta = current - baseline
    if abs(delta) < 0.005:
        delta = 0.0
    result = "통과" if current >= minimum else "실패"
    return (
        f"| {name} | {current:.2f}% ({covered:,}/{total:,}) | {baseline:.2f}% | "
        f"{delta:+.2f}pp | ≥{minimum:.2f}% | {result} |"
    )


def main() -> None:
    args = parse_args()
    root = ElementTree.parse(args.report).getroot()
    properties = read_properties(args.properties)

    line = read_metric(root, "LINE")
    branch = read_metric(root, "BRANCH")
    line_baseline = require_number(properties, "focusedCoverageLineBaseline")
    branch_baseline = require_number(properties, "focusedCoverageBranchBaseline")
    line_minimum = require_number(properties, "focusedCoverageLineMinimum")
    branch_minimum = require_number(properties, "focusedCoverageBranchMinimum")

    lines = [
        "## Focused coverage",
        "",
        "| 지표 | 현재 | baseline | 차이 | gate | 결과 |",
        "|---|---:|---:|---:|---:|---|",
        render_row("Line", *line, line_baseline, line_minimum),
        render_row("Branch", *branch, branch_baseline, branch_minimum),
        "",
        "`app`, `data`, `domain`, `presentation`의 business logic과 순수 상태·계산 helper를 JVM unit test로 측정합니다.",
        "Android instrumented test 결과와 `@Composable` 렌더링 declaration은 포함되지 않습니다.",
    ]

    if args.commit:
        lines.extend(("", f"측정 commit: `{args.commit}`"))
    if args.artifact_url:
        lines.append(f"상세 HTML·XML report: [artifact 다운로드]({args.artifact_url})")
    if args.policy_url:
        lines.append(f"측정 범위와 하락 대응: [focused coverage 운영 정책]({args.policy_url})")

    print("\n".join(lines))


if __name__ == "__main__":
    main()
