package pl.allegro.tech.hermes.benchmark.consumer;

import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;

public class NoOpUndeliveredMessageLog implements UndeliveredMessageLog {
  @Override
  public void add(SentMessageTrace undeliveredMessage) {}

  @Override
  public void persist() {}
}
