import importlib.util
import io
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


class FocusedCoverageSummaryTest(unittest.TestCase):
    def test_라인_기준선과_같으면_미달이_아니다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5658, 6292, 5658, 6292))

    def test_라인_기준선보다_낮으면_미달로_비교한다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5600, 6292, 5658, 6292))

    def test_반올림값이_같아도_브랜치_기준선보다_낮으면_미달로_비교한다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(2451, 3267, 2452, 3267))

    def test_기준선보다_높은_비율은_미달이_아니다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(5659, 6292, 5658, 6292))

    def test_분모와_분자가_같은_비율로_증가하면_미달이_아니다(self) -> None:
        self.assertFalse(MODULE.is_below_baseline(11316, 12584, 5658, 6292))

    def test_미실행_라인이_추가되면_기준선보다_낮아진다(self) -> None:
        self.assertTrue(MODULE.is_below_baseline(5658, 6357, 5658, 6292))

    def test_기준선_분모가_올바르지_않으면_실패한다(self) -> None:
        properties = {
            "focusedCoverageLineBaselineCovered": "1",
            "focusedCoverageLineBaselineTotal": "0",
        }
        with self.assertRaises(ValueError):
            MODULE.read_baseline(properties, "Line")

    def test_report에_필수_counter가_없으면_실패한다(self) -> None:
        root = MODULE.ElementTree.fromstring(
            '<report><counter type="BRANCH" missed="815" covered="2452"/></report>'
        )

        with self.assertRaisesRegex(ValueError, "LINE counter"):
            MODULE.read_metric(root, "LINE")

    def test_기준선보다_낮아도_정보성_요약을_출력한다(self) -> None:
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
                "focusedCoverageBranchBaselineTotal=3267\n",
                encoding="utf-8",
            )
            argv = [
                "focused_coverage_summary.py",
                "--report",
                str(report),
                "--properties",
                str(properties),
            ]

            output = io.StringIO()
            with unittest.mock.patch.object(sys, "argv", argv), unittest.mock.patch(
                "sys.stdout", output
            ):
                MODULE.main()

            summary = output.getvalue()
            self.assertIn("| 지표 | 현재 | baseline | 차이 |", summary)
            self.assertIn("| Line | 89.00% (5,600/6,292) | 89.92% | -0.92pp |", summary)
            self.assertNotIn("검증 기준", summary)
            self.assertNotIn("통과", summary)
            self.assertNotIn("실패", summary)


if __name__ == "__main__":
    unittest.main()
