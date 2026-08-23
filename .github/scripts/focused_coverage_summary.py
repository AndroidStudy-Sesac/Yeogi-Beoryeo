#!/usr/bin/env python3
"""Render a Kover XML report as a concise GitHub Actions summary."""

from __future__ import annotations

import argparse
import math
from html import escape
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
BAR_WIDTH = 10


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--properties", type=Path, required=True)
    parser.add_argument("--commit", default="")
    parser.add_argument("--artifact-url", default="")
    parser.add_argument("--policy-url", default="")
    parser.add_argument("--html-output", type=Path)
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
    below_baseline = is_below_baseline(covered, total, baseline_covered, baseline_total)
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
        f"| {name} | `{render_bar(current)}` **{current_text}** | "
        f"{baseline_text} | {delta_text} |"
    )


def render_bar(percent: float) -> str:
    filled = min(BAR_WIDTH, max(0, int(percent * BAR_WIDTH / 100 + 0.5)))
    if percent < 100:
        filled = min(filled, BAR_WIDTH - 1)
    return "█" * filled + "░" * (BAR_WIDTH - filled)


def render_change(metric: Metric, baseline: tuple[int, int]) -> str:
    _, _, delta_text = render_cells(*metric, *baseline)
    baseline_percent = baseline[0] * 100 / baseline[1]
    return f"{baseline_percent:.2f}% → **{metric[2]:.2f}%** ({delta_text})"


