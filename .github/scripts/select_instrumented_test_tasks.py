#!/usr/bin/env python3
"""Select Gradle Managed Device tasks for the current CI event."""

from __future__ import annotations

import argparse
import json
import sys
from collections.abc import Iterable


MODULE_TASKS = {
    "app": ":app:pixel9ProApi36DebugAndroidTest",
    "data": ":data:pixel9ProApi36DebugAndroidTest",
    "presentation": ":presentation:pixel9ProApi36DebugAndroidTest",
}
FULL_TEST_PREFIXES = (".github/workflows/", "common/", "gradle/")
FULL_TEST_FILES = {
    ".github/scripts/select_instrumented_test_tasks.py",
    ".github/scripts/test_select_instrumented_test_tasks.py",
    "build.gradle.kts",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "settings.gradle.kts",
}
NO_INSTRUMENTED_TEST_FILES = {
    ".github/scripts/check_release_warnings.py",
    ".github/scripts/focused_coverage_summary.py",
    ".github/scripts/test_check_release_warnings.py",
    ".github/scripts/test_focused_coverage_summary.py",
}


def select_modules(event_name: str, changed_files: Iterable[str]) -> tuple[str, ...]:
    if event_name != "pull_request":
        return tuple(MODULE_TASKS)

    normalized_files = tuple(
        file.strip().replace("\\", "/")
        for file in changed_files
        if file.strip()
    )
    if not normalized_files or any(requires_full_test(file) for file in normalized_files):
        return tuple(MODULE_TASKS)

    modules = tuple(
        module
        for module in MODULE_TASKS
        if any(file.startswith(f"{module}/") for file in normalized_files)
    )
    if any(not is_known_file(file) for file in normalized_files):
        return tuple(MODULE_TASKS)

    return modules


def requires_full_test(file: str) -> bool:
    return (
        file in FULL_TEST_FILES
        or file.endswith(".gradle.kts")
        or file.startswith(FULL_TEST_PREFIXES)
    )


def is_known_file(file: str) -> bool:
    return (
        requires_full_test(file)
        or file in NO_INSTRUMENTED_TEST_FILES
        or file.startswith("domain/")
        or any(file.startswith(f"{module}/") for module in MODULE_TASKS)
    )


def build_matrix(modules: tuple[str, ...]) -> dict[str, list[dict[str, str]]]:
    include = [
        {"module": module, "task": MODULE_TASKS[module]}
        for module in modules
    ]
    return {"include": include or [{"module": "none", "task": ""}]}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--event-name", required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    modules = select_modules(args.event_name, sys.stdin)
    print(f"modules={','.join(modules)}")
    print(f"has_tests={'true' if modules else 'false'}")
    print(f"matrix={json.dumps(build_matrix(modules), separators=(',', ':'))}")


if __name__ == "__main__":
    main()
