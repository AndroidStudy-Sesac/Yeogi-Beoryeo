"""Connect a PR's committed source changes to the measured Kover report."""

from __future__ import annotations

import re
import subprocess
from pathlib import Path, PurePosixPath
from typing import TypedDict
from xml.etree import ElementTree

SourceKey = tuple[str, str, str]


class ChangedSource(TypedDict):
    path: str
    change: str
    status: str
    classes: list[str]
    missed_lines: int | None
    missed_branches: int | None


class ReviewCandidate(TypedDict):
    module: str
    package: str
    class_name: str
    source_file: str
    source_paths: list[str]
    changed_in_pr: bool | None
    missed_lines: int
    missed_branches: int


class PrAnalysis(TypedDict):
    base_commit: str
    head_commit: str
    merge_base: str
    report_commit: str
    changed_file_count: int
    changed_sources: list[ChangedSource]
    candidates: list[ReviewCandidate]


def git_bytes(repository: Path, *arguments: str) -> bytes:
    return subprocess.run(
        ["git", "-C", str(repository), *arguments],
        check=True,
        capture_output=True,
    ).stdout


def read_changes(data: bytes) -> list[tuple[str, str]]:
    """Read --name-status -z --no-renames without splitting valid filenames."""
    if not data:
        return []
    if not data.endswith(b"\0"):
        raise ValueError("변경 파일 목록이 NUL로 끝나지 않습니다.")
    fields = data[:-1].decode("utf-8", errors="strict").split("\0")
    if len(fields) % 2:
        raise ValueError("변경 파일 목록의 상태와 경로가 짝을 이루지 않습니다.")
    changes = []
    for change, path in zip(fields[::2], fields[1::2]):
        parts = path.split("/")
        if change not in {"A", "M", "D", "T"} or any(
            part in {"", ".", ".."} for part in parts
        ):
            raise ValueError(
                "변경 파일 목록의 상태 또는 상대 경로가 올바르지 않습니다."
            )
        changes.append((change, path))
    return changes


def is_measured_source(path: str, modules: dict[str, tuple[str, ...]]) -> bool:
    parts = PurePosixPath(path).parts
    return (
        len(parts) >= 5
        and parts[0] in modules
        and parts[1] == "src"
        and parts[2] in {"main", "debug"}
        and parts[3] in {"java", "kotlin"}
        and PurePosixPath(path).suffix in {".kt", ".java"}
    )


def source_key(repository: Path, path: str) -> SourceKey | None:
    source = repository / path
    if source.is_symlink() or not source.resolve().is_relative_to(repository):
        raise ValueError("측정 소스는 저장소 안의 일반 파일이어야 합니다.")
    text = source.read_text(encoding="utf-8", errors="strict")
    text = re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.DOTALL)
    packages = re.findall(r"(?m)^[ \t]*package[ \t]+([\w.`]+)", text)
    if len(packages) != 1:
        return None
    package = packages[0].replace("`", "").replace(".", "/")
    return path.split("/", 1)[0], package, PurePosixPath(path).name


def missed_count(element: ElementTree.Element, metric: str) -> int:
    counters = [
        item for item in element.findall("counter") if item.get("type") == metric
    ]
    if len(counters) != 1:
        raise ValueError(f"{element.tag}의 {metric} counter는 하나여야 합니다.")
    counter = counters[0]
    missed = int(counter.attrib["missed"])
    covered = int(counter.attrib["covered"])
    if missed < 0 or covered < 0:
        raise ValueError(f"{metric} counter는 음수일 수 없습니다.")
    return missed


