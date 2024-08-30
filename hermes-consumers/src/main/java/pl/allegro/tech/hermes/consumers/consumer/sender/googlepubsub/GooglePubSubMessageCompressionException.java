package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

public class GooglePubSubMessageCompressionException extends RuntimeException {

  public GooglePubSubMessageCompressionException(String message, Throwable e) {
    super(message, e);
  }
}
