package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.net.URI;
import java.util.Optional;

public class MessageSendingResultLogInfo {
  private final Optional<URI> url;
  private final String rootCause;
  private final Throwable failure;

  public MessageSendingResultLogInfo(Optional<URI> url, Throwable failure, String rootCause) {
    this.url = url;
    this.failure = failure;
    this.rootCause = rootCause;
  }

  public Optional<URI> getUrl() {
    return url;
  }

  public String getUrlString() {
    return url.isPresent() ? url.get().toString() : "";
  }

  public String getRootCause() {
    return rootCause;
  }

  public Throwable getFailure() {
    return failure;
  }
}
