import importlib.util
import pathlib
import sys
import tempfile
import unittest
import unittest.mock

SCRIPT_PATH = pathlib.Path(__file__).with_name("focused_coverage_summary.py")
SPEC = importlib.util.spec_from_file_location("focused_coverage_summary", SCRIPT_PATH)
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("focused coverage summary module을 불러올 수 없습니다.")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FocusedCoverageBaselineTest(unittest.TestCase):
    def test_exact_line_baseline_passes(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5658, 6292, 5658, 6292))

    def test_line_ratio_below_baseline_fails_even_above_floor(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5600, 6292, 5658, 6292))

    def test_branch_ratio_below_baseline_fails_even_when_rounded_equal(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(2451, 3267, 2452, 3267))

    def test_ratio_above_baseline_passes(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5659, 6292, 5658, 6292))

    def test_same_ratio_with_larger_counts_passes(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(11316, 12584, 5658, 6292))

    def test_added_uncovered_line_fails_even_above_floor(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5658, 6357, 5658, 6292))

    def test_invalid_baseline_total_fails(self) -> None:
        properties = {
            "focusedCoverageLineBaselineCovered": "1",
            "focusedCoverageLineBaselineTotal": "0",
        }
        with self.assertRaises(ValueError):
            MODULE.read_baseline(properties, "Line")

    def test_verify_mode_exits_for_baseline_regression(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            report = pathlib.Path(temp_dir) / "report.xml"
            properties = pathlib.Path(temp_dir) / "gradle.properties"
            report.write_text(
                '<report><counter type="LINE" missed="692" covered="5600"/>'
                '<counter type="BRANCH" missed="815" covered="2452"/></report>',
                encoding="utf-8",
            )
            properties.write_text(
                "focusedCoverageLineBaselineCovered=5658\n"
                "focusedCoverageLineBaselineTotal=6292\n"
                "focusedCoverageBranchBaselineCovered=2452\n"
                "focusedCoverageBranchBaselineTotal=3267\n"
                "focusedCoverageLineMinimum=89\n"
                "focusedCoverageBranchMinimum=75\n",
                encoding="utf-8",
            )
            argv = [
                "focused_coverage_summary.py",
                "--report",
                str(report),
                "--properties",
                str(properties),
                "--verify",
            ]

            with (
                unittest.mock.patch.object(sys, "argv", argv),
                self.assertRaisesRegex(SystemExit, "Line.*baseline 미달"),
            ):
                MODULE.main()


if __name__ == "__main__":
    unittest.main()
