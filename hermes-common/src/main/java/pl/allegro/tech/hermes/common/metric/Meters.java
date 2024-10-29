package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.metrics.PathsCompiler.GROUP;
import static pl.allegro.tech.hermes.metrics.PathsCompiler.TOPIC;

public class Meters {

  public static final String THROUGHPUT_BYTES = "throughput";
  public static final String TOPIC_THROUGHPUT_BYTES = THROUGHPUT_BYTES + "." + GROUP + "." + TOPIC;

  public static final String PERSISTED_UNDELIVERED_MESSAGES_METER =
      "undelivered-messages.persisted";
}
