package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;

import java.net.URI;

public interface BatchHttpRequestFactory {
    Request buildRequest(MessageBatch message, URI uri, HttpRequestHeaders headers, int requestTimeout);

    void stop();
}
