package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import static java.util.Collections.emptySet;

import java.util.Collection;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;

public class EmptyHttpHeadersProvidersFactory implements HttpHeadersProvidersFactory {

  @Override
  public Collection<HttpHeadersProvider> createAll() {
    return emptySet();
  }
}
