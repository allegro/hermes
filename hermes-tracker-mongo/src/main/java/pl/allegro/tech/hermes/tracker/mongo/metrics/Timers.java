package pl.allegro.tech.hermes.tracker.mongo.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Timers {
    public static final String PRODUCER_TRACKER_COMMIT_LATENCY = "producer." + HOSTNAME + ".tracker-commit-latency",
            CONSUMER_TRACKER_COMMIT_LATENCY = "consumer." + HOSTNAME + ".tracker-commit-latency";

}
