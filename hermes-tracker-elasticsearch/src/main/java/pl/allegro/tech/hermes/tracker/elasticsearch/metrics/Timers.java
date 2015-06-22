package pl.allegro.tech.hermes.tracker.elasticsearch.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Timers {
    public static final String
            PRODUCER_TRACKER_ELASTICSEARCH_COMMIT_LATENCY = "producer." + HOSTNAME + ".tracker.elasticsearch.commit-latency",
            CONSUMER_TRACKER_ELASTICSEARCH_COMMIT_LATENCY = "consumer." + HOSTNAME + ".tracker.elasticsearch.commit-latency";

}
