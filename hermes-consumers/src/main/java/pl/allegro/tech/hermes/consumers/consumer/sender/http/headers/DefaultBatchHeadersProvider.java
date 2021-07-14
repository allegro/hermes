package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;

import java.util.Collections;

public final class DefaultBatchHeadersProvider implements BatchHttpHeadersProvider {

    @Override
    public HttpRequestHeaders getHeaders(EndpointAddress address) {
        return new HttpRequestHeaders(Collections.emptyMap());
    }
}
