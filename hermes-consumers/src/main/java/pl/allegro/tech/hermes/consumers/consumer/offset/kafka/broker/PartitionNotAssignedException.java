package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.broker;

import pl.allegro.tech.hermes.common.exception.RetransmissionException;

public class PartitionNotAssignedException extends RetransmissionException {
  public PartitionNotAssignedException() {
    super("");
  }

  public PartitionNotAssignedException(Throwable cause) {
    super(cause);
  }
}
