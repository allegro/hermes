package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.PathsCompiler.*;

public class Gauges {

    public static final String PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES = "producer." + HOSTNAME + ".everyone-confirms-buffer-total-bytes",
            PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES = "producer." + HOSTNAME + ".everyone-confirms-buffer-available-bytes",
            PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES = "producer." + HOSTNAME + ".leader-confirms-buffer-total-bytes",
            PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES = "producer." + HOSTNAME + ".leader-confirms-buffer-available-bytes",
            PRODUCER_TRACKER_QUEUE_SIZE = "producer." + HOSTNAME + ".tracker-queue-size",
            PRODUCER_TRACKER_REMAINING_CAPACITY = "producer." + HOSTNAME + ".tracker-remaining-capacity";

    public static final String CONSUMER_TRACKER_QUEUE_SIZE = "consumer." + HOSTNAME + ".tracker-queue-size",
            CONSUMER_TRACKER_REMAINING_CAPACITY = "consumer." + HOSTNAME + ".tracker-remaining-capacity",
            CONSUMER_THREADS = "consumer." + HOSTNAME + ".threads",
            CONSUMER_OUTPUT_RATE = "consumer." + HOSTNAME + ".output-rate." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
}