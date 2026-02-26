#!/usr/bin/env python3
"""
Transforms JMH results.json into a simplified benchmark format.
"""

import json
import sys
from pathlib import Path
from typing import Any


def transform_jmh_results(input_file: Path) -> list:
    jmh_results = read_report(input_file)

    transformed_results = []

    for benchmark in jmh_results:
        transformed_results.append(transform_primary_metric(benchmark))
        transformed_results.append(transform_gc_metric(benchmark))
    return transformed_results


def read_report(input_file) -> Any:
    with open(input_file, 'r') as f:
        jmh_results = json.load(f)
    return jmh_results


def transform_gc_metric(benchmark) -> Any:
    secondary_metrics = benchmark.get('secondaryMetrics', {})
    gc_alloc_rate_norm = secondary_metrics.get('gc.alloc.rate.norm')
    gc_result = {
        'name': f"{benchmark['benchmark']} ({benchmark['mode']}) - gc.alloc.rate.norm",
        'unit': gc_alloc_rate_norm['scoreUnit'],
        'value': gc_alloc_rate_norm['score']
    }
    return gc_result


def transform_primary_metric(benchmark) -> Any:
    primary_metric = benchmark['primaryMetric']
    primary_result = {
        'name': f"{benchmark['benchmark']} ({benchmark['mode']})",
        'unit': primary_metric['scoreUnit'],
        'value': primary_metric['score']
    }

    score_error = primary_metric.get('scoreError')
    if score_error is not None and str(score_error) != 'NaN':
        primary_result['range'] = str(score_error)
    return primary_result


def main():
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <jmh-results.json>", file=sys.stderr)
        sys.exit(1)

    input_file = Path(sys.argv[1])
    if not input_file.exists():
        print(f"Error: JMH results file not found: {input_file}", file=sys.stderr)
        sys.exit(1)

    transformed_results = transform_jmh_results(input_file)
    print(json.dumps(transformed_results, indent=2))


if __name__ == '__main__':
    main()
