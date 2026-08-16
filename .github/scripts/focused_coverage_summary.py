#!/usr/bin/env python3
"""Render a Kover XML report as a concise GitHub Actions summary."""

from __future__ import annotations

import argparse
from pathlib import Path
from xml.etree import ElementTree

Metric = tuple[int, int, float]

MODULE_PACKAGE_PREFIXES = {
    "app": (
        "com/team/yeogibeoryeo/appguide",
        "com/team/yeogibeoryeo/navigation",
    ),
    "data": ("com/team/yeogibeoryeo/data",),
    "domain": ("com/team/yeogibeoryeo/domain",),
    "presentation": ("com/team/yeogibeoryeo/presentation",),
}


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


def read_counter(
    parent: ElementTree.Element,
    metric_type: str,
    owner: str,
    allow_empty: bool = False,
) -> Metric:
    counter = next(
        (item for item in parent.findall("counter") if item.get("type") == metric_type),
        None,
    )
    if counter is None:
        raise ValueError(f"{owner}의 {metric_type} counter가 report에 없습니다.")

    covered = int(counter.get("covered", "0"))
    missed = int(counter.get("missed", "0"))
    total = covered + missed
    if total == 0 and not allow_empty:
        raise ValueError(f"{owner}의 {metric_type} counter 분모가 0입니다.")
    percent = covered * 100 / total if total else 0.0
    return covered, total, percent


def read_metric(root: ElementTree.Element, metric_type: str) -> Metric:
    return read_counter(root, metric_type, "report")


def read_module_metrics(
    root: ElementTree.Element,
    metric_type: str,
) -> dict[str, Metric]:
    packages = root.findall("package")
    module_metrics: dict[str, Metric] = {}
    unknown_packages = [
        package.get("name", "")
        for package in packages
        if not any(
            package.get("name", "") == prefix
            or package.get("name", "").startswith(f"{prefix}/")
            for prefixes in MODULE_PACKAGE_PREFIXES.values()
            for prefix in prefixes
        )
    ]
    if unknown_packages:
        raise ValueError(
            f"{metric_type} 측정 module로 분류할 수 없는 package가 있습니다: "
            + ", ".join(unknown_packages)
        )

    for module, prefixes in MODULE_PACKAGE_PREFIXES.items():
        covered = 0
        total = 0
        matched = False
        for package in packages:
            package_name = package.get("name", "")
            if not any(
                package_name == prefix or package_name.startswith(f"{prefix}/")
                for prefix in prefixes
            ):
                continue

            package_covered, package_total, _ = read_counter(
                package,
                metric_type,
                f"{module} module package {package_name}",
                allow_empty=True,
            )
            covered += package_covered
            total += package_total
            matched = True

        if not matched or total == 0:
            raise ValueError(f"{module} module의 {metric_type} 측정값이 없습니다.")
        module_metrics[module] = (covered, total, covered * 100 / total)

    return module_metrics


def validate_module_total(
    metric_type: str,
    aggregate: Metric,
    module_metrics: dict[str, Metric],
) -> None:
    module_covered = sum(metric[0] for metric in module_metrics.values())
    module_total = sum(metric[1] for metric in module_metrics.values())
    if (module_covered, module_total) != aggregate[:2]:
        raise ValueError(
            f"{metric_type} 모듈 합계 {module_covered}/{module_total}가 "
            f"report 합계 {aggregate[0]}/{aggregate[1]}와 다릅니다."
        )


def validate_baseline_total(
    metric: str,
    aggregate: tuple[int, int],
    module_baselines: dict[str, tuple[int, int]],
) -> None:
    module_covered = sum(baseline[0] for baseline in module_baselines.values())
    module_total = sum(baseline[1] for baseline in module_baselines.values())
    if (module_covered, module_total) != aggregate:
        raise ValueError(
            f"{metric} module baseline 합계 {module_covered}/{module_total}가 "
            f"전체 baseline {aggregate[0]}/{aggregate[1]}와 다릅니다."
        )


def require_int(properties: dict[str, str], key: str) -> int:
    try:
        return int(properties[key])
    except KeyError as error:
        raise ValueError(f"{key} 값이 properties에 없습니다.") from error
    except ValueError as error:
        raise ValueError(f"{key} 값은 정수여야 합니다.") from error


