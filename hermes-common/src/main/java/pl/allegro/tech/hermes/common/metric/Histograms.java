package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.PARTITION;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.common.metric.PathsCompiler.TOPIC;

public class Histograms {
    public static final String CONSUMER_OFFSET_TIME_LAG = "consumer.offset" + "." + GROUP + "." + TOPIC + "." + SUBSCRIPTION + "." + PARTITION + ".timeLag";
}