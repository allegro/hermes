package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.ByteBufferRequestContent;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;

public class DefaultBatchHttpRequestFactory implements BatchHttpRequestFactory {
  private final HttpClient client;

  public DefaultBatchHttpRequestFactory(HttpClient client) {
    this.client = started(client);
  }

  public Request buildRequest(
      MessageBatch message, URI uri, HttpRequestHeaders headers, int requestTimeout) {
    return client
        .newRequest(uri)
        .method(HttpMethod.POST)
        .timeout(requestTimeout, TimeUnit.MILLISECONDS)
        .body(new ByteBufferRequestContent(message.getContent()))
        .headers(httpHeaders -> headers.asMap().forEach(httpHeaders::add));
  }

  private static HttpClient started(HttpClient httpClient) {
    try {
      httpClient.start();
      return httpClient;
    } catch (Exception e) {
      throw new InternalProcessingException("Failed to start http batch client", e);
    }
  }

  public void stop() {
    try {
      client.stop();
    } catch (Exception e) {
      throw new InternalProcessingException("Failed to stop http batch client", e);
    }
  }
}
