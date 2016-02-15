package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.SUBSCRIPTION;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Gauges {

    public static final String EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES = "everyone-confirms-buffer-total-bytes",
            EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES = "everyone-confirms-buffer-available-bytes",
            EVERYONE_CONFIRMS_COMPRESSION_RATE = "everyone-confirms-compression-rate-avg",
            LEADER_CONFIRMS_BUFFER_TOTAL_BYTES = "leader-confirms-buffer-total-bytes",
            LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES = "leader-confirms-buffer-available-bytes",
            LEADER_CONFIRMS_COMPRESSION_RATE = "leader-confirms-compression-rate-avg",
            BATCH_BUFFER_TOTAL_BYTES = "batch-buffer-total-bytes",
            BATCH_BUFFER_AVAILABLE_BYTES = "batch-buffer-available-bytes",
            JMX_PREFIX = "jmx",

            THREADS = "threads",
            OUTPUT_RATE = "output-rate." + GROUP + "." + TOPIC + "." + SUBSCRIPTION;
}