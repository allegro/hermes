package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.net.URI;
import org.eclipse.jetty.client.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;

public interface HttpRequestFactory {
  Request buildRequest(Message message, URI uri, HttpRequestHeaders headers);
}
