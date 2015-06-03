package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.HOSTNAME;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.EXECUTOR_NAME;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.PARTITION;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.TOPIC;

public class Counters {

    public static final String PRODUCER_PUBLISHED = "producer." + HOSTNAME + ".published." + GROUP + "." + TOPIC,
            PRODUCER_UNPUBLISHED = "producer." + HOSTNAME + ".unpublished." + GROUP + "." + TOPIC,
            CONSUMER_DELIVERED = "consumer." + HOSTNAME + ".delivered." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            CONSUMER_DISCARDED = "consumer." + HOSTNAME + ".discarded." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            CONSUMER_INFLIGHT = "consumer." + HOSTNAME + ".inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION,
            CONSUMER_OFFSET_LAG = "consumer.offset" + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + "." + PARTITION + ".lag",
            CONSUMER_OFFSET_COMMIT_IDLE = "consumer." + HOSTNAME + ".offset-commit-idle." + GROUP + "." + TOPIC + "." + SUBSCRIPTION
                    + "." + PARTITION,
            CONSUMER_EXECUTOR_RUNNING = "consumer." + HOSTNAME + ".executors." + EXECUTOR_NAME + ".running";
}
