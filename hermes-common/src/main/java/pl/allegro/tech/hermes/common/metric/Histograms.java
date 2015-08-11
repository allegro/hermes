package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.*;

public class Histograms {
    public static final String PRODUCER_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size." + GROUP + "." + TOPIC,
                               PRODUCER_GLOBAL_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size",
                               CONSUMER_INFLIGHT_TIME = "consumer." + HOSTNAME + ".inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".time";
}
