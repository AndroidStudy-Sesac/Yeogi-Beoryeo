import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

from build_warning_summary import collect_warnings, parse_warning, render_summary

REPOSITORY = "/home/runner/work/Yeogi-Beoryeo/Yeogi-Beoryeo"
SCRIPT = Path(__file__).with_name("build_warning_summary.py")
D8_MESSAGE = "Expected stack map table for method with non-linear control flow."
R8_MESSAGE = (
    D8_MESSAGE + " In later version of R8, the method may be assumed not reachable."
)


class BuildWarningSummaryTest(unittest.TestCase):
    def test_groups_real_sdk_diagnostics_without_losing_occurrences(self) -> None:
        lines = ["> Task :app:dexBuilderDebug"]
        for cache_hash in ("aaa", "bbb"):
            lines.append(
                "WARNING: /home/runner/.gradle/caches/9.4.1/transforms/"
                f"{cache_hash}/transformed/map-sdk-3.23.2-runtime.jar: D8: {D8_MESSAGE}"
            )
        lines.extend(
            [
                "> Task :app:minifyReleaseWithR8",
                "WARNING: /home/runner/.gradle/caches/9.4.1/transforms/"
                f"ccc/transformed/map-sdk-3.23.2-runtime.jar: R8: {R8_MESSAGE}",
            ]
        )
        warnings = collect_warnings(lines, REPOSITORY)
        self.assertEqual(len(warnings), 2)
        self.assertEqual(list(warnings.values()), [[2, 3], [5]])
        self.assertEqual({item.origin for item in warnings}, {"NAVER Map SDK"})
        self.assertEqual({item.tool for item in warnings}, {"D8", "R8"})
        self.assertEqual(
            {item.source for item in warnings}, {"map-sdk-3.23.2-runtime.jar"}
        )
        summary = render_summary(
            warnings, "https://github.com/example/repo/actions/runs/1"
        )
        self.assertIn("경고 3회, 고유 유형 2개", summary)
        self.assertNotIn("transforms/", summary)

    def test_parses_kotlin_compiler_locations_and_ansi(self) -> None:
        for location in (
            f"file://{REPOSITORY}/presentation/src/main/Example.kt:12:8",
            f"{REPOSITORY}/presentation/src/main/Example.kt: (12, 8):",
            "presentation/src/main/Example.kt:12:8:",
        ):
            with self.subTest(location=location):
                warning = parse_warning(
                    f"\x1b[33mw: {location} Unused parameter\x1b[0m", REPOSITORY
                )
                self.assertEqual(warning.origin, "앱 코드")
                self.assertEqual(warning.tool, "Kotlin")
                self.assertEqual(warning.source, "presentation/src/main/Example.kt")
                self.assertEqual(warning.message, "Unused parameter")

    def test_parses_windows_uri_and_java_locations(self) -> None:
        warning = parse_warning(
            "w: file:///C:/project%20name/app/src/Example.kt:2:3 Deprecated API",
            "C:\\project name",
        )
        self.assertEqual(warning.source, "app/src/Example.kt")
        self.assertEqual(warning.origin, "앱 코드")
        warning = parse_warning(
            "C:\\project name\\app\\src\\Example.java:5: warning: unchecked",
            "C:/project name",
        )
        self.assertEqual(warning.source, "app/src/Example.java")
        self.assertEqual(warning.tool, "Java")

    def test_keeps_wrapper_and_unknown_library_separate_from_naver_sdk(self) -> None:
        for filename in ("naver-map-compose-1.9.0.jar", "another-sdk-3.23.2.jar"):
            with self.subTest(filename=filename):
                warning = parse_warning(
                    f"WARNING: /cache/{filename}: D8: {D8_MESSAGE}", REPOSITORY
                )
                self.assertEqual(warning.origin, "외부 라이브러리")
        warning = parse_warning("WARNING: naver request unavailable", REPOSITORY)
        self.assertEqual(warning.origin, "출처 미확인")

    def test_does_not_treat_an_outside_source_tree_as_app_code(self) -> None:
        warning = parse_warning(
            f"w: {REPOSITORY}-other/app/Example.kt:1:1 Deprecated API", REPOSITORY
        )
        self.assertEqual(warning.origin, "출처 미확인")

    def test_classifies_root_and_module_gradle_scripts_as_build_configuration(
        self,
    ) -> None:
        for source in (
            "build.gradle.kts",
            "app/build.gradle.kts",
            "data/build.gradle.kts",
        ):
            with self.subTest(source=source):
                warning = parse_warning(
                    f"w: {REPOSITORY}/{source}:1:1 Deprecated API", REPOSITORY
                )
                self.assertEqual(warning.origin, "빌드 설정")

    def test_preserves_different_artifact_versions_and_messages(self) -> None:
        lines = [
            f"WARNING: /cache/map-sdk-{version}-runtime.jar: R8: {message}"
            for version, message in (
                ("3.23.2", "first"),
                ("3.23.3", "first"),
                ("3.23.3", "second"),
            )
        ]
        self.assertEqual(len(collect_warnings(lines, REPOSITORY)), 3)

    def test_reports_gradle_deprecation_once_without_counting_its_footer(self) -> None:
        warnings = collect_warnings(
            [
                "The old property has been deprecated. It will be removed in Gradle 10.",
                "\tat build_abcd.run(build.gradle:5)",
                "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.",
            ],
            REPOSITORY,
        )
        self.assertEqual(len(warnings), 1)
        self.assertEqual(next(iter(warnings)).tool, "Gradle")

    def test_counts_warning_starts_not_tasks_errors_or_continuations(self) -> None:
        warnings = collect_warnings(
            [
                "> Task :app:compileDebugKotlin UP-TO-DATE",
                "e: file:///app/Example.kt:2:3 Unresolved reference",
                "FAILURE: Build failed with an exception.",
                "WARNING: an unexpected tool warning",
                "    with more details on the next line",
                "BUILD FAILED in 1s",
            ],
            REPOSITORY,
        )
        self.assertEqual(list(warnings.values()), [[4]])
        summary = render_summary(warnings, "")
        self.assertIn("출처 미확인", summary)
        self.assertNotIn("BUILD FAILED", summary)

    def test_empty_or_cached_log_has_a_scoped_zero_message(self) -> None:
        for lines in ([], ["> Task :app:assembleDebug FROM-CACHE", "BUILD SUCCESSFUL"]):
            with self.subTest(lines=lines):
                summary = render_summary(collect_warnings(lines, REPOSITORY), "")
                self.assertIn("이번 빌드 로그에서 경고 0회", summary)
                self.assertIn("캐시로 실행하지 않은 작업", summary)

    def test_escapes_untrusted_diagnostics_and_links(self) -> None:
        warnings = collect_warnings(
            ["WARNING: <script>alert(1)</script> | `x` **x** [link](https://bad.test)"],
            REPOSITORY,
        )
        summary = render_summary(warnings, 'https://github.com/a/b?x="&y=1')
        self.assertNotIn("<script>", summary)
        self.assertIn("&lt;script&gt;", summary)
        self.assertIn("&#124;", summary)
        self.assertIn("&#96;", summary)
        self.assertIn("&#91;", summary)
        self.assertIn("&quot;&amp;y=1", summary)
        with self.assertRaises(ValueError):
            render_summary(warnings, "javascript:alert(1)")

    def test_caps_display_size_while_preserving_total(self) -> None:
        warnings = collect_warnings(
            [f"WARNING: {i} " + "x" * 1000 for i in range(60)], REPOSITORY
        )
        summary = render_summary(warnings, "")
        self.assertIn("경고 60회, 고유 유형 60개", summary)
        self.assertIn("50개 유형을 표시", summary)
        self.assertLess(len(summary), 30000)

    def test_cli_reports_missing_log_as_failure_not_zero(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(
                [
                    sys.executable,
                    "-X",
                    "utf8",
                    "-B",
                    str(SCRIPT),
                    "--log",
                    str(Path(directory) / "missing.log"),
                ],
                capture_output=True,
                text=True,
                encoding="utf-8",
                check=False,
            )
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("집계하지 못했습니다", result.stdout)
        self.assertNotIn("경고 0회", result.stdout)

    def test_cli_accepts_failed_build_log_and_rejects_invalid_utf8(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "build.log"
            command = [
                sys.executable,
                "-X",
                "utf8",
                "-B",
                str(SCRIPT),
                "--log",
                str(log),
            ]
            log.write_text(
                "WARNING: compiler warning\nBUILD FAILED\n", encoding="utf-8"
            )
            result = subprocess.run(
                command, capture_output=True, text=True, encoding="utf-8", check=False
            )
            self.assertEqual(result.returncode, 0)
            self.assertIn("경고 1회", result.stdout)
            log.write_bytes(b"\xff")
            result = subprocess.run(
                command, capture_output=True, text=True, encoding="utf-8", check=False
            )
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn("경고 0회", result.stdout)


if __name__ == "__main__":
    unittest.main()
