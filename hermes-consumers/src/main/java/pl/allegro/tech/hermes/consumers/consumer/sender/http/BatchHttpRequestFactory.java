package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.net.URI;
import org.eclipse.jetty.client.Request;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;

public interface BatchHttpRequestFactory {
  Request buildRequest(
      MessageBatch message, URI uri, HttpRequestHeaders headers, int requestTimeout);

  void stop();
}
