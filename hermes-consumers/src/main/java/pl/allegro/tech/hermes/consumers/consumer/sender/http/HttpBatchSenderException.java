package pl.allegro.tech.hermes.consumers.consumer.sender.http;

class HttpBatchSenderException extends RuntimeException {
  HttpBatchSenderException(String message, Throwable cause) {
    super(message, cause);
  }
}
