package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.EXECUTOR_NAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.PARTITION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Counters {

    public static final String PUBLISHED = "published." + GROUP + "." + TOPIC,
            UNPUBLISHED = "unpublished." + GROUP + "." + TOPIC,
            DELIVERED = "delivered." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            DISCARDED = "discarded." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            FILTERED = "filtered." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            INFLIGHT = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            OFFSET_COMMIT_IDLE = "offset-commit-idle." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + "." + PARTITION,
            EXECUTOR_RUNNING = "executors." + EXECUTOR_NAME + ".running",
            SCHEDULED_EXECUTOR_OVERRUN = "executors." + EXECUTOR_NAME + ".overrun";
}
