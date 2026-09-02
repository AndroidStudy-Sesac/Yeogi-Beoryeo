import json
import pathlib
import subprocess
import sys
import tempfile
import unittest
from xml.etree import ElementTree

from focused_coverage_analysis import analyze_pr, read_changes
from focused_coverage_summary import render_analysis_html, render_analysis_markdown

MODULES = {
    "data": ("example/data",),
    "domain": ("example/domain",),
}


def report_xml() -> ElementTree.Element:
    return ElementTree.fromstring("""<report>
      <package name="example/data">
        <class name="example/data/SharedKt" sourcefilename="Shared.kt">
          <method name="nested"><counter type="LINE" missed="90" covered="0"/></method>
          <counter type="LINE" missed="3" covered="7"/>
          <counter type="BRANCH" missed="2" covered="2"/>
        </class>
        <class name="example/data/SharedKt$lambda" sourcefilename="Shared.kt">
          <counter type="LINE" missed="2" covered="1"/>
          <counter type="BRANCH" missed="0" covered="0"/>
        </class>
        <class name="example/data/Untouched" sourcefilename="Untouched.kt">
          <counter type="LINE" missed="4" covered="1"/>
          <counter type="BRANCH" missed="3" covered="1"/>
        </class>
        <sourcefile name="Shared.kt">
          <counter type="LINE" missed="3" covered="7"/>
          <counter type="BRANCH" missed="2" covered="2"/>
        </sourcefile>
        <sourcefile name="Untouched.kt">
          <counter type="LINE" missed="4" covered="1"/>
          <counter type="BRANCH" missed="3" covered="1"/>
        </sourcefile>
      </package>
      <package name="example/domain">
        <class name="example/domain/Shared" sourcefilename="Shared.kt">
          <counter type="LINE" missed="1" covered="9"/>
          <counter type="BRANCH" missed="0" covered="0"/>
        </class>
        <sourcefile name="Shared.kt">
          <counter type="LINE" missed="1" covered="9"/>
          <counter type="BRANCH" missed="0" covered="0"/>
        </sourcefile>
      </package>
    </report>""")


class FocusedCoverageAnalysisTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.repo = pathlib.Path(self.temp.name)
        self.git("init", "--initial-branch=base")
        self.data = "data/src/main/kotlin/different/folder/Shared.kt"
        self.domain = "domain/src/main/kotlin/example/domain/Shared.kt"
        self.untouched = "data/src/main/kotlin/example/data/Untouched.kt"
        self.deleted = "data/src/main/kotlin/example/data/Deleted.kt"
        for path, package in (
            (self.data, "example.data"),
            (self.domain, "example.domain"),
            (self.untouched, "example.data"),
            (self.deleted, "example.data"),
        ):
            self.write(path, f"/* package fake.name */\npackage {package}\n")
        self.base = self.commit()
        self.git("checkout", "-b", "pr")
        self.write(self.data, "package example.data\nfun answer() = 42\n")
        self.missing = "data/src/main/kotlin/example/data/UnlistedLogic.kt"
        self.write(self.missing, "package example.data\nclass UnlistedLogic\n")
        self.test = "data/src/test/kotlin/example/data/UnlistedLogicTest.kt"
        self.write(self.test, "package example.data\nclass UnlistedLogicTest\n")
        (self.repo / self.deleted).unlink()
        self.head = self.commit()

    def git(self, *arguments: str) -> str:
        return subprocess.run(
            ["git", "-C", str(self.repo), *arguments],
            check=True,
            capture_output=True,
            encoding="utf-8",
        ).stdout.strip()

    def write(self, path: str, text: str) -> None:
        file = self.repo / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")

    def commit(self) -> str:
        self.git("add", ".")
        self.git(
            "-c",
            "user.name=Coverage Test",
            "-c",
            "user.email=coverage@example.invalid",
            "commit",
            "-m",
            "test input",
        )
        return self.git("rev-parse", "HEAD")

    def analyze(
        self, root: ElementTree.Element | None = None, below: set[str] | None = None
    ):
        return analyze_pr(
            report_xml() if root is None else root,
            self.repo,
            self.base,
            self.head,
            self.git("rev-parse", "HEAD"),
            MODULES,
            {"data", "domain"} if below is None else below,
        )

    def test_actual_git_boundary_and_direct_source_counters(self) -> None:
        analysis = self.analyze()
        rows = {row["path"]: row for row in analysis["changed_sources"]}
        measured = rows[self.data]
        self.assertEqual("measured", measured["status"])
        self.assertEqual(3, measured["missed_lines"])
        self.assertEqual(2, measured["missed_branches"])
        self.assertEqual(
            ["example/data/SharedKt", "example/data/SharedKt$lambda"],
            measured["classes"],
        )
        self.assertEqual("not_measured", rows[self.missing]["status"])
        self.assertEqual("outside_scope", rows[self.test]["status"])
        self.assertEqual("deleted", rows[self.deleted]["status"])
        self.assertNotIn(self.domain, rows)
        self.assertEqual(4, analysis["changed_file_count"])
        self.assertEqual(self.base, analysis["merge_base"])

    def test_same_filename_is_resolved_by_module_and_declared_package(self) -> None:
        candidates = {row["class_name"]: row for row in self.analyze()["candidates"]}
        self.assertEqual(
            [self.data], candidates["example/data/SharedKt"]["source_paths"]
        )
        self.assertTrue(candidates["example/data/SharedKt"]["changed_in_pr"])
        self.assertEqual(
            [self.domain], candidates["example/domain/Shared"]["source_paths"]
        )
        self.assertFalse(candidates["example/domain/Shared"]["changed_in_pr"])
        self.assertEqual(0, candidates["example/domain/Shared"]["missed_branches"])

    def test_multiple_module_candidates_and_unchanged_classes_are_not_causes(
        self,
    ) -> None:
        candidates = self.analyze()["candidates"]
        self.assertEqual(
            ["data", "data", "data", "domain"], [c["module"] for c in candidates]
        )
        self.assertEqual("example/data/Untouched", candidates[0]["class_name"])
        self.assertFalse(candidates[0]["changed_in_pr"])
        self.assertTrue(
            all(
                c["module"] == "domain"
                for c in self.analyze(below={"domain"})["candidates"]
            )
        )
        self.assertEqual([], self.analyze(below=set())["candidates"])

    def test_ambiguous_or_unmapped_sources_are_not_marked_unchanged(self) -> None:
        duplicate = "data/src/debug/java/example/data/Shared.kt"
        self.write(duplicate, "package example.data\n")
        self.head = self.commit()
        root = report_xml()
        root.find("package/class").set("sourcefilename", "Missing.kt")
        analysis = self.analyze(root)
        rows = {row["path"]: row for row in analysis["changed_sources"]}
        self.assertEqual("ambiguous", rows[self.data]["status"])
        self.assertEqual("ambiguous", rows[duplicate]["status"])
        candidates = {row["class_name"]: row for row in analysis["candidates"]}
        self.assertIsNone(candidates["example/data/SharedKt"]["changed_in_pr"])
        self.assertIsNone(candidates["example/data/SharedKt$lambda"]["changed_in_pr"])

    def test_base_only_changes_are_excluded_from_pr_diff(self) -> None:
        self.git("checkout", "base")
        base_only = "data/src/main/kotlin/example/data/BaseOnly.kt"
        self.write(base_only, "package example.data\n")
        newer_base = self.commit()
        original_base = self.base
        self.base = newer_base
        self.git("checkout", "pr")
        self.git(
            "-c",
            "user.name=Coverage Test",
            "-c",
            "user.email=coverage@example.invalid",
            "merge",
            "--no-ff",
            "base",
            "-m",
            "merge base",
        )
        analysis = self.analyze()
        self.assertEqual(original_base, analysis["merge_base"])
        self.assertNotIn(
            base_only, [row["path"] for row in analysis["changed_sources"]]
        )
        self.assertNotEqual(analysis["head_commit"], analysis["report_commit"])

    def test_rename_is_explicit_delete_and_add(self) -> None:
        renamed = "data/src/main/kotlin/example/data/Renamed.kt"
        self.git("mv", self.untouched, renamed)
        self.head = self.commit()
        rows = {row["path"]: row for row in self.analyze()["changed_sources"]}
        self.assertEqual("deleted", rows[self.untouched]["status"])
        self.assertEqual("not_measured", rows[renamed]["status"])

    def test_git_failure_commit_mismatch_and_dirty_source_do_not_become_empty(
        self,
    ) -> None:
        with self.assertRaises(ValueError):
            analyze_pr(
                report_xml(), self.repo, "--help", self.head, self.head, MODULES, set()
            )
        with self.assertRaises(ValueError):
            analyze_pr(
                report_xml(), self.repo, self.base, self.head, self.base, MODULES, set()
            )
        with self.assertRaises(subprocess.CalledProcessError):
            analyze_pr(
                report_xml(), self.repo, "0" * 40, self.head, self.head, MODULES, set()
            )
        self.write(self.data, "package changed.package\n")
        with self.assertRaises(subprocess.CalledProcessError):
            self.analyze()

    def test_invalid_report_counters_fail_analysis(self) -> None:
        root = report_xml()
        root.find("package/class/counter[@type='LINE']").set("missed", "-1")
        with self.assertRaises(ValueError):
            self.analyze(root)
        root = report_xml()
        source = root.find("package/sourcefile")
        source.remove(source.find("counter[@type='BRANCH']"))
        with self.assertRaises(ValueError):
            self.analyze(root)

    def test_report_commit_must_include_the_pr_head(self) -> None:
        self.git("checkout", "base")
        with self.assertRaises(subprocess.CalledProcessError):
            self.analyze()

    def test_nul_diff_preserves_paths_and_rejects_malformed_input(self) -> None:
        self.assertEqual(
            [("M", "data/a\nb.kt"), ("A", "data/space name.kt")],
            read_changes(b"M\0data/a\nb.kt\0A\0data/space name.kt\0"),
        )
        for data in (b"M\0path", b"M\0", b"R100\0old\0new\0", b"A\0../secret.kt\0"):
            with self.subTest(data=data), self.assertRaises(ValueError):
                read_changes(data)

    def test_cli_emits_consistent_html_json_and_markdown_below_baseline(self) -> None:
        # Exercise the real CLI, Git history, XML parser and all output formats together.
        for path in self.repo.glob("*/src/**/*.kt"):
            path.write_text(
                path.read_text(encoding="utf-8").replace(
                    "example.", "com.team.yeogibeoryeo."
                ),
                encoding="utf-8",
            )
        self.head = self.commit()
        root = ElementTree.fromstring(
            ElementTree.tostring(report_xml(), encoding="unicode").replace(
                "example/", "com/team/yeogibeoryeo/"
            )
        )
        for name in ("appguide", "presentation"):
            package = ElementTree.SubElement(
                root, "package", name=f"com/team/yeogibeoryeo/{name}"
            )
            source = ElementTree.SubElement(package, "sourcefile", name="Other.kt")
            for metric in ("LINE", "BRANCH"):
                ElementTree.SubElement(
                    source, "counter", type=metric, missed="1", covered="1"
                )
        domain = root.find("package[@name='com/team/yeogibeoryeo/domain']")
        for counter in domain.findall(".//counter[@type='BRANCH']"):
            counter.set("covered", "1")
        properties = []
        for metric in ("LINE", "BRANCH"):
            aggregate_covered = aggregate_missed = 0
            for package in root.findall("package"):
                counters = package.findall(f"sourcefile/counter[@type='{metric}']")
                covered = sum(int(c.attrib["covered"]) for c in counters)
                missed = sum(int(c.attrib["missed"]) for c in counters)
                ElementTree.SubElement(
                    package,
                    "counter",
                    type=metric,
                    covered=str(covered),
                    missed=str(missed),
                )
                module = (
                    package.attrib["name"].rsplit("/", 1)[1].replace("appguide", "app")
                )
                for suffix in ("Covered", "Total"):
                    properties.append(
                        f"focusedCoverage{module.title()}{metric.title()}Baseline{suffix}={covered + missed}"
                    )
                aggregate_covered += covered
                aggregate_missed += missed
            ElementTree.SubElement(
                root,
                "counter",
                type=metric,
                covered=str(aggregate_covered),
                missed=str(aggregate_missed),
            )
            for suffix in ("Covered", "Total"):
                properties.append(
                    f"focusedCoverage{metric.title()}Baseline{suffix}={aggregate_covered + aggregate_missed}"
                )
        report = self.repo / "report.xml"
        report.write_text(
            ElementTree.tostring(root, encoding="unicode"), encoding="utf-8"
        )
        config = self.repo / "gradle.properties"
        config.write_text("\n".join(properties), encoding="utf-8")
        html = self.repo / "output/summary.html"
        script = pathlib.Path(__file__).with_name("focused_coverage_summary.py")
        result = subprocess.run(
            [
                sys.executable,
                "-X",
                "utf8",
                "-B",
                str(script),
                "--report",
                str(report),
                "--properties",
                str(config),
                "--repository",
                str(self.repo),
                "--commit",
                self.head,
                "--base-commit",
                self.base,
                "--head-commit",
                self.head,
                "--html-output",
                str(html),
            ],
            check=True,
            capture_output=True,
            encoding="utf-8",
        )
        structured = json.loads(
            html.with_name("analysis.json").read_text(encoding="utf-8")
        )
        analysis = structured["pr_analysis"]
        self.assertEqual(self.head, analysis["report_commit"])
        self.assertEqual(self.base, analysis["merge_base"])
        self.assertIn("data", structured["below_baseline_modules"])
        self.assertEqual(
            "measured",
            next(c for c in analysis["changed_sources"] if c["path"] == self.data)[
                "status"
            ],
        )
        self.assertIn("XML에 없음: 포함 여부 확인", result.stdout)
        self.assertIn("XML에 없음: 포함 여부 확인", html.read_text(encoding="utf-8"))
        self.assertIn("하락 원인으로 확정할 수 없습니다", result.stdout)
        self.assertTrue((html.parent / "fonts/pretendard_regular.otf").is_file())
        self.assertTrue((html.parent / "fonts/THIRD_PARTY_NOTICES.md").is_file())

    def test_report_output_escapes_paths_and_preserves_full_json_candidates(
        self,
    ) -> None:
        analysis = self.analyze()
        path = "data/<img src=x onerror=alert(1)>|`file\n.kt"
        analysis["changed_sources"][0]["path"] = path
        candidate = analysis["candidates"][0]
        analysis["candidates"] = [
            {**candidate, "class_name": f"data/Class{number}"} for number in range(9)
        ]
        markdown = "\n".join(render_analysis_markdown(analysis))
        html = render_analysis_html(analysis)
        self.assertNotIn("<img", markdown)
        self.assertNotIn("<img", html)
        self.assertIn("&#124;", markdown)
        self.assertIn("&lt;img", html)
        self.assertIn("Class4", html)
        self.assertNotIn("Class5", html)
        self.assertEqual(9, len(analysis["candidates"]))


if __name__ == "__main__":
    unittest.main()
