package pl.allegro.tech.hermes.common.message.undelivered;

import pl.allegro.tech.hermes.api.SentMessageTrace;

public interface UndeliveredMessageLog {

  void add(SentMessageTrace undeliveredMessage);

  void persist();
}
