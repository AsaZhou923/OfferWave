#!/usr/bin/env python3
"""Run the public job-list latency SLO check without third-party packages."""

from __future__ import annotations

import argparse
import concurrent.futures
import json
import math
import statistics
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class Scenario:
    name: str
    path: str
    p95_limit_seconds: float


@dataclass(frozen=True)
class Sample:
    elapsed_seconds: float
    ok: bool
    error: str | None = None


@dataclass(frozen=True)
class ScenarioResult:
    name: str
    requests: int
    successes: int
    failures: int
    p50_seconds: float
    p95_seconds: float
    max_seconds: float
    p95_limit_seconds: float
    passed: bool
    errors: list[str]


SCENARIOS = (
    Scenario("job_list", "/jobs?page=1&size=20&sort=newest", 3.0),
    Scenario("job_search", "/jobs?page=1&size=20&sort=newest&keyword=java", 2.0),
)


def percentile(values: list[float], percentile_value: float) -> float:
    if not values:
        return math.inf
    ordered = sorted(values)
    index = max(0, math.ceil(percentile_value * len(ordered)) - 1)
    return ordered[index]


def fetch(url: str, timeout_seconds: float) -> Sample:
    started = time.perf_counter()
    try:
        request = urllib.request.Request(
            url,
            headers={"Accept": "application/json", "User-Agent": "offerwave-slo-check/1.0"},
        )
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            payload = response.read()
            status = response.status
        elapsed = time.perf_counter() - started
        if status != 200:
            return Sample(elapsed, False, f"HTTP {status}")
        try:
            body = json.loads(payload)
        except json.JSONDecodeError:
            return Sample(elapsed, False, "response is not JSON")
        if body.get("code") != 200:
            return Sample(elapsed, False, f"business code {body.get('code')}")
        return Sample(elapsed, True)
    except (urllib.error.URLError, TimeoutError, OSError) as exc:
        return Sample(time.perf_counter() - started, False, str(exc))


def run_scenario(
    base_url: str,
    scenario: Scenario,
    requests: int,
    concurrency: int,
    timeout_seconds: float,
    warmup_requests: int,
) -> ScenarioResult:
    url = base_url.rstrip("/") + scenario.path
    for _ in range(warmup_requests):
        fetch(url, timeout_seconds)

    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        samples = list(
            executor.map(
                lambda _: fetch(url, timeout_seconds),
                range(requests),
            )
        )

    successful_elapsed = [sample.elapsed_seconds for sample in samples if sample.ok]
    errors = sorted({sample.error for sample in samples if sample.error})
    successes = len(successful_elapsed)
    failures = requests - successes
    p95 = percentile(successful_elapsed, 0.95)
    return ScenarioResult(
        name=scenario.name,
        requests=requests,
        successes=successes,
        failures=failures,
        p50_seconds=statistics.median(successful_elapsed) if successful_elapsed else math.inf,
        p95_seconds=p95,
        max_seconds=max(successful_elapsed, default=math.inf),
        p95_limit_seconds=scenario.p95_limit_seconds,
        passed=failures == 0 and p95 <= scenario.p95_limit_seconds,
        errors=errors[:5],
    )


def write_report(results: Iterable[ScenarioResult], output: Path | None) -> str:
    payload = {
        "generated_at_epoch": int(time.time()),
        "passed": all(result.passed for result in results),
        "results": [asdict(result) for result in results],
    }
    text = json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False)
    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(text + "\n", encoding="utf-8")
    return text


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify OfferWave public job-list p95 latency and response correctness.",
    )
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1:8080/api/v1",
        help="API v1 base URL (default: %(default)s)",
    )
    parser.add_argument("--requests", type=int, default=50)
    parser.add_argument("--concurrency", type=int, default=5)
    parser.add_argument("--warmup", type=int, default=5)
    parser.add_argument("--timeout", type=float, default=5.0)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.requests < 1 or args.concurrency < 1 or args.warmup < 0 or args.timeout <= 0:
        parser.error("requests/concurrency must be positive, warmup non-negative, timeout positive")
    return args


def main() -> int:
    args = parse_args()
    results = [
        run_scenario(
            args.base_url,
            scenario,
            args.requests,
            args.concurrency,
            args.timeout,
            args.warmup,
        )
        for scenario in SCENARIOS
    ]
    try:
        report = write_report(results, args.output)
    except ValueError:
        # Infinity is useful internally but invalid JSON; all-failure results are
        # normalized for a stable machine-readable report.
        normalized = [
            ScenarioResult(
                **{
                    **asdict(result),
                    "p50_seconds": result.p50_seconds if math.isfinite(result.p50_seconds) else -1.0,
                    "p95_seconds": result.p95_seconds if math.isfinite(result.p95_seconds) else -1.0,
                    "max_seconds": result.max_seconds if math.isfinite(result.max_seconds) else -1.0,
                }
            )
            for result in results
        ]
        report = write_report(normalized, args.output)
    print(report)
    return 0 if all(result.passed for result in results) else 1


if __name__ == "__main__":
    sys.exit(main())
