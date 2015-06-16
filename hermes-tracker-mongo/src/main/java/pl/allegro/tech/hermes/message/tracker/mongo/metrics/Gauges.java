package pl.allegro.tech.hermes.message.tracker.mongo.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Gauges {
    public static final String PRODUCER_TRACKER_QUEUE_SIZE = "producer." + HOSTNAME + ".tracker-queue-size",
    PRODUCER_TRACKER_REMAINING_CAPACITY = "producer." + HOSTNAME + ".tracker-remaining-capacity",

    CONSUMER_TRACKER_QUEUE_SIZE = "consumer." + HOSTNAME + ".tracker-queue-size",
    CONSUMER_TRACKER_REMAINING_CAPACITY = "consumer." + HOSTNAME + ".tracker-remaining-capacity";
}
