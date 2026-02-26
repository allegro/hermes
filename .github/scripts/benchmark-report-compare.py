import json
import sys
from dataclasses import dataclass
from enum import Enum
from typing import NamedTuple


class ComparisonStatus(Enum):
    PASS = "✅ PASS"
    FAIL = "❌ FAIL"
    NEW = "⚠️ NEW"
    REMOVED = "⚠️ REMOVED"


class BenchmarkMetric(NamedTuple):
    name: str
    unit: str
    value: float

    @property
    def key(self) -> tuple[str, str]:
        return self.name, self.unit


@dataclass
class ComparisonResult:
    name: str
    status: ComparisonStatus
    change_percent: float | None = None
    direction: str | None = None

    def format(self) -> str:
        change = f"{self.direction} {self.change_percent:.2f}%" if self.change_percent is not None and self.direction is not None else "N/A"
        return f"| {self.name} | {self.status.value} | {change} |"


BENCHMARK_PREFIX = "pl.allegro.tech.hermes.benchmark."


def strip_benchmark_prefix(name: str) -> str:
    return name.removeprefix(BENCHMARK_PREFIX)


def load_metrics(file_path: str) -> list[BenchmarkMetric]:
    with open(file_path) as f:
        return [BenchmarkMetric(strip_benchmark_prefix(m["name"]), m["unit"], m["value"]) for m in json.load(f)]


def print_header() -> None:
    print("| Metric | Status | Change |")
    print("|--------|--------|--------|")


def compare_metric(
    metric: BenchmarkMetric, base_value: float, threshold: float
) -> ComparisonResult:
    diff = abs(metric.value - base_value) / base_value
    percent = diff * 100
    direction = "↑" if metric.value > base_value else "↓"
    status = ComparisonStatus.FAIL if percent > threshold else ComparisonStatus.PASS
    return ComparisonResult(metric.name, status, percent, direction)


def compare_benchmarks(base_path: str, current_path: str, threshold: float) -> bool:
    base_metrics = {m.key: m.value for m in load_metrics(base_path)}
    current_metrics = sorted(load_metrics(current_path), key=lambda m: m.name)
    current_keys = {m.key for m in current_metrics}

    results: list[ComparisonResult] = []

    for metric in current_metrics:
        base_value = base_metrics.get(metric.key)
        if base_value is None:
            results.append(ComparisonResult(metric.name, ComparisonStatus.NEW))
        else:
            results.append(compare_metric(metric, base_value, threshold))

    for key in sorted(base_metrics.keys()):
        if key not in current_keys:
            results.append(ComparisonResult(key[0], ComparisonStatus.REMOVED))

    has_failures = any(r.status == ComparisonStatus.FAIL for r in results)
    print_summary(has_failures)
    print_header()

    for result in results:
        print(result.format())

    return has_failures


def print_summary(has_failures):
    summary = "❌ Benchmark comparison: FAILED" if has_failures else "✅ Benchmark comparison: PASSED"
    print(f"### {summary}\n")


def write_comparison_result(result: str, file_path: str) -> None:
    with open(file_path, "w") as f:
        f.write(result + "\n")


def main() -> None:
    if len(sys.argv) != 5:
        print("Usage: benchmark-report-compare.py <base_path> <current_path> <threshold> <result_file>")
        sys.exit(2)

    base_path, current_path, threshold, result_file = sys.argv[1], sys.argv[2], float(sys.argv[3]), sys.argv[4]

    if compare_benchmarks(base_path, current_path, threshold):
        write_comparison_result("failed", result_file)
    else:
        write_comparison_result("passed", result_file)


if __name__ == "__main__":
    main()
