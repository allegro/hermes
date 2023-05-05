package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Histograms {
    public static final String MESSAGE_SIZE = "message-size." + GROUP + "." + TOPIC;
    public static final String GLOBAL_MESSAGE_SIZE = "message-size";
    public static final String INFLIGHT_TIME = "inflight." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + ".time";
    public static final String PERSISTED_UNDELIVERED_MESSAGE_SIZE = "undelivered-messages.persisted.message-size";
}
