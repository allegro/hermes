package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Histograms {
    public static final String MESSAGE_SIZE = "message-size." + GROUP + "." + TOPIC,
            GLOBAL_MESSAGE_SIZE = "message-size",
            INFLIGHT_TIME = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".time";
}
