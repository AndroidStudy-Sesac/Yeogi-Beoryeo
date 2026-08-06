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
    parser.add_argument("--verify", action="store_true")
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


def require_int(properties: dict[str, str], key: str) -> int:
    try:
        return int(properties[key])
    except KeyError as error:
        raise ValueError(f"{key} 값이 properties에 없습니다.") from error
    except ValueError as error:
        raise ValueError(f"{key} 값은 정수여야 합니다.") from error


def read_baseline(properties: dict[str, str], metric: str) -> tuple[int, int]:
    covered = require_int(properties, f"focusedCoverage{metric}BaselineCovered")
    total = require_int(properties, f"focusedCoverage{metric}BaselineTotal")
    if total <= 0 or covered < 0 or covered > total:
        raise ValueError(f"{metric} baseline covered/total 값이 올바르지 않습니다.")
    return covered, total


def is_below_baseline(
    covered: int,
    total: int,
    baseline_covered: int,
    baseline_total: int,
) -> bool:
    """Compare ratios without losing precision to decimal rounding."""
    return covered * baseline_total < baseline_covered * total


def metric_failure_reason(
    covered: int,
    total: int,
    current: float,
    baseline_covered: int,
    baseline_total: int,
    minimum: float,
) -> str:
    reasons: list[str] = []
    if is_below_baseline(covered, total, baseline_covered, baseline_total):
        reasons.append("baseline 미달")
    if current < minimum:
        reasons.append("최소선 미달")
    return " · ".join(reasons)


def render_row(
    name: str,
    covered: int,
    total: int,
    current: float,
    baseline_covered: int,
    baseline_total: int,
    minimum: float,
) -> str:
    baseline = baseline_covered * 100 / baseline_total
    delta = current - baseline
    below_baseline = is_below_baseline(
        covered, total, baseline_covered, baseline_total
    )
    if abs(delta) < 0.005 and delta != 0:
        delta_text = "-<0.01pp" if below_baseline else "+<0.01pp"
    else:
        delta_text = f"{delta:+.2f}pp"
    reason = metric_failure_reason(
        covered,
        total,
        current,
        baseline_covered,
        baseline_total,
        minimum,
    )
    result = f"실패 ({reason})" if reason else "통과"
    return (
        f"| {name} | {current:.2f}% ({covered:,}/{total:,}) | {baseline:.2f}% | "
        f"{delta_text} | baseline 이상 · ≥{minimum:.2f}% | {result} |"
    )


def main() -> None:
    args = parse_args()
    root = ElementTree.parse(args.report).getroot()
    properties = read_properties(args.properties)

    line = read_metric(root, "LINE")
    branch = read_metric(root, "BRANCH")
    line_baseline_covered, line_baseline_total = read_baseline(properties, "Line")
    branch_baseline_covered, branch_baseline_total = read_baseline(properties, "Branch")
    line_minimum = require_number(properties, "focusedCoverageLineMinimum")
    branch_minimum = require_number(properties, "focusedCoverageBranchMinimum")

    failures = [
        f"{name} ({reason})"
        for name, metric, baseline_covered, baseline_total, minimum in (
            ("Line", line, line_baseline_covered, line_baseline_total, line_minimum),
            ("Branch", branch, branch_baseline_covered, branch_baseline_total, branch_minimum),
        )
        if (
            reason := metric_failure_reason(
                *metric, baseline_covered, baseline_total, minimum
            )
        )
    ]

    if args.verify:
        if failures:
            raise SystemExit(f"Focused coverage 검증 실패: {', '.join(failures)}")
        print("Focused coverage baseline과 최소선을 통과했습니다.")
        return

    lines = [
        "## Focused coverage",
        "",
        "| 지표 | 현재 | baseline | 차이 | 검증 기준 | 결과 |",
        "|---|---:|---:|---:|---:|---|",
        render_row(
            "Line",
            *line,
            line_baseline_covered,
            line_baseline_total,
            line_minimum,
        ),
        render_row(
            "Branch",
            *branch,
            branch_baseline_covered,
            branch_baseline_total,
            branch_minimum,
        ),
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
