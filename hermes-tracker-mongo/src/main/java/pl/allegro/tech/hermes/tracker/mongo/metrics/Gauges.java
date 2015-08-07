package pl.allegro.tech.hermes.tracker.mongo.metrics;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;

public class Gauges {
    public static final String
            PRODUCER_TRACKER_MONGO_QUEUE_SIZE = "producer." + HOSTNAME + ".tracker.mongo.queue-size",
            PRODUCER_TRACKER_MONGO_REMAINING_CAPACITY = "producer." + HOSTNAME + ".tracker.mongo.remaining-capacity",

            CONSUMER_TRACKER_MONGO_QUEUE_SIZE = "consumer." + HOSTNAME + ".tracker.mongo.queue-size",
            CONSUMER_TRACKER_MONGO_REMAINING_CAPACITY = "consumer." + HOSTNAME + ".tracker.mongo.remaining-capacity";
}
