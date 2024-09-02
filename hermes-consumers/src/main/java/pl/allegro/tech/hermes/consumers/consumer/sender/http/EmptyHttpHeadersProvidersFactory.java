package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import static java.util.Collections.emptySet;

import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;

import java.util.Collection;

public class EmptyHttpHeadersProvidersFactory implements HttpHeadersProvidersFactory {

    @Override
    public Collection<HttpHeadersProvider> createAll() {
        return emptySet();
    }
}
