package pl.allegro.tech.hermes.consumers.consumer;

import java.time.Duration;

public interface SerialConsumerParameters {

  Duration getSignalProcessingInterval();

  int getInflightSize();
}
