package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class DefaultHttpRequestFactory implements HttpRequestFactory {

    private final HttpClient client;
    private final long timeout;
    private final long socketTimeout;
    private final MetadataAppender<Request> metadataAppender;

    public DefaultHttpRequestFactory(HttpClient client,
                              long timeout,
                              long socketTimeout,
                              MetadataAppender<Request> metadataAppender) {
        this.client = client;
        this.timeout = timeout;
        this.socketTimeout = socketTimeout;
        this.metadataAppender = metadataAppender;
    }

    @Override
    public Request buildRequest(Message message, URI uri, HttpRequestHeaders headers) {
        return new HttpRequestBuilder(message, uri, headers).build();
    }

    private class HttpRequestBuilder {

        private final Message message;
        private final URI uri;
        private final HttpRequestHeaders headers;

        HttpRequestBuilder(Message message, URI uri, HttpRequestHeaders headers) {
            this.message = message;
            this.uri = uri;
            this.headers = headers;
        }

        Request build() {
            Request request = baseRequest();

            headers.asMap()
                    .forEach(request::header);

            return request;
        }

        private Request baseRequest() {
            Request baseRequest = client.newRequest(uri)
                    .method(HttpMethod.POST)
                    .timeout(timeout, TimeUnit.MILLISECONDS)
                    .idleTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                    .content(new BytesContentProvider(message.getData()));

            metadataAppender.append(baseRequest, message);

            return baseRequest;
        }

    }
}
