package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.PARTITION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Histograms {
    public static final String CONSUMER_OFFSET_TIME_LAG = "consumer.offset" + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + "." + PARTITION + ".timeLag";
}