package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

final class HttpRequestHeaders {

    private final Map<String, String> headers;

    HttpRequestHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    Map<String, String> asMap() {
        return ImmutableMap.copyOf(headers);
    }

}