def read_baseline(
    properties: dict[str, str],
    metric: str,
    module: str = "",
) -> tuple[int, int]:
    scope = module.title() if module else ""
    key_prefix = f"focusedCoverage{scope}{metric}Baseline"
    covered = require_int(properties, f"{key_prefix}Covered")
    total = require_int(properties, f"{key_prefix}Total")
    if total <= 0 or covered < 0 or covered > total:
        label = f"{module} {metric}".strip()
        raise ValueError(f"{label} baseline covered/total 값이 올바르지 않습니다.")
    return covered, total


def is_below_baseline(
    covered: int,
    total: int,
    baseline_covered: int,
    baseline_total: int,
) -> bool:
    """Compare ratios without losing precision to decimal rounding."""
    return covered * baseline_total < baseline_covered * total


def render_cells(
    covered: int,
    total: int,
    current: float,
    baseline_covered: int,
    baseline_total: int,
) -> tuple[str, str, str]:
    baseline = baseline_covered * 100 / baseline_total
    delta = current - baseline
    below_baseline = is_below_baseline(
        covered, total, baseline_covered, baseline_total
    )
    if abs(delta) < 0.005 and delta != 0:
        delta_text = "-<0.01pp" if below_baseline else "+<0.01pp"
    else:
        delta_text = f"{delta:+.2f}pp"
    return (
        f"{current:.2f}% ({covered:,}/{total:,})",
        f"{baseline:.2f}% ({baseline_covered:,}/{baseline_total:,})",
        delta_text,
    )


def render_row(
    name: str,
    covered: int,
    total: int,
    current: float,
    baseline_covered: int,
    baseline_total: int,
) -> str:
    current_text, baseline_text, delta_text = render_cells(
        covered,
        total,
        current,
        baseline_covered,
        baseline_total,
    )
    return (
        f"| {name} | {current_text} | {baseline_text} | {delta_text} |"
    )


def render_module_row(
    module: str,
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> str:
    line_cells = render_cells(*line, *line_baseline)
    branch_cells = render_cells(*branch, *branch_baseline)
    needs_review = is_below_baseline(line[0], line[1], *line_baseline) or (
        is_below_baseline(branch[0], branch[1], *branch_baseline)
    )
    status = "확인 필요" if needs_review else "기준 이상"
    return f"| `{module}` | {' | '.join((*line_cells, *branch_cells, status))} |"


def main() -> None:
    args = parse_args()
    root = ElementTree.parse(args.report).getroot()
    properties = read_properties(args.properties)

    line = read_metric(root, "LINE")
    branch = read_metric(root, "BRANCH")
    module_lines = read_module_metrics(root, "LINE")
    module_branches = read_module_metrics(root, "BRANCH")
    validate_module_total("LINE", line, module_lines)
    validate_module_total("BRANCH", branch, module_branches)
    line_baseline = read_baseline(properties, "Line")
    branch_baseline = read_baseline(properties, "Branch")
    module_line_baselines = {
        module: read_baseline(properties, "Line", module)
        for module in MODULE_PACKAGE_PREFIXES
    }
    module_branch_baselines = {
        module: read_baseline(properties, "Branch", module)
        for module in MODULE_PACKAGE_PREFIXES
    }
    validate_baseline_total("Line", line_baseline, module_line_baselines)
    validate_baseline_total("Branch", branch_baseline, module_branch_baselines)

    lines = [
        "## Focused coverage",
        "",
        "| 지표 | 현재 | baseline | 차이 |",
        "|---|---:|---:|---:|",
        render_row(
            "Line",
            *line,
            *line_baseline,
        ),
        render_row(
            "Branch",
            *branch,
            *branch_baseline,
        ),
        "",
        "### 모듈별 coverage",
        "",
        "| 모듈 | Line 현재 | Line baseline | Line 차이 | Branch 현재 | Branch baseline | Branch 차이 | 상태 |",
        "|---|---:|---:|---:|---:|---:|---:|---|",
        *(
            render_module_row(
                module,
                module_lines[module],
                module_line_baselines[module],
                module_branches[module],
                module_branch_baselines[module],
            )
            for module in MODULE_PACKAGE_PREFIXES
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
