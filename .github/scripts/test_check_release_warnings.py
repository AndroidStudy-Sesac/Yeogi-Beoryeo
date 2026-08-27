import unittest

from check_release_warnings import analyze_warnings, validate_warnings


POSIX_WARNING = (
    "WARNING: /home/runner/.gradle/caches/transformed/"
    "map-sdk-3.23.2-runtime.jar: R8: Expected stack map table for method with "
    "non-linear control flow. In later version of R8, the method may be assumed "
    "not reachable."
)
WINDOWS_WARNING = POSIX_WARNING.replace(
    "/home/runner/.gradle/caches/transformed/",
    "C:\\Users\\runner\\.gradle\\caches\\transformed\\",
)


class CheckReleaseWarningsTest(unittest.TestCase):
    def test_accepts_exact_naver_r8_baseline(self) -> None:
        known_count, unexpected = analyze_warnings(
            [POSIX_WARNING] * 292 + [WINDOWS_WARNING] * 292
        )

        self.assertEqual(584, known_count)
        self.assertEqual((), unexpected)
        validate_warnings(known_count, unexpected)

    def test_rejects_naver_r8_count_changes(self) -> None:
        for count in (0, 583, 585):
            with self.subTest(count=count), self.assertRaises(ValueError):
                known_count, unexpected = analyze_warnings([POSIX_WARNING] * count)
                validate_warnings(known_count, unexpected)

    def test_rejects_other_compiler_and_shrinker_warnings(self) -> None:
        warning_lines = (
            "w: file:///FavoriteTab.kt:9:5 Kotlin warning",
            "Example.java:7: warning: Java warning",
            POSIX_WARNING.replace(": R8:", ": D8:"),
            "WARNING: other.jar: R8: Different warning",
        )
        known_count, unexpected = analyze_warnings(
            [POSIX_WARNING] * 584 + list(warning_lines)
        )

        self.assertEqual(584, known_count)
        self.assertEqual(4, len(unexpected))
        with self.assertRaises(ValueError):
            validate_warnings(known_count, unexpected)

    def test_rejects_a_different_map_sdk_version(self) -> None:
        known_count, unexpected = analyze_warnings(
            [POSIX_WARNING] * 584 + [POSIX_WARNING.replace("3.23.2", "3.23.3")]
        )

        self.assertEqual(584, known_count)
        self.assertEqual(1, len(unexpected))
        with self.assertRaises(ValueError):
            validate_warnings(known_count, unexpected)

    def test_ignores_non_diagnostic_warning_text(self) -> None:
        benign_line = "This prose mentions a warning without emitting one."
        known_count, unexpected = analyze_warnings(
            [POSIX_WARNING] * 584 + [benign_line]
        )

        self.assertEqual(584, known_count)
        self.assertEqual((), unexpected)


if __name__ == "__main__":
    unittest.main()
