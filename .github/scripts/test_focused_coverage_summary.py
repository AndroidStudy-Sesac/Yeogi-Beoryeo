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

    def test_coverage_비율을_열_칸_막대로_표시한다(self) -> None:
        self.assertEqual("█████████░", MODULE.render_bar(91.02))
        self.assertEqual("█████████░", MODULE.render_bar(95.86))
        self.assertEqual("░░░░░░░░░░", MODULE.render_bar(0.0))
        self.assertEqual("██████████", MODULE.render_bar(100.0))

    def test_모듈별_증감을_mermaid_막대_chart로_표시한다(self) -> None:
        module_lines = {
            "app": (2, 4, 50.0),
            "data": (3, 4, 75.0),
            "domain": (4, 4, 100.0),
            "presentation": (1, 4, 25.0),
        }
        module_line_baselines = {
            "app": (2, 4),
            "data": (2, 4),
            "domain": (3, 4),
            "presentation": (2, 4),
        }
        module_branches = {
            "app": (1, 4, 25.0),
            "data": (2, 4, 50.0),
            "domain": (3, 4, 75.0),
            "presentation": (4, 4, 100.0),
        }
        module_branch_baselines = {
            "app": (2, 4),
            "data": (2, 4),
            "domain": (2, 4),
            "presentation": (3, 4),
        }

        chart = "\n".join(
            MODULE.render_delta_chart(
                module_lines,
                module_line_baselines,
                module_branches,
                module_branch_baselines,
            )
        )

        self.assertIn("```mermaid\nxychart-beta", chart)
        self.assertIn("x-axis [app, data, domain, presentation]", chart)
        self.assertIn('y-axis "pp" -25 --> 25', chart)
        self.assertIn("bar [0.00, 25.00, 25.00, -25.00]", chart)
        self.assertIn("bar [-25.00, 0.00, 25.00, 25.00]", chart)
        self.assertIn("accTitle:", chart)
        self.assertIn("accDescr:", chart)

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

    def test_모듈별_package_counter를_합산한다(self) -> None:
        root = MODULE.ElementTree.fromstring(
            "<report>"
            '<package name="com/team/yeogibeoryeo/appguide">'
            '<class name="Nested"><counter type="LINE" missed="100" covered="100"/></class>'
            '<counter type="LINE" missed="1" covered="2"/></package>'
            '<package name="com/team/yeogibeoryeo/navigation/route">'
            '<counter type="LINE" missed="2" covered="5"/></package>'
            '<package name="com/team/yeogibeoryeo/data/repository">'
            '<counter type="LINE" missed="3" covered="7"/></package>'
            '<package name="com/team/yeogibeoryeo/domain/usecase">'
            '<counter type="LINE" missed="4" covered="11"/></package>'
            '<package name="com/team/yeogibeoryeo/presentation/home">'
            '<counter type="LINE" missed="5" covered="13"/></package>'
            "</report>"
        )

        metrics = MODULE.read_module_metrics(root, "LINE")

        self.assertEqual((7, 10), metrics["app"][:2])
        self.assertEqual((7, 10), metrics["data"][:2])
        self.assertEqual((11, 15), metrics["domain"][:2])
        self.assertEqual((13, 18), metrics["presentation"][:2])

    def test_package의_빈_branch는_모듈_합산에서_허용한다(self) -> None:
        root = MODULE.ElementTree.fromstring(
            "<report>"
            '<package name="com/team/yeogibeoryeo/appguide">'
            '<counter type="BRANCH" missed="0" covered="0"/></package>'
            '<package name="com/team/yeogibeoryeo/navigation">'
            '<counter type="BRANCH" missed="1" covered="1"/></package>'
            '<package name="com/team/yeogibeoryeo/data">'
            '<counter type="BRANCH" missed="1" covered="1"/></package>'
            '<package name="com/team/yeogibeoryeo/domain">'
            '<counter type="BRANCH" missed="1" covered="1"/></package>'
            '<package name="com/team/yeogibeoryeo/presentation">'
            '<counter type="BRANCH" missed="1" covered="1"/></package>'
            "</report>"
        )

        metrics = MODULE.read_module_metrics(root, "BRANCH")

        self.assertEqual((1, 2), metrics["app"][:2])

    def test_분류할_수_없는_package가_있으면_실패한다(self) -> None:
        root = MODULE.ElementTree.fromstring(
            "<report>"
            '<package name="com/team/yeogibeoryeo/unknown">'
            '<counter type="LINE" missed="1" covered="1"/></package>'
            "</report>"
        )

        with self.assertRaisesRegex(ValueError, "분류할 수 없는 package"):
            MODULE.read_module_metrics(root, "LINE")

    def test_모듈_합계가_report와_다르면_실패한다(self) -> None:
        aggregate = (10, 20, 50.0)
        module_metrics = {
            "app": (2, 4, 50.0),
            "data": (2, 4, 50.0),
            "domain": (2, 4, 50.0),
            "presentation": (2, 4, 50.0),
        }

        with self.assertRaisesRegex(ValueError, "모듈 합계"):
            MODULE.validate_module_total("LINE", aggregate, module_metrics)

    def test_모듈_baseline_합계가_전체와_다르면_실패한다(self) -> None:
        module_baselines = {
            "app": (2, 4),
            "data": (2, 4),
            "domain": (2, 4),
            "presentation": (2, 4),
        }

        with self.assertRaisesRegex(ValueError, "module baseline 합계"):
            MODULE.validate_baseline_total("Line", (10, 20), module_baselines)

    def test_기준선보다_낮아도_정보성_요약을_출력한다(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            report = pathlib.Path(temp_dir) / "report.xml"
            properties = pathlib.Path(temp_dir) / "gradle.properties"
            report.write_text(
                "<report>"
                '<package name="com/team/yeogibeoryeo/appguide">'
                '<counter type="LINE" missed="1" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></package>'
                '<package name="com/team/yeogibeoryeo/data">'
                '<counter type="LINE" missed="1" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></package>'
                '<package name="com/team/yeogibeoryeo/domain">'
                '<counter type="LINE" missed="1" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></package>'
                '<package name="com/team/yeogibeoryeo/presentation">'
                '<counter type="LINE" missed="1" covered="2"/>'
                '<counter type="BRANCH" missed="1" covered="1"/></package>'
                '<counter type="LINE" missed="4" covered="8"/>'
                '<counter type="BRANCH" missed="4" covered="4"/>'
                "</report>",
                encoding="utf-8",
            )
            properties.write_text(
                "focusedCoverageLineBaselineCovered=10\n"
                "focusedCoverageLineBaselineTotal=12\n"
                "focusedCoverageBranchBaselineCovered=6\n"
                "focusedCoverageBranchBaselineTotal=8\n"
                "focusedCoverageAppLineBaselineCovered=3\n"
                "focusedCoverageAppLineBaselineTotal=3\n"
                "focusedCoverageAppBranchBaselineCovered=2\n"
                "focusedCoverageAppBranchBaselineTotal=2\n"
                "focusedCoverageDataLineBaselineCovered=3\n"
                "focusedCoverageDataLineBaselineTotal=3\n"
                "focusedCoverageDataBranchBaselineCovered=2\n"
                "focusedCoverageDataBranchBaselineTotal=2\n"
                "focusedCoverageDomainLineBaselineCovered=2\n"
                "focusedCoverageDomainLineBaselineTotal=3\n"
                "focusedCoverageDomainBranchBaselineCovered=1\n"
                "focusedCoverageDomainBranchBaselineTotal=2\n"
                "focusedCoveragePresentationLineBaselineCovered=2\n"
                "focusedCoveragePresentationLineBaselineTotal=3\n"
                "focusedCoveragePresentationBranchBaselineCovered=1\n"
                "focusedCoveragePresentationBranchBaselineTotal=2\n",
                encoding="utf-8",
            )
            argv = [
                "focused_coverage_summary.py",
                "--report",
                str(report),
                "--properties",
                str(properties),
                "--artifact-url",
                "https://example.test/report",
                "--policy-url",
                "https://example.test/policy",
            ]

            output = io.StringIO()
            with unittest.mock.patch.object(sys, "argv", argv), unittest.mock.patch(
                "sys.stdout", output
            ):
                MODULE.main()

            summary = output.getvalue()
            self.assertLess(
                summary.index("[상세 HTML/XML report]"),
                summary.index("| 지표 | 현재 | baseline | 차이 |"),
            )
            self.assertIn("| 지표 | 현재 | baseline | 차이 |", summary)
            self.assertIn(
                "| Line | `███████░░░` **66.67% (8/12)** | "
                "83.33% (10/12) | -16.67pp |",
                summary,
            )
            self.assertIn("### 모듈별 coverage", summary)
            self.assertIn("### 모듈별 baseline 대비 변화", summary)
            self.assertIn("```mermaid\nxychart-beta", summary)
            self.assertIn("bar [-33.33, -33.33, 0.00, 0.00]", summary)
            self.assertIn("bar [-50.00, -50.00, 0.00, 0.00]", summary)
            self.assertIn(
                "| `app` | 100.00% → **66.67%** (-33.33pp) | "
                "100.00% → **50.00%** (-50.00pp) | ⚠️ 확인 필요 |",
                summary,
            )
            self.assertIn("<details>", summary)
            self.assertIn("<summary>raw covered/total과 측정 정보</summary>", summary)
            self.assertIn(
                "| `app` | 2/3 | 3/3 | 1/2 | 2/2 |",
                summary,
            )
            self.assertIn("</details>", summary)
            self.assertNotIn("\u00b7", summary)
            self.assertNotIn("검증 기준", summary)
            self.assertNotIn("통과", summary)
            self.assertNotIn("실패", summary)


if __name__ == "__main__":
    unittest.main()
