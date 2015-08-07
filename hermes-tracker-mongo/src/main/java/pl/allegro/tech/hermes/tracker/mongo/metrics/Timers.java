package pl.allegro.tech.hermes.tracker.mongo.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Timers {
    public static final String
            PRODUCER_TRACKER_MONGO_COMMIT_LATENCY = "producer." + HOSTNAME + ".tracker.mongo.commit-latency",
            CONSUMER_TRACKER_MONGO_COMMIT_LATENCY = "consumer." + HOSTNAME + ".tracker.mongo.commit-latency";

}
