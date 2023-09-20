package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

public class OffsetCommitsMetrics {

    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    OffsetCommitsMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesCounter skippedCounter() {
        return HermesCounters.from(
                meterRegistry.counter("offset-commits.skipped"),
                hermesMetrics.counter("offset-committer.skipped")
        );
    }

    public HermesCounter obsoleteCounter() {
        return HermesCounters.from(
                meterRegistry.counter("offset-commits.obsolete"),
                hermesMetrics.counter("offset-committer.obsolete")
        );
    }

    public HermesCounter committedCounter() {
        return HermesCounters.from(
                meterRegistry.counter("offset-commits.committed"),
                hermesMetrics.counter("offset-committer.committed")
        );
    }

    public HermesTimer duration() {
        return HermesTimer.from(
                meterRegistry.timer("offset-commits.duration"),
                hermesMetrics.timer("offset-committer.duration")
        );
    }

    public HermesCounter failuresCounter() {
        return HermesCounters.from(
                meterRegistry.counter("offset-commits.failures"),
                hermesMetrics.counter("offset-committer.failed")
        );
    }
}
