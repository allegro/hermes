package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.MetricRegistryPathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.MetricRegistryPathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.MetricRegistryPathsCompiler.TOPIC;

public class Counters {

    public static final String PUBLISHED = "published." + GROUP + "." + TOPIC;
    public static final String DELIVERED = "delivered." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
    public static final String DISCARDED = "discarded." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
    public static final String INFLIGHT = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
    public static final String MAXRATE_RATE_HISTORY_FAILURES =
            "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".history.failures";
    public static final String MAXRATE_FETCH_FAILURES =
            "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".fetch.failures";
}
