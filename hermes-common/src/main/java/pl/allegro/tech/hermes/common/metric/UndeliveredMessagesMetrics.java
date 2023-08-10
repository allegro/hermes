package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.DefaultHermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.counters.HermesCounters;

import static pl.allegro.tech.hermes.common.metric.Histograms.PERSISTED_UNDELIVERED_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.common.metric.Meters.PERSISTED_UNDELIVERED_MESSAGES_METER;

public class UndeliveredMessagesMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;

    public UndeliveredMessagesMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
    }

    public HermesCounter undeliveredMessagesCounter() {
        return HermesCounters.from(
                meterRegistry.counter("undelivered-messages.persisted"),
                hermesMetrics.meter(PERSISTED_UNDELIVERED_MESSAGES_METER)
        );
    }

    public HermesHistogram undeliveredMessagesSizeHistogram() {
        return DefaultHermesHistogram.of(
                DistributionSummary.builder("undelivered-messages.persisted.message-size.bytes")
                        .register(meterRegistry),
                hermesMetrics.histogram(PERSISTED_UNDELIVERED_MESSAGE_SIZE)
        );
    }
}
