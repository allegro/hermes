package pl.allegro.tech.hermes.frontend.producer.kafka;

public class ChaosException extends RuntimeException {

  public ChaosException(String datacenter, long delayMs, String messageId) {
    super(
        "Scheduled failure occurred for datacenter: "
            + datacenter
            + ", messageId: "
            + messageId
            + " after "
            + delayMs
            + "ms");
  }
}
