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
    def test_라인_기준선과_같으면_통과한다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5658, 6292, 5658, 6292))

    def test_최소선보다_높아도_라인_기준선보다_낮으면_실패한다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5600, 6292, 5658, 6292))

    def test_반올림값이_같아도_브랜치_기준선보다_낮으면_실패한다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(2451, 3267, 2452, 3267))

    def test_기준선보다_높은_비율은_통과한다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5659, 6292, 5658, 6292))

    def test_분모와_분자가_같은_비율로_증가하면_통과한다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(11316, 12584, 5658, 6292))

    def test_미실행_라인이_추가되면_최소선_이상이어도_실패한다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5658, 6357, 5658, 6292))

    def test_기준선_분모가_올바르지_않으면_실패한다(self) -> None:
        properties = {
            "focusedCoverageLineBaselineCovered": "1",
            "focusedCoverageLineBaselineTotal": "0",
        }
        with self.assertRaises(ValueError):
            MODULE.read_baseline(properties, "Line")

    def test_최소선은_0과_100을_허용한다(self) -> None:
        key = "focusedCoverageLineMinimum"

        self.assertEqual(MODULE.require_percentage({key: "0"}, key), 0)
        self.assertEqual(MODULE.require_percentage({key: "100"}, key), 100)

    def test_최소선이_유한한_백분율이_아니면_실패한다(self) -> None:
        key = "focusedCoverageLineMinimum"

        for value in ("숫자 아님", "NaN", "Infinity", "-0.01", "100.01"):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, key):
                MODULE.require_percentage({key: value}, key)

    def test_엄격_검증은_기준선_회귀에서_종료한다(self) -> None:
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
