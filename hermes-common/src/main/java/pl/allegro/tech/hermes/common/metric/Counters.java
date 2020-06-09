package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.EXECUTOR_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Counters {

    public static final String PUBLISHED = "published." + GROUP + "." + TOPIC,
            UNPUBLISHED = "unpublished." + GROUP + "." + TOPIC,
            DELIVERED = "delivered." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            DISCARDED = "discarded." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            INFLIGHT = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            EXECUTOR_RUNNING = "executors." + EXECUTOR_NAME + ".running",
            SCHEDULED_EXECUTOR_OVERRUN = "executors." + EXECUTOR_NAME + ".overrun",
            MAXRATE_RATE_HISTORY_FAILURES =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".history.failures",
            MAXRATE_FETCH_FAILURES =
                    "consumers-rate.max-rate.node." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".fetch.failures";
}