def render_status(
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> str:
    return (
        "⚠️ 확인 필요"
        if needs_coverage_review(line, line_baseline, branch, branch_baseline)
        else "✅ 기준 이상"
    )


def needs_coverage_review(
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> bool:
    return is_below_baseline(line[0], line[1], *line_baseline) or (
        is_below_baseline(branch[0], branch[1], *branch_baseline)
    )


def coverage_delta(metric: Metric, baseline: tuple[int, int]) -> float:
    return metric[2] - baseline[0] * 100 / baseline[1]


def render_chart_number(value: float) -> str:
    return "0.00" if abs(value) < 0.005 else f"{value:.2f}"


def render_delta_charts(
    module_lines: dict[str, Metric],
    module_line_baselines: dict[str, tuple[int, int]],
    module_branches: dict[str, Metric],
    module_branch_baselines: dict[str, tuple[int, int]],
) -> list[str]:
    modules = list(MODULE_PACKAGE_PREFIXES)
    line_deltas = [
        coverage_delta(module_lines[module], module_line_baselines[module])
        for module in modules
    ]
    branch_deltas = [
        coverage_delta(module_branches[module], module_branch_baselines[module])
        for module in modules
    ]
    axis_limit = max(1, math.ceil(max(map(abs, line_deltas + branch_deltas))))

    def render_chart(metric: str, deltas: list[float]) -> list[str]:
        description = "; ".join(
            f"{module} {render_chart_number(delta)}"
            for module, delta in zip(modules, deltas, strict=True)
        )
        return [
            f"#### {metric}",
            "",
            "```mermaid",
            "xychart",
            f"  accTitle: {metric} coverage changes from baseline",
            (
                f"  accDescr: {metric} percentage point changes from baseline. "
                f"{description}."
            ),
            f"  x-axis [{', '.join(modules)}]",
            f'  y-axis "pp" -{axis_limit} --> {axis_limit}',
            f"  bar [{', '.join(map(render_chart_number, deltas))}]",
            "```",
        ]

    return [
        *render_chart("Line", line_deltas),
        "",
        *render_chart("Branch", branch_deltas),
    ]


def render_module_row(
    module: str,
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> str:
    return (
        f"| `{module}` | {render_change(line, line_baseline)} | "
        f"{render_change(branch, branch_baseline)} | "
        f"{render_status(line, line_baseline, branch, branch_baseline)} |"
    )


def render_raw_module_row(
    module: str,
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> str:
    return (
        f"| `{module}` | {line[0]:,}/{line[1]:,} | "
        f"{line_baseline[0]:,}/{line_baseline[1]:,} | "
        f"{branch[0]:,}/{branch[1]:,} | "
        f"{branch_baseline[0]:,}/{branch_baseline[1]:,} |"
    )


HTML_STYLES = """
:root {
  color: #17212b;
  background: #f8fafc;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  font-size: 16px;
  line-height: 1.6;
}
* {
  box-sizing: border-box;
}
body {
  margin: 0;
}
a {
  color: #14532d;
  font-weight: 700;
  text-underline-offset: 0.2em;
}
a:hover {
  text-decoration-thickness: 0.14em;
}
a:focus-visible {
  outline: 3px solid #1d4ed8;
  outline-offset: 3px;
  border-radius: 0.2rem;
}
.skip-link {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  z-index: 1;
  padding: 0.65rem 0.9rem;
  color: #ffffff;
  background: #17212b;
  transform: translateY(-150%);
}
.skip-link:focus {
  transform: translateY(0);
}
header {
  padding: 2.5rem max(1rem, calc((100% - 70rem) / 2));
  background: #ffffff;
  border-bottom: 1px solid #cbd5e1;
}
h1,
h2,
h3,
p {
  margin-top: 0;
}
h1 {
  margin-bottom: 0.5rem;
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 1.15;
  letter-spacing: -0.03em;
}
header p {
  max-width: 52rem;
  margin-bottom: 0;
  color: #475569;
}
main {
  width: min(70rem, calc(100% - 2rem));
  margin: 0 auto;
  padding: 2rem 0 4rem;
}
section + section {
  margin-top: 2.5rem;
}
.report-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem 1.25rem;
  margin-bottom: 1.5rem;
}
.commit {
  overflow-wrap: anywhere;
  color: #475569;
}
code {
  padding: 0.12rem 0.35rem;
  border-radius: 0.25rem;
  color: #17212b;
  background: #e2e8f0;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr));
  gap: 1rem;
}
.metric-card {
  padding: 1.25rem;
  border: 1px solid #cbd5e1;
  border-radius: 0.75rem;
  background: #ffffff;
}
.metric-card h3 {
  margin-bottom: 0.25rem;
  font-size: 1.125rem;
}
.metric-current {
  margin-bottom: 0.75rem;
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.bar {
  height: 0.65rem;
  overflow: hidden;
  margin-bottom: 1rem;
  border-radius: 999px;
  background: #e2e8f0;
}
.bar > span {
  display: block;
  height: 100%;
  background: #166534;
}
dl {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: 0.35rem 0.75rem;
  margin: 0;
}
dt {
  color: #475569;
}
dd {
  margin: 0;
  font-weight: 700;
}
.status {
  display: inline-block;
  width: fit-content;
  margin-top: 0.85rem;
  padding: 0.2rem 0.55rem;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 800;
}
.status-good {
  color: #166534;
  background: #f0fdf4;
}
.status-review {
  color: #92400e;
  background: #fffbeb;
}
.table-note {
  color: #475569;
}
table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #cbd5e1;
  background: #ffffff;
}
caption {
  padding: 0.75rem 0;
  color: #17212b;
  font-size: 1.125rem;
  font-weight: 800;
  text-align: left;
}
th,
td {
  padding: 0.9rem;
  border-bottom: 1px solid #cbd5e1;
  text-align: left;
  vertical-align: top;
}
thead th {
  color: #334155;
  background: #f1f5f9;
}
tbody tr:last-child th,
tbody tr:last-child td {
  border-bottom: 0;
}
.metric-value,
.metric-detail {
  display: block;
}
.metric-value {
  font-weight: 800;
}
.metric-detail {
  margin-top: 0.2rem;
  color: #475569;
  font-size: 0.9rem;
}
@media (max-width: 44rem) {
  header {
    padding-top: 2rem;
    padding-bottom: 2rem;
  }
  thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    clip-path: inset(50%);
    white-space: nowrap;
  }
  tbody,
  tr,
  th,
  td {
    display: block;
    width: 100%;
  }
  tbody tr + tr {
    border-top: 0.75rem solid #f8fafc;
  }
  tbody th {
    border-bottom: 1px solid #94a3b8;
    background: #f1f5f9;
    font-size: 1.1rem;
  }
  tbody td {
    display: grid;
    grid-template-columns: minmax(6.5rem, 0.7fr) minmax(0, 1fr);
    gap: 0.75rem;
  }
  tbody td::before {
    content: attr(data-label);
    color: #475569;
    font-weight: 700;
  }
}
@media print {
  :root,
  body {
    background: #ffffff;
  }
  .skip-link {
    display: none;
  }
}
""".strip()


def render_html_metric_card(
    metric_name: str,
    metric: Metric,
    baseline: tuple[int, int],
) -> str:
    current_text, baseline_text, delta_text = render_cells(*metric, *baseline)
    below_baseline = is_below_baseline(metric[0], metric[1], *baseline)
    status_text = "확인 필요" if below_baseline else "기준 이상"
    status_class = "status-review" if below_baseline else "status-good"
    width = min(100.0, max(0.0, metric[2]))
    escaped_name = escape(metric_name)
    return f"""
<article class="metric-card" aria-labelledby="metric-{escaped_name.lower()}">
  <h3 id="metric-{escaped_name.lower()}">전체 {escaped_name}</h3>
  <p class="metric-current">{escape(current_text)}</p>
  <div class="bar" aria-hidden="true"><span style="width: {width:.2f}%"></span></div>
  <dl>
    <dt>baseline</dt><dd>{escape(baseline_text)}</dd>
    <dt>차이</dt><dd>{escape(delta_text)}</dd>
  </dl>
  <span class="status {status_class}">{status_text}</span>
</article>""".strip()


def render_html_module_row(
    module: str,
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
) -> str:
    line_current, line_base, line_delta = render_cells(
        *line,
        *line_baseline,
    )
    branch_current, branch_base, branch_delta = render_cells(
        *branch,
        *branch_baseline,
    )
    needs_review = needs_coverage_review(
        line,
        line_baseline,
        branch,
        branch_baseline,
    )
    status_text = "확인 필요" if needs_review else "기준 이상"
    status_class = "status-review" if needs_review else "status-good"
    return f"""
<tr>
  <th scope="row"><code>{escape(module)}</code></th>
  <td data-label="Line">
    <span class="metric-value">{escape(line_current)}</span>
    <span class="metric-detail">baseline {escape(line_base)}</span>
    <span class="metric-detail">차이 {escape(line_delta)}</span>
  </td>
  <td data-label="Branch">
    <span class="metric-value">{escape(branch_current)}</span>
    <span class="metric-detail">baseline {escape(branch_base)}</span>
    <span class="metric-detail">차이 {escape(branch_delta)}</span>
  </td>
  <td data-label="상태"><span class="status {status_class}">{status_text}</span></td>
</tr>""".strip()


def render_html(
    line: Metric,
    line_baseline: tuple[int, int],
    branch: Metric,
    branch_baseline: tuple[int, int],
    module_lines: dict[str, Metric],
    module_line_baselines: dict[str, tuple[int, int]],
    module_branches: dict[str, Metric],
    module_branch_baselines: dict[str, tuple[int, int]],
    commit: str = "",
    policy_url: str = "",
) -> str:
    module_rows = "\n".join(
        render_html_module_row(
            module,
            module_lines[module],
            module_line_baselines[module],
            module_branches[module],
            module_branch_baselines[module],
        )
        for module in MODULE_PACKAGE_PREFIXES
    )
    commit_html = (
        f'<p class="commit">측정 commit <code>{escape(commit)}</code></p>'
        if commit
        else ""
    )
    policy_link = (
        f'<a href="{escape(policy_url, quote=True)}">운영 정책</a>'
        if policy_url
        else ""
    )
    links = "\n".join(
        link
        for link in (
            '<a href="html/index.html">상세 Kover report</a>',
            policy_link,
        )
        if link
    )
    return f"""<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="description" content="전체와 모듈별 focused coverage를 baseline과 비교한 정적 요약입니다.">
  <title>Focused coverage 요약</title>
  <style>{HTML_STYLES}</style>
</head>
<body>
  <a class="skip-link" href="#main-content">본문으로 이동</a>
  <header>
    <h1>Focused coverage 요약</h1>
    <p>JVM unit test로 측정한 business logic coverage를 baseline과 비교합니다.</p>
  </header>
  <main id="main-content">
    <nav class="report-links" aria-label="관련 report">
      {links}
    </nav>
    {commit_html}
    <section aria-labelledby="overall-heading">
      <h2 id="overall-heading">전체 coverage</h2>
      <div class="metric-grid">
        {render_html_metric_card("Line", line, line_baseline)}
        {render_html_metric_card("Branch", branch, branch_baseline)}
      </div>
    </section>
    <section aria-labelledby="modules-heading">
      <h2 id="modules-heading">모듈별 coverage</h2>
      <p class="table-note" id="coverage-note">
        Line이나 Branch가 baseline보다 낮은 모듈은 '확인 필요'로 표시합니다.
        Coverage 하락 자체는 CI 실패 조건이 아닙니다.
      </p>
      <table aria-describedby="coverage-note">
        <caption>모듈별 coverage</caption>
        <thead>
          <tr>
            <th scope="col">모듈</th>
            <th scope="col">Line</th>
            <th scope="col">Branch</th>
            <th scope="col">상태</th>
          </tr>
        </thead>
        <tbody>
          {module_rows}
        </tbody>
      </table>
    </section>
  </main>
</body>
</html>
"""


def write_html(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8", newline="\n")


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

    if args.html_output:
        write_html(
            args.html_output,
            render_html(
                line,
                line_baseline,
                branch,
                branch_baseline,
                module_lines,
                module_line_baselines,
                module_branches,
                module_branch_baselines,
                args.commit,
                args.policy_url,
            ),
        )

    lines = ["## Focused coverage", ""]
    links = []
    if args.artifact_url:
        links.append(f"[상세 HTML/XML report]({args.artifact_url})")
    if args.policy_url:
        links.append(f"[운영 정책]({args.policy_url})")
    if links:
        lines.extend((" / ".join(links), ""))

    lines += [
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
        "### 모듈별 baseline 대비 변화",
        "",
        (
            "각 차트는 모듈별 baseline 대비 변화를 표시합니다. "
            "0보다 작으면 baseline보다 낮으며, 단위는 pp입니다."
        ),
        "",
        *render_delta_charts(
            module_lines,
            module_line_baselines,
            module_branches,
            module_branch_baselines,
        ),
        "",
        "### 모듈별 coverage",
        "",
        "| 모듈 | Line baseline → 현재 | Branch baseline → 현재 | 상태 |",
        "|---|---:|---:|---|",
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
        "<details>",
        "<summary>raw covered/total과 측정 정보</summary>",
        "",
        "| 모듈 | Line 현재 | Line baseline | Branch 현재 | Branch baseline |",
        "|---|---:|---:|---:|---:|",
        *(
            render_raw_module_row(
                module,
                module_lines[module],
                module_line_baselines[module],
                module_branches[module],
                module_branch_baselines[module],
            )
            for module in MODULE_PACKAGE_PREFIXES
        ),
        "",
        "`app`, `data`, `domain`, `presentation`의 business logic과 순수 상태, 계산 helper를 JVM unit test로 측정합니다.",
        "Android instrumented test 결과와 `@Composable` 렌더링 declaration은 포함되지 않습니다.",
    ]

    if args.commit:
        lines.extend(("", f"측정 commit: `{args.commit}`"))

    lines.extend(("", "</details>"))

    print("\n".join(lines))


if __name__ == "__main__":
    main()
