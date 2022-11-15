package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;

import java.net.URI;
import java.util.concurrent.TimeUnit;

class DefaultBatchHttpRequestFactory implements BatchHttpRequestFactory {
    private final HttpClient client;

    DefaultBatchHttpRequestFactory(HttpClient client) {
        this.client = client;
    }

    public Request buildRequest(MessageBatch message, URI uri, HttpRequestHeaders headers, int requestTimeout) {
        Request request = client.newRequest(uri)
                .method(HttpMethod.POST)
                .timeout(requestTimeout, TimeUnit.MILLISECONDS)
                .content(new ByteBufferContentProvider(message.getContent()));


        headers.asMap()
                .forEach(request::header);

        return request;
    }

}
