package pl.allegro.tech.hermes.common.exception;

@SuppressWarnings("serial")
public class BrokerNotFoundForPartitionException extends InternalProcessingException {

  public BrokerNotFoundForPartitionException(String topic, int partition, Throwable cause) {
    super(String.format("Broker not found for topic %s and partition %d", topic, partition), cause);
  }
}
