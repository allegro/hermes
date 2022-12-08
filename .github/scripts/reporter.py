import dataclasses
import os
import sys
from typing import List, Set, Dict
import xml.etree.ElementTree as ET


@dataclasses.dataclass
class TestResults:
    failed: Set[str]
    passed: Set[str]
    run_id: str
    test_cnt: int = 0
    skipped_cnt: int = 0
    failed_cnt: int = 0
    error_cnt: int = 0


def get_test_files(dir: str) -> List[str]:
    files = [
        os.path.join(dp, f)
        for dp, dn, filenames in os.walk(dir)
        for f in filenames
        if os.path.splitext(f)[1] == '.xml'
           and os.path.splitext(f)[0].startswith("TEST-")
    ]
    return files


def parse_test_file(file: str, run_id: str) -> TestResults:
    root = ET.parse(file).getroot()
    result = TestResults(
        skipped_cnt=int(root.get("skipped")),
        test_cnt=int(root.get("tests")),
        failed_cnt=int(root.get("failures")),
        error_cnt=int(root.get("errors")),
        failed=set(),
        passed=set(),
        run_id=run_id
    )
    name = root.get("name")
    name = '.'.join(name.split('.')[4:])  # remove common prefix

    for testcase in root.findall("testcase"):
        testname = testcase.get("name")
        test_failed = testcase.findall("failure")
        if test_failed:
            result.failed.add(f"{name}#{testname}")
        else:
            result.passed.add(f"{name}#{testname}")
    return result


def aggregate_results(test_dir: str, run_id: str) -> TestResults:
    test_files = get_test_files(test_dir)
    results = []
    for test_file in test_files:
        result = parse_test_file(test_file, run_id)
        results.append(result)

    agg = TestResults(set(), set(), run_id)

    for result in results:
        agg.test_cnt += result.test_cnt
        agg.skipped_cnt += result.skipped_cnt
        agg.error_cnt += result.error_cnt
        agg.failed_cnt += result.failed_cnt
        for fail in result.failed:
            agg.failed.add(fail)
        for pas in result.passed:
            agg.passed.add(pas)
    return agg


def report(type: str, runs: List[TestResults]) -> str:
    failed = {}
    markdown = ""
    for run in runs:
        for fail in run.failed:
            if fail in failed:
                failed[fail]["count"] += 1
                failed[fail]["runs"].append(run.run_id)
            else:
                failed[fail] = {"count": 1, "runs": [run.run_id]}
    markdown += f"## {type} \n"
    markdown += "| Test name | Fail count | Failed in runs |\n"
    markdown += "|--|--|--|\n"
    for k, v in sorted(failed.items(), key=lambda item: -item[1]["count"]):
        markdown += f"| {k} | {v['count']} | {v['runs']} |\n"
    markdown += "\n"
    return markdown


if __name__ == '__main__':
    root = sys.argv[1]
    print(f"Parsing tests results from directory: {root}")
    run_dirs = [f.path for f in os.scandir(root) if f.is_dir()]
    print(f"Found {len(run_dirs)} run dirs")
    unittest_runs = []
    e2e_runs = []

    for run in run_dirs:
        run_id = os.path.basename(os.path.normpath(run))
        unit_test_dir = os.path.join(run, "check-test-report")
        unit_test_results = aggregate_results(unit_test_dir, run_id)
        unittest_runs.append(unit_test_results)

        e2e_test_dir = os.path.join(run, "integrationTest-test-report")
        e2e_test_results = aggregate_results(e2e_test_dir, run_id)
        e2e_runs.append(e2e_test_results)

    step_summary = "# Failing tests report\n"
    step_summary += report("Unit tests", unittest_runs)
    step_summary += report("Integration tests", e2e_runs)

    result_file = os.environ["GITHUB_STEP_SUMMARY"]
    with open(result_file, 'w') as f:
        f.write(step_summary)
