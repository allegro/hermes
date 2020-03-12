package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;

import java.util.Collection;

public interface HttpHeadersProvidersFactory {

    Collection<HttpHeadersProvider> createAll();
}