def analyze_pr(
    root: ElementTree.Element,
    repository: Path,
    base_commit: str,
    head_commit: str,
    report_commit: str,
    modules: dict[str, tuple[str, ...]],
    below_modules: set[str],
) -> PrAnalysis:
    repository = repository.resolve()
    if not all(
        re.fullmatch(r"[0-9a-f]{40}", value)
        for value in (base_commit, head_commit, report_commit)
    ):
        raise ValueError(
            "PR base, head와 측정 commit에는 전체 commit SHA가 필요합니다."
        )
    if git_bytes(repository, "rev-parse", "HEAD").decode().strip() != report_commit:
        raise ValueError("소스 checkout과 측정 commit이 다릅니다.")
    git_bytes(repository, "merge-base", "--is-ancestor", head_commit, report_commit)

    # Read package declarations only from the same clean source checkout as the report.
    git_bytes(repository, "diff", "--exit-code", report_commit, "--", *modules)
    merge_base = (
        git_bytes(repository, "merge-base", base_commit, head_commit).decode().strip()
    )
    changes = read_changes(
        git_bytes(
            repository,
            "diff",
            "--name-status",
            "-z",
            "--no-renames",
            f"{merge_base}..{head_commit}",
            "--",
        )
    )
    changed_paths = {path for _, path in changes}
    tracked = git_bytes(repository, "ls-files", "-z", "--", *modules)
    paths = tracked.decode("utf-8", errors="strict").rstrip("\0").split("\0")
    source_keys: dict[str, SourceKey | None] = {}
    source_paths: dict[SourceKey, list[str]] = {}
    for path in paths:
        if not is_measured_source(path, modules):
            continue
        key = source_key(repository, path)
        source_keys[path] = key
        if key is not None:
            source_paths.setdefault(key, []).append(path)

    reports: dict[SourceKey, ElementTree.Element] = {}
    classes: dict[SourceKey, list[str]] = {}
    candidates: list[ReviewCandidate] = []
    for package in root.findall("package"):
        package_name = package.attrib["name"]
        matched_modules = [
            module
            for module, prefixes in modules.items()
            if any(
                package_name == prefix or package_name.startswith(prefix + "/")
                for prefix in prefixes
            )
        ]
        if len(matched_modules) != 1:
            raise ValueError("Kover package를 하나의 모듈에 연결할 수 없습니다.")
        module = matched_modules[0]
        for source in package.findall("sourcefile"):
            key = module, package_name, source.attrib["name"]
            if key in reports:
                raise ValueError("Kover report에 중복 sourcefile이 있습니다.")
            missed_count(source, "LINE")
            missed_count(source, "BRANCH")
            reports[key] = source
        for item in package.findall("class"):
            class_name = item.attrib["name"]
            filename = item.get("sourcefilename", "")
            key = module, package_name, filename
            classes.setdefault(key, []).append(class_name)
            missed_lines = missed_count(item, "LINE")
            missed_branches = missed_count(item, "BRANCH")
            if module not in below_modules or not (missed_lines or missed_branches):
                continue
            matches = sorted(source_paths.get(key, []))
            candidates.append(
                {
                    "module": module,
                    "package": package_name,
                    "class_name": class_name,
                    "source_file": filename,
                    "source_paths": matches,
                    "changed_in_pr": matches[0] in changed_paths
                    if len(matches) == 1
                    else None,
                    "missed_lines": missed_lines,
                    "missed_branches": missed_branches,
                }
            )

    changed_sources: list[ChangedSource] = []
    for change, path in sorted(changes, key=lambda entry: entry[1]):
        if PurePosixPath(path).suffix not in {".kt", ".java"}:
            continue
        key = source_keys.get(path)
        source = reports.get(key) if key else None
        if change == "D":
            status = "deleted"
        elif not is_measured_source(path, modules):
            status = "outside_scope"
        elif key is None or len(source_paths[key]) != 1:
            status = "ambiguous"
        elif source is None:
            status = "not_measured"
        else:
            status = "measured"
        measured_source = source if status == "measured" else None
        changed_sources.append(
            {
                "path": path,
                "change": change,
                "status": status,
                "classes": sorted(classes.get(key, []))
                if key and measured_source is not None
                else [],
                "missed_lines": missed_count(measured_source, "LINE")
                if measured_source is not None
                else None,
                "missed_branches": missed_count(measured_source, "BRANCH")
                if measured_source is not None
                else None,
            }
        )
    candidates.sort(
        key=lambda item: (
            item["module"],
            -item["missed_branches"],
            -item["missed_lines"],
            item["class_name"],
        )
    )
    return {
        "base_commit": base_commit,
        "head_commit": head_commit,
        "merge_base": merge_base,
        "report_commit": report_commit,
        "changed_file_count": len(changes),
        "changed_sources": changed_sources,
        "candidates": candidates,
    }
