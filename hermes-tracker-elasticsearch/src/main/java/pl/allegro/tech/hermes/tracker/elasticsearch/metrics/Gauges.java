package pl.allegro.tech.hermes.tracker.elasticsearch.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Gauges {
    public static final String PRODUCER_TRACKER_ELASTICSEARCH_QUEUE_SIZE =
            "producer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
    public static final String PRODUCER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
            "producer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";

    public static final String CONSUMER_TRACKER_ELASTICSEARCH_QUEUE_SIZE = "consumer." + HOSTNAME + ".tracker.elasticsearch.queue-size";
    public static final String CONSUMER_TRACKER_ELASTICSEARCH_REMAINING_CAPACITY =
            "consumer." + HOSTNAME + ".tracker.elasticsearch.remaining-capacity";
}
