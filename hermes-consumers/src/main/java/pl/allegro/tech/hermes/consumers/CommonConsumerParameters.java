package pl.allegro.tech.hermes.consumers;

import pl.allegro.tech.hermes.consumers.consumer.SerialConsumerParameters;
import pl.allegro.tech.hermes.consumers.supervisor.SupervisorParameters;

public interface CommonConsumerParameters {

  SupervisorParameters getBackgroundSupervisor();

  SerialConsumerParameters getSerialConsumer();

  int getSignalProcessingQueueSize();

  boolean isUseTopicMessageSizeEnabled();
}
