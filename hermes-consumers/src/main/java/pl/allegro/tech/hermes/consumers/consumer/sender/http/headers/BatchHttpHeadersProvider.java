package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import pl.allegro.tech.hermes.api.EndpointAddress;

public interface BatchHttpHeadersProvider {
    HttpRequestHeaders getHeaders(EndpointAddress address);
}
