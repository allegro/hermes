package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;

import java.util.Collection;

import static java.util.Collections.emptySet;

public class EmptyHttpHeadersProvidersFactory implements HttpHeadersProvidersFactory {

    @Override
    public Collection<HttpHeadersProvider> createAll() {
        return emptySet();
    }
}
