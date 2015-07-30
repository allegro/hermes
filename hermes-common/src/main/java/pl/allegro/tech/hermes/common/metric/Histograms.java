package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.HOSTNAME;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Histograms {
    public static final String PRODUCER_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size." + GROUP + "." + TOPIC,
                               PRODUCER_GLOBAL_MESSAGE_SIZE = "producer." + HOSTNAME + ".message-size";
}
