package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import java.util.Collection;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;

public interface HttpHeadersProvidersFactory {

  Collection<HttpHeadersProvider> createAll();
}
