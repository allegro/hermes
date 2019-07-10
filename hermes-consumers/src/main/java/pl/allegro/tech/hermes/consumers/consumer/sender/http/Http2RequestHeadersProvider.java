package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.util.HashMap;
import java.util.Map;

final class Http2RequestHeadersProvider implements HttpRequestHeadersProvider {

    private final HttpRequestHeadersProvider baseRequestHeadersProvider;

    Http2RequestHeadersProvider(HttpRequestHeadersProvider baseRequestHeadersProvider) {
        this.baseRequestHeadersProvider = baseRequestHeadersProvider;
    }

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        Map<String, String> headers = new HashMap<>(baseRequestHeadersProvider.getHeaders(message).asMap());
        headers.remove(HttpHeader.KEEP_ALIVE.toString());

        return new HttpRequestHeaders(headers);
    }

}
