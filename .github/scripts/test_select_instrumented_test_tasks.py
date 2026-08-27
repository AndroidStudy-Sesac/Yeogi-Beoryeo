import unittest

from select_instrumented_test_tasks import build_matrix, select_modules


ALL_MODULES = ("app", "data", "presentation")


class SelectInstrumentedTestTasksTest(unittest.TestCase):
    def test_selects_changed_modules_without_duplicates(self) -> None:
        self.assertEqual(
            select_modules(
                "pull_request",
                [
                    "data/src/main/ItemRepository.kt",
                    "presentation/src/main/ItemScreen.kt",
                    "data/src/test/ItemRepositoryTest.kt",
                ],
            ),
            ("data", "presentation"),
        )

    def test_selects_all_modules_for_shared_or_build_changes(self) -> None:
        full_test_paths = (
            "common/src/main/Theme.kt",
            ".github/workflows/android-ci.yml",
            "gradle/libs.versions.toml",
            "app/build.gradle.kts",
            "build.gradle.kts",
            "gradle.properties",
            "settings.gradle.kts",
            "gradlew",
            ".github/scripts/select_instrumented_test_tasks.py",
        )

        for path in full_test_paths:
            with self.subTest(path=path):
                self.assertEqual(
                    select_modules("pull_request", [path]),
                    ALL_MODULES,
                )

    def test_skips_instrumented_tests_for_unrelated_pr_changes(self) -> None:
        self.assertEqual(
            select_modules(
                "pull_request",
                [
                    "domain/src/main/NormalizeItemNameUseCase.kt",
                    ".github/scripts/check_release_warnings.py",
                    ".github/scripts/focused_coverage_summary.py",
                    ".github/scripts/test_check_release_warnings.py",
                ],
            ),
            (),
        )

    def test_selects_all_modules_for_push(self) -> None:
        self.assertEqual(select_modules("push", []), ALL_MODULES)

    def test_falls_back_to_all_modules_when_pr_scope_is_unknown(self) -> None:
        self.assertEqual(select_modules("pull_request", []), ALL_MODULES)
        self.assertEqual(
            select_modules("pull_request", ["unexpected/path.txt"]),
            ALL_MODULES,
        )

    def test_builds_one_matrix_entry_per_module(self) -> None:
        self.assertEqual(
            build_matrix(("app", "data")),
            {
                "include": [
                    {
                        "module": "app",
                        "task": ":app:pixel9ProApi36DebugAndroidTest",
                    },
                    {
                        "module": "data",
                        "task": ":data:pixel9ProApi36DebugAndroidTest",
                    },
                ],
            },
        )

    def test_builds_valid_matrix_when_no_module_is_selected(self) -> None:
        self.assertEqual(
            build_matrix(()),
            {"include": [{"module": "none", "task": ""}]},
        )


if __name__ == "__main__":
    unittest.main()
